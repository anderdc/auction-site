package client;
 
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;

import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import auction.AuctionObject;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Client extends Application {
	private Socket socket;
	private DataOutputStream toServer = null;   //input to server
	private DataInputStream fromServer = null; //data from server
	
	private byte [][] photos;
	private ArrayList<AuctionObject> auctionItems = new ArrayList<>();

	@Override
	public void start(Stage mainStage) throws Exception {
		mainStage.setTitle("Web3 Auction Site");
		StackPane root = new StackPane();
		root.setPadding(new Insets(20));
		
		//**Login screen**
		Pane login = new Pane();
		Pane usernameHbox = new HBox();
		Pane passwordHbox = new HBox();
		Pane buttons = new HBox();              //putting buttons horizontal
		Pane overall = new VBox();         //vertical box for adding buttons n such
		//error message
		Text errorMessage = new Text();
		errorMessage.setFont(Font.font("Verdana", 12));
		errorMessage.setFill(Color.RED);
		//username
		Text username = new Text("username: ");
		TextField usernameField = new TextField();
		usernameHbox.getChildren().addAll(username, usernameField);
		//password
		Text password = new Text("password: ");
		PasswordField passwordTextField = new PasswordField();
		passwordHbox.getChildren().addAll(password, passwordTextField);
		//buttons
		Button loginButton = new Button("login");
		loginButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				errorMessage.setText("");
				//connect to website
				try { 
					connectToServer();
					//send login key
					toServer.writeInt(1);
					toServer.flush();
					//send username
					toServer.writeUTF(usernameField.getText());
					toServer.flush();
					//send password
					toServer.writeUTF(getHash(passwordTextField.getText()));
					toServer.flush();
					
					//server sends 0 if did not go through with login
					int status = fromServer.readInt();
					if(status == 0) {
						errorMessage.setText("user does not exist.");
						disconnectFromServer();
						return;
					}
					
					mainStage.close();
					playSound("login.mp3");
					auctionSite(usernameField.getText());
				}
				catch(Exception e){ 
					errorMessage.setText("cannot connect to server try another time.");
				}
				

			}	
		});
		Button createButton = new Button("create account");
		createButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				errorMessage.setText("");
				
				//then go to auction site
				try { 
					connectToServer(); 
					//send create key
					toServer.writeInt(2);
					toServer.flush();
					//send username
					toServer.writeUTF(usernameField.getText());
					toServer.flush();
					//send password
					toServer.writeUTF(getHash(passwordTextField.getText()));
					toServer.flush();
					
					//server sends 0 if did not go through with create
					int status = fromServer.readInt();
					if(status == 0) {
						errorMessage.setText("user already exists.");
						disconnectFromServer();
						return;
					}
					
					mainStage.close();
					playSound("login.mp3");
					auctionSite(usernameField.getText());
				} catch(Exception e){ 
					errorMessage.setText("cannot connect to server try another time.");
				}					
			}
		});
		buttons.getChildren().addAll(loginButton, createButton);
			
		overall.getChildren().addAll(usernameHbox, passwordHbox, buttons, errorMessage);
		login.getChildren().add(overall);
		//**end of login screen**
		
		
		
		root.getChildren().add(login);
		Scene mainScene = new Scene(root, 300, 300);
		//set enter as a login hotkey
		mainScene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent a) {
				if(a.getCode() == KeyCode.ENTER) loginButton.fire();
			}
	    });
		
		
		mainStage.setScene(mainScene);
		mainStage.show();
	}
	
	
	
	/**
	 * method that executes the actual auction site
	 * @param name of user who accessed site
	 */
	public void auctionSite(String name) {
		Stage auctionStage = new Stage();
		auctionStage.setTitle("Opensea Java Edition");
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(10));

		Text greetText = new Text("Welcome "+ name +"!");
		greetText.setFont(Font.font("Verdana", 20));
		greetText.setFill(Color.DARKCYAN);
		root.setTop(greetText);
		
		Text errorMessage = new Text("");
		errorMessage.setFont(Font.font("Verdana", 13));
		errorMessage.setFill(Color.RED);
		
		//Scrollpane set in center of root
		ScrollPane s = new ScrollPane();
		s.setPannable(true);
		VBox itemsInScroll = new VBox(20); //initializes spacing to 20
		s.setContent(itemsInScroll);
		
		//**read in the items from server**
		String itemJson = "";  //string with all the items & delimiter "!!"
		photos = new byte[1][];
		try {
			//**getting items from server as json strings**
			itemJson = fromServer.readUTF();
			int numItems = fromServer.readInt();
			//**getting images as byte arrays**
			photos = new byte[numItems][];
			for(int i = 0; i < numItems; i++) {
				int byteLength = fromServer.readInt(); 		//get length of byte array
				byte b[] = new byte[byteLength];
				fromServer.readFully(b);
				photos[i] = b;
			}
		} catch (JsonSyntaxException | IOException e) {
			e.printStackTrace();
		}
		//**adding all items to the vbox within the scrollpane**
		String items[] = itemJson.split("!!");
		auctionItems.clear();
		for(String x: items) {
			auctionItems.add(new Gson().fromJson(x, AuctionObject.class));
		}
		
		for(int i = 0; i < items.length; i++) {
			BorderPane itemPane = new BorderPane();  //within vbox there is borderpane for item
			AuctionObject item = auctionItems.get(i);
			String itemInfoString = item.getName() + "    $" + String.format("%.2f",item.getPrice()) + "    highest bidder: " + item.getLastBidder();
			Label itemInfo = new Label(itemInfoString);
			itemInfo.setFont(Font.font("Verdana", 15));
			itemPane.setTop(itemInfo);  //within borderpane, on top there is hbox
			
			//**read in photo byte arrays & add to pane**
			Image x = new Image(new ByteArrayInputStream(photos[i]));
			ImageView itemPhoto = new ImageView(x);
			itemPhoto.setFitHeight(150);
			itemPhoto.setFitWidth(150);
			itemPane.setCenter(itemPhoto);
			
			HBox bidInfoPane = new HBox(5);
			//**add the bid buttons and other bidding things**
			Button bidButton = new Button("bid");
			bidButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					
					//if item is sold out
					if(item.getStatus().equals("Sold")){
						errorMessage.setText("item has been sold... sorry");
						return;
					}
					//otherwise run bid procedure
					try {
						errorMessage.setText("");
						toServer.writeUTF("1");
						toServer.flush();
						//**retrieve solana value**
						double solValue = 0;
						solValue = fromServer.readDouble();
						if(solValue == 0) solValue = 1; //if there was error with api it would return 0, so set to 1
						bidStage(item.getId(), item.getName(), item.getPrice(), solValue);
					} catch (IOException e) {
						errorMessage.setText("error occured on bid attempt");
					}
				}		
			});	
			
			
			Button logButton = new Button("bid log");
			logButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					bidLogStage(item);
				}		
			});	
			Label itemStatus = new Label(item.getStatus());
			itemStatus.setFont(Font.font("Verdana", 15));
			itemStatus.setTextFill(Color.VIOLET);
			bidInfoPane.getChildren().addAll(bidButton, logButton, itemStatus);
			itemPane.setBottom(bidInfoPane);
					
			itemsInScroll.getChildren().add(itemPane);
		}
		root.setCenter(s);

		//logout button
		Button logoutButton = new Button("quit");
		logoutButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				playSound("logout.mp3");
				auctionStage.close();
			}		
		});
		//refresh
		Button refreshButton = new Button("refresh");
		refreshButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent arg0) {
				try {
					toServer.writeUTF("2");
					toServer.flush();
					//**********read in and rewrite the items***********
					refreshHelper(itemsInScroll, errorMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		HBox otherStuffPane = new HBox(15);
		otherStuffPane.getChildren().addAll(logoutButton, refreshButton, errorMessage);
		root.setBottom(otherStuffPane);
		
					
		Scene scene = new Scene(root, 500, 700);
		auctionStage.setScene(scene);
		auctionStage.show();
	}
	
	/**
	 * bidding creates its own stage
	 */
	public void bidStage(int itemId, String itemName, double oldPrice, double solValue) {
		//**refresh all items**
		
		Stage bidStage = new Stage();
		//send the server some info that the bidder changed mind
		bidStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent arg0) {
				try {
					toServer.writeInt(0);  //send 0 saying there is no transaction
					toServer.flush(); 
				} catch (IOException e) {
					e.printStackTrace();
				}
				bidStage.close();
			}
		});
		bidStage.initModality(Modality.APPLICATION_MODAL); //set this to lock the other stage under bid 
		bidStage.setTitle("bid finalization");
		VBox root = new VBox(10);
		HBox trunk = new HBox(10);
		root.setPadding(new Insets(15));
		
		Label warning = new Label("you are bidding in dollars");
		warning.setTextFill(Color.RED);
		
		Label name = new Label(itemName);
		name.setFont(Font.font("Verdana", 13));

		Label price = new Label(((Double)oldPrice).toString() + " dollars");
		price.setFont(Font.font("Verdana", 13));
		//button for currency type
		ToggleButton toggleButton = new ToggleButton("$");
		toggleButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent arg0) {
				if(toggleButton.isSelected()) {
					toggleButton.setText("sol"); //if togglebutton is set, pay in sol
					price.setText( String.format("%.4f", (Double)(oldPrice/solValue)) + " sol");
					warning.setText("you are bidding in sol");
				}
				if(!toggleButton.isSelected()) {
					toggleButton.setText("$");
					price.setText(String.format("%.2f", (Double)oldPrice) + " dollars");
					warning.setText("you are bidding in dollars");
				}
			}
		});
				
		//**box for how much user wants to bid**
		HBox itemInfo = new HBox(10);
		Label bidAmount = new Label("enter bid amount: ");
		bidAmount.setFont(Font.font("Verdana", 13));
		itemInfo.getChildren().add(bidAmount);
		
		TextField textField = new TextField();
		itemInfo.getChildren().add(textField);
		
		//button for final clickage
		Button bidButton = new Button("finalize");
		bidButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent arg0) {
				warning.setText("");
				try {
					toServer.writeInt(1); //tell server for bid to go through
					toServer.flush();	
					
					double newPrice = Double.parseDouble(textField.getText());
					if(toggleButton.isSelected() && ((newPrice) <= (oldPrice/solValue))) { //if bidding in sol and bid too low
						warning.setText("cannot bid, price is too low");
						toServer.writeDouble(0);
						toServer.flush();
						return;
					}
					else if(!toggleButton.isSelected() && newPrice <= oldPrice) {     //if bidding in dollars and bid too low
						warning.setText("cannot bid, price is too low");
						toServer.writeDouble(0);
						toServer.flush();
						return;
					}
					else {
						//if everything is ok
						if(toggleButton.isSelected()) newPrice = newPrice*solValue;
						toServer.writeDouble(newPrice); //send new price in dollars ONLY
						toServer.flush();
						//send which item we're bidding on
						toServer.writeInt(itemId);
						toServer.flush();
					}
					bidStage.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch(NumberFormatException e) {
					warning.setText("Please enter a valid number");
					try {
						toServer.writeDouble(0);
						toServer.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				//bidStage.close();
			}
		});
		
		trunk.getChildren().addAll(name, toggleButton, price);
		root.getChildren().addAll(trunk, itemInfo, bidButton, warning);
		Scene scene = new Scene(root, 350, 180);
		bidStage.setScene(scene);
		bidStage.show();
	}
	
	/**
	 * method creates a stage to display bid log of a certain item
	 */
	public void bidLogStage(AuctionObject item) {
		Stage stage = new Stage();
		stage.setTitle(item.getName()+ " bid log");
		VBox bids = new VBox(15);
		ScrollPane root = new ScrollPane();
		root.setPannable(true);
		root.setContent(bids);
		root.setPadding(new Insets(15));
		String[] log = item.getbidLog();
		for(int i = 0; i < log.length; i++) {
			Text t = new Text(log[i]);
			t.setFont(Font.font("Verdana", 13));
			bids.getChildren().add(t);
		}
		
		Scene scene = new Scene(root, 400, 400);
		stage.setScene(scene);
		stage.show();
	}
	
	
	/**
	 * function helps with the refresh process of the items on auction site
	 */
	public void refreshHelper(VBox root, Text errorMessage) {
		String itemJson = "";  
		try {
			itemJson = fromServer.readUTF();	//string with all the items & delimiter "!!"
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//**adding all items to the vbox within the scrollpane**
		//has to be same amt of items as there are pictures
		String items[] = itemJson.split("!!");
		auctionItems.clear();
		for(int i = 0; i < items.length; i++) {
			auctionItems.add(new Gson().fromJson(items[i], AuctionObject.class));
		}
		
		for(int i = 0; i < items.length; i++) {
			AuctionObject item = auctionItems.get(i);
			BorderPane itemPane = (BorderPane) root.getChildren().get(i);
			
			String itemInfoString = item.getName() + "    $" + String.format("%.2f", item.getPrice()) + "    highest bidder: " + item.getLastBidder();
			Label itemInfo = new Label(itemInfoString);
			itemInfo.setFont(Font.font("Verdana", 15));
			itemPane.setTop(itemInfo);  
			
			HBox bottom = (HBox) itemPane.getBottom();
			bottom.getChildren().clear(); //remove the previous status
			
			Button bidButton = new Button("bid");
			bidButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					
					//if item is sold out
					if(item.getStatus().equals("Sold")){
						errorMessage.setText("item has been sold... sorry");
						return;
					}
					//otherwise run bid procedure
					try {
						errorMessage.setText("");
						toServer.writeUTF("1");
						toServer.flush();
						//**retrieve solana value**
						double solValue = 0;
						solValue = fromServer.readDouble();
						if(solValue == 0) solValue = 1; //if there was error with api it would return 0, so set to 1
						bidStage(item.getId(), item.getName(), item.getPrice(), solValue);
					} catch (IOException e) {
						errorMessage.setText("error occured on bid attempt");
					}
				}		
			});	
			
			Button logButton = new Button("bid log");
			logButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					bidLogStage(item);
				}		
			});
			
			Label itemStatus = new Label(item.getStatus());
			itemStatus.setFont(Font.font("Verdana", 15));
			itemStatus.setTextFill(Color.VIOLET);
			bottom.getChildren().addAll(bidButton, logButton, itemStatus);
		}
	}
	
	
	/**
	 * connects to the server 
	 */
	public void connectToServer() throws Exception {
		try {
			//@SuppressWarnings("resource")
			//Socket socket = new Socket("127.0.0.1", 8087);
			this.socket = new Socket("127.0.0.1", 8087);
			toServer = new DataOutputStream(socket.getOutputStream());
			fromServer = new DataInputStream(socket.getInputStream());
			
		} catch(Exception e) {
			throw new Exception();
		}
	}
	
	public void disconnectFromServer() {
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("could not disconnect...");
		}
	}
	
	
    /**
     * Returns a hexadecimal encoded SHA-256 hash for the input String.
     * @param data
     * @return string of hashed data
     */
    private String getHash(String data) {
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            return DatatypeConverter.printHexBinary(hash); // make it printable
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }
	
	public static void main(String args[]) {
		launch(args);
	}
	
	
	public static void playSound(String fileName) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				File soundFile = new File("src/resources/"+fileName);
				Media s = new Media(soundFile.toURI().toString());
				MediaPlayer mp = new MediaPlayer(s);
				mp.play();
			}
			
		}).start();
	}
	
}
