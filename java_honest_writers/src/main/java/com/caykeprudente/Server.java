package com.caykeprudente;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by cayke on 10/11/16.
 */
public class Server
{
    public String host = "";
    public int port = -1;
    public int id = -1;

    public String variable = "";
    public int timestamp = -1;
    public String data_signature = "";
    public int client_id = -1;

    //TODO IMPLEMENTAR O LOCK


    /*
    Server constructor.
    param: id - Server id
    param: ip - Server ip
    param: port - Server port
    */
    public Server (int id, String ip, int port)
    {
        this.host = ip;
        this.id = id;
        this.port = port;
    }


    /*
    Fuction where server's main thread keeps waiting for a new connection from client.
    Starts a new thread to manipulate new connections.
     */
    public void waitForConnection() throws IOException
    {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Porta " + String.valueOf(port) +  " aberta!");

        while (true)
        {
            Socket clientSocket = serverSocket.accept();
            Thread thread = new Thread(new ServerHandler(this, clientSocket));
            thread.start();
        }
    }
}