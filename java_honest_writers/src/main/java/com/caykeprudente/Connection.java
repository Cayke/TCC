package com.caykeprudente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cayke on 19/03/17.
 */
public class Connection {
    /*
    Sends response to client.
    param: message - A dictionary with serve's response data.
    param: socket - socket to send the message
    */
    public static void sendMessage(Map<String, Object> message, Socket socket) throws IOException {
        String requestJSON = new Gson().toJson(message);
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        dOut.write(requestJSON.getBytes());
        dOut.flush(); // Send the data
        dOut.close();
    }

    /*
    Read message from socket. Keeps waiting if no message yet
    param: socket - socket to get the message
    return: (Dictionary) Message from client; null if error.
     */
    public static HashMap<String, Object> read(Socket socket) {
        boolean shouldContinue = true;
        while (shouldContinue) {
            String jsonMessage = readInputStream(socket);
            if (jsonMessage != null) {
                return new GsonBuilder().create().fromJson(jsonMessage, HashMap.class);
            }
            else
                shouldContinue = false;
        }

        return null;
    }

    /*
    Read message that is on the socket right now
    return: (String) Message from client; null if error.
    */
    private static String readInputStream(Socket socket) {
        try {
            int read = -1;
            byte[] buffer = new byte[2048];
            byte[] readData;
            String readDataText;

            read = socket.getInputStream().read(buffer);
            readData = new byte[read];
            System.arraycopy(buffer, 0, readData, 0, read);
            readDataText = new String(readData, "UTF-8"); // assumption that client sends data UTF-8 encoded
            //System.out.println("message part recieved:" + readDataText);
            return readDataText;
        } catch (Exception e) {
            return null;
        }
    }
}