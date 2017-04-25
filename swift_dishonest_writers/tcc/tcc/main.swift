//
//  main.swift
//  tcc
//
//  Created by Cayke Prudente on 21/04/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

import Foundation

let id = 0
Server(id: id, ip: "127.0.0.1", port: 5000 + id).waitForConnection()
