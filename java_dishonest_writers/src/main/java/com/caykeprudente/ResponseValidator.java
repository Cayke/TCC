package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.util.List;

/**
 * Created by cayke on 16/04/17.
 */
public class ResponseValidator {
    //armazena "tuplas do python" (repeatTimes, value, timestamp, [(server_id, data_sign)], [server])
    int repeatTimes;
    String value;
    int timestamp;
    List<Pair<Integer, String>> echoes;
    List<Pair<String, Integer>> servers;

    public ResponseValidator(int repeatTimes, String value, int timestamp, List<Pair<Integer, String>> echoes, List<Pair<String, Integer>> servers) {
        this.repeatTimes = repeatTimes;
        this.value = value;
        this.timestamp = timestamp;
        this.echoes = echoes;
        this.servers = servers;
    }
}