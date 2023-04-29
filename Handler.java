package com.server;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.net.httpserver.*;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class Handler implements HttpHandler {

    private MessageDatabase db = MessageDatabase.getInstance();

    //There is stored possible query parameters for reuse.
    private String[] queryParams = new String[5];

    @Override
    public void handle(HttpExchange t) throws IOException {

        if (t.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handling POST requests here (users use this to get messages)


            //Handler works like a state machine. HandlePostRequest returns a string depending on what happened when handling the request.
            //That string is (RequestSituation) is given to handlePostResponse method as parameter.
            String requestSituation = " ";

            try {
                requestSituation = handlePostRequest(t);
                System.out.println(requestSituation);
            } catch (NumberFormatException | JSONException | SQLException | DateTimeParseException | UnirestException e) {
                e.printStackTrace();
            }

            //Checking if request was type of query.
            if (requestSituation.equals("query")){
                if(queryParams[0].equals("user")) {
                    try {
                        handleQueryUserResponse(t, queryParams[1]);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
                else if(queryParams[0].equals("time")){
                    try {
                        handleQueryTimeResponse(t, queryParams[1], queryParams[2]);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
                else if(queryParams[0].equals("location")) {
                    try {
                        handleQueryLocationResponse(t, queryParams[1], queryParams[2], queryParams[3], queryParams[4]);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
            }
            else {
                handlePostResponse(t, requestSituation);
            }
        } else if (t.getRequestMethod().equalsIgnoreCase("GET")) {
            // Handling GET requests here (users use this to get messages)
            try {
                handleGetResponse(t);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            handleResponse(t);
        }

    }

    private String handlePostRequest(HttpExchange httpExchange) throws IOException, NumberFormatException, JSONException, SQLException, DateTimeParseException, UnirestException {
        JSONObject messageJSONobj = null;
        if(httpExchange.getRequestHeaders().get("Content-Type").get(0).equals("application/json")){

            InputStream stream = httpExchange.getRequestBody();
            String newMessage = new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            stream.close();
            System.out.println(newMessage);

            if (newMessage == null || newMessage.length() == 0) {
                return "IncorrectData";
            } else {
                try{
                    messageJSONobj = new JSONObject(newMessage);

                    //Checking is post request query or normal post request
                    if (messageJSONobj.has("query")){
                        
                        if (messageJSONobj.getString("query").equals("user")) {
                            queryParams[0] = messageJSONobj.getString("query");
                            queryParams[1] = messageJSONobj.getString("nickname");
                            
                        } else if (messageJSONobj.getString("query").equals("time")) {
                            queryParams[0] = messageJSONobj.getString("query");
                            queryParams[1] = messageJSONobj.getString("timestart");
                            queryParams[2] = messageJSONobj.getString("timeend");
                            
                        } else if (messageJSONobj.getString("query").equals("location")) {
                            queryParams[0] = messageJSONobj.getString("query");
                            queryParams[1] = Double.toString(messageJSONobj.getDouble("uplongitude"));
                            queryParams[2] = Double.toString(messageJSONobj.getDouble("uplatitude"));
                            queryParams[3] = Double.toString(messageJSONobj.getDouble("downlongitude"));
                            queryParams[4] = Double.toString(messageJSONobj.getDouble("downlatitude"));

                        }

                        return "query";

                    }

                    //continue handling normal message POST request
                    System.out.println(messageJSONobj.getString("sent"));
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    LocalDateTime sent = LocalDateTime.parse(messageJSONobj.getString("sent"), formatter);
                    
                    //checking allowed dangertypes
                    if(!(messageJSONobj.getString("dangertype").equals("Reindeer") || messageJSONobj.getString("dangertype").equals("Moose") || messageJSONobj.getString("dangertype").equals("Deer") || messageJSONobj.getString("dangertype").equals("Other") )) {
                        return "IncorrectData";
                    }
 
                    if(messageJSONobj.has("areacode") && messageJSONobj.has("phonenumber") && messageJSONobj.has("weather")) {
                        WarningMessage newWarningMessage = new WarningMessage(messageJSONobj.getString("nickname"), 
                        messageJSONobj.getDouble("latitude"), 
                        messageJSONobj.getDouble("longitude"), 
                        messageJSONobj.getString("dangertype"), 
                        sent, 
                        messageJSONobj.getString("areacode"), 
                        messageJSONobj.getString("phonenumber"),
                        getWeather(messageJSONobj.getDouble("latitude"), messageJSONobj.getDouble("longitude") ));
                        db.setMessage(newWarningMessage);
                    }
                    else if(messageJSONobj.has("areacode") && messageJSONobj.has("phonenumber")) {
                        WarningMessage newWarningMessage = new WarningMessage(messageJSONobj.getString("nickname"), 
                        messageJSONobj.getDouble("latitude"), 
                        messageJSONobj.getDouble("longitude"), 
                        messageJSONobj.getString("dangertype"), 
                        sent, 
                        messageJSONobj.getString("areacode"), 
                        messageJSONobj.getString("phonenumber"),
                        null);
                        db.setMessage(newWarningMessage);

                    }
                    else {
                        WarningMessage newWarningMessage = new WarningMessage(messageJSONobj.getString("nickname"), 
                        messageJSONobj.getDouble("latitude"), 
                        messageJSONobj.getDouble("longitude"), 
                        messageJSONobj.getString("dangertype"), 
                        sent, 
                        null, 
                        null,
                        null);
                        db.setMessage(newWarningMessage);
                    }
                    
                    return "OK";
                }catch(JSONException e){
                    System.out.println("json parse error, faulty message json");
                    return "IncorrectData";
                }
                catch (DateTimeParseException e) {
                    System.out.println("json parse error, faulty message json");
                    return "IncorrectData";
                }
            }
        }
        return "IncorrectData";
    }

    private void handlePostResponse(HttpExchange httpExchange, String requestSituation) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        String responseString;

        if(requestSituation.equals("OK")){
            responseString = "Message received successfully";
            httpExchange.sendResponseHeaders(200, responseString.length());
        }
        else if(requestSituation.equals("IncorrectData")){
            responseString = "Infalid data.";
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

    private void handleGetResponse(HttpExchange httpExchange) throws IOException, SQLException {
        
            OutputStream outputStream = httpExchange.getResponseBody();
            JSONArray responseMessages = new JSONArray();

            responseMessages = db.getMessages();

            String responseString = responseMessages.toString();
    
            byte [] bytes = responseString.getBytes("UTF-8");
    
            httpExchange.sendResponseHeaders(200, bytes.length);
    
            outputStream.write(responseString.getBytes());
    
            outputStream.flush();
    
            outputStream.close(); 

    }

    private void handleResponse(HttpExchange httpExchange) throws IOException{
        OutputStream outputStream = httpExchange.getResponseBody();
        String responseString = "Not supported";

        httpExchange.sendResponseHeaders(400, responseString.length());

        outputStream.write(responseString.getBytes());

        outputStream.flush();

        outputStream.close();
    }

     
    private String getWeather(double latitude, double longitude) throws IOException, UnirestException {

        //Request for weather server with using unirest library
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("http://localhost:4001/weather")
        .header("Content-Type", "application/xml")
        .body("<coordinates>\r\n    <latitude>" + latitude + "</latitude>\r\n    <longitude>" + longitude + "</longitude>\r\n</coordinates>")
        .asString();

        //Splitting only temperature from body.
        String responseAsString = response.getBody();
        String[] responseAsStringArr = responseAsString.split(">");
        String[] temperatureAsStringArr = responseAsStringArr[6].split("<");
        String temperatureAsString = temperatureAsStringArr[0];
    
        return temperatureAsString;

    }
    
    private void handleQueryUserResponse(HttpExchange httpExchange, String nickname) throws IOException, SQLException {

        OutputStream outputStream = httpExchange.getResponseBody();
            JSONArray responseMessages = new JSONArray();

            responseMessages = db.getQueryUserMessages(nickname);

            String responseString = responseMessages.toString();
    
            byte [] bytes = responseString.getBytes("UTF-8");
    
            httpExchange.sendResponseHeaders(200, bytes.length);
    
            outputStream.write(responseString.getBytes());
    
            outputStream.flush();
    
            outputStream.close(); 

    }

    private void handleQueryTimeResponse(HttpExchange httpExchange, String timestart, String timeend) throws IOException, SQLException {

        OutputStream outputStream = httpExchange.getResponseBody();
            JSONArray responseMessages = new JSONArray();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            LocalDateTime timeStartDate = LocalDateTime.parse(timestart, formatter);
            LocalDateTime timeEndDate = LocalDateTime.parse(timeend, formatter);

            long timeStartAsNumber = timeStartDate.toInstant(ZoneOffset.UTC).toEpochMilli();
            long timeEndAsNumber = timeEndDate.toInstant(ZoneOffset.UTC).toEpochMilli();

            responseMessages = db.getQueryTimeMessages(timeStartAsNumber, timeEndAsNumber);

            String responseString = responseMessages.toString();
    
            byte [] bytes = responseString.getBytes("UTF-8");
    
            httpExchange.sendResponseHeaders(200, bytes.length);
    
            outputStream.write(responseString.getBytes());
    
            outputStream.flush();
    
            outputStream.close(); 

    }


    private void handleQueryLocationResponse(HttpExchange httpExchange, String uplongitude, String uplatitude, String downlongitude, String downlatitude) throws IOException, SQLException {

        OutputStream outputStream = httpExchange.getResponseBody();
            JSONArray responseMessages = new JSONArray();

            responseMessages = db.getQueryLocationMessages(uplongitude, uplatitude, downlongitude, downlatitude);

            String responseString = responseMessages.toString();
    
            byte [] bytes = responseString.getBytes("UTF-8");
    
            httpExchange.sendResponseHeaders(200, bytes.length);
    
            outputStream.write(responseString.getBytes());
    
            outputStream.flush();
    
            outputStream.close(); 

    }
    
}
