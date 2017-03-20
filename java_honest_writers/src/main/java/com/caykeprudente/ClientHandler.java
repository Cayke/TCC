package com.caykeprudente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.tools.javac.util.Pair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.SocketHandler;

/**
 * Created by cayke on 19/03/17.
 */
public class ClientHandler implements Runnable {
    enum Function {
        write, read, readTimestamp
    }

    private Client client;
    private Function function;
    private ResponseData data;

    /*
    Client's  messages handler constructor.
    param: server - Client main class
    */
    public ClientHandler(Client client, Function function, ResponseData data) {
        this.client = client;
        this.function = function;
        this.data = data;
    }


    /*
    Sends the client's message.
    */
    @Override
    public void run() {
        if (function == Function.write)
            writeOnServer();
        else if (function == Function.read)
            readFromServer();
        else if (function == Function.readTimestamp)
            readTimestampFromServer();
    }


    /*
    Sends the data to be written in a specific server.
    param: server - Server to send the request
    param: value - Value to be written
    param: timestamp - Timestamp from value
    param: data_signature - Signature from value+timestamp
    param: client_id - Id from the client that created the value
    param: request_code - Request ID identifier
    */
    private void writeOnServer() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put( "type", Define.write);
        request.put( "timestamp", data.timestamp);
        request.put( "variable", data.value);
        request.put( "request_code", data.request_code);
        request.put( "client_id", data.client_id);
        request.put( "data_signature", data.data_signature);

        Socket clientSocket = null;
        try {
            clientSocket = new Socket(data.server.fst, data.server.snd);
            Connection.sendMessage(request, clientSocket);
        } catch (IOException e) {
            e.printStackTrace();

            client.lock_print.lock();
            System.out.println("Error updating");
            client.lock_print.unlock();
        }

        HashMap<String, Object> messageFromServer = Connection.read(clientSocket);
        try {
            if (messageFromServer.get(Define.status).equals(Define.success)) {
                client.lock_print.lock();
                System.out.println("Variable updated");
                client.lock_print.unlock();
            } else {
                client.lock_print.lock();
                System.out.println("Error updating");
                client.lock_print.unlock();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("No response from server - writeOnServer method");
        }
    }

    /*
    Gets the data from a server and append in the responses array if possible
    param: server - Server to send the request
    param: request_code - Request ID identifier
    */
    private void readFromServer() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put( "type", Define.read);
        request.put( "request_code", data.request_code);
        request.put( "client_id", client.id);

        Socket clientSocket = null;
        try {
            clientSocket = new Socket(data.server.fst, data.server.snd);
            Connection.sendMessage(request, clientSocket);
        } catch (IOException e) {
            e.printStackTrace();

            client.lock_print.lock();
            System.out.println("Error reading value from server");
            client.lock_print.unlock();
        }

        HashMap<String, Object> messageFromServer = Connection.read(clientSocket);

        if (messageFromServer.get(Define.status).equals(Define.success)
                && ((Integer) messageFromServer.get(Define.request_code)) == data.request_code) {
            HashMap<String, Object> dataDict = (HashMap<String, Object>) messageFromServer.get(Define.data);
            ResponseData responseData = new ResponseData(dataDict, data.server);

            client.lock.lock();
            if (client.responses.size() < client.quorum-1) {
                client.responses.add(responseData);
            }
            else if (client.responses.size() == client.quorum-1) {
                client.responses.add(responseData);
                client.semaphore.release();
            }
            else {
                client.lock_print.lock();
                System.out.println("Quorum ja encheu. Jogando request fora...");
                client.lock_print.unlock();
            }
            client.lock.unlock();
        }
        else if (messageFromServer.get(Define.status).equals(Define.error)) {
            client.lock_print.lock();
            System.out.println("Ocorreu algum erro na request");
            client.lock_print.unlock();
        }
        else if (((Integer) messageFromServer.get(Define.request_code)) != data.request_code) {
            client.lock_print.lock();
            System.out.println("Response atrasada");
            client.lock_print.unlock();
        }
    }

    /*
    Gets the timestamp from a server and append in the responses array if possible
    param: server - Server to send the request
    param: request_code - Request ID identifier
     */
    private void readTimestampFromServer() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put( "type", Define.read_timestamp);
        request.put( "request_code", data.request_code);
        request.put( "client_id", client.id);

        Socket clientSocket = null;
        try {
            clientSocket = new Socket(data.server.fst, data.server.snd);
            Connection.sendMessage(request, clientSocket);
        } catch (IOException e) {
            e.printStackTrace();

            client.lock_print.lock();
            System.out.println("Error reading value from server");
            client.lock_print.unlock();
        }

        HashMap<String, Object> messageFromServer = Connection.read(clientSocket);

        if (messageFromServer.get(Define.status).equals(Define.success)
                && ((Integer) messageFromServer.get(Define.request_code)) == data.request_code) {
            HashMap<String, Object> dataDict = (HashMap<String, Object>) messageFromServer.get(Define.data);
            ResponseData responseData = new ResponseData(dataDict, data.server);

            client.lock.lock();
            if (client.responses.size() < client.quorum-1) {
                client.responses.add(responseData);
            }
            else if (client.responses.size() == client.quorum-1) {
                client.responses.add(responseData);
                client.semaphore.release();
            }
            else {
                client.lock_print.lock();
                System.out.println("Quorum ja encheu. Jogando request fora...");
                client.lock_print.unlock();
            }
            client.lock.unlock();
        }
        else if (messageFromServer.get(Define.status).equals(Define.error)) {
            client.lock_print.lock();
            System.out.println("Ocorreu algum erro na request");
            client.lock_print.unlock();
        }
        else if (((Integer) messageFromServer.get(Define.request_code)) != data.request_code) {
            client.lock_print.lock();
            System.out.println("Response atrasada");
            client.lock_print.unlock();
        }
    }
}