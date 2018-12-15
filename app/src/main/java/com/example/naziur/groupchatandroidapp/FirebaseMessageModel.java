package com.example.naziur.groupchatandroidapp;

public class FirebaseMessageModel {

    private String body;
    private String senderEmail;

    public FirebaseMessageModel() {
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
}
