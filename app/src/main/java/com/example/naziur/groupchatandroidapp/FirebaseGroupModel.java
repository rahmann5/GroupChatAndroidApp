package com.example.naziur.groupchatandroidapp;

import com.google.firebase.database.Exclude;

public class FirebaseGroupModel {

    private String groupName = "";
    private String members = "";

    public FirebaseGroupModel() {

    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getMembers() {
        return members;
    }

    public void setMembers(String members) {
        this.members = members;
    }

    @Exclude
    @Override
    public String toString () {
        return groupName + "\n" + members;
    }

}
