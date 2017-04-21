//
//  define.h
//  TCC
//
//  Created by Cayke Prudente on 23/11/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

#ifndef define_h
#define define_h

namespace Define {
    // plataform description
    const std::string plataform = "c++";
    
    const int timeout = 30;
    
    
    /*
     REQUEST (client sends to server)
     READ - {type: "read", request_code: int}
     GET_ECHOES - {type: 'get_echoes', request_code: int, variable: dict, timestamp: int}
     READ TIMESTAMP - {type: "read_timestamp", request_code: int}
     WRITE - {type: 'write', request_code: int, client_id: int, variable: dict, timestamp: int, echoes: array(server_id, data_signature)}
     CLOSE SOCKET - {type: "bye"}
     */
    const std::string type = "type";
    const std::string read = "read";
    const std::string read_timestamp = "read_timestamp";
    const std::string write = "write";
    const std::string write_back = "write_back";
    const std::string get_echoe = "get_echoe";
    const std::string variable = "variable";
    const std::string timestamp = "timestamp";
    const std::string data_signature = "data_signature";
    const std::string echoes = "echoes";
    const std::string client_id = "client_id";
    const std::string bye = "bye";
    const std::string request_code = "request_code";
    
    
    /*
     RESPONSE (server sends to client)
     BASIC STRUCTURE - {server_id: int, plataform: string, request_code: int, status: string, msg: string, data: dictionary or array}
     */
    const std::string server_id = "server_id";
    const std::string server_plataform = "plataform";
    const std::string status = "status";
    const std::string success = "success";
    const std::string error = "error";
    const std::string msg = "msg";
    const std::string data = "data";
    const std::string variable_updated = "variable_updated";
    //errors
    const std::string undefined_type = "undefined_type";
    const std::string unknown_error = "unknown_error";
    const std::string outdated_timestamp = "outdated_timestamp";
    const std::string invalid_signature = "invalid_signature";
    const std::string timestamp_already_echoed = "timestamp_already_echoed";
    const std::string invalid_echoes = "invalid_echoes";
    
    /*
     DIGITAL SIGN
     OBS: The signature is in BASE64 format (to reduce the size)
     It is signed: Variable+str(timestamp) -> data_signature
     */
}


#endif /* define_h */
