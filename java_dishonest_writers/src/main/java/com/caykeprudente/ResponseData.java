package com.caykeprudente;

import com.google.gson.internal.LinkedTreeMap;
import com.sun.tools.javac.util.Pair;

import java.util.HashMap;
import java.util.List;

/**
 * Created by cayke on 19/03/17.
 */
public class ResponseData {
    //armazena "tuplas do python"  (value, timestamp, data_signature, server_id, server)

    /*
    param: server - Server to send the request
    param: value - Value to be written
    param: timestamp - Timestamp from value
    param: data_signature - Signature from value+timestamp
    param: server_id - Id from the server that signed the value
    */

    String value;
    int timestamp;
    String data_signature;
    int server_id;
    List<Pair<Integer, String>> echoes;
    Pair<String, Integer> server;

    int request_code;

    public ResponseData(String value, int timestamp, String data_signature, int server_id, int request_code, Pair<String, Integer> server, List<Pair<Integer, String>> echoes) {
        this.value = value;
        this.timestamp = timestamp;
        this.data_signature = data_signature;
        this.server_id = server_id;
        this.request_code = request_code;
        this.server = server;
        this.echoes = echoes;
    };

    public ResponseData(LinkedTreeMap<String, Object> dictionary, Pair<String, Integer> server) {
        LinkedTreeMap<String, Object> dataDict = (LinkedTreeMap<String, Object>) dictionary.get(Define.data);
        timestamp = ((Double) dataDict.get(Define.timestamp)).intValue();
        value = (String) dataDict.get(Define.variable);
        data_signature = (String) dataDict.get(Define.data_signature);

        server_id = ((Double) dictionary.get(Define.server_id)).intValue();
        this.server = server;
    }
}
