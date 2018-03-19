package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static boolean DEBUG = true;
    public static int VERBOSE;

    public static void main(String[] args) {
        // write your code here
        runClient(args);
        //runServer(args);
    }

    private static void runServer(String[] args) {
        if (DEBUG) {
            int id = 3;
            Server server = new Server(id, "localhost", 5000 + id, 2, "/OneDrive/unb/TCC/git/certs/");
            VERBOSE = 2;
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
            VERBOSE = verbose;
            String cert_path = args[3];
            int port = 5000 + id.intValue();

            Server server = new Server(id.intValue(), ip, port, verbose, cert_path);
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
            servers.add(new Pair<String, Integer>("localhost", 5000));
            servers.add(new Pair<String, Integer>("localhost", 5001));
            servers.add(new Pair<String, Integer>("localhost", 5002));
            servers.add(new Pair<String, Integer>("localhost", 5003));
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


            Double id = Double.valueOf(args[0]);
            int verbose = Integer.parseInt(args[1]);
            VERBOSE = verbose;
            String cert_path = args[2];

            List<Pair<String, Integer>> servers = new ArrayList<Pair<String, Integer>>();
            int i = 3;
            while (i < args.length) {
                servers.add(new Pair<String, Integer>(args[i], Integer.parseInt(args[i+1])));
                i = i+2;
            }

            try {
                new Client(id.intValue(), servers, verbose, cert_path);
            } catch (Exception e) {
                System.out.println("Deu ruim no client: " + e.toString());
            }
        }
    }
}
