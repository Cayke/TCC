package com.caykeprudente;

import com.google.gson.GsonBuilder;

import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

/**
 * Created by cayke on 11/11/16.
 */
public class ServerHandler implements Runnable {
    private Socket socket;
    private Server server;

    /*
    Server's  messages handler constructor.
    param: server - Server main class
    param: socket - Socket that has been created for the pair (Server, Client)
    */
    public ServerHandler (Server server, Socket socket)
    {
        this.server = server;
        this.socket = socket;
//        try {
//            this.socket.setTcpNoDelay(true);
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
    }


    /*
    Waits for client's message.
    */
    @Override
    public void run() {
        System.out.println("Novo cliente conectado, nova thread criada");

        boolean shouldContinue = true;
        while (shouldContinue)
        {
            String jsonMessage = readInputStream();
            if (jsonMessage != null)
            {
                HashMap<String, Object> request = new GsonBuilder().create().fromJson(jsonMessage, HashMap.class);

                getRequestStatus(request);
            }
            else
                shouldContinue = false;
        }
    }


    /*
    Read message that is on the socket right now
    return: (String) Message from client; null if error.
    */
    private String readInputStream()
    {
        try {
            int read = -1;
            byte[] buffer = new byte[2048];
            byte[] readData;
            String readDataText;

            read = socket.getInputStream().read(buffer);
            readData = new byte[read];
            System.arraycopy(buffer, 0, readData, 0, read);
            readDataText = new String(readData, "UTF-8"); // assumption that client sends data UTF-8 encoded
            System.out.println("message part recieved:" + readDataText);
            return readDataText;
        }
        catch (Exception e)
        {
            return null;
        }
    }


    /*
    Analyse user's message and forwards to the correct method.
    param: request - A dictionary with client's request data.
    return: (boolean) True for success, False for error.
    */
    private boolean getRequestStatus(HashMap<String, Object> request)
    {
        String type = (String) request.get("type");
        if (type.equals("read")) {
            System.out.println("Chegou a request certinho");
            return false;
        }
        else {
            System.out.println("Deu ruim nos valores da request");
            return true;
        }
    }
}