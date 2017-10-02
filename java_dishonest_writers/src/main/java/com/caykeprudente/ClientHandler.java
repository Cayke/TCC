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

        request.put( "timestamp", data.timestamp.intValue());
        request.put( "variable", data.value);
        request.put( "request_code", data.request_code.intValue());
        request.put( "echoes", echoesArray);

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

        LinkedTreeMap<String, Object> messageFromServer = (LinkedTreeMap<String, Object>) Connection.read(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        String status = (String) messageFromServer.get(Define.status);
        if (status.equals(Define.success)) {
            client.lock.lock();
            client.increment_timestamp_by = 1;
            client.lock.unlock();

            client.lock_print.lock();
            System.out.println("Variable updated");
            client.lock_print.unlock();
        } else {
            client.lock_print.lock();
            System.out.println("Error updating");
            client.lock_print.unlock();
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
        request.put( "request_code", data.request_code.intValue());

        Socket clientSocket = null;
        try {
            clientSocket = new Socket(data.server.fst, data.server.snd);
            Connection.sendMessage(request, clientSocket);
        } catch (Exception e) {
            e.printStackTrace();

            client.lock_print.lock();
            System.out.println("Error reading value from server");
            client.lock_print.unlock();
        }

        LinkedTreeMap<String, Object> messageFromServer = (LinkedTreeMap<String, Object>) Connection.read(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String status = (String) messageFromServer.get(Define.status);
        Double request_code = (Double) messageFromServer.get(Define.request_code);

        if (status.equals(Define.success) && request_code.equals(data.request_code)) {
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
                client.lock_print.lock();
                System.out.println("Quorum ja encheu. Jogando request fora...");
                client.lock_print.unlock();
            }
            client.lock.unlock();
        }
        else if (status.equals(Define.error)) {
            client.lock_print.lock();
            System.out.println("Ocorreu algum erro na request");
            client.lock_print.unlock();
        }
        else if (!request_code.equals(data.request_code)) {
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
        request.put( "request_code", data.request_code.intValue());

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

        LinkedTreeMap<String, Object> messageFromServer = (LinkedTreeMap<String, Object>) Connection.read(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String status = (String) messageFromServer.get(Define.status);
        Double request_code = (Double) messageFromServer.get(Define.request_code);

        if (status.equals(Define.success) && request_code.equals(data.request_code)) {
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
                client.lock_print.lock();
                System.out.println("Quorum ja encheu. Jogando request fora...");
                client.lock_print.unlock();
            }
            client.lock.unlock();
        }
        else if (status.equals(Define.error)) {
            client.lock_print.lock();
            System.out.println("Ocorreu algum erro na request");
            client.lock_print.unlock();
        }
        else if (!request_code.equals(data.request_code)) {
            client.lock_print.lock();
            System.out.println("Response atrasada");
            client.lock_print.unlock();
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
        request.put( "request_code", data.request_code.intValue());
        request.put( "variable", data.value);
        request.put( "timestamp", data.timestamp.intValue());


        Socket clientSocket = null;
        try {
            clientSocket = new Socket(data.server.fst, data.server.snd);
            Connection.sendMessage(request, clientSocket);
        } catch (Exception e) {
            e.printStackTrace();

            client.lock_print.lock();
            System.out.println("Error reading value from server");
            client.lock_print.unlock();
        }

        LinkedTreeMap<String, Object> messageFromServer = (LinkedTreeMap<String, Object>) Connection.read(clientSocket);
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String status = (String) messageFromServer.get(Define.status);
        Double request_code = (Double) messageFromServer.get(Define.request_code);
        String msg = (String) messageFromServer.get(Define.msg);

        if (status.equals(Define.success) && request_code.equals(data.request_code)) {
            ResponseData responseData = new ResponseData(messageFromServer, data.server);

            client.lock.lock();
            if (client.echoes.size() < client.quorum-1) {
                client.echoes.add(new Pair<Integer, String>(responseData.server_id.intValue(), responseData.data_signature));
            }
            else if (client.echoes.size() == client.quorum-1) {
                client.echoes.add(new Pair<Integer, String>(responseData.server_id.intValue(), responseData.data_signature));
                client.semaphore.release();
            }
            else {
                client.lock_print.lock();
                System.out.println("Quorum ja encheu. Jogando request fora...");
                client.lock_print.unlock();
            }
            client.lock.unlock();
        }
        else if (status.equals(Define.error) && msg.equals(Define.timestamp_already_echoed)) {
            client.lock.lock();
            client.timestamp_already_echoed_by_any_server = true;

            Double server_id = (Double) messageFromServer.get(Define.server_id);
            if (client.echoes.size() < client.quorum - 1) {
                client.echoes.add(new Pair<Integer, String>(server_id.intValue(), ""));
            }
            else if (client.echoes.size() == client.quorum - 1) {
                client.echoes.add(new Pair<Integer, String>(server_id.intValue(), ""));
                client.semaphore.release();
            }
            else {
                client.lock_print.lock();
                System.out.println("Quorum ja encheu. Jogando request fora...");
                client.lock_print.unlock();
            }
            client.lock.unlock();
        }
        else if (status.equals(Define.error)) {
            client.lock_print.lock();
            System.out.println("Ocorreu algum erro na request");
            client.lock_print.unlock();
        }
        else if (!request_code.equals(data.request_code)) {
            client.lock_print.lock();
            System.out.println("Response atrasada");
            client.lock_print.unlock();
        }
    }
}