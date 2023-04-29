package com.server;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.*;

public class UserAuthenticator extends BasicAuthenticator {

    private MessageDatabase db = null;

    public UserAuthenticator (String realm){
        super(realm);
        db = MessageDatabase.getInstance();
    }

    @Override
    public boolean checkCredentials(String username, String password){

        boolean isValidUser;
        try {
            System.out.println();
            isValidUser = db.authenticateUser(username, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return isValidUser;

    }

    public boolean addUser(String userName, String password, String email) throws JSONException, SQLException {
        // This function returns ture if user is not already registered.
        // Also sets new user to database.
        boolean result = db.setUser(new JSONObject().put("username", userName).put("password", password).put("email", email));
        if(!result){
            System.out.println("cannot register user");
            return false;
        }
       System.out.println(userName + " registered");

       return true;
        
    }
    
}
