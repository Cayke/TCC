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
    
    //todo
    //let LOCK : pthread_mutex_t
    
    
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
    }
    
    
    /*
     Fuction where server's main thread keeps waiting for a new connection from client.
     Starts a new thread to manipulate new connections.
     */
    func waitForConnection() {
        
    }
}

















