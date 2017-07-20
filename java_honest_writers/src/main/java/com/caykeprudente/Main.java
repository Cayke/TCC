package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
	    // write your code here
        //runClient();
        runServer(args);
    }

    private static void runServer(String[] args) {
        if (args.length < 2) {
            System.out.println("Numero de argumentos invalidos");
            return;
        }

        Server server = new Server(0, args[1], 5000);
        try {
            server.waitForConnection();
        } catch (IOException e) {
            System.out.println("Deu ruim na main");
        }
    }

    private static void runClient() {
        List<Pair<String, Integer>> servers = new ArrayList<Pair<String, Integer>>();
        servers.add(new Pair<String, Integer>("localhost", 5000));
        servers.add(new Pair<String, Integer>("localhost", 5001));
        servers.add(new Pair<String, Integer>("localhost", 5002));
        new Client(0d, servers);
    }
}
