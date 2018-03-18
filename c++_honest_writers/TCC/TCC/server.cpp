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
#include <thread>
#include <mutex>

#include "define.h"
#include "jsonHelper.hpp"

namespace server{
    
    std::string HOST = "";
    int PORT = -1;
    int ID = -1;
    int VERBOSE = 0;
    
    std::string VARIABLE = "";
    unsigned int TIMESTAMP = -1;
    std::string DATA_SIGNATURE = "";
    int CLIENT_ID = -1;
    
    std::mutex LOCK;
    
    
    /*
     Server constructor.
     param: id - Server id
     param: ip - Server ip
     param: port - Server port
     param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all
     */
    void init (int id, std::string ip, int port, int verbose)
    {
        std::cout << "Servidor " + Define::plataform + " " + std::to_string(id) + "rodando...\n";
        ID = id;
        HOST = ip;
        PORT = port;
        VERBOSE = verbose;
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
            
            std::thread t(clientConnected, newClientSocket);
            t.detach();
        }
        
        close(serverSocket);
    }
    
    
    /*
     Waits for client's message.
     param: socketTCPThread - Socket that has been created for the pair (Server, Client)
     */
    void clientConnected(int socketTCPThread)
    {
        if (VERBOSE > 0)
            std::cout << "Novo cliente conectado, nova thread criada\n";
        
        char data[2048];
        int n;
        
        bzero(data,2048);
        n = read(socketTCPThread,data,2047);
        if (n < 0)
            error("ERROR reading from socket");
        
        if (VERBOSE == 2)
            printf("-----REQUEST CHEGANDO:----- %s\n",data);
        
        rapidjson::Document doc = parseJsonStringToDocument(data);
        getRequestStatus(&doc,socketTCPThread);
        
        close(socketTCPThread);
        
        if (VERBOSE > 0)
            std::cout << "matando thread\n";
    }
    
    
    /*
     Analyse user's message and forwards to the correct handler.
     param: request - A rapidJson::Document with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    void getRequestStatus(rapidjson::Document *request, int socketTCP)
    {
        std::string type = getStringWithKeyFromDocument(request, Define::type);
        if (type == Define::write)
            write(request, socketTCP);
        else if (type == Define::read)
            readData(request, socketTCP);
        else if (type == Define::read_timestamp)
            readTimestamp(request, socketTCP);
        else if (type == Define::bye)
        {
            close(socketTCP);
            if (VERBOSE > 0)
                std::cout << "Cliente desconectou propositalmente";
        }
        else
        {
            rapidjson::Document document;
            document.SetObject();
            
            addValueToDocument(&document, Define::server_id, ID);
            addValueToDocument(&document, "plataform", Define::plataform);
            addValueToDocument(&document, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
            addValueToDocument(&document, Define::status, Define::error);
            addValueToDocument(&document, Define::msg, Define::undefined_type);
            
            std::string responseJSON = getJSONStringForDocument(&document);
            sendResponse(responseJSON, socketTCP);
        }
    }
    
    
    /*
     Send a message to the client of the socket.
     param: responseJSON - A message in JSON format
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    void sendResponse(std::string responseJSON, int socketTCP)
    {
        int n = send(socketTCP, responseJSON.c_str(), responseJSON.length(), 0);
        if (n < 0)
            error("ERROR writing to socket");
        if (VERBOSE == 2)
            std::cout << "-----REQUEST SAINDO:----- " + responseJSON;
    }
    
    
    /*
    Write data in register if the requirements are followed.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    */
    void write(rapidjson::Document *request, int socketTCP)
    {
        std::string variable = getStringWithKeyFromDocument(request, Define::variable);
        unsigned int timestamp = getUnsignedIntWithKeyFromDocument(request, Define::timestamp);
        std::string data_signature = getStringWithKeyFromDocument(request, Define::data_signature);
        int client_id = getIntWithKeyFromDocument(request, Define::client_id);
        
        if (VERBOSE > 0)
            std::cout << "Recebido variable = " + variable + " e timestamp " + std::to_string(timestamp) + "\n";
        
        LOCK.lock();
        if (timestamp > TIMESTAMP || (timestamp == TIMESTAMP && client_id > CLIENT_ID))
        {
            VARIABLE = variable;
            TIMESTAMP = timestamp;
            DATA_SIGNATURE = data_signature;
            CLIENT_ID = client_id;
            
            rapidjson::Document document;
            document.SetObject();
            
            addValueToDocument(&document, Define::server_id, ID);
            addValueToDocument(&document, "plataform", Define::plataform);
            addValueToDocument(&document, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
            addValueToDocument(&document, Define::status, Define::success);
            addValueToDocument(&document, Define::msg, Define::variable_updated);
            
            std::string responseJSON = getJSONStringForDocument(&document);
            LOCK.unlock();
            sendResponse(responseJSON, socketTCP);
        }
        else
        {
            LOCK.unlock();
            
            rapidjson::Document document;
            document.SetObject();
            
            addValueToDocument(&document, Define::server_id, ID);
            addValueToDocument(&document, "plataform", Define::plataform);
            addValueToDocument(&document, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
            addValueToDocument(&document, Define::status, Define::error);
            addValueToDocument(&document, Define::msg, Define::outdated_timestamp);
            
            std::string responseJSON = getJSONStringForDocument(&document);
            sendResponse(responseJSON, socketTCP);
        }
    }
    
    
    /*
    Sends data in register for client.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    void readData(rapidjson::Document *request, int socketTCP)
    {
        LOCK.lock();
        rapidjson::Document response;
        response.SetObject();
        
        rapidjson::Value dataDict(rapidjson::kObjectType);
        addValueToValueStruct(&dataDict, &response, Define::variable, VARIABLE);
        addValueToValueStruct(&dataDict, &response, Define::timestamp, TIMESTAMP);
        addValueToValueStruct(&dataDict, &response, Define::data_signature, DATA_SIGNATURE);
        addValueToValueStruct(&dataDict, &response, Define::client_id, CLIENT_ID);
        
        addValueToDocument(&response, Define::data, &dataDict);
        addValueToDocument(&response, Define::server_id, ID);
        addValueToDocument(&response, "plataform", Define::plataform);
        addValueToDocument(&response, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
        addValueToDocument(&response, Define::status, Define::success);
        addValueToDocument(&response, Define::msg, Define::read);
        
        std::string responseJSON = getJSONStringForDocument(&response);
        LOCK.unlock();
        sendResponse(responseJSON, socketTCP);
    }
    
    
    /*
    Sends timestamp in register for client.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    */
    void readTimestamp(rapidjson::Document *request, int socketTCP)
    {
        LOCK.lock();
        rapidjson::Document response;
        response.SetObject();
        
        rapidjson::Value dataDict(rapidjson::kObjectType);
        addValueToValueStruct(&dataDict, &response, Define::timestamp, TIMESTAMP);
        
        addValueToDocument(&response, Define::data, &dataDict);
        addValueToDocument(&response, Define::server_id, ID);
        addValueToDocument(&response, "plataform", Define::plataform);
        addValueToDocument(&response, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
        addValueToDocument(&response, Define::status, Define::success);
        addValueToDocument(&response, Define::msg, Define::read);
        
        std::string responseJSON = getJSONStringForDocument(&response);
        LOCK.unlock();
        sendResponse(responseJSON, socketTCP);
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
