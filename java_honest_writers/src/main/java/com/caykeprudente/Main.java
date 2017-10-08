package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static boolean DEBUG = true;

    public static void main(String[] args) {
        // write your code here
        //runClient(args);
        runServer(args);
    }

    private static void runServer(String[] args) {
        if (DEBUG) {
            Server server = new Server(0d, "localhost", 5000, 2);
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
            int port = 5000 + id.intValue();

            Server server = new Server(id, ip, port, verbose);
            try {
                server.waitForConnection();
            } catch (Exception e) {
                System.out.println("Deu ruim no server: " + e.toString());
            }
        }
    }

    private static void runClient(String[] args) {
        if (DEBUG) {
            List<Pair<String, Double>> servers = new ArrayList<Pair<String, Double>>();
            servers.add(new Pair<String, Double>("node0.caykequoruns.freestore.emulab.net", 5000d));
            servers.add(new Pair<String, Double>("node1.caykequoruns.freestore.emulab.net", 5000d));
            servers.add(new Pair<String, Double>("node2.caykequoruns.freestore.emulab.net", 5000d));
            try {
                new Client(0d, servers, 2, "/OneDrive/unb/TCC/DEV/certs/");
            } catch (Exception e) {
                System.out.println("Deu ruim no client: " + e.toString());
            }
        }
        else {
            if (args.length < 9) {
                System.out.println(String.format("Numero de argumentos invalidos: %d. Favor ler a documentacao", args.length));
                return;
            }

            Double id = Double.valueOf(args[0]);
            int verbose = Integer.parseInt(args[1]);
            String cert_path = args[2];

            List<Pair<String, Double>> servers = new ArrayList<Pair<String, Double>>();
            int i = 4;
            while (i <= args.length) {
                servers.add(new Pair<String, Double>(args[i], Double.valueOf(args[i+1])));
                i = i+2;
            }

            try {
                new Client(id, servers, verbose, cert_path);
            } catch (Exception e) {
                System.out.println("Deu ruim no client: " + e.toString());
            }
        }
    }
}
