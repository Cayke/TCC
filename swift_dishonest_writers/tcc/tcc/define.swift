//
//  define.swift
//  tcc
//
//  Created by Cayke Prudente on 06/12/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

import Foundation

struct Define {
    
    // plataform description
    static let plataform = "swift";
    
    static let timeout = 30;
    
    
    /*
     REQUEST (client sends to server)
     READ - {type: "read", request_code: int}
     READ TIMESTAMP - {type: "read_timestamp", request_code: int}
     WRITE - {type: "write", request_code: int, client_id: int, variable: dict, timestamp: int, data_signature: string}
     CLOSE SOCKET - {type: "bye"}
     */
    static let type = "type";
    static let read = "read";
    static let read_timestamp = "read_timestamp";
    static let write = "write";
    static let variable = "variable";
    static let timestamp = "timestamp";
    static let data_signature = "data_signature";
    static let client_id = "client_id";
    static let bye = "bye";
    static let request_code = "request_code";
    
    
    /*
     RESPONSE (server sends to client)
     BASIC STRUCTURE - {server_id: int, plataform: string, request_code: int, status: string, msg = string, data = dictionary or array}
     */
    static let server_id = "server_id";
    static let server_plataform = "plataform";
    static let status = "status";
    static let success = "success";
    static let error = "error";
    static let msg = "msg";
    static let data = "data";
    static let variable_updated = "variable_updated";
    //errors
    static let undefined_type = "undefined_type";
    static let unknown_error = "unknown_error";
    static let outdated_timestamp = "outdated_timestamp";
    static let invalid_signature = "invalid_signature";
    
    /*
     DIGITAL SIGN
     OBS: The signature is in BASE64 format (to reduce the size)
     It is signed: Variable+str(timestamp) -> data_signature
     */
}
