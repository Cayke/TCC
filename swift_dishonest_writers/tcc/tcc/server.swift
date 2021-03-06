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
    var CLIENT_ID = -1;
    
    var LOCK = pthread_mutex_t();
    
    var ECHOED_VALUES : Array<(Int, Int, String)> = []
    
    var VERBOSE = 0;
    var CERT_PATH = "";
    
    
    
    /*
     Server constructor.
     param: id - Server id
     param: ip - Server ip
     param: port - Server port
     param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all
     param: cert_path - Path to certificates
     */
    init(id: Int, ip: String, port: Int, verbose: Int, cert_path: String) {
        print ("Servidor \(Define.plataform) \(id) rodando...")
        
        self.ID = id;
        self.HOST = ip;
        self.PORT = port;
        self.VERBOSE = verbose;
        self.CERT_PATH = cert_path;
        
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
                    let thread = Thread(block: {
                        self.clientConnected(clientSocket: clientSocket)
                    })
                    thread.start()
                } else {
                    if self.VERBOSE > 0 {
                        print("accept error")
                    }
                }
            }
        case .failure(let error):
            if self.VERBOSE > 0 {
                print(error)
            }
        }
        serverSocket.close();
    }
    
    
    /*
     Waits for client's message.
     param: socketTCPThread - Socket that has been created for the pair (Server, Client)
     */
    func clientConnected(clientSocket: TCPClient) {
        if self.VERBOSE > 0 {
            print("Novo cliente conectado, nova thread criada");
        }
        guard let data = clientSocket.read(2048) else {return}
        
        let requestJSON = String(bytes: data, encoding: .utf8)
        if (self.VERBOSE == 2) {
            print ("-----REQUEST CHEGANDO:----- " + requestJSON!)
        }
        let request = try? JSONSerialization.jsonObject(with: (requestJSON?.data(using: .utf8))!) as! [String:Any]
        
        getRequestStatus(request: request!, clientSocket: clientSocket)
        
        clientSocket.close()
        
        if self.VERBOSE > 0 {
            print("matando thread")
        }
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
            if self.VERBOSE > 0 {
                print("cliente desconectou propositalmente")
            }
            return
        }
        else {
            let response : JSON = [Define.server_id: JSON(self.ID),
                                   "plataform": JSON(Define.plataform),
                                   Define.request_code: JSON(request[Define.request_code] as! Int),
                                   Define.status: JSON(Define.error),
                                   Define.msg: JSON(Define.undefined_type)]
            
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
        let client_id : Int = request[Define.client_id] as! Int
        
        let echoesArray : [Dictionary<String, Any>] = request[Define.echoes] as! [Dictionary<String, Any>]
        var echoes : [(Int, String)] = []
        for var dict in echoesArray {
            echoes.append((dict[Define.server_id] as! Int, dict[Define.data_signature] as! String))
        }
        
        
        pthread_mutex_lock(&self.LOCK)
        if timestamp > self.TIMESTAMP || (timestamp == self.TIMESTAMP && client_id > self.CLIENT_ID) {
            if (isEchoValid(echoes: echoes, value: variable, timestamp: timestamp, type: type)) {
                self.VARIABLE = variable
                self.TIMESTAMP = timestamp
                let message = variable + String(timestamp)
                
                //                guard let data_signature = signature.signData(server_id: self.ID, message: message) else {
                //                    return
                //                }
                //                self.DATA_SIGNATURE = data_signature
                
                if let data_signature = signData(String(self.ID), message, Int32(message.characters.count), self.CERT_PATH) {
                    self.DATA_SIGNATURE = String(cString: data_signature)
                }
                
                pthread_mutex_unlock(&self.LOCK)
                
                print ("Recebido variable = \(variable) e timestamp = \(timestamp)")
                
                let response :JSON = [Define.server_id: JSON(self.ID),
                                      "plataform": JSON(Define.plataform),
                                      Define.request_code: JSON(request[Define.request_code] as! Int),
                                      Define.status: JSON(Define.success),
                                      Define.msg: JSON(Define.variable_updated)]
                
                
                sendResponse(response: response, clientSocket: clientSocket)
            }
            else {
                pthread_mutex_unlock(&self.LOCK)
                
                let response :JSON = [Define.server_id: JSON(self.ID),
                                      "plataform": JSON(Define.plataform),
                                      Define.request_code: JSON(request[Define.request_code] as! Int),
                                      Define.status: JSON(Define.error),
                                      Define.msg: JSON(Define.invalid_echoes)]
                
                
                sendResponse(response: response, clientSocket: clientSocket)
            }
        }
        else {
            pthread_mutex_unlock(&self.LOCK)
            
            let response :JSON = [Define.server_id: JSON(self.ID),
                                  "plataform": JSON(Define.plataform),
                                  Define.request_code: JSON(request[Define.request_code] as! Int),
                                  Define.status: JSON(Define.error),
                                  Define.msg: JSON(Define.outdated_timestamp)]
            
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
        let clientID : Int = request[Define.client_id] as! Int
        
        if !shouldEcho(variable: variable, timestamp: timestamp, client_id: clientID) {
            let response :JSON = [Define.server_id: JSON(self.ID),
                                  "plataform": JSON(Define.plataform),
                                  Define.request_code: JSON(request[Define.request_code] as! Int),
                                  Define.status: JSON(Define.error),
                                  Define.msg: JSON(Define.timestamp_already_echoed)]
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
        else {
            pthread_mutex_lock(&self.LOCK)
            self.ECHOED_VALUES.append((clientID, timestamp, variable))
            pthread_mutex_unlock(&self.LOCK)
            
            let message = variable + String(timestamp)
            //            let data_signature = signature.signData(server_id: self.ID, message: message)
            if let data_signature = signData(String(self.ID), message, Int32(message.characters.count), self.CERT_PATH) {
                let DATA_SIGNATURE = String(cString: data_signature)
                
                let dataDict : JSON = [Define.data_signature: JSON(DATA_SIGNATURE)]
                let response : JSON = [Define.server_id: JSON(self.ID),
                                       "plataform": JSON(Define.plataform),
                                       Define.request_code: JSON(request[Define.request_code] as! Int),
                                       Define.status: JSON(Define.success),
                                       Define.msg: JSON(Define.get_echoe),
                                       Define.data: dataDict]
                
                sendResponse(response: response, clientSocket: clientSocket)
            }
            else {
                let response :JSON = [Define.server_id: JSON(self.ID),
                                      "plataform": JSON(Define.plataform),
                                      Define.request_code: JSON(request[Define.request_code] as! Int),
                                      Define.status: JSON(Define.error),
                                      Define.msg: JSON(Define.unknown_error)]
                
                sendResponse(response: response, clientSocket: clientSocket)
            }
        }
    }
    
    
    /*
     Check if value was echoed before
     param: value - Variable to sign.
     param: timestamp - Timestamp.
     return: (bool) If server should echo value and timestamp
     */
    func shouldEcho(variable: String, timestamp : Int, client_id : Int) -> Bool {
        pthread_mutex_lock(&self.LOCK)
        for (clientID, time, value) in self.ECHOED_VALUES {
            if clientID == client_id {
                if time == timestamp && !(value == variable) {
                    pthread_mutex_unlock(&self.LOCK)
                    return false;
                }
                else if time == timestamp && value == variable {
                    pthread_mutex_unlock(&self.LOCK)
                    return true;
                }
            }
        }
        pthread_mutex_unlock(&self.LOCK)
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
            //if signature.verifySignature(server_id: server_id, originalMessage: data, signature: data_sign) {
            if verifySignature(NSString(string: String(server_id)).utf8String, data, Int32(data.characters.count), data_sign, self.CERT_PATH) == 1 {
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
        let dataDict : JSON = [Define.variable: JSON(self.VARIABLE),
                               Define.timestamp: JSON(self.TIMESTAMP),
                               Define.data_signature: JSON(self.DATA_SIGNATURE)]
        pthread_mutex_unlock(&self.LOCK)
        
        let response : JSON = [Define.server_id: JSON(self.ID),
                               "plataform": JSON(Define.plataform),
                               Define.request_code: JSON(request[Define.request_code] as! Int),
                               Define.status: JSON(Define.success),
                               Define.msg: JSON(Define.read),
                               Define.data: dataDict]
        
        sendResponse(response: response, clientSocket: clientSocket)
    }
    
    
    /*
     Sends timestamp in register for client.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func readTimestamp (request: Dictionary<String, Any>, clientSocket: TCPClient) {
        pthread_mutex_lock(&self.LOCK)
        let dataDict : JSON = [Define.timestamp: JSON(self.TIMESTAMP)]
        pthread_mutex_unlock(&self.LOCK)
        
        let response : JSON = [Define.server_id: JSON(self.ID),
                               "plataform": JSON(Define.plataform),
                               Define.request_code: JSON(request[Define.request_code] as! Int),
                               Define.status: JSON(Define.success),
                               Define.msg: JSON(Define.read_timestamp),
                               Define.data: dataDict]
        
        sendResponse(response: response, clientSocket: clientSocket)
    }
    
    
    /*
     Send a message to the client of the socket.
     param: responseJSON - A message in JSON format
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func sendResponse(response: JSON, clientSocket: TCPClient)
    {
        let responseJSONString = response.dump()
        
        _ = clientSocket.send(string: responseJSONString)
        
        if (self.VERBOSE == 2) {
            print ("-----REQUEST SAINDO:----- " + responseJSONString)
        }
    }
}
