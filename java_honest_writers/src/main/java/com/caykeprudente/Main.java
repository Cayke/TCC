package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static boolean DEBUG = false;
    public static int VERBOSE;

    public static void main(String[] args) {
        // write your code here
        runClient(args);
        //runServer(args);
    }

    private static void runServer(String[] args) {
        if (DEBUG) {
            int id = 4;
            Server server = new Server(id, "localhost", 5000 + id, 2);
            VERBOSE = 2;
            try {
                server.waitForConnection();
            } catch (Exception e) {
                System.out.println("Deu ruim no server: " + e.toString());
            }
        }
        else {
            if (args.length < 3) {
                System.out.println(String.format("Numero de argumentos invalidos: %d. Favor ler a documentacao", args.length));
                return;
            }
            else if (VERBOSE == 2) {
                System.out.println(args.toString());
            }

            String ip = args[0];
            int id = Double.valueOf(args[1]).intValue();
            int verbose = Integer.parseInt(args[2]);
            VERBOSE = verbose;
            int port = 5000 + id;

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
            List<Pair<String, Integer>> servers = new ArrayList<Pair<String, Integer>>();
            servers.add(new Pair<String, Integer>("node0.caykequoruns.freestore.emulab.net", 5000));
            servers.add(new Pair<String, Integer>("node1.caykequoruns.freestore.emulab.net", 5001));
            servers.add(new Pair<String, Integer>("node2.caykequoruns.freestore.emulab.net", 5002));
            servers.add(new Pair<String, Integer>("node3.caykequoruns.freestore.emulab.net", 5003));
            try {
                VERBOSE = 2;
                new RobotClient(0, servers, 2, "/OneDrive/unb/TCC/git/certs/", 100, "write", "/OneDrive/unb/TCC/git/results/");
            } catch (Exception e) {
                System.out.println("Deu ruim no client: " + e.toString());
            }
        }
        else {
            if (args.length < 9) {
                System.out.println(String.format("Numero de argumentos invalidos: %d. Favor ler a documentacao", args.length));
                return;
            }
            else if (VERBOSE == 2) {
                System.out.println(args.toString());
            }

            int id = Integer.parseInt(args[0]);
            int verbose = Integer.parseInt(args[1]);
            VERBOSE = verbose;
            String cert_path = args[2];
            int repeatOperation = Integer.parseInt(args[3]);
            String operation = args[4];
            String resultsPath = args[5];

            List<Pair<String, Integer>> servers = new ArrayList<Pair<String, Integer>>();
            int i = 6;
            while (i < args.length) {
                servers.add(new Pair<String, Integer>(args[i], Integer.parseInt(args[i+1])));
                i = i+2;
            }

            if (VERBOSE == 2) {
                System.out.println(args.toString());
            }

            try {
                new RobotClient(id, servers, verbose, cert_path, repeatOperation, operation, resultsPath);
            } catch (Exception e) {
                System.out.println("Deu ruim no client: " + e.toString());
            }
        }
    }
}
