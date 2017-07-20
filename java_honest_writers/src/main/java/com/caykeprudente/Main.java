package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
	    // write your code here
        runClient();
        //runServer(args);
    }

    private static void runServer(String[] args) {
        if (args.length < 1) {
            System.out.println(String.format("Numero de argumentos invalidos: %d", args.length));
            return;
        }

        Server server = new Server(0, args[0], 5000);
        try {
            server.waitForConnection();
        } catch (IOException e) {
            System.out.println("Deu ruim na main");
        }
    }

    private static void runClient() {
        List<Pair<String, Integer>> servers = new ArrayList<Pair<String, Integer>>();
        servers.add(new Pair<String, Integer>("node0.caykequoruns.freestore.emulab.net", 5000));
        servers.add(new Pair<String, Integer>("node1.caykequoruns.freestore.emulab.net", 5000));
        servers.add(new Pair<String, Integer>("node2.caykequoruns.freestore.emulab.net", 5000));
        servers.add(new Pair<String, Integer>("node3.caykequoruns.freestore.emulab.net", 5000));
        new Client(0d, servers);
    }
}
