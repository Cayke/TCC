package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static boolean DEBUG = true;

    public static void main(String[] args) {
        // write your code here
        //runClient();
        runServer(args);
    }

    private static void runServer(String[] args) {
        if (DEBUG) {
            Server server = new Server(0d, "localhost", 5000, 2, "/OneDrive/unb/TCC/DEV/certs/");
            try {
                server.waitForConnection();
            } catch (Exception e) {
                System.out.println("Deu ruim no server: " + e.toString());
            }
        }
        else {
            if (args.length < 4) {
                System.out.println(String.format("Numero de argumentos invalidos: %d. Favor ler a documentacao", args.length));
                return;
            }

            String ip = args[0];
            Double id = Double.valueOf(args[1]);
            int verbose = Integer.parseInt(args[2]);
            String cert_path = args[3];
            int port = 5000 + id.intValue();

            Server server = new Server(id, ip, port, verbose, cert_path);
            try {
                server.waitForConnection();
            } catch (Exception e) {
                System.out.println("Deu ruim no server: " + e.toString());
            }
        }
    }

    private static void runClient() {
        List<Pair<String, Double>> servers = new ArrayList<Pair<String, Double>>();
        servers.add(new Pair<String, Double>("node0.caykequoruns.freestore.emulab.net", 5000d));
        servers.add(new Pair<String, Double>("node1.caykequoruns.freestore.emulab.net", 5000d));
        servers.add(new Pair<String, Double>("node2.caykequoruns.freestore.emulab.net", 5000d));
        servers.add(new Pair<String, Double>("node3.caykequoruns.freestore.emulab.net", 5000d));
        new Client(0d, servers);
    }
}
