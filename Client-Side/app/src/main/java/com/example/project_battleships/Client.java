package com.example.project_battleships;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client extends AsyncTask<JSONObject, Void, JSONObject> {
    // Constants
    private final static String IP_ADDRESS = "192.168.1.19"; // Host's IP Address
    private final static int PORT = 1225; // The port which will be used to transfer the data in it.
    private final static int SIZE = 1024;

    private JSONObject received;
    private JSONObject toSend;
    private Socket socket;
    private InputStreamReader inputStreamReader;
    private OutputStreamWriter outputStreamWriter;

    public Client(JSONObject object) {
        this.toSend = object;
    }

    private void send() {
        String data = this.toSend.toString();
        try {
            this.outputStreamWriter.write(data);
            this.outputStreamWriter.flush();
            System.out.println("Successfully sent data");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void receive() {
        try {
            char[] charBuffer = new char[SIZE];
            StringBuilder stringBuilder = new StringBuilder();
            while (this.inputStreamReader.read(charBuffer) != -1) {
                stringBuilder.append(new String(charBuffer));
            }
            validateStringBuilder(stringBuilder);
            // System.out.println(stringBuilder);
            this.received = new JSONObject(stringBuilder.toString());
            this.inputStreamReader.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void validateStringBuilder(StringBuilder stringBuilder) {
        /*
        The purpose of this function is to add a json object closer ('}') to the the string builder
        (if needed)
        Sometimes the response from the server is received on the client side without
        a json object closer, and because of that, actions aren't being accepted)
        */
        int jsonStringLength = 0;
        // The character type 15 represents the replacement character
        for (int i = 0; i < stringBuilder.length() && Character.getType(stringBuilder.charAt(i)) != 15; i++) {
            jsonStringLength++;
        }
        if (stringBuilder.charAt(jsonStringLength - 1) != '}') {
            stringBuilder.setCharAt(jsonStringLength, '}');
        }

        /*
        boolean isJsonCloserExists = stringBuilder.indexOf("}") != -1;
        if (!isJsonCloserExists) {
            stringBuilder.setCharAt(1023, '}');
        }
        */
    }

    @Override
    protected JSONObject doInBackground(JSONObject... jsonObjects) {
        try {
            // Socket Setting
            this.socket = new Socket(IP_ADDRESS, PORT);
            this.inputStreamReader = new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8);
            this.outputStreamWriter = new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8);
            send();
            receive();
            this.socket.close();
            return this.received;
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        return null;
    }
}