//
//  server.swift
//  tcc
//
//  Created by Cayke Prudente on 06/12/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

import Foundation

class Server: NSObject {
    var HOST = "";
    var PORT = -1;
    let ID : Int
    
    let FAULTS = 1
    let QUORUM = 2 * 1 + 1 //2*FAULTS + 1
    
    var VARIABLE = "";
    var TIMESTAMP = -1;
    var DATA_SIGNATURE = "";
    
    var LOCK = pthread_mutex_t();
    
    var LAST_ECHOED_VALUES : Array<(Int, String)> = []
    
    
    /*
     Server constructor.
     param: id - Server id
     param: ip - Server ip
     param: port - Server port
     */
    init(id: Int, ip: String, port: Int) {
        print ("Servidor \(Define.plataform) \(id) rodando...")
        
        self.ID = id;
        self.HOST = ip;
        self.PORT = port;
        
        pthread_mutex_init(&self.LOCK, nil)
    }
    
    
    /*
     Fuction where server's main thread keeps waiting for a new connection from client.
     Starts a new thread to manipulate new connections.
     */
    func waitForConnection() {
        let serverSocket = TCPServer(address: self.HOST, port: Int32(self.PORT))
        switch serverSocket.listen() {
        case .success:
            while true {
                if let clientSocket = serverSocket.accept() {
                    DispatchQueue.global(qos: .background).async {
                        self.clientConnected(clientSocket: clientSocket)
                    }
                } else {
                    print("accept error")
                }
            }
        case .failure(let error):
            print(error)
        }
        serverSocket.close();
    }
    
    
    /*
     Waits for client's message.
     param: socketTCPThread - Socket that has been created for the pair (Server, Client)
     */
    func clientConnected(clientSocket: TCPClient) {
        print("Novo cliente conectado, nova thread criada");
        
        guard let data = clientSocket.read(2048) else {return}
        
        let requestJSON = String(bytes: data, encoding: .utf8)
        let request = try? JSONSerialization.jsonObject(with: (requestJSON?.data(using: .utf8))!) as! [String:Any]
        
        getRequestStatus(request: request!, clientSocket: clientSocket)
        
        print("matando thread")
    }
    
    
    /*
     Analyse user's message and forwards to the correct handler.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func getRequestStatus(request: Dictionary<String, Any>, clientSocket: TCPClient) {
        let type : String = request[Define.type] as! String
        
        if type == Define.write || type == Define.write_back {
            self.write(request: request, clientSocket: clientSocket, type: type)
        }
        else if type == Define.read {
            self.read(request: request, clientSocket: clientSocket)
        }
        else if type == Define.read_timestamp {
            self.readTimestamp(request: request, clientSocket: clientSocket)
        }
        else if type == Define.get_echoe {
            self.getEchoe(request: request, clientSocket: clientSocket)
        }
        else if type == Define.bye {
            clientSocket.close()
            print("cliente desconectou propositalmente")
            return
        }
        else {
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code] as! Int,
                            Define.status: Define.error,
                            Define.msg: Define.undefined_type] as Dictionary<String, Any>
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
    }
    
    
    /*
     Write data in register if the requirements are followed.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func write (request: Dictionary<String, Any>, clientSocket: TCPClient, type:String) {
        let variable : String = request[Define.variable] as! String
        let timestamp : Int = request[Define.timestamp] as! Int
        
        let echoesArray : [Dictionary<String, Any>] = request[Define.echoes] as! [Dictionary<String, Any>]
        var echoes : [(Int, String)] = []
        for var dict in echoesArray {
            echoes.append((dict[Define.server_id] as! Int, dict[Define.data_signature] as! String))
        }
        
        
        pthread_mutex_lock(&self.LOCK)
        if timestamp > self.TIMESTAMP {
            if (isEchoValid(echoes: echoes, value: variable, timestamp: timestamp, type: type)) {
                
                print ("Recebido variable = \(variable) e timestamp = \(timestamp)")
                
                self.VARIABLE = variable
                self.TIMESTAMP = timestamp
                let message = variable + String(timestamp)
                self.DATA_SIGNATURE = String(cString:signData(String(self.ID), message, Int32(message.characters.count)))
                self.LAST_ECHOED_VALUES = []
                
                let response = [Define.server_id: self.ID,
                                "plataform": Define.plataform,
                                Define.request_code: request[Define.request_code] as! Int,
                                Define.status: Define.success,
                                Define.msg: Define.variable_updated] as Dictionary<String, Any>
                pthread_mutex_unlock(&self.LOCK)
                
                sendResponse(response: response, clientSocket: clientSocket)
            }
            else {
                let response = [Define.server_id: self.ID,
                                "plataform": Define.plataform,
                                Define.request_code: request[Define.request_code] as! Int,
                                Define.status: Define.error,
                                Define.msg: Define.invalid_echoes] as Dictionary<String, Any>
                pthread_mutex_unlock(&self.LOCK)
                
                sendResponse(response: response, clientSocket: clientSocket)
            }
        }
        else {
            pthread_mutex_unlock(&self.LOCK)
            
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code] as! Int,
                            Define.status: Define.error,
                            Define.msg: Define.outdated_timestamp] as Dictionary<String, Any>
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
    }
    
    
    /*
     Sends echo for timestamp and value
     param: request - A dictionary with client's request data.
     */
    func getEchoe(request: Dictionary<String, Any>, clientSocket: TCPClient) {
        let variable : String = request[Define.variable] as! String
        let timestamp : Int = request[Define.timestamp] as! Int
        
        if timestamp < self.TIMESTAMP {
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code] as! Int,
                            Define.status: Define.error,
                            Define.msg: Define.outdated_timestamp]as Dictionary<String, Any>
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
        else if !shouldEcho(variable: variable, timestamp: timestamp) {
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code] as! Int,
                            Define.status: Define.error,
                            Define.msg: Define.timestamp_already_echoed] as Dictionary<String, Any>
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
        else {
            let message = variable + String(timestamp)
            let data_signature = String(cString:signData(String(self.ID), message, Int32(message.characters.count)))
            
            self.LAST_ECHOED_VALUES.append((timestamp, variable))
            
            let dataDict = [Define.data_signature: data_signature] as [String : Any]
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code] as! Int,
                            Define.status: Define.success,
                            Define.msg: Define.get_echoe,
                            Define.data: dataDict] as Dictionary<String, Any>
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
    }
    
    
    /*
     Check if value was echoed before
     param: value - Variable to sign.
     param: timestamp - Timestamp.
     return: (bool) If server should echo value and timestamp
     */
    func shouldEcho(variable: String, timestamp : Int) -> Bool {
        for (time, value) in self.LAST_ECHOED_VALUES {
            if time == timestamp && !(value == variable) {
                return false;
            }
            else if time == timestamp && value == variable {
                return true;
            }
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
    func isEchoValid(echoes : [(Int, String)], value : String, timestamp : Int, type : String) -> Bool {
        var validEchoes = 0
        for (server_id, data_sign) in echoes {
            let data = value+String(timestamp)
            if verifySignature(String(server_id), data, Int32(data.characters.count), data_sign) {
                validEchoes = validEchoes + 1
            }
        }
        
        if type == Define.write {
            return validEchoes >= self.QUORUM
        }
        else { //write_back
            return validEchoes >= self.FAULTS + 1
        }
    }
    
    
    /*
     Sends data in register for client.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func read (request: Dictionary<String, Any>, clientSocket: TCPClient) {
        pthread_mutex_lock(&self.LOCK)
        let dataDict = [Define.variable: self.VARIABLE,
                        Define.timestamp: self.TIMESTAMP,
                        Define.data_signature: self.DATA_SIGNATURE] as [String : Any]
        let response = [Define.server_id: self.ID,
                        "plataform": Define.plataform,
                        Define.request_code: request[Define.request_code] as! Int,
                        Define.status: Define.success,
                        Define.msg: Define.read,
                        Define.data: dataDict] as Dictionary<String, Any>
        pthread_mutex_unlock(&self.LOCK)
        
        sendResponse(response: response, clientSocket: clientSocket)
    }
    
    
    /*
     Sends timestamp in register for client.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func readTimestamp (request: Dictionary<String, Any>, clientSocket: TCPClient) {
        pthread_mutex_lock(&self.LOCK)
        let dataDict = [Define.timestamp: self.TIMESTAMP]
        let response = [Define.server_id: self.ID,
                        "plataform": Define.plataform,
                        Define.request_code: request[Define.request_code] as! Int,
                        Define.status: Define.success,
                        Define.msg: Define.read,
                        Define.data: dataDict] as Dictionary<String, Any>
        pthread_mutex_unlock(&self.LOCK)
        
        sendResponse(response: response, clientSocket: clientSocket)
    }
    
    
    /*
     Send a message to the client of the socket.
     param: responseJSON - A message in JSON format
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func sendResponse(response: Dictionary<String, Any>, clientSocket: TCPClient)
    {
        do{
            let responseJSON = try JSONSerialization.data(withJSONObject: response, options: .prettyPrinted)
            
            _ = clientSocket.send(data: responseJSON)
        }
        catch {
            print (error.localizedDescription)
        }
    }
}
