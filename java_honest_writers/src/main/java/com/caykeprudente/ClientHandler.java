package com.caykeprudente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
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

    private RobotClient client;
    private Function function;
    private ResponseData data;

    /*
    Client's  messages handler constructor.
    param: server - Client main class
    */
    public ClientHandler(RobotClient client, Function function, ResponseData data) {
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

            if (this.client.verbose > 0)
                System.out.println("Error updating");
        }

        LinkedTreeMap<String, Object> messageFromServer = (LinkedTreeMap<String, Object>) Connection.read(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String status = (String) messageFromServer.get(Define.status);
        String msg = (String) messageFromServer.get(Define.msg);
        if (status.equals(Define.success)) {
            if (this.client.verbose > 0)
                System.out.println("Variable updated");
        }
        else if (status.equals(Define.error) && msg.equals(Define.outdated_timestamp)) {
            if (this.client.verbose > 0)
                System.out.println("Tried to write, but there is a newest data already");
        }
        else {
            if (this.client.verbose > 0)
                System.out.println("Error updating");
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
        } catch (Exception e) {
            e.printStackTrace();

            if (this.client.verbose > 0)
                System.out.println("Error reading value from server");
        }

        LinkedTreeMap<String, Object> messageFromServer = (LinkedTreeMap<String, Object>) Connection.read(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String status = (String) messageFromServer.get(Define.status);
        int request_code = ((Double) messageFromServer.get(Define.request_code)).intValue();

        if (request_code != data.request_code) {
            if (this.client.verbose > 0)
                System.out.println("Response atrasada");
        }
        else if (status.equals(Define.success)) {
            LinkedTreeMap<String, Object> dataDict = (LinkedTreeMap<String, Object>) messageFromServer.get(Define.data);
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
                if (this.client.verbose > 0)
                    System.out.println("Quorum ja encheu. Jogando request fora...");
            }
            client.lock.unlock();
        }
        else if (status.equals(Define.error)) {
            if (this.client.verbose > 0)
                System.out.println("Ocorreu algum erro na request");
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

            if (this.client.verbose > 0)
                System.out.println("Error reading value from server");
        }

        LinkedTreeMap<String, Object> messageFromServer = (LinkedTreeMap<String, Object>) Connection.read(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String status = (String) messageFromServer.get(Define.status);
        int request_code = ((Double) messageFromServer.get(Define.request_code)).intValue();

        if (request_code != data.request_code) {
            if (this.client.verbose > 0)
                System.out.println("Response atrasada");
        }
        else if (status.equals(Define.success)) {
            LinkedTreeMap<String, Object> dataDict = (LinkedTreeMap<String, Object>) messageFromServer.get(Define.data);
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
                if (this.client.verbose > 0)
                    System.out.println("Quorum ja encheu. Jogando request fora...");
            }
            client.lock.unlock();
        }
        else if (status.equals(Define.error)) {
            if (this.client.verbose > 0)
                System.out.println("Ocorreu algum erro na request");
        }
    }
}