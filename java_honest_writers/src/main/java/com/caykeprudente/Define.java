package com.caykeprudente;

/**
 * Created by cayke on 11/11/16.
 */
public class Define {
    // plataform description
    public static final String plataform = "java";

    public static final int timeout = 30;


    /*
    REQUEST (client sends to server)
    READ - {type: "read", request_code: int}
    READ TIMESTAMP - {type: "read_timestamp", request_code: int}
    WRITE - {type: "write", request_code: int, client_id: int, variable: dict, timestamp: int, data_signature: string}
    CLOSE SOCKET - {type: "bye"}
    */
    public static final String type = "type";
    public static final String read = "read";
    public static final String read_timestamp = "read_timestamp";
    public static final String write = "write";
    public static final String variable = "variable";
    public static final String timestamp = "timestamp";
    public static final String data_signature = "data_signature";
    public static final String client_id = "client_id";
    public static final String bye = "bye";
    public static final String request_code = "request_code";


    /*
    RESPONSE (server sends to client)
    BASIC STRUCTURE - {server_id: int, plataform: string, request_code: int, status: string, msg = string, data = dictionary or array}
    */
    public static final String server_id = "server_id";
    public static final String server_plataform = "plataform";
    public static final String status = "status";
    public static final String success = "success";
    public static final String error = "error";
    public static final String msg = "msg";
    public static final String data = "data";
    public static final String variable_updated = "variable_updated";
    //errors
    public static final String undefined_type = "undefined_type";
    public static final String unknown_error = "unknown_error";
    public static final String outdated_timestamp = "outdated_timestamp";
    public static final String invalid_signature = "invalid_signature";

    /*
    DIGITAL SIGN
    OBS: The signature is in BASE64 format (to reduce the size)
    It is signed: Variable+str(timestamp) -> data_signature
    */

    public static final String execution_file_header = "number_of_executions;average_time;initial_time;final_time";
}
