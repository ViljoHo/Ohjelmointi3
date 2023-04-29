package com.server;

import java.io.File;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;

public class MessageDatabase {

    private SecureRandom randomNumberGenerator = new SecureRandom();
    private Connection dbConnection = null;
    private static MessageDatabase dbInstance = null;

	public static synchronized MessageDatabase getInstance() {
		if (null == dbInstance) {
			dbInstance = new MessageDatabase();
		}
        return dbInstance;
    }

    private MessageDatabase(){

        try {
            open("MyDatabase");
        } catch (SQLException e) {
            System.out.println("Log - SQLexception");
        }

    }

    private boolean init() throws SQLException{

        String dbName = "MyDatabase";

        String database = "jdbc:sqlite:" + dbName;
        dbConnection = DriverManager.getConnection(database);

        if (null != dbConnection) {

            String createTableForUsers = "create table users (username varchar(50) NOT NULL, password varchar(50) NOT NULL, email varchar(50) NOT NULL, primary key(username))";
            String createTableForMessages = "create table messages (nickname varchar(50) NOT NULL, dangertype varchar(50) NOT NULL, latitude real NOT NULL, longitude real NOT NULL, sent integer NOT NULL, areacode varchar(50), phonenumber varchar(50), weather varchar(50))";
            Statement createStatement = dbConnection.createStatement();
            createStatement.executeUpdate(createTableForUsers);
            createStatement.executeUpdate(createTableForMessages);
            createStatement.close();
            System.out.println("DB successfully created");

            return true;
        }

        System.out.println("DB creation failed");
        return false;

    }

    public void open(String dbName) throws SQLException {
        File dbFile = new File("./" + dbName);

        Boolean exists = dbFile.exists();

        if (exists){
            String database = "jdbc:sqlite:" + dbName;
            dbConnection = DriverManager.getConnection(database);
            System.out.println("Connection opened again");
        } else {
            init();
        }
    }

    public void closeDB() throws SQLException {
		if (null != dbConnection) {
			dbConnection.close();
            System.out.println("closing db connection");
			dbConnection = null;
		}
    }

    public boolean setUser(JSONObject user) throws SQLException {

        if(checkIfUserExists(user.getString("username"))){
            return false;
        }

        byte bytes[] = new byte[13];
        randomNumberGenerator.nextBytes(bytes);

        String saltedBytes = new String(Base64.getEncoder().encode(bytes));
        
        String salt = "$6$" + saltedBytes;

        String userPasswordHashed = Crypt.crypt(user.getString("password"), salt);

		String setUserString = "insert into users " +
					"VALUES('" + user.getString("username") + "','" + userPasswordHashed + "','" + user.getString("email") + "')"; 
		Statement createStatement;
		createStatement = dbConnection.createStatement();
		createStatement.executeUpdate(setUserString);
		createStatement.close();

        return true;
    }

    public boolean checkIfUserExists(String givenUserName) throws SQLException{

        Statement queryStatement = null;
        ResultSet rs;

        String checkUser = "select username from users where username = '" + givenUserName + "'";
        System.out.println("checking user");

        
        queryStatement = dbConnection.createStatement();
		rs = queryStatement.executeQuery(checkUser);
        
        if(rs.next()){
            System.out.println("user exists");
            return true;
        }else{
            return false;
        }
    }

    public void setMessage(WarningMessage message) throws SQLException {

		String setMessageString = "insert into messages " +
					"VALUES('" + message.getNick() + "','" 
                    + message.getDangertype() + "','" 
                    + message.getLatitude() + "','" 
                    + message.getLongitude() + "','"
                    + message.dateAsInt() + "','"
                    + message.getAreacode() + "','"
                    + message.getPhonenumber() + "','"
                    + message.getWeather() + "')"; 

        System.out.println(setMessageString);
		Statement createStatement;
		createStatement = dbConnection.createStatement();
		createStatement.executeUpdate(setMessageString);
		createStatement.close();
    }
    
    public JSONArray getMessages() throws SQLException {

        Statement queryStatement = null;
        JSONArray allMessagesArr = new JSONArray();
        

        String getMessagesString = "select * from messages";

        queryStatement = dbConnection.createStatement();
		ResultSet rs = queryStatement.executeQuery(getMessagesString);

        while (rs.next()) {
            WarningMessage message = new WarningMessage(rs.getString("nickname"), 
            rs.getDouble("latitude"), 
            rs.getDouble("longitude"), 
            rs.getString("dangertype"), 
            null, 
            rs.getString("areacode"), 
            rs.getString("phonenumber"),
            rs.getString("weather"));

            message.setSent(rs.getLong("sent"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String sentAsString = message.getSent().format(formatter);

            JSONObject singleMessage = new JSONObject();
            
            if (message.getAreacode().equals("null") && message.getPhonenumber().equals("null") ) {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString);
                allMessagesArr.put(singleMessage);
            }else if(message.getWeather().equals("null")) {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString)
                .put("areacode", message.getAreacode())
                .put("phonenumber", message.getPhonenumber());
                allMessagesArr.put(singleMessage);
            }else {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString)
                .put("areacode", message.getAreacode())
                .put("phonenumber", message.getPhonenumber())
                .put("weather", message.getWeather());
                allMessagesArr.put(singleMessage);
                
            }
		}

        return allMessagesArr;

    }

    public boolean authenticateUser(String givenUserName, String givenPassword) throws SQLException {

        Statement queryStatement = null;
        ResultSet rs;

        String getMessagesString = "select username, password from users where username = '" + givenUserName + "'";
        System.out.println(givenUserName);


        queryStatement = dbConnection.createStatement();
		rs = queryStatement.executeQuery(getMessagesString);

        if(rs.next() == false){

            System.out.println("cannot find such user");
            return false;

        }else{

            String hashedPassword = rs.getString("password");

            if(hashedPassword.equals(Crypt.crypt(givenPassword, hashedPassword))){

                return true;

            }else{

                return false;
            }


        }

    }
    
    public JSONArray getQueryUserMessages(String nickname) throws SQLException {

        Statement queryStatement = null;
        JSONArray allMessagesArr = new JSONArray();
        

        String getMessagesString = "select * from messages where nickname = '" + nickname + "'";

        queryStatement = dbConnection.createStatement();
		ResultSet rs = queryStatement.executeQuery(getMessagesString);

        while (rs.next()) {
            WarningMessage message = new WarningMessage(rs.getString("nickname"), 
            rs.getDouble("latitude"), 
            rs.getDouble("longitude"), 
            rs.getString("dangertype"), 
            null, 
            rs.getString("areacode"), 
            rs.getString("phonenumber"),
            rs.getString("weather"));

            message.setSent(rs.getLong("sent"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String sentAsString = message.getSent().format(formatter);

            JSONObject singleMessage = new JSONObject();
            
            if (message.getAreacode().equals("null") && message.getPhonenumber().equals("null") ) {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString);
                allMessagesArr.put(singleMessage);
            } else {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString)
                .put("areacode", message.getAreacode())
                .put("phonenumber", message.getPhonenumber())
                .put("weather", message.getWeather());
                allMessagesArr.put(singleMessage);
                
            }
		}

        return allMessagesArr;

    }

    public JSONArray getQueryTimeMessages(Long timestart, Long timeend) throws SQLException {

        Statement queryStatement = null;
        JSONArray allMessagesArr = new JSONArray();
        

        String getMessagesString = "SELECT * FROM messages WHERE sent >= " + timestart + " AND sent <= "+ timeend;

        queryStatement = dbConnection.createStatement();
		ResultSet rs = queryStatement.executeQuery(getMessagesString);

        while (rs.next()) {
            WarningMessage message = new WarningMessage(rs.getString("nickname"), 
            rs.getDouble("latitude"), 
            rs.getDouble("longitude"), 
            rs.getString("dangertype"), 
            null, 
            rs.getString("areacode"), 
            rs.getString("phonenumber"),
            rs.getString("weather"));

            message.setSent(rs.getLong("sent"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String sentAsString = message.getSent().format(formatter);

            JSONObject singleMessage = new JSONObject();
            
            if (message.getAreacode().equals("null") && message.getPhonenumber().equals("null") ) {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString);
                allMessagesArr.put(singleMessage);
            } else {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString)
                .put("areacode", message.getAreacode())
                .put("phonenumber", message.getPhonenumber())
                .put("weather", message.getWeather());
                allMessagesArr.put(singleMessage);
                
            }
		}

        return allMessagesArr;

    }

    public JSONArray getQueryLocationMessages(String uplongitude, String uplatitude, String downlongitude, String downlatitude) throws SQLException {

        Statement queryStatement = null;
        JSONArray allMessagesArr = new JSONArray();
        

        String getMessagesString = "SELECT * FROM messages WHERE longitude <= " + downlongitude + " AND longitude >= "+ uplongitude + " AND latitude >= " + downlatitude + " AND latitude <= " + uplatitude;

        queryStatement = dbConnection.createStatement();
		ResultSet rs = queryStatement.executeQuery(getMessagesString);

        while (rs.next()) {
            WarningMessage message = new WarningMessage(rs.getString("nickname"), 
            rs.getDouble("latitude"), 
            rs.getDouble("longitude"), 
            rs.getString("dangertype"), 
            null, 
            rs.getString("areacode"), 
            rs.getString("phonenumber"),
            rs.getString("weather"));

            message.setSent(rs.getLong("sent"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String sentAsString = message.getSent().format(formatter);

            JSONObject singleMessage = new JSONObject();
            
            if (message.getAreacode().equals("null") && message.getPhonenumber().equals("null") ) {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString);
                allMessagesArr.put(singleMessage);
            } else {
                singleMessage.put("nickname", message.getNick())
                .put("longitude", message.getLongitude())
                .put("latitude", message.getLatitude())
                .put("dangertype", message.getDangertype())
                .put("sent", sentAsString)
                .put("areacode", message.getAreacode())
                .put("phonenumber", message.getPhonenumber())
                .put("weather", message.getWeather());
                allMessagesArr.put(singleMessage);
                
            }
		}

        return allMessagesArr;

    }

}

