package com.example.naziur.groupchatandroidapp;

public class FirebaseUserModel {

    private String name;
    private String email;
    String deviceToken = "";
    public FirebaseUserModel() {
        /*Blank default constructor essential for Firebase*/
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
