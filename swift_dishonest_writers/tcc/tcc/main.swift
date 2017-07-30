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
    Server(id: 0, ip: "127.0.0.1", port: 5000).waitForConnection();
}
else {
    let arguments = CommandLine.arguments
    if arguments.count < 2 {
        print ("Numero de argumentos inválidos")
    }
    else {
        Server(id: 0, ip: arguments[1], port: 5000).waitForConnection();
    }
}
