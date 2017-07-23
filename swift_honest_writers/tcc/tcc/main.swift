//
//  main.swift
//  tcc
//
//  Created by Cayke Prudente on 06/12/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

import Foundation

let arguments = CommandLine.arguments
if arguments.count < 2 {
    print ("Numero de argumentos inválidos")
}
else {
    Server(id: 0, ip: arguments[1], port: 5000).waitForConnection();
}
