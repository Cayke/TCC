package com.caykeprudente;

import com.sun.tools.javac.util.Pair;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RobotClient {
    List<Pair<String, Integer>> servers; // contains a tuple of (ip,port)
    int quorum = 2;

    int id = -1; // id do client. serve para identificar qual chave usar no RSA

    Lock lock = new ReentrantLock();

    private int request_code = 0;

    List<ResponseData> responses;
    List<Pair<String, Integer>> out_dated_servers;

    Semaphore semaphore = new Semaphore(0);

    public int verbose = 0;
    public String cert_path = "";
    public String results_path = "";

    boolean exit = false;

    public int number_of_executions = 0;
    public List<Double> operation_timers = new ArrayList<Double>();
    public long init_time = 0;
    public long final_time = 0;


    /*
    Client constructor.
    param: id - Client id
    param: servers - Array with servers(ip+port)
    param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all
    param: cert_path - Path to certificates
    param: repeat_operations - number of times to repeat operation
    param: operation - read or write
    param: results_path - Path to save result txt
    */
    public RobotClient(int id, List<Pair<String, Integer>> servers, int verbose, String cert_path, int repeat_operations,
                       String operation, String results_path) {
        this.id = id;
        this.servers = servers;
        this.verbose = verbose;
        this.cert_path = cert_path;
        this.results_path = results_path;

        System.out.println("Client " + Define.plataform + " " + id + "running....");

        //initUserInterface();
        makeRequests(repeat_operations, operation);
    }

    /*
    Makes automated requests to servers
    param: (int) n - Number of times to send requests
    param: (string) type - read or write operarion
     */
    private void makeRequests(int n, String type) {
        this.number_of_executions = n;
        this.init_time = System.currentTimeMillis();

        int i = 0;
        if (type.equals("read")) {
            while (i < n) {
                long init_time = System.currentTimeMillis();
                this.read();
                long final_time = System.currentTimeMillis();
                this.operation_timers.add((final_time-init_time)/1000.0); //save seconds in array
                i++;
            }
            this.final_time = new Date().getTime();

            this.writeExecutionInfo(this.results_path + "client_" + this.id + "_read.txt");
        }
        else if (type.equals("write")) {
            while (i < n) {
                long init_time = System.currentTimeMillis();
                String data = RepresentedData.getFakeData(200);
                this.write(data);
                long final_time = System.currentTimeMillis();
                this.operation_timers.add((final_time-init_time)/1000.0); //save seconds in array
                i++;
            }
            this.final_time = System.currentTimeMillis();

            this.writeExecutionInfo(this.results_path + "client_" + this.id + "_write.txt");
        }
    }


    /*
    Writes basic infos of execution in file
    param: (string) path - File's path to write info
     */
    private void writeExecutionInfo(String path) {
        try {
            PrintWriter writer = new PrintWriter(path, "UTF-8");
            writer.println(Define.execution_file_header);
            writer.println(this.number_of_executions + "|" + getAverageOperationTime() + "|" + this.init_time/1000.0 + "|" + this.final_time/1000.0);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Calculates average time for operations
    return: (float) average_time - Avarage time for operations
     */
    private Double getAverageOperationTime(){
        Double count = 0d;

        for (Double time : this.operation_timers)
            count += time;

        return count/this.operation_timers.size();
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
        int timestamp = readTimestamp();
        timestamp = incrementTimestamp(timestamp);

        String dataSignature = MySignature.signData(MySignature.getPrivateKey(-1, id % 2, this.cert_path), value+timestamp);

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
    private int readTimestamp() {
        lock.lock();
        responses = new ArrayList<ResponseData>();
        out_dated_servers = new ArrayList<Pair<String, Integer>>();
        lock.unlock();

        if (this.verbose > 0)
            System.out.println("Lendo timestamp dos servidores....");

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

                    if(this.verbose > 0)
                        System.out.println("Li o timestamp do server: " + timestamp);

                    return timestamp;
                }
                else {
                    if(this.verbose > 0)
                        System.out.println("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.");

                    return -1;
                }
            }
            else {
                if(this.verbose > 0)
                    System.out.println("Nao foi possivel ler nenhum dado. Timeout da conexao expirado");
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

        if(this.verbose > 0)
            System.out.println("Lendo dados dos servidores....");

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
    private int incrementTimestamp(int timestamp) {
        return timestamp+1;
    }


    /*
    Gets the data with the highest timestamp. Maps the outdated servers too.
    param: response - Servers' responses
    return: (tuple) Tuple with the actual data (value, timestamp, signature, writter_id)
    */
    private ResponseData analyseResponse(List<ResponseData> responses) {
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
                if (MySignature.verifySign(MySignature.getPublicKey(-1, response.client_id % 2, this.cert_path), response.data_signature, response.value+response.timestamp)) {
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
    private int analyseTimestampResponse(List<ResponseData> responses) {
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
