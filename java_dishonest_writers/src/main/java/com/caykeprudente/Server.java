package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by cayke on 10/11/16.
 */
public class Server
{
    public String host = "";
    public int port = -1;
    public Double id = -1d;

    public int faults = 1; //number of faults system can handle
    public int quorum = 2*faults + 1;

    public String variable = "";
    public int timestamp = -1;
    public String data_signature = "";

    public List<Pair<Integer, String>> last_echoed_values = new ArrayList<Pair<Integer, String>>();

    Lock lock = new ReentrantLock();

    /*
    Server constructor.
    param: id - Server id
    param: ip - Server ip
    param: port - Server port
    */
    public Server (Double id, String ip, int port)
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