package com.caykeprudente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.sun.tools.javac.util.Pair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.SocketHandler;

/**
 * Created by cayke on 19/03/17.
 */
public class ClientHandler implements Runnable {
    enum Function {
        write, write_back, read, readTimestamp, readEchoeFromServer
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
        if (function == Function.write || function == Function.write_back)
            writeOnServer(function);
        else if (function == Function.read)
            readFromServer();
        else if (function == Function.readTimestamp)
            readTimestampFromServer();
        else if (function == Function.readEchoeFromServer)
            readEchoeFromServer();
    }


    /*
    Sends the data to be written in a specific server.
    param: server - Server to send the request
    param: value - Value to be written
    param: timestamp - Timestamp from value
    param: echos - Signatures for value+timestamp from servers
    param: request_code - Request ID identifier
    */
    private void writeOnServer(Function type) {
        List<Map<String, Object>> echoesArray = new ArrayList<Map<String, Object>>();
        for (Pair<Integer, String> echoe : data.echoes) {
            Map<String, Object> echoeDict = new HashMap<String, Object>();
            echoeDict.put("server_id", echoe.fst);
            echoeDict.put("data_signature", echoe.snd);
            echoesArray.add(echoeDict);
        }

        Map<String, Object> request = new HashMap<String, Object>();
        if (type == Function.write)
            request.put( "type", Define.write);
        else
            request.put( "type", Define.write_back);

        request.put( "timestamp", data.timestamp);
        request.put( "variable", data.value);
        request.put( "request_code", data.request_code);
        request.put( "echoes", echoesArray);
        injectClientInfo(request);

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
            client.lock.lock();
            client.increment_timestamp_by = 1;
            client.lock.unlock();

            if (this.client.verbose > 0)
                System.out.println("Variable updated");
        }
        else if (status.equals(Define.error) && msg.equals(Define.outdated_timestamp)) {
            client.lock.lock();
            client.increment_timestamp_by = 1;
            client.lock.unlock();

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
        injectClientInfo(request);

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
            ResponseData responseData = new ResponseData(messageFromServer, data.server);

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
        injectClientInfo(request);

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
            ResponseData responseData = new ResponseData(messageFromServer, data.server);

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
    Gets the echoe from a server and append in the echoes array if possible
    param: server - Server to send the request
    param: request_code - Request ID identifier
     */
    private void readEchoeFromServer() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put( "type", Define.get_echoe);
        request.put( "request_code", data.request_code);
        request.put( "variable", data.value);
        request.put( "timestamp", data.timestamp);
        injectClientInfo(request);

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
        String msg = (String) messageFromServer.get(Define.msg);

        if (request_code!=data.request_code) {
            if (this.client.verbose > 0)
                System.out.println("Response atrasada");
        }
        else if (status.equals(Define.success)) {
            ResponseData responseData = new ResponseData(messageFromServer, data.server);

            client.lock.lock();
            if (client.echoes.size() < client.quorum-1) {
                client.echoes.add(new Pair<Integer, String>(responseData.server_id, responseData.data_signature));
            }
            else if (client.echoes.size() == client.quorum-1) {
                client.echoes.add(new Pair<Integer, String>(responseData.server_id, responseData.data_signature));
                client.semaphore.release();
            }
            else {
                if (this.client.verbose > 0)
                    System.out.println("Quorum ja encheu. Jogando request fora...");
                
            }
            client.lock.unlock();
        }
        else if (status.equals(Define.error) && msg.equals(Define.timestamp_already_echoed)) {
            client.lock.lock();
            client.timestamp_already_echoed_by_any_server = true;

            int server_id = ((Double) messageFromServer.get(Define.server_id)).intValue();
            if (client.echoes.size() < client.quorum - 1) {
                client.echoes.add(new Pair<Integer, String>(server_id, ""));
            }
            else if (client.echoes.size() == client.quorum - 1) {
                client.echoes.add(new Pair<Integer, String>(server_id, ""));
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

    private void injectClientInfo(Map<String, Object> request) {
        request.put(Define.client_id, this.client.id);
        request.put(Define.server_plataform, Define.plataform);
    }
}