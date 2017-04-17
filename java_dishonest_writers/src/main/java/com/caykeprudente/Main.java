package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
	    // write your code here
        //runServer(0d);
        runClient();
    }

    private static void runServer(Double id) {
        Server server = new Server(id, "localhost", 5000 + id.intValue());
        try {
            server.waitForConnection();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Deu ruim na main");
        }
    }

    private static void runClient() {
        List<Pair<String, Integer>> servers = new ArrayList<Pair<String, Integer>>();
        servers.add(new Pair<String, Integer>("localhost", 5000));
        servers.add(new Pair<String, Integer>("localhost", 5001));
        servers.add(new Pair<String, Integer>("localhost", 5002));
        servers.add(new Pair<String, Integer>("localhost", 5003));
        new Client(servers);
    }
}
