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
#include <vector>

#include "define.h"
#include "jsonHelper.hpp"
#include "signature.hpp"

namespace server{
    
    std::string HOST = "";
    int PORT = -1;
    int ID = -1;
    
    int FAULTS = 1; //number of faults system can handle
    int QUORUM = 2*FAULTS + 1;
    
    std::string VARIABLE = "";
    int TIMESTAMP = -1;
    std::string DATA_SIGNATURE = "";
    
    std::vector<std::pair<int, std::string>> LAST_ECHOED_VALUES; //contem uma tupla (timestamp,value)
    
    std::mutex LOCK;
    
    
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
            
            std::thread t(clientConnected, newClientSocket);
            t.detach();
        }
        
        //close(serverSocket);
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
        n = (int) read(socketTCPThread,data,2047);
        if (n < 0)
            error("ERROR reading from socket");
        
        printf("Here is the message: %s\n",data);
        
        rapidjson::Document doc = parseJsonStringToDocument(data);
        getRequestStatus(&doc,socketTCPThread);
        
        close(socketTCPThread);
        
        std::cout << "matando thread";
    }
    
    
    /*
     Analyse user's message and forwards to the correct handler.
     param: request - A rapidJson::Document with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    void getRequestStatus(rapidjson::Document *request, int socketTCP)
    {
        std::string type = getStringWithKeyFromDocument(request, Define::type);
        if (type == Define::write || type == Define::write_back)
            write(request, socketTCP, type);
        else if (type == Define::read)
            readData(request, socketTCP);
        else if (type == Define::read_timestamp)
            readTimestamp(request, socketTCP);
        else if (type == Define::get_echoe)
            getEchoe(request, socketTCP);
        else if (type == Define::bye) {
            close(socketTCP);
            std::cout << "Cliente desconectou propositalmente";
        }
        else {
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
        int n = (int) send(socketTCP, responseJSON.c_str(), responseJSON.length(), 0);
        if (n < 0)
            error("ERROR writing to socket");
    }
    
    
    /*
     Write data in register if the requirements are followed.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    void write(rapidjson::Document *request, int socketTCP, std::string type)
    {
        std::string variable = getStringWithKeyFromDocument(request, Define::variable);
        int timestamp = getIntWithKeyFromDocument(request, Define::timestamp);
        std::vector<std::pair<int, std::string>> echoes = getEchoesArrayWithKeyFromDocument(request, Define::echoes);
        
        LOCK.lock();
        if (timestamp > TIMESTAMP)
        {
            if (isEchoValid(echoes, variable, timestamp, type)) {
                VARIABLE = variable;
                TIMESTAMP = timestamp;
                DATA_SIGNATURE = signData(ID, variable + std::to_string(timestamp));
                LAST_ECHOED_VALUES.clear();
                LOCK.unlock();
                
                rapidjson::Document document;
                document.SetObject();
                
                addValueToDocument(&document, Define::server_id, ID);
                addValueToDocument(&document, "plataform", Define::plataform);
                addValueToDocument(&document, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
                addValueToDocument(&document, Define::status, Define::success);
                addValueToDocument(&document, Define::msg, Define::variable_updated);
                
                std::string responseJSON = getJSONStringForDocument(&document);
                
                std::cout << "Recebido variable = " + variable + " e timestamp " + std::to_string(timestamp) + "\n";
                
                sendResponse(responseJSON, socketTCP);
            }
            else {
                LOCK.unlock();
                
                rapidjson::Document document;
                document.SetObject();
                
                addValueToDocument(&document, Define::server_id, ID);
                addValueToDocument(&document, "plataform", Define::plataform);
                addValueToDocument(&document, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
                addValueToDocument(&document, Define::status, Define::error);
                addValueToDocument(&document, Define::msg, Define::invalid_echoes);
                
                std::string responseJSON = getJSONStringForDocument(&document);
                sendResponse(responseJSON, socketTCP);
            }
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
    Sends echo for timestamp and value
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    */
    void getEchoe(rapidjson::Document *request, int socketTCP) {
        std::string variable = getStringWithKeyFromDocument(request, Define::variable);
        int timestamp = getIntWithKeyFromDocument(request, Define::timestamp);
        
        LOCK.lock();
        if (timestamp < TIMESTAMP) {
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
        else if (!shouldEcho(variable, timestamp)) {
            LOCK.unlock();
            
            rapidjson::Document document;
            document.SetObject();
            
            addValueToDocument(&document, Define::server_id, ID);
            addValueToDocument(&document, "plataform", Define::plataform);
            addValueToDocument(&document, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
            addValueToDocument(&document, Define::status, Define::error);
            addValueToDocument(&document, Define::msg, Define::timestamp_already_echoed);
            
            std::string responseJSON = getJSONStringForDocument(&document);
            sendResponse(responseJSON, socketTCP);
        }
        else {
            LOCK.unlock();
            
            std::string data_signature = signData(ID, variable + std::to_string(timestamp));
            
            LAST_ECHOED_VALUES.push_back(std::make_pair(timestamp, variable));
            
            rapidjson::Document response;
            response.SetObject();
            
            rapidjson::Value dataDict(rapidjson::kObjectType);
            addValueToValueStruct(&dataDict, &response, Define::data_signature, data_signature);
            
            addValueToDocument(&response, Define::data, &dataDict);
            addValueToDocument(&response, Define::server_id, ID);
            addValueToDocument(&response, "plataform", Define::plataform);
            addValueToDocument(&response, Define::request_code, getIntWithKeyFromDocument(request, Define::request_code));
            addValueToDocument(&response, Define::status, Define::success);
            addValueToDocument(&response, Define::msg, Define::get_echoe);
            
            std::string responseJSON = getJSONStringForDocument(&response);
            
            sendResponse(responseJSON, socketTCP);
        }
    }
    
    
    /*
    Check if value was echoed before
    param: value - Variable to sign.
    param: timestamp - Timestamp.
    return: (bool) If server should echo value and timestamp
    */
    bool shouldEcho(std::string value, int timestamp) {
        for(std::vector<std::pair<int, std::string>>::const_iterator iterator = LAST_ECHOED_VALUES.begin() ; iterator < LAST_ECHOED_VALUES.end(); iterator ++) {
            std::pair<int, std::string> tuple = *iterator;
            if (tuple.first == TIMESTAMP && tuple.second != value)
                return false;
            else if (tuple.first == TIMESTAMP && tuple.second == value)
                return true;
        }
        
        return true;
    }
    
    
    /*
    Check if echoes are valid
    param: echoes - Array with tuples(server_id, data_signature)
    param: value - Variable to sign.
    param: timestamp - Timestamp.
    param: type - If is a write or write_back
    return: (bool) If echoes are valid
    */
    bool isEchoValid(std::vector<std::pair<int, std::string>> echoes, std::string value, int timestamp, std::string type) {
        int validEchoes = 0;
        
        for(std::vector<std::pair<int, std::string>>::const_iterator iterator = echoes.begin() ; iterator < echoes.end(); iterator ++) {
            std::pair<int, std::string> echoe = *iterator;
            if (verify(echoe.first, value + std::to_string(timestamp), echoe.second))
                validEchoes++;
        }
        
        if (type == Define::write)
            return validEchoes >= QUORUM;
        else
            return validEchoes >= FAULTS + 1;
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
