package com.caykeprudente;

import com.sun.glass.ui.SystemClipboard;
import com.sun.tools.javac.util.Pair;

import java.net.Socket;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by cayke on 19/03/17.
 */
public class Client {
    ArrayList<Pair<String, Integer>> servers; // contains a tuple of (ip,port)
    int quorum = 2;

    int id = -1; // id do client. serve para identificar qual chave usar no RSA

    Lock lock = new ReentrantLock();

    private int request_code = 0;

    ArrayList<ResponseData> responses;
    ArrayList<Pair<String, Integer>> out_dated_servers;

    Semaphore semaphore = new Semaphore(0);
    Lock lock_print = new ReentrantLock();

    boolean exit = false;



    /*
    Client constructor.
    param: id - Client id
    param: servers - Array with servers(ip+port)
    */
    public Client(int id, ArrayList<Pair<String, Integer>> servers) {
        this.id = id;
        this.servers = servers;

        System.out.println("Client " + Define.plataform + " " + id + "running....");

        initUserInterface();
    }


    /*
    Shows user interface.
     */
    private void initUserInterface() {
        while (!exit) {
            Character choice = '0';
            while (choice != '1' && choice != '2' && choice != '3') {
                cleanScreen();

                lock_print.lock();
                System.out.println ("*********************************");
                System.out.println ("O que deseja fazer?");
                System.out.println ("1 - Escrever valor na variavel");
                System.out.println ("2 - Ler valor da variavel");
                System.out.println ("3 - Sair");
                System.out.println ("*********************************");
                lock_print.unlock();

                Scanner scanner = new Scanner(System.in);
                choice = scanner.next().charAt(0);
            }

            if (choice == '1') {
                cleanScreen();

                String data = RepresentedData.getData();
                if (data != null)
                    write(data);
            }
            else if (choice == '2') {
                read();
            }
            else if (choice == '3') {
                terminate();
            }
        }

        System.out.println("Adeus");
    }


    /*
    Clean screen.
     */
    private void cleanScreen() {
        //todo apagar terminal
    }


    /*
    Writes a value on servers.
    param: value - Value to be written in servers (dictionary from RepresentedData class)
    */
    private void write(String value) {
        int timestamp = readTimestamp();
        timestamp = incrementTimestamp(timestamp);

        //todo
        //data_signature = Signature.signData(Signature.getPrivateKey(-1,self.ID), value+str(timestamp))

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(value, timestamp, null, id, request_code, server);
            ClientHandler handler = new ClientHandler(this, ClientHandler.Function.write, data);
            Thread thread = new Thread(handler);
            thread.start();
        }

        request_code++;
    }


    /*
    Read Timestamps from servers
    return: (int) Timestamp value; -1 if cant get timestamp.
    */
    private int readTimestamp() {
        lock.lock();
        responses = new ArrayList<ResponseData>();
        out_dated_servers = new ArrayList<Pair<String, Integer>>();
        lock.unlock();

        lock_print.lock();
        System.out.println("Lendo timestamp dos servidores....");
        lock_print.unlock();

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(null, 0, null, id, request_code, server);
            ClientHandler handler = new ClientHandler(this, ClientHandler.Function.readTimestamp, data);
            Thread thread = new Thread(handler);
            thread.start();
        }

        try {
            boolean wasReleased = semaphore.tryAcquire(Define.timeout, TimeUnit.SECONDS);

            if (wasReleased) {
                lock.lock();
                request_code++;
                lock.unlock();

                if (responses.size() >= quorum) {
                    int timestamp = analyseTimestampResponse(responses);

                    lock_print.lock();
                    System.out.println("Li o timestamp do server: " + timestamp);
                    lock_print.unlock();

                    return timestamp;
                }
                else {
                    lock_print.lock();
                    System.out.println("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.");
                    lock_print.unlock();
                    return -1;
                }
            }
            else {
                lock_print.lock();
                System.out.println("Nao foi possivel ler nenhum dado. Timeout da conexao expirado");
                lock_print.unlock();
                return -1;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }


    /*
    Read Data from servers. Print on screen the response.
     */
    private void read() {
        lock.lock();
        responses = new ArrayList<ResponseData>();
        out_dated_servers = new ArrayList<Pair<String, Integer>>();
        lock.unlock();

        lock_print.lock();
        System.out.println("Lendo dados dos servidores....");
        lock_print.unlock();

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(null, 0, null, id, request_code, server);
            ClientHandler handler = new ClientHandler(this, ClientHandler.Function.read, data);
            Thread thread = new Thread(handler);
            thread.start();
        }

        try {
            boolean wasReleased = semaphore.tryAcquire(Define.timeout, TimeUnit.SECONDS);

            if (wasReleased) {
                lock.lock();
                request_code++;
                lock.unlock();

                if (responses.size() >= quorum) {
                    ResponseData mostRecentData = analyseResponse(responses);

                    writeBack(mostRecentData);

                    RepresentedData visibleData = new RepresentedData(mostRecentData.value);
                    lock_print.lock();
                    System.out.println("Li o dado do server no timestamp " + mostRecentData.timestamp + ": ");
                    visibleData.showInfo();
                    lock_print.unlock();
                }
                else {
                    lock_print.lock();
                    System.out.println("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.");
                    lock_print.unlock();
                }
            }
            else {
                lock_print.lock();
                System.out.println("Nao foi possivel ler nenhum dado. Timeout da conexao expirado");
                lock_print.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /*
    Writes a value readed on servers that are outdated.
    param: value - Value to be written in servers (dictionary from RepresentedData class)
    param: timestamp - Timestamp from value
    param: data_signature - Signature from value+timestamp
    param: client_id - ID from client that written the value
    */
    private void writeBack(ResponseData data) {
        for (Pair<String, Integer> server : out_dated_servers) {
            if (data.timestamp != -1 && data.client_id != -1) {
                data.server = server;
                data.request_code = request_code;

                ClientHandler handler = new ClientHandler(this, ClientHandler.Function.write, data);
                Thread thread = new Thread(handler);
                thread.start();
            }
        }
        request_code++;
    }


    /*
    Makes the client exit.
     */
    private void terminate(){
        exit = true;
    }


    /*
    Increments the timestamp
    param: serverTimestamp - Highest timestamp from a quorum of servers
    return: (int) Incremented timestamp
    */
    private int incrementTimestamp(int timestamp) {
        return timestamp+1;
    }


    /*
    Gets the data with the highest timestamp. Maps the outdated servers too.
    param: response - Servers' responses
    return: (tuple) Tuple with the actual data (value, timestamp, signature, writter_id)
    */
    private ResponseData analyseResponse(ArrayList<ResponseData> responses) {
        String value = "";
        int timestamp = -1;
        int repeatTimes = 0;
        ArrayList<Pair> auxServers = new ArrayList<Pair>();
        String data_signature = "";
        int client_id = -1;

        for (ResponseData response : responses) {
            if (response.timestamp == -1 || response.client_id == -1) {
                //nao ha dado escrito no servidor em questao
                out_dated_servers.add(response.server);
            }
            else {
                if (true) { //todo //Signature.verifySign(Signature.getPublicKey(-1, r_client_id), data_sign, rValue + str(rTimestamp)):
                    if (response.timestamp == timestamp) {
                        repeatTimes = repeatTimes + 1;
                        auxServers.add(response.server);
                    }
                    else if (response.timestamp > timestamp) {
                        timestamp = response.timestamp;
                        repeatTimes = 1;
                        value = response.value;
                        data_signature = response.data_signature;
                        client_id = response.client_id;
                        transferObject(auxServers, out_dated_servers);
                        auxServers.add(response.server);
                    }
                    else
                        out_dated_servers.add(response.server);
                }
                else {
                    out_dated_servers.add(response.server);
                }
            }
        }

        return new ResponseData(value, timestamp, data_signature, client_id, 0, null);
    }


    /*
    Gets the highest timestamp from an array containing various timestamps.
    param: responses - Servers' responses
    return: (int) Highest timestamp founded
    */
    private int analyseTimestampResponse(ArrayList<ResponseData> responses) {
        int timestamp = -1;
        int repeatTimes = 0;

        for (ResponseData response : responses) {
            if (response.timestamp == timestamp)
                repeatTimes++;
            else if (response.timestamp > timestamp) {
                timestamp = response.timestamp;
                repeatTimes = 1;
            }
            else
                break;
        }

        return timestamp;
    }


    /*
    Transfer Objects from one array to other
    param: oldArray - Array where objects will leave
    param: newArray - Array where objects will come
    */
    private void transferObject(List oldArray, List newArray) {
        for (Object object : oldArray) {
            oldArray.remove(object);
            newArray.add(object);
        }
    }
}