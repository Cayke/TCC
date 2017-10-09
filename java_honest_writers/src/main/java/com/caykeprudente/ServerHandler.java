package com.caykeprudente;

import com.google.gson.GsonBuilder;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Deque;
import java.util.HashMap;

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
        if (this.server.verbose > 0)
            System.out.println("Novo cliente conectado, nova thread criada");

        boolean shouldContinue = true;
        while (shouldContinue) {
            String jsonMessage = readInputStream();
            if (jsonMessage != null) {
                HashMap<String, Object> request = new GsonBuilder().create().fromJson(jsonMessage, HashMap.class);

                shouldContinue = getRequestStatus(request);
            }
            else
                shouldContinue = false;
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.server.verbose > 0)
            System.out.println("matando thread");
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
        if (type.equals(Define.write))
            return write(request);

        else if (type.equals(Define.read))
            return read(request);

        else if (type.equals(Define.read_timestamp))
            return readTimestamp(request);

        else if (type.equals(Define.bye)) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            HashMap<String, Object> dictionary = new HashMap<String, Object>();
            dictionary.put(Define.server_id, server.id);
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
        HashMap<String, Object> response = new HashMap<String, Object>();
        try {
            HashMap<String, Object> dataDict = new HashMap<String, Object>();
            dataDict.put(Define.variable, server.variable);
            dataDict.put(Define.timestamp, server.timestamp);
            dataDict.put(Define.data_signature, server.data_signature);
            dataDict.put(Define.client_id, server.client_id);

            response.put(Define.server_id, server.id);
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.success);
            response.put(Define.msg, Define.read);
            response.put(Define.data, dataDict);
        }
        finally {
            server.lock.unlock();
        }

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
        HashMap<String, Object> response = new HashMap<String, Object>();
        try {
            HashMap<String, Object> dataDict = new HashMap<String, Object>();
            dataDict.put(Define.timestamp, server.timestamp);

            response.put(Define.server_id, server.id);
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.success);
            response.put(Define.msg, Define.read_timestamp);
            response.put(Define.data, dataDict);
        }
        finally {
            server.lock.unlock();
        }

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
    private boolean write(HashMap request) {
        String variable = (String)request.get(Define.variable);
        int timestamp = ((Double) request.get(Define.timestamp)).intValue();
        String data_signature = (String) request.get(Define.data_signature);
        int client_id = ((Double) request.get(Define.client_id)).intValue();

        server.lock.lock();
        if (timestamp > server.timestamp)
        {
            if (this.server.verbose > 0)
                System.out.println("Recebido variable = " + variable + " e timestamp " + timestamp);

            server.variable = variable;
            server.timestamp = timestamp;
            server.data_signature = data_signature;
            server.client_id = client_id;

            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put(Define.server_id, server.id);
            response.put("plataform", Define.plataform);
            response.put(Define.request_code, request.get(Define.request_code));
            response.put(Define.status, Define.success);
            response.put(Define.msg, Define.variable_updated);

            server.lock.unlock();

            try {
                sendMessageToClient(response);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        else
        {
            server.lock.unlock();

            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put(Define.server_id, server.id);
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
}