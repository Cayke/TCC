package com.caykeprudente;

import com.sun.tools.javac.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
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

    int faults = 1; //number of faults system can handle
    int quorum = 2*faults + 1;


    Lock lock = new ReentrantLock();

    private Double request_code = 0d;

    List<ResponseData> responses;
    List<Pair<Integer, String>> echoes; //armazena as assinaturas dos servidores (server_id, data_signature)
    List<Pair<String, Integer>> out_dated_servers;

    Semaphore semaphore = new Semaphore(0);
    Lock lock_print = new ReentrantLock();

    boolean exit = false;



    /*
    Client constructor.
    param: id - Client id
    param: servers - Array with servers(ip+port)
    */
    public Client(List<Pair<String, Integer>> servers) {
        this.servers = servers;

        System.out.println("Client " + Define.plataform + " running....");

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
        Double timestamp = readTimestamp();
        timestamp = incrementTimestamp(timestamp);
        List<Pair<Integer, String>> echoes = getEchoes(value, timestamp);

        if (echoes != null) {
            for (Pair<String, Integer> server : servers) {
                ResponseData data = new ResponseData(value, timestamp, null, null, request_code, server, echoes);
                ClientHandler handler = new ClientHandler(this, ClientHandler.Function.write, data);
                Thread thread = new Thread(handler);
                thread.start();
            }

            request_code++;
        }
        else {
            lock_print.lock();
            System.out.println("Nao foi possivel fazer a escrita");
            lock_print.unlock();
        }
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

        lock_print.lock();
        System.out.println("Lendo timestamp dos servidores....");
        lock_print.unlock();

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(null, 0d, null, 0d, request_code, server, null);
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

                    lock_print.lock();
                    System.out.println("Li o timestamp do server: " + timestamp);
                    lock_print.unlock();

                    return timestamp;
                }
                else {
                    lock_print.lock();
                    System.out.println("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.");
                    lock_print.unlock();
                    return -1d;
                }
            }
            else {
                lock_print.lock();
                System.out.println("Nao foi possivel ler nenhum dado. Timeout da conexao expirado");
                lock_print.unlock();
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

        lock_print.lock();
        System.out.println("Lendo dados dos servidores....");
        lock_print.unlock();

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(null, 0d, null, 0d, request_code, server, null);
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
                    ResponseValidator mostRecentData = analyseResponse(responses);

                    if (mostRecentData != null) {
                        writeBack(mostRecentData);

                        RepresentedData visibleData = new RepresentedData(mostRecentData.value);
                        lock_print.lock();
                        System.out.println("Li o dado do server no timestamp " + mostRecentData.timestamp + ": ");
                        visibleData.showInfo();
                        lock_print.unlock();
                    }
                    else {
                        lock_print.lock();
                        System.out.println("Nao foi possivel ler nenhum dado. Chegou o quorum de mensagens mas nao havia b+1 iguais.");
                        lock_print.unlock();
                    }


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
    param: echoes - Signatures from servers - value+timestamp
    */
    private void writeBack(ResponseValidator responseValidator) {
        for (Pair<String, Integer> server : out_dated_servers) {
            if (responseValidator.timestamp != -1) {
                Double timestamp = Double.valueOf(responseValidator.timestamp);
                ResponseData data = new ResponseData(responseValidator.value, timestamp, null, null, request_code, server, responseValidator.echoes);
                ClientHandler handler = new ClientHandler(this, ClientHandler.Function.write_back, data);
                Thread thread = new Thread(handler);
                thread.start();
            }
        }
        request_code++;
    }


    /*
    Obtains echoes from servers.
    param: value - Value to be written in servers (dictionary from RepresentedData class)
    param: timestamp - Timestamp from value
    return: (List) Valid echoes from server; Null if cant get.
     */
    private List<Pair<Integer, String>> getEchoes(String value, Double timestamp) {
        lock.lock();
        echoes = new ArrayList<Pair<Integer, String>>();
        lock.unlock();

        lock_print.lock();
        System.out.println("Obtendo echos dos servidores....");
        lock_print.unlock();

        for (Pair<String, Integer> server : servers) {
            ResponseData data = new ResponseData(value, timestamp, null, null, request_code, server, null);
            ClientHandler handler = new ClientHandler(this, ClientHandler.Function.readEchoeFromServer, data);
            Thread thread = new Thread(handler);
            thread.start();
        }

        try {
            boolean wasReleased = semaphore.tryAcquire(Define.timeout, TimeUnit.SECONDS);

            if (wasReleased) {
                lock.lock();
                request_code++;
                lock.unlock();

                if (echoes.size() >= quorum) {
                    List<Pair<Integer, String>> validEchoes = analyseEchoes(echoes, value, timestamp);

                    if (validEchoes.size() >= quorum) {
                        lock_print.lock();
                        System.out.println("Li os echos com sucesso");
                        lock_print.unlock();

                        return validEchoes;
                    }
                    else {
                        lock_print.lock();
                        System.out.println("Li os echos, mas nao deu quorum. Algum echo veio errado.");
                        lock_print.unlock();

                        return null;
                    }
                }
                else {
                    lock_print.lock();
                    System.out.println("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.");
                    lock_print.unlock();

                    return null;
                }
            }
            else {
                lock_print.lock();
                System.out.println("Nao foi possivel ler nenhum dado. Timeout da conexao expirado");
                lock_print.unlock();

                return null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();

            return null;
        }
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
    return: (tuple) Tuple with the actual data (value, timestamp, [echoes]). None if error.
    */
    private ResponseValidator analyseResponse(List<ResponseData> responses) {
        List array = new ArrayList();

        for (ResponseData response : responses) {
            if (response.timestamp == -1)
                out_dated_servers.add(response.server);
            else {
                if (MySignature.verifySign(MySignature.getPublicKey(response.server_id,-1d), response.data_signature, response.value+response.timestamp.intValue()))
                    addResponseToArrayWithRepeatTimes(array, response);
                else
                    out_dated_servers.add(response.server);
            }
        }

        return getValidResponse(array);
    }


    /*
    Adds a response on an array. If value+timestamp already there, increments value.
    param: array - Array to add timestamp. Contains tuples (repeatTimes, value, timestamp, [(server_id, data_sign)], [server])
    param: value - Value to be added.
    param: timestamp - Timestamp to be added.
    param: data_sign - Data signature.
    param: server_id - Id of the server
    param: server - Server ip and port
     */
    private void addResponseToArrayWithRepeatTimes(List<ResponseValidator> array, ResponseData responseData) {
        boolean wasAdded = false;
        for (ResponseValidator validator : array) {
            if (validator.timestamp == responseData.timestamp && validator.value.equals(responseData.value)) {
                validator.repeatTimes++;
                validator.echoes.add(new Pair<Integer, String>(responseData.server_id.intValue(), responseData.data_signature));
                validator.servers.add(responseData.server);
                wasAdded = true;
            }
        }

        if (!wasAdded) {
            ArrayList<Pair<Integer, String>> echoes = new ArrayList<Pair<Integer, String>>();
            echoes.add(new Pair<Integer, String>(responseData.server_id.intValue(), responseData.data_signature));
            ArrayList<Pair<String, Integer>> servers = new ArrayList<Pair<String, Integer>>();
            servers.add(responseData.server);

            array.add(new ResponseValidator(1, responseData.value, responseData.timestamp.intValue(), echoes, servers));
        }
    }


    /*
    Gets the valid response from an array containing various responses.
    param: response - Servers' responses. Tuple (repeatTimes, value, timestamp, [(server_id, data_sign)], [server])
    return: (tuple - (value, timestamp, [echoes])) Valid response founded. None if error.
     */
    private ResponseValidator getValidResponse(List<ResponseValidator> responses) {
        ResponseValidator validResponse = null;

        for (ResponseValidator response : responses) {
            if (response.repeatTimes >= faults + 1)
                validResponse = response;
            else
                out_dated_servers.addAll(response.servers);
        }

        return validResponse;
    }


    /*
    Analyse if echoes are valid.
    param: echoes - Servers' echoes
    param: value - Value signed
    param: timestamp - Timestamp signed
    return: (Array) Valid echoes.
     */
    private List analyseEchoes(List<Pair<Integer, String>> echoes, String value, Double timestamp) {
        List<Pair<Integer, String>> validEchoes = new ArrayList<Pair<Integer, String>>();
        for (Pair<Integer, String> echo : echoes) {
            if (MySignature.verifySign(MySignature.getPublicKey(echo.fst.doubleValue(), -1d), echo.snd, value+timestamp.intValue()))
                validEchoes.add(echo);
        }

        return validEchoes;
    }


    /*
    Gets the highest timestamp from an array containing various timestamps.
    param: response - Servers' responses
    return: (int) Highest timestamp founded. -1 if error.
    */
    private Double analyseTimestampResponse(List<ResponseData> responses) {
        List<Pair<Double, Integer>> array = new ArrayList<Pair<Double, Integer>>();
        for (ResponseData response : responses) {
            addTimestampOnArray(response.timestamp, array);
        }

        return getLastestTimestamp(array);
    }


    /*
    Adds a timestamp on an array. If timestamp already there, increments value.
    param: timestamp - Timestamp to be added.
    param: array - Array to add timestamp. Contains tuples (timestamp, repeatTimes)
     */
    private void addTimestampOnArray(Double timestamp, List<Pair<Double, Integer>> array) {
        boolean wasAdded = false;
        for (int i = 0; i < array.size(); i++) {
            Pair<Double, Integer> tuple = array.get(i);
            if (tuple.fst.equals(timestamp)) {
                array.remove(i);
                array.add(i, new Pair<Double, Integer>(tuple.fst, tuple.snd + 1));
                wasAdded = true;
            }
        }

        if (!wasAdded)
            array.add(new Pair<Double, Integer>(timestamp, 1));
    }


    /*
    Returns the lastest timestamp that. (that occurs in b + 1 responses)
    param: array - Array to add timestamp. Contains tuples (timestamp, repeatTimes)
    return: (int) Highest timestamp founded. -1 if error.
     */
    private Double getLastestTimestamp(List<Pair<Double, Integer>> array) {
        Double timestamp = -1d;
        for (Pair<Double, Integer> tuple : array) {
            if (tuple.fst > timestamp && tuple.snd >= faults + 1)
                timestamp = tuple.fst;
        }

        return timestamp;
    }
}