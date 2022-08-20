package server;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import auction.AuctionObject;


public class Server {
	Object itemLock = new Object();
	ArrayList<AuctionObject> auctionItems;
	private byte[][] photos;
	Object dbLock = new Object();
	private MySql database;
	
	private double solPrice; //value of solana in dollars
	private double solToDol; //conversion for turning solana into dollars
	
	
	public static void main(String args[]) {
		Server s = new Server();
		s.setUp();
	}
	
	//constructor
	public Server(){
		//**get all items from json text files**
		auctionItems = new ArrayList<>();
		String[] files = getFilesInDirectory(); //get array of all file paths
		//for each file
		for(String file: files) {
			//create an AuctionObject from json information & add to auctionItems array
			try {
				AuctionObject thing = new Gson().fromJson(readFile(file), AuctionObject.class);
				auctionItems.add(thing);
			} catch (JsonSyntaxException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}	
		
		//**get all photos in resources as byte arrays** 
		photos = readPhotos();
		
		//**start a thread that periodically updates solana's value... every 1.5 minutes**
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					solPrice = PriceScraper.getSolanaPrice();
					System.out.println("updated solana price: "+ solPrice);
					if(solPrice == 0){
						System.out.println("could not retrieve solana value");
						solPrice = 1;
					}
					try {
						Thread.sleep(90000);  //1.5 min
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		
		//**initialize user database**
		database = new MySql();
	}
	/**
	 * 	Task that takes care of the clients as they connect to server
	 */
	class ClientHandler implements Runnable{
		//private Socket clientSocket;
		private DataInputStream input;
		private DataOutputStream output; 
		private String user;      //name of whoever logged in

		public ClientHandler(Socket socket) throws IOException{
			//this.clientSocket = socket;
			this.input = new DataInputStream(socket.getInputStream());
			this.output = new DataOutputStream(socket.getOutputStream());
		}
		@Override
		public void run() {
			//when a client connects, read the username/password, send all json items, send all pictures
			String password = "";
			try {
				int key = input.readInt(); //if 1, user attempted login, if 2 user attempted a create
				
				user = input.readUTF(); 
				password = input.readUTF();
				
				//**database stuff**
				//if user tried to login
				synchronized(dbLock) {
					if(key == 1) {
						//if we couldn't find user
						if(!database.findUser(user, password)) {
							output.writeInt(0);
							output.flush();
							return;
						}
						else {
							output.writeInt(1);
							output.flush();
						}
					}
					else if(key == 2) {
						//if a user cannot be added, send 0
						if(!database.addUser(user, password)){
							output.writeInt(0);
							output.flush();
							return;
						}
						else {
							output.writeInt(1);
							output.flush();
						}
					}
				}
				//**end of database stuff**
				
				//**send all objects as json for client to parse**
				refreshClient();
				//**send all pictures**
				output.writeInt(photos.length); //send how many photos there are
				output.flush();
				for(int i = 0; i < photos.length; i++) {
					//send each byte array individually
					output.writeInt(photos[i].length);       //send size of photo byte array
					output.flush();
					output.write(photos[i], 0, photos[i].length);
					output.flush();
				}		
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(user+" connected.");
			
			//**infinite loop until they disconnect**
			while(true) {
				try {
					String fromClient = input.readUTF();
					switch(fromClient) {
					case "1": //if a user wants to bid						
						System.out.println(user+ " wants to bid");
						output.writeDouble(solPrice);
						output.flush();
						while(true) {
							int check = input.readInt(); //will be 0 if user exited from bid stage
							if(check == 0) break;

							double newPrice = input.readDouble();
							if(newPrice == 0) continue; //price was invalid for a bid

							int itemId = input.readInt(); //get id for item being bid on
							synchronized(itemLock) {
								updateItems(newPrice, itemId);
							}
							break;
						}
						break;
					case "2": //if user presses 'refresh' button
						refreshClient();
						break;
					default:
						System.out.println("unknown user input");
						break;
					}
				} catch(IOException e){
					System.out.println(user+" disconnected.");
					break;
				} 
				
			}
			//**end of while loop**
		}
		/**
		 * if client wants to refresh their items this will resend all items, as json
		 * @return true if client auction items were sent, false otherwise
		 */
		private boolean refreshClient(){
			try {
				output.writeUTF(allItemsAsJson());
				output.flush();
				return true;
			} catch(Exception e) {
				System.out.println("error refreshing " +user);
				return false;
			}
		}
		/*
		 * updates the auctionItems array w a new price and item id 
		 */
		@SuppressWarnings("resource")
		private void updateItems(double newPrice, int id) {
			for(AuctionObject x: auctionItems) {
				//find the item
				if(x.getId() == id) {
					//if a new bid was able to be placed
					if(x.newBid(newPrice, user)) return; //if bid went thru
					break;
				}
			}
		}
	}
	
	/*
	 * responsible for set up of client threads 
	 */
	private void setUp() {
		int port = 8087;
		try {
			System.out.println("booting up server...");
			ServerSocket socket = new ServerSocket(port); //ServerSocket is for starting a server
			System.out.println("booted.");
			while(true) {
				Socket clientSocket = socket.accept();     //Socket is for connecting to a server
				System.out.println("connecting to " + clientSocket);
				Thread clientThread = new Thread(new ClientHandler(clientSocket));
				clientThread.start();
				//******neds more stuff?******				
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * reads a single txt file which has the JSON information of an AuctionObject
	 * @returns String json of object
	 * @throws FileNotFoundException 
	 */
	private String readFile(String fileName) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(fileName));
		String json = "";
		while(sc.hasNext()) {
			json += sc.nextLine();
		}
		return json;
	}
	/**
	 * searches the 'initial_items' directory to get all the file names... WITH DIRECTORY PATH PREPENDED
	 * files are in the src directory
	 * @return
	 */
	private String[] getFilesInDirectory() {
		File[] allFiles = new File("src/initial_items/").listFiles();  //get all files in directory
		int numFiles = allFiles.length; //get number of files directory
		String[] fileNames = new String[numFiles];                           //new array to store all names
		for(int i = 0; i < numFiles; i++) {
			fileNames[i] = "src/initial_items/" + allFiles[i].getName();
		}
		return fileNames;
	}
	
	/**
	 * @return a string with all the auction items as a json, each item seperated by a "!!" delimiter
	 */
	private String allItemsAsJson() {
		String items = "";
		for(int i = 0; i < auctionItems.size(); i++) {
			items += auctionItems.get(i).toString();
			items += "!!";
		}
		return items;
	}
	
	/**
	 * 
	 * @return all the byte arrays of the photos in resources directory
	 */
	private byte[][] readPhotos() {
		File[] photoFiles = new File("src/resources/").listFiles();
		
		byte[][] photosInBytes = new byte[photoFiles.length][];
		
		//for all the photos in resources/
		//put their byte arrays in the photos matrix
		for(int i = 0; i < photoFiles.length; i++) {
			byte[] b = new byte[(int) photoFiles[i].length()];  //byte array for single photo
			DataInputStream fis;
			try {
				fis = new DataInputStream(new FileInputStream(photoFiles[i]));    
				fis.readFully(b);    //reads file to byte array
				fis.close();
			} catch (IOException e) {
				System.out.println("error parsing photo");
				e.printStackTrace();
			}  

			photosInBytes[i] = b;
		}
		return photosInBytes;
	}
}

