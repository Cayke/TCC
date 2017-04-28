package com.caykeprudente;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.sun.tools.javac.util.Pair;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

/**
 * Created by cayke on 11/11/16.
 */
public class ServerHandler implements Runnable {
    private Socket socket;
    private Server server;

    /*
    Server's  messages handler constructor.
    param: server - Server main class
    param: socket - Socket that has been created for the pair (Server, Client)
    */
    public ServerHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
//        try {
//            this.socket.setTcpNoDelay(true);
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
    }


    /*
    Waits for client's message.
    */
    @Override
    public void run() {
        System.out.println("Novo cliente conectado, nova thread criada");

        Boolean success = false;

        String jsonMessage = readInputStream();
        if (jsonMessage != null) {
            HashMap<String, Object> request = new GsonBuilder().create().fromJson(jsonMessage, HashMap.class);

            success = getRequestStatus(request);
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (success)
            System.out.println("matando thread, operacao feita com sucesso");
        else
            System.out.println("matando thread, operacao nao obteve sucesso");
    }


    /*
    Read message that is on the socket right now
    return: (String) Message from client; null if error.
    */
    private String readInputStream() {
        try {
            int read = -1;
            byte[] buffer = new byte[2048];
            byte[] readData;
            String readDataText;

            read = socket.getInputStream().read(buffer);
            readData = new byte[read];
            System.arraycopy(buffer, 0, readData, 0, read);
            readDataText = new String(readData, "UTF-8"); // assumption that client sends data UTF-8 encoded
            //System.out.println("message part recieved:" + readDataText);
            return readDataText;
        } catch (Exception e) {
            return null;
        }
    }


    /*
    Analyse user's message and forwards to the correct method.
    param: request - A dictionary with client's request data.
    return: (boolean) True for success, False for error.
    */
    private boolean getRequestStatus(HashMap<String, Object> request) {
        String type = (String) request.get(Define.type);
        if (type.equals(Define.write) || type.equals(Define.write_back))
            return write(request, type);

        else if (type.equals(Define.read))
            return read(request);

        else if (type.equals(Define.read_timestamp))
            return readTimestamp(request);

        else if (type.equals(Define.get_echoe))
            return getEchoe(request);

        else {
            HashMap<String, Object> dictionary = new HashMap<String, Object>();
            dictionary.put(Define.server_id, server.id.intValue());
            dictionary.put("plataform", Define.plataform);
            dictionary.put(Define.request_code, request.get(Define.request_code));
            dictionary.put(Define.status, Define.error);
            dictionary.put(Define.msg, Define.undefined_type);

            try {
                sendMessageToClient(dictionary);
            } catch (IOException e) {
                return false;
            }
            return true;
        }
    }

    /*
    Sends response to client.
    param: response - A dictionary with serve's response data.
    */
    private void sendMessageToClient(HashMap response) throws IOException {
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        dOut.write(new GsonBuilder().create().toJson(response).getBytes());
        dOut.flush(); // Send the data
        dOut.close();
    }


    /*
    Sends data in register for client.
    param: request - A dictionary with client's request data.
    */
    private boolean read(HashMap request) {
        server.lock.lock();
        HashMap<String, Object> dataDict = new HashMap<String, Object>();
        dataDict.put(Define.variable, server.variable);
        dataDict.put(Define.timestamp, server.timestamp);
        dataDict.put(Define.data_signature, server.data_signature);
        server.lock.unlock();

        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put(Define.server_id, server.id.intValue());
        response.put("plataform", Define.plataform);
        response.put(Define.request_code, request.get(Define.request_code));
        response.put(Define.status, Define.success);
        response.put(Define.msg, Define.read);
        response.put(Define.data, dataDict);


        try {
            sendMessageToClient(response);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    /*
    Sends timestamp in register for client.
    param: request - A dictionary with client's request data.
    */
    private boolean readTimestamp(HashMap request) {
        server.lock.lock();
        HashMap<String, Object> dataDict = new HashMap<String, Object>();
        dataDict.put(Define.timestamp, server.timestamp);
        server.lock.unlock();

        HashMap<String, Object> response = new HashMap<String, Object>();
            response.put(Define.server_id, server.id.intValue());
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.success);
            response.put(Define.msg, Define.read_timestamp);
            response.put(Define.data, dataDict);

        try {
            sendMessageToClient(response);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    /*
    Write data in register if the requirements are followed.
    param: request - A dictionary with client's request data.
    */
    private boolean write(HashMap request, String type) {
        String variable = (String)request.get(Define.variable);
        int timestamp = ((Double) request.get(Define.timestamp)).intValue();

        List<LinkedTreeMap<String, Object>> echoesArray = (List<LinkedTreeMap<String, Object>>) request.get(Define.echoes);
        List<Pair<Double, String>> echoes = new ArrayList<Pair<Double, String>>();
        for (LinkedTreeMap<String, Object> dict : echoesArray) {
            echoes.add(new Pair<Double, String>((Double) dict.get(Define.server_id), (String) dict.get(Define.data_signature)));
        }

        server.lock.lock();
        if (timestamp > server.timestamp)
        {
            if (isEchoValid(echoes, variable, timestamp, type)) {
                server.variable = variable;
                server.timestamp = timestamp;
                server.data_signature = MySignature.signData(MySignature.getPrivateKey(server.id, -1d), variable+timestamp);
                server.last_echoed_values = new ArrayList<Pair<Integer, String>>();

                server.lock.unlock();

                System.out.println("Recebido variable = " + variable + " e timestamp " + timestamp);

                HashMap<String, Object> response = new HashMap<String, Object>();
                response.put(Define.server_id, server.id.intValue());
                response.put("plataform", Define.plataform);
                response.put(Define.request_code, request.get(Define.request_code));
                response.put(Define.status, Define.success);
                response.put(Define.msg, Define.variable_updated);

                try {
                    sendMessageToClient(response);
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
            else {
                server.lock.unlock();

                HashMap<String, Object> response = new HashMap<String, Object>();
                response.put(Define.server_id, server.id.intValue());
                response.put("plataform", Define.plataform);
                response.put(Define.request_code, request.get(Define.request_code));
                response.put(Define.status, Define.error);
                response.put(Define.msg, Define.invalid_echoes);

                try {
                    sendMessageToClient(response);
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }
        else
        {
            server.lock.unlock();

            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put(Define.server_id, server.id.intValue());
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.error);
            response.put(Define.msg, Define.outdated_timestamp);

            try {
                sendMessageToClient(response);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }


    /*
    Sends echo for timestamp and value
    param: request - A dictionary with client's request data.
    */
    public boolean getEchoe(HashMap request) {
        String variable = (String)request.get(Define.variable);
        int timestamp = ((Double) request.get(Define.timestamp)).intValue();

        HashMap<String, Object> response = new HashMap<String, Object>();
        if (timestamp < server.timestamp) {
            response.put(Define.server_id, server.id.intValue());
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.error);
            response.put(Define.msg, Define.outdated_timestamp);
        }
        else if (!shouldEcho(variable, timestamp)) {
            response.put(Define.server_id, server.id.intValue());
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.error);
            response.put(Define.msg, Define.timestamp_already_echoed);
        }
        else {
            String data_signature = MySignature.signData(MySignature.getPrivateKey(server.id, -1d), variable+timestamp);

            server.lock.lock();
            server.last_echoed_values.add(new Pair<Integer, String>(timestamp, variable));
            server.lock.unlock();

            HashMap<String, Object> dataDict = new HashMap<String, Object>();
            dataDict.put(Define.data_signature, data_signature);

            response.put(Define.server_id, server.id.intValue());
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.success);
            response.put(Define.msg, Define.read);
            response.put(Define.data, dataDict);
        }

        try {
            sendMessageToClient(response);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    /*
    Check if value was echoed before
    param: value - Variable to sign.
    param: timestamp - Timestamp.
    return: (bool) If server should echo value and timestamp
     */
    public boolean shouldEcho(String value, int timestamp) {
        server.lock.lock();
        for (Pair<Integer, String> pair : server.last_echoed_values) {
            if (pair.fst == timestamp && !pair.snd.equals(value)) {
                server.lock.unlock();
                return false;
            }
            else if (pair.fst == timestamp && pair.snd.equals(value)) {
                server.lock.unlock();
                return true;
            }
        }
        server.lock.unlock();
        return true;
    }


    /*
    Check if echoes are valid
    param: echoes - Array with tuples(server_id, data_signature)
    param: value - Variable to sign.
    param: timestamp - Timestamp.
    param: type - If is a write or write_back
    return: (bool) If echoes are valid
     */
    public boolean isEchoValid(List<Pair<Double, String>> echoes, String value, int timestamp, String type) {
        int validEchoes = 0;
        for (Pair<Double, String> echo: echoes) {
            if (MySignature.verifySign(MySignature.getPublicKey(echo.fst, -1d), echo.snd, value+timestamp))
                validEchoes++;
        }

        if (type.equals(Define.write))
            return validEchoes >= server.quorum;
        else //write_back
            return validEchoes >= server.faults + 1;

    }
}