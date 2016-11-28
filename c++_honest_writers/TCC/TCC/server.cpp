//
//  server.cpp
//  TCC
//
//  Created by Cayke Prudente on 23/11/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

#include "server.hpp"
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>

#include "define.h"
#include "jsonHelper.hpp"

namespace server{
    
    std::string HOST = "";
    int PORT = -1;
    int ID = -1;
    
    std::string VARIABLE = "";
    int TIMESTAMP = -1;
    std::string DATA_SIGNATURE = "";
    int CLIENT_ID = -1;
    
    //LOCK = threading.Lock();
    
    
    
    /*
     Server constructor.
     param: id - Server id
     param: ip - Server ip
     param: port - Server port
     */
    void init (int id, std::string ip, int port)
    {
        std::cout << "Servidor " + Define::plataform + " " + std::to_string(id) + "rodando...\n";
        ID = id;
        HOST = ip;
        PORT = port;
        waitForConnection();
    }
    
    
    /*
     Fuction where server's main thread keeps waiting for a new connection from client.
     Starts a new thread to manipulate new connections.
     */
    void waitForConnection()
    {
        int serverSocket, newClientSocket;
        struct sockaddr_in server_addr, client_addr;
        socklen_t clientLenth;
        
        serverSocket = socket(AF_INET, SOCK_STREAM, 0);
        if (serverSocket < 0)
            error("ERROR opening socket");
        
        bzero((char *) &server_addr, sizeof(server_addr));
        server_addr.sin_family = AF_INET;
        server_addr.sin_port = htons(PORT);
        server_addr.sin_addr.s_addr = INADDR_ANY;
        
        if (bind(serverSocket, (struct sockaddr *) &server_addr, sizeof(server_addr)) < 0)
            error("ERROR on binding");
        
        
        listen(serverSocket, 1);
        
        while (true) {
            clientLenth = sizeof(client_addr);
            newClientSocket = accept(serverSocket, (struct sockaddr *) &client_addr, &clientLenth);
            if (newClientSocket < 0)
                error("ERROR on accept");
            
            //todo cria thread para comunicar com o cliente
            clientConnected(newClientSocket);
        }
        
        close(serverSocket);
    }
    
    
    /*
     Waits for client's message.
     param: socketTCPThread - Socket that has been created for the pair (Server, Client)
     */
    void clientConnected(int socketTCPThread)
    {
        std::cout << "Novo cliente conectado, nova thread criada\n";
        
        char data[2048];
        int n;
        
        bzero(data,2048);
        n = read(socketTCPThread,data,2047);
        if (n < 0)
            error("ERROR reading from socket");
        
        printf("Here is the message: %s\n",data);

        rapidjson::Document doc = parseJsonStringToDocument(data);
        getRequestStatus(&doc,socketTCPThread);
        
        close(socketTCPThread);
    }
    
    
    /*
    Analyse user's message and forwards to the correct handler.
     param: request - A rapidJson::Document with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
    */
    void getRequestStatus(rapidjson::Document *request, int socketTCP)
    {
        std::string type = getStringWithValueFromDocument(request, Define::type);
        if (type == Define::write)
            write(request, socketTCP);
        else if (type == Define::read)
            readData(request, socketTCP);
        else if (type == Define::read_timestamp)
            readTimestamp(request, socketTCP);
        else if (type == Define::bye)
        {
            close(socketTCP);
            std::cout << "Cliente desconectou propositalmente";
        }
        else
        {
//            response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.error, msg = Define.undefined_type)
//            responseJSON = json.dumps(response)
//            socketTCP.send(responseJSON.encode('utf-8'))
        }
    }
    
    void write(rapidjson::Document *request, int socketTCP)
    {
        
    }
    void readData(rapidjson::Document *request, int socketTCP)
    {
        
    }
    void readTimestamp(rapidjson::Document *request, int socketTCP)
    {
        
    }
    
    
    /*
     This function is called when a system call fails
     */
    void error(std::string msg)
    {
        std::cout << msg + "\n";
        exit(1);
    }
    
}
