package com.caykeprudente;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
	    // write your code here
        runServer();
    }

    private static void runServer() {
        Server server = new Server(0, "localhost", 5000);
        try {
            server.waitForConnection();
        } catch (IOException e) {
            System.out.println("Deu ruim na main");
        }
    }
}
