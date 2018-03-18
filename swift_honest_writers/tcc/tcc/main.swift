//
//  main.swift
//  tcc
//
//  Created by Cayke Prudente on 06/12/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

import Foundation

let debug = true

if debug {
    let id = 1
    Server(id: id, ip: "127.0.0.1", port: 5000 + id, verbose: 2).waitForConnection();
}
else {
    let arguments = CommandLine.arguments
    if arguments.count < 4 {
        print ("Numero de argumentos inválidos")
    }
    else {
        let ip = arguments[1]
        let id = Int(arguments[2])
        let verbose = Int(arguments[3])
        
        Server(id: id!, ip: ip, port: 5000+id!, verbose: verbose!).waitForConnection();
    }
}

