package server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//will create a file called serverDatabase.db somewhere on your system
public class MySql {
	//used to debug
	public static void main(String[] args) {
		MySql db = new MySql();		
		//db.addUser("test", "9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08");
		System.out.println("checking if random user exists (should not):");
		System.out.println(db.findUser("test", "3"));
	}
	
	
	public MySql() {
		createDBFile();
		
	}
	
	/**
	 * creates a db file with empty table on system called 'serverDatabase.db'
	 * but only if it doesnt already exist
	 */
	private static void createDBFile() {
		try {
			//Connection con = DriverManager.getConnection("jdbc:sqlite::memory:");

			Connection con = DriverManager.getConnection("jdbc:sqlite:serverDatabase.db");

			Statement s = con.createStatement();  //create statement from connection

			s.execute("create table if not exists server_users " +
					"(user text, hash text)");

			File tmpFile = File.createTempFile("serverDatabase", ".db");
			s.executeUpdate("backup to " + tmpFile.getAbsolutePath());
			s.close();
			con.close();
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * adds test user to a new database and creates a db file if not created
	 */
	private static void addTestUser() {
		try {
			//Connection con = DriverManager.getConnection("jdbc:sqlite::memory:");

			Connection con = DriverManager.getConnection("jdbc:sqlite:file:serverDatabase.db");

			Statement s = con.createStatement();  //create statement from connection
			
			s.execute("create table if not exists server_users " +
					"(user text, hash text)");

			//put test user in database w name and password 'test'
			//SHA256 hash for 'test' is '9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08'
			s.executeUpdate("insert into server_users values ('test', '9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08')");

			ResultSet answer = s.executeQuery("select * from server_users");

			//getting username and password by getting its value from the appropriate table column
			String user =answer.getString("user");
			String hash = answer.getString("hash");

			File tmpFile = File.createTempFile("serverDatabase", ".db");
			s.executeUpdate("backup to " + tmpFile.getAbsolutePath());
			s.close();
			con.close();
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * test function to check all users in db
	 */
	private static void printAllUsers() {
		try {
			//Connection con = DriverManager.getConnection("jdbc:sqlite::memory:");
			Connection con = DriverManager.getConnection("jdbc:sqlite:serverDatabase.db");

			Statement s = con.createStatement();  //create statement from connection
			
			ResultSet answer = s.executeQuery("select * from server_users");
			while(answer.next()) {
				System.out.println("user: " + answer.getString("user") + " hash: " + answer.getString("hash"));
			}
			
			//System.out.println("result: " + user + " "+hash);
			s.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * for creating account
	 * adds user if they do not exist in system
	 * @return true if successful appendage to db
	 * false otherwise
	 */
	public boolean addUser(String user, String password) {
		try {
			//if user does not exist
			if(!findUser(user, password)) {
				Connection c = DriverManager.getConnection("jdbc:sqlite:serverDatabase.db");
				Statement s = c.createStatement();
				s.executeUpdate("insert into server_users values ('"+user+ "', '" + password +"')");
				s.close();
				c.close();
				return true;
			}
			else return false;
			
		} catch(SQLException e) {
			System.out.println("error trying to add user...");
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * for logging in
	 * checks and returns true if a user exists 
	 * @param user name
	 * @param password encrypted password
	 */
	public boolean findUser(String user, String password) {
		try {
			Connection c = DriverManager.getConnection("jdbc:sqlite:serverDatabase.db");
			Statement s = c.createStatement();
			ResultSet response = s.executeQuery("select * from server_users where user = '" + user + "'");
			
			if(response.next() == false) {
				s.close();
				c.close();
				return false; //if there were no users found with username 
			}
			 
			if(response.getString("hash").equals(password)) {
				s.close();
				c.close();
				return true; //if user and password match to someone in system
			}
			
			else {
				s.close();
				c.close();
				return false;
			}
			
		} catch(SQLException e) {
			System.out.println("error trying to find user...");
		}
		
		return false;
	}
	
}
