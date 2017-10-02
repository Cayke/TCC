package com.caykeprudente;

import com.google.gson.internal.LinkedTreeMap;
import com.sun.tools.javac.util.Pair;

import java.util.HashMap;

/**
 * Created by cayke on 19/03/17.
 */
public class ResponseData {
    //armazena "tuplas do python"  (value, timestamp, data_signature, client_id, server)

    /*
    param: server - Server to send the request
    param: value - Value to be written
    param: timestamp - Timestamp from value
    param: data_signature - Signature from value+timestamp
    param: client_id - Id from the client that created the value
    */

    String value;
    Double timestamp;
    String data_signature;
    Double client_id;
    Pair<String, Double> server;

    Double request_code;

    public ResponseData(String value, Double timestamp, String data_signature, Double client_id, Double request_code, Pair<String, Double> server) {
        this.value = value;
        this.timestamp = timestamp;
        this.data_signature = data_signature;
        this.client_id = client_id;
        this.request_code = request_code;
        this.server = server;
    };

    public ResponseData(LinkedTreeMap<String, Object> dictionary, Pair<String, Double> server) {
        timestamp = (Double) dictionary.get(Define.timestamp);
        value = (String) dictionary.get(Define.variable);
        data_signature = (String) dictionary.get(Define.data_signature);
        client_id = (Double) dictionary.get(Define.client_id);
        this.server = server;
    }
}
