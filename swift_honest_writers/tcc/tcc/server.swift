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
        
        clientSocket.close()
        print("matando thread")
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
            
            let response :JSON = [Define.server_id: JSON(self.ID),
                            "plataform": JSON(Define.plataform),
                            Define.request_code: JSON(request[Define.request_code] as! Int),
                            Define.status: JSON(Define.success),
                            Define.msg: JSON(Define.variable_updated)]
            pthread_mutex_unlock(&self.LOCK)
            
            sendResponse(response: response, clientSocket: clientSocket)
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
     Sends data in register for client.
     param: request - A dictionary with client's request data.
     param: socketTCP - Socket that has been created for the pair (Server, Client)
     */
    func read (request: Dictionary<String, Any>, clientSocket: TCPClient) {
        pthread_mutex_lock(&self.LOCK)
        let dataDict : JSON = [Define.variable: JSON(self.VARIABLE),
                        Define.timestamp: JSON(self.TIMESTAMP),
                        Define.data_signature: JSON(self.DATA_SIGNATURE),
                        Define.client_id: JSON(self.CLIENT_ID)]
        let response : JSON = [Define.server_id: JSON(self.ID),
                        "plataform": JSON(Define.plataform),
                        Define.request_code: JSON(request[Define.request_code] as! Int),
                        Define.status: JSON(Define.success),
                        Define.msg: JSON(Define.read),
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
        let dataDict : JSON = [Define.timestamp: JSON(self.TIMESTAMP)]
        let response : JSON = [Define.server_id: JSON(self.ID),
                        "plataform": JSON(Define.plataform),
                        Define.request_code: JSON(request[Define.request_code] as! Int),
                        Define.status: JSON(Define.success),
                        Define.msg: JSON(Define.read_timestamp),
                        Define.data: dataDict]
        pthread_mutex_unlock(&self.LOCK)
        
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
    }
}
