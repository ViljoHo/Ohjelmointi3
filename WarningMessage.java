package com.server;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class WarningMessage {
    
    private LocalDateTime sent;
    private String nick;
    private Double latitude;
    private Double longitude;
    private String dangertype;
    private String areacode;
    private String phonenumber;
    private String weather;


    WarningMessage(String nick, Double latitude, Double longitude, String dangertype, LocalDateTime sent, String areacode, String phonenumber, String weather){
        this.nick = nick;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dangertype = dangertype;
        this.sent = sent;
        this.areacode = areacode;
        this.phonenumber = phonenumber;
        this.weather = weather;
    }



    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getNick() {
        return nick;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLatitude() {
        return latitude;
    }


    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setDangertype(String dangertype) {
        this.dangertype = dangertype;
    }

    public String getDangertype() {
        return dangertype;
    }

    public void setSent(long epoch) {
        sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }

    public LocalDateTime getSent() {
        return sent;
    }

    public long dateAsInt() {
        return sent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public void setAreacode(String areacode) {
        this.areacode = areacode;
    }

    public String getAreacode() {
        return areacode;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getWeather() {
        return weather;
    }



}
