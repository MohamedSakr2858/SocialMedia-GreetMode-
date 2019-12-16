package com.example.socialmediaapp.notifications;

public class Token {
    // a FCM token is an ID issued by the GCM
    //connection server to the the client app that allows to recives messages

    String token;

    public Token(String token){
        this.token = token;
    }

    public Token() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
