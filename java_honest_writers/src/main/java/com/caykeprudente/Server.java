package com.caykeprudente;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    Lock lock = new ReentrantLock();

    public int verbose = 0;

    /*
    Server constructor.
    param: id - Server id
    param: ip - Server ip
    param: port - Server port
    param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all
    */
    public Server (int id, String ip, int port, int verbose)
    {
        this.host = ip;
        this.id = id;
        this.port = port;
        this.verbose = verbose;
    }


    /*
    Fuction where server's main thread keeps waiting for a new connection from client.
    Starts a new thread to manipulate new connections.
     */
    public void waitForConnection() throws IOException
    {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor " + Define.plataform + " " + String.valueOf(id) + " rodando!");

        while (true)
        {
            Socket clientSocket = serverSocket.accept();
            Thread thread = new Thread(new ServerHandler(this, clientSocket));
            thread.start();
        }
    }
}