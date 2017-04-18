//
//  main.swift
//  tcc
//
//  Created by Cayke Prudente on 06/12/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

import Foundation

//Server(id: 0, ip: "127.0.0.1", port: 5000).waitForConnection();
let signature = MySignature.signData(privateKey: MySignature.getPrivateKey(server: 0, client: -1)!, data: "{\"carrer\": \"ios dev\", \"age\": 23, \"name\": \"cayke\"}0")
print(signature)

