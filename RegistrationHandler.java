package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.*;

public class RegistrationHandler implements HttpHandler {
    private UserAuthenticator userAuthenticator;

    RegistrationHandler(UserAuthenticator userAuthenticator){
        this.userAuthenticator = userAuthenticator;

    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        try {
            if (t.getRequestMethod().equalsIgnoreCase("POST")) {
                // Handle POST requests here (users send this for sending messages)
                String registrationSituation = handlePostRequest(t);
                handlePostResponse(t, registrationSituation);
            } else {
                // Inform user here that only POST and GET functions are supported and send an error code
                // 400 with a message “Not supported” (witouth the “)
                handleResponse(t, "Not supported", 400);
            }
            
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            handleResponse(t, "Internal server error", 500);
        }
        
    }

    private void handlePostResponse(HttpExchange httpExchange, String registratioSituation) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        String responseString;

        if(registratioSituation.equals("OK")){
            responseString = "User account is successfully created.";
            httpExchange.sendResponseHeaders(200, responseString.length());
        }
        else if(registratioSituation.equals("isAlreadyRegistered")){
            responseString = "User already registered";
            httpExchange.sendResponseHeaders(403, responseString.length());
        }
        else if(registratioSituation.equals("IncorrectData")){
            responseString = "Infalid data. Correct form is 'username:password'";
            httpExchange.sendResponseHeaders(400, responseString.length());
        }
        else{
            responseString = "Not supported";
            httpExchange.sendResponseHeaders(400, responseString.length());
        }
    
        outputStream.write(responseString.getBytes());

        outputStream.flush();

        outputStream.close();
    }

    private String handlePostRequest(HttpExchange httpExchange) throws IOException, JSONException, SQLException{
        JSONObject obj = null;
        if(httpExchange.getRequestHeaders().get("Content-Type").get(0).equals("application/json")){

            InputStream stream = httpExchange.getRequestBody();
            String newUser = new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            stream.close();

            if (newUser == null || newUser.length() == 0) {
                return "IncorrectData";
            } else {
                try{
                    obj = new JSONObject(newUser);
                }catch(JSONException e){
                    System.out.println("json parse error, faulty user json");
                    return "IncorrectData";
                }
                if(obj.getString("username").equals("") || obj.getString("password").equals("") || obj.getString("email").equals("") ){
                    return "IncorrectData";
                }
                Boolean NotAlreadyRegistered = userAuthenticator.addUser(obj.getString("username"), obj.getString("password"), obj.getString("email"));
                if(NotAlreadyRegistered == true){
                    return "OK";
                }
                else{
                    return "isAlreadyRegistered";
                }
                
            }
        }
        return "IncorrectData";
        
    }


    private void handleResponse(HttpExchange httpExchange, String responseString, int responseCode) throws IOException{
        OutputStream outputStream = httpExchange.getResponseBody();
        String responseStr = responseString;

        httpExchange.sendResponseHeaders(responseCode, responseStr.length());

        outputStream.write(responseStr.getBytes());

        outputStream.flush();

        outputStream.close();
    }

    
}
