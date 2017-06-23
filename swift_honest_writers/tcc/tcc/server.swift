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
    
    var VARIABLE = "";
    var TIMESTAMP = -1;
    var DATA_SIGNATURE = "";
    var CLIENT_ID = -1;
    
    var LOCK = pthread_mutex_t();
    
    
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
                    let thread = Thread(block: {
                        self.clientConnected(clientSocket: clientSocket)
                    })
                    thread.start()
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
    }
    
    
    /*
     Analyse user's message and forwards to the correct handler.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func getRequestStatus(request: Dictionary<String, Any>, clientSocket: TCPClient) {
        let type : String = request[Define.type] as! String
        
        if type == Define.write {
            self.write(request: request, clientSocket: clientSocket)
        }
        else if type == Define.read {
            self.read(request: request, clientSocket: clientSocket)
        }
        else if type == Define.read_timestamp {
            self.readTimestamp(request: request, clientSocket: clientSocket)
        }
        else if type == Define.bye {
            clientSocket.close()
            print("cliente desconectou propositalmente")
            return
        }
        else {
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code],
                            Define.status: Define.error,
                            Define.msg: Define.undefined_type]
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
    }
    
    
    /*
     Write data in register if the requirements are followed.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func write (request: Dictionary<String, Any>, clientSocket: TCPClient) {
        let variable : String = request[Define.variable] as! String
        let timestamp : Int = request[Define.timestamp] as! Int
        let signature : String = request[Define.data_signature] as! String
        let client_id : Int = request[Define.client_id] as! Int
        
        pthread_mutex_lock(&self.LOCK)
        if timestamp > self.TIMESTAMP {
            print ("Recebido variable = \(variable) e timestamp = \(timestamp)")
            
            self.VARIABLE = variable
            self.TIMESTAMP = timestamp
            self.DATA_SIGNATURE = signature
            self.CLIENT_ID = client_id
            
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code],
                            Define.status: Define.success,
                            Define.msg: Define.variable_updated]
            pthread_mutex_unlock(&self.LOCK)
            
            sendResponse(response: response, clientSocket: clientSocket)
        }
        else {
            pthread_mutex_unlock(&self.LOCK)
            
            let response = [Define.server_id: self.ID,
                            "plataform": Define.plataform,
                            Define.request_code: request[Define.request_code],
                            Define.status: Define.error,
                            Define.msg: Define.outdated_timestamp]
            
            sendResponse(response: response, clientSocket: clientSocket)
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
                        Define.data_signature: self.DATA_SIGNATURE,
                        Define.client_id: self.CLIENT_ID] as [String : Any]
        let response = [Define.server_id: self.ID,
                        "plataform": Define.plataform,
                        Define.request_code: request[Define.request_code],
                        Define.status: Define.success,
                        Define.msg: Define.read,
                        Define.data: dataDict]
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
                        Define.request_code: request[Define.request_code],
                        Define.status: Define.success,
                        Define.msg: Define.read,
                        Define.data: dataDict]
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
            
            clientSocket.send(data: responseJSON)
        }
        catch {
            print (error.localizedDescription)
        }
    }
}
