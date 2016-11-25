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

        
//        request = json.loads(data.decode('utf-8'))
//        self.getRequestStatus(request,socketTCPThread)
        
        close(socketTCPThread);
        break;
        
    }
    
    /*
     def clientConnected(self, socketTCPThread):
     print("Novo cliente conectado, nova thread criada")
     while True:
     try:
     data = socketTCPThread.recv(2048)
     request = json.loads(data.decode('utf-8'))
     self.getRequestStatus(request,socketTCPThread)
     except socket.error as msg:
     print('Error code: ' + str(msg[0]) + ', Error message: ' + str(msg[1]))
     print('matando thread')
     socketTCPThread.close()
     return False
     except:
     print('matando thread')
     socketTCPThread.close()
     return False
     */
    
    
    /*
     This function is called when a system call fails
     */
    void error(std::string msg)
    {
        std::cout << msg + "\n";
        exit(1);
    }
    
}
