//
//  main.swift
//  tcc
//
//  Created by Cayke Prudente on 21/04/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

import Foundation

let debug = false

if debug {
    Server(id: 0, ip: "127.0.0.1", port: 5000, verbose: 2, cert_path: "/OneDrive/unb/TCC/DEV/certs/").waitForConnection();
}
else {
    let arguments = CommandLine.arguments
    if arguments.count < 5 {
        print ("Numero de argumentos inválidos")
    }
    else {
        let ip = arguments[1]
        let id = Int(arguments[2])
        let verbose = Int(arguments[3])
        let cert_path = arguments[4]
        
        Server(id: id!, ip: ip, port: 5000+id!, verbose: verbose!, cert_path: cert_path).waitForConnection();
    }
}
