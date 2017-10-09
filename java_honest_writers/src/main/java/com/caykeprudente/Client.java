package com.caykeprudente;

import com.sun.tools.javac.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by cayke on 19/03/17.
 */
public class Client {
    List<Pair<String, Integer>> servers; // contains a tuple of (ip,port)
    int quorum = 2;

    Double id = -1d; // id do client. serve para identificar qual chave usar no RSA

    Lock lock = new ReentrantLock();

    private Double request_code = 0d;

    List<ResponseData> responses;
    List<Pair<String, Integer>> out_dated_servers;

    Semaphore semaphore = new Semaphore(0);

    public int verbose = 0;
    public String cert_path = "";

    boolean exit = false;



    /*
    Client constructor.
    param: id - Client id
    param: servers - Array with servers(ip+port)
    param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all
    param: cert_path - Path to certificates
    */
    public Client(Double id, List<Pair<String, Integer>> servers, int verbose, String cert_path) {
        this.id = id;
        this.servers = servers;
        this.verbose = verbose;
        this.cert_path = cert_path;

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
                
                System.out.println ("*********************************");
                System.out.println ("O que deseja fazer?");
                System.out.println ("1 - Escrever valor na variavel");
                System.out.println ("2 - Ler valor da variavel");
                System.out.println ("3 - Sair");
                System.out.println ("*********************************");

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
        Double timestamp = readTimestamp();
        timestamp = incrementTimestamp(timestamp);

        String dataSignature = MySignature.signData(MySignature.getPrivateKey(-1d, id, this.cert_path), value+timestamp.intValue());

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(value, timestamp, dataSignature, id, request_code, server);
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
    private Double readTimestamp() {
        lock.lock();
        responses = new ArrayList<ResponseData>();
        out_dated_servers = new ArrayList<Pair<String, Integer>>();
        lock.unlock();

        if (this.verbose > 0)
            System.out.println("Lendo timestamp dos servidores....");

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(null, 0d, null, id, request_code, server);
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
                    Double timestamp = analyseTimestampResponse(responses);

                    if(this.verbose > 0)
                        System.out.println("Li o timestamp do server: " + timestamp);

                    return timestamp;
                }
                else {
                    if(this.verbose > 0)
                        System.out.println("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.");

                    return -1d;
                }
            }
            else {
                if(this.verbose > 0)
                    System.out.println("Nao foi possivel ler nenhum dado. Timeout da conexao expirado");
                return -1d;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1d;
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

        if(this.verbose > 0)
            System.out.println("Lendo dados dos servidores....");

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(null, 0d, null, id, request_code, server);
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
                    if(this.verbose > 0)
                        System.out.println("Li o dado do server no timestamp " + mostRecentData.timestamp + ": ");
                    visibleData.showInfo();
                }
                else {
                    if(this.verbose > 0)
                        System.out.println("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.");
                }
            }
            else {
                if(this.verbose > 0)
                    System.out.println("Nao foi possivel ler nenhum dado. Timeout da conexao expirado")    ;
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
    private Double incrementTimestamp(Double timestamp) {
        return timestamp+1;
    }


    /*
    Gets the data with the highest timestamp. Maps the outdated servers too.
    param: response - Servers' responses
    return: (tuple) Tuple with the actual data (value, timestamp, signature, writter_id)
    */
    private ResponseData analyseResponse(List<ResponseData> responses) {
        String value = "";
        Double timestamp = -1d;
        int repeatTimes = 0;
        ArrayList<Pair> auxServers = new ArrayList<Pair>();
        String data_signature = "";
        Double client_id = -1d;

        for (ResponseData response : responses) {
            if (response.timestamp == -1 || response.client_id == -1) {
                //nao ha dado escrito no servidor em questao
                out_dated_servers.add(response.server);
            }
            else {
                if (MySignature.verifySign(MySignature.getPublicKey(-1d, response.client_id, this.cert_path), response.data_signature, response.value+response.timestamp.intValue())) {
                    if (response.timestamp.equals(timestamp)) {
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

        return new ResponseData(value, timestamp, data_signature, client_id, 0d, null);
    }


    /*
    Gets the highest timestamp from an array containing various timestamps.
    param: responses - Servers' responses
    return: (int) Highest timestamp founded
    */
    private Double analyseTimestampResponse(List<ResponseData> responses) {
        Double timestamp = -1d;
        int repeatTimes = 0;

        for (ResponseData response : responses) {
            if (response.timestamp.equals(timestamp))
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