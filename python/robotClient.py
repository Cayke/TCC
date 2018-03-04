import socket
import json
import threading
import Define
from representedData import RepresentedData
from signature import Signature
import time

class RobotClient ():
    SERVERS = [] # contains a tuple of (ip, port)
    QUORUM = 2

    ID = -1 # id do cliente. serve para identificar qual chave usar no RSA


    LOCK = threading.Lock()
    REQUEST_CODE = 0
    RESPONSES = [] # armazena tuplas (value, timestamp, data_signature, client_id, server)
    OUT_DATED_SERVERS = []


    SEMAPHORE = threading.Semaphore(0)

    VERBOSE = 0
    CERT_PATH = ''
    RESULTS_PATH = ''

    exit = False

    NUMBER_OF_EXECUTIONS = 0
    OPERATION_TIMERS = [] #array with timers of operations
    INIT_TIME = 0
    FINAL_TIME = 0

    '''
    Client constructor.
    param: id - Client id
    param: servers - Array with servers(ip+port)
    param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all  
    param: cert_path - Path to certificates
    param: repeat_operations - number of times to repeat operation
    param: operation - read or write
    param: results_path - Path to save result txt
    '''
    def __init__(self, id, servers, verbose, cert_path, repeat_operations, operation, results_path):
        self.ID = id
        self.SERVERS = servers
        self.VERBOSE = verbose
        self.CERT_PATH = cert_path
        self.RESULTS_PATH = results_path

        print('Client ' + Define.plataform + " " + str(self.ID) + ' running...')
        #self.initUserInterface()
        self.makeRequests(repeat_operations, operation)


    '''
    Makes automated requests to servers
    param: (int) n - Number of times to send requests
    param: (string) type - read or write operarion
    '''
    def makeRequests(self, n, type):
        self.NUMBER_OF_EXECUTIONS = n
        self.INIT_TIME = time.time()

        i = 0
        if type == 'read':
            while (i < n):
                init_time = time.time()
                self.read()
                final_time = time.time()
                self.OPERATION_TIMERS.append(final_time-init_time)
                i += 1

            self.FINAL_TIME = time.time()
            self.writeExecutionInfo(self.RESULTS_PATH + 'client_' + str(self.ID) + '_read.txt')

        elif type == 'write':
            while (i < n):
                init_time = time.time()
                data = RepresentedData.getFakeData(200)  # 100kb
                self.write(data)
                final_time = time.time()
                self.OPERATION_TIMERS.append(final_time-init_time)
                i += 1

            self.FINAL_TIME = time.time()
            self.writeExecutionInfo(self.RESULTS_PATH + 'client_' + str(self.ID) + '_write.txt')


    '''
    Writes basic infos of execution in file
    param: (string) path - File's path to write info
    '''
    def writeExecutionInfo(self, path):
        file = open(path, 'w')

        file.write(Define.execution_file_header)
        infos = str(self.NUMBER_OF_EXECUTIONS) + ';' + str(self.getAverageOperationTime()) + ';' \
                + str(self.INIT_TIME) + ';' + str(self.FINAL_TIME)
        file.write(infos)
        file.close()

    '''
    Calculates average time for operations
    return: (float) average_time - Avarage time for operations
    '''
    def getAverageOperationTime(self):
        count = 0
        for time in self.OPERATION_TIMERS:
            count = count + time

        return count/len(self.OPERATION_TIMERS)

    '''
    Shows user inteface.
    '''
    def initUserInterface(self):
        while not self.exit:
            choice = 0
            while choice != '1' and choice != 1 and choice != '2' and choice != 2 and choice != '3' and choice != 3:
                self.cleanScreen()
                print ('*********************************\n')
                print ('O que deseja fazer?\n')
                print ('1 - Escrever valor na variavel\n')
                print ('2 - Ler valor da variavel\n')
                print ('3 - Sair\n')
                print ('*********************************\n')
                choice = input()

            if choice == '1' or choice == 1:
                self.cleanScreen()

                data = RepresentedData.getData()

                if data != '':
                    init_time = time.time()
                    self.write(data)
                    final_time = time.time()
                    print('Write function time: %.2f seconds' % (final_time - init_time))

            elif choice == '2' or choice  == 2:
                init_time = time.time()
                self.read()
                final_time = time.time()
                print ('Read function time: %.2f seconds' % (final_time-init_time))

            elif choice == '3' or choice == 3:
                self.terminate()

        print('Adeus')


    '''
    Clean screen.
    '''
    def cleanScreen(self):
        pass
        #todo os.system('cls' if os.name == 'nt' else 'clear')


    '''
    Writes a value on servers.
    param: value - Value to be written in servers (dictionary from RepresentedData class)
    '''
    def write(self, value):
        timestamp = self.readTimestamp()
        timestamp = self.incrementTimestamp(timestamp)

        data_signature = Signature.signData(Signature.getPrivateKey(-1,self.ID%2, self.CERT_PATH), value+str(timestamp))

        for server in self.SERVERS:
            threading.Thread(target=self.writeOnServer, args=(server, value, timestamp, data_signature, self.ID, self.REQUEST_CODE)).start()

        with self.LOCK:
            self.REQUEST_CODE = self.REQUEST_CODE + 1


    '''
    Read Timestamps from servers
    return: (int) Timestamp value; -1 if cant get timestamp.
    '''
    def readTimestamp(self):
        with self.LOCK:
            self.RESPONSES = []
            self.OUT_DATED_SERVERS = []

        if self.VERBOSE > 0:
            print("Lendo timestamp dos servidores....")
        for server in self.SERVERS:
            threading.Thread(target=self.readTimestampFromServer, args=(server, self.REQUEST_CODE)).start()

        wasReleased = self.SEMAPHORE.acquire(True, Define.timeout)

        if wasReleased:
            with self.LOCK:
                self.REQUEST_CODE = self.REQUEST_CODE + 1

            if (len(self.RESPONSES) >= self.QUORUM):
                timestamp = self.analyseTimestampResponse(self.RESPONSES)

                if self.VERBOSE > 0:
                    print('Li o dado do server:')
                    print("Timestamp: " + str(timestamp))

                return timestamp

            else:
                if self.VERBOSE > 0:
                    print("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.")
                return -1

        else:
            if self.VERBOSE > 0:
                print("Nao foi possivel ler nenhum dado. Timeout da conexao expirado")
            return -1


    '''
    Read Data from servers. Print on screen the response.
    '''
    def read(self):
        with self.LOCK:
            self.RESPONSES = []
            self.OUT_DATED_SERVERS = []

            if self.VERBOSE > 0:
                print("Lendo dados dos servidores....")
        for server in self.SERVERS:
            threading.Thread(target=self.readFromServer, args=(server, self.REQUEST_CODE)).start()

        wasReleased = self.SEMAPHORE.acquire(True, Define.timeout)

        if wasReleased:
            with self.LOCK:
                self.REQUEST_CODE = self.REQUEST_CODE + 1

            if (len(self.RESPONSES) >= self.QUORUM):
                value, timestamp, data_signature, client_id = self.analyseResponse(self.RESPONSES)

                self.writeBack(value, timestamp, data_signature, client_id)

                data = RepresentedData(value)
                if self.VERBOSE > 0:
                    print('Li o dado do server:')
                    data.showInfo()
                    print ("Timestamp: " + str(timestamp))

            else:
                if self.VERBOSE > 0:
                    print ("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.")

        else:
            if self.VERBOSE > 0:
                print ("Nao foi possivel ler nenhum dado. Timeout da conexao expirado")


    '''
    Writes a value readed on servers that are outdated.
    param: value - Value to be written in servers (dictionary from RepresentedData class)
    param: timestamp - Timestamp from value
    param: data_signature - Signature from value+timestamp
    param: client_id - ID from client that written the value
    '''
    def writeBack(self, value, timestamp, data_signature, client_id):
        for server in self.OUT_DATED_SERVERS:
            if timestamp != -1 and client_id != -1:
                threading.Thread(target=self.writeOnServer, args=(server, value, timestamp, data_signature, client_id, self.REQUEST_CODE)).start()

        with self.LOCK:
            self.REQUEST_CODE = self.REQUEST_CODE + 1


    '''
    Sends the data to be written in a specific server.
    param: server - Server to send the request
    param: value - Value to be written
    param: timestamp - Timestamp from value
    param: data_signature - Signature from value+timestamp
    param: client_id - Id from the client that created the value
    param: request_code - Request ID identifier
    '''
    def writeOnServer(self, server, value, timestamp, data_signature, client_id, request_code):
            request = dict(type=Define.write, timestamp=timestamp, variable=value, request_code = request_code, client_id = client_id, data_signature = data_signature)
            requestJSON = json.dumps(request)

            TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            TCPSocket.connect(server)
            if self.VERBOSE == 2:
                print('-----REQUEST SAINDO:-----' + requestJSON)
            TCPSocket.send(requestJSON.encode('utf-8'))

            messageFromServer, server = TCPSocket.recvfrom(2048)
            messageFromServerJSON = messageFromServer.decode('utf-8')
            if self.VERBOSE == 2:
                print('-----REQUEST CHEGANDO:-----' + messageFromServerJSON)

            messageFromServerJSON = json.loads(messageFromServerJSON)
            if messageFromServerJSON[Define.status] == Define.success:
                if self.VERBOSE > 0:
                    print('Variable updated')
            else:
                if self.VERBOSE > 0:
                    print('Error updating')


    '''
    Gets the data from a server and append in the responses array if possible
    param: server - Server to send the request
    param: request_code - Request ID identifier
    '''
    def readFromServer(self, server, request_code):
        messageFromServer = self.getValueFromServer(server, request_code)

        if messageFromServer[Define.status] == Define.success and messageFromServer[Define.request_code] == self.REQUEST_CODE:
            data = messageFromServer[Define.data]
            serverTimestamp = data[Define.timestamp]
            serverVariable = data[Define.variable]
            dataSignature = data[Define.data_signature]
            clientID = data[Define.client_id]

            with self.LOCK:
                if len(self.RESPONSES) < self.QUORUM-1:
                    self.RESPONSES.append((serverVariable, serverTimestamp, dataSignature, clientID, server))

                elif len(self.RESPONSES) == self.QUORUM-1:
                    self.RESPONSES.append((serverVariable, serverTimestamp, dataSignature, clientID, server))
                    self.SEMAPHORE.release()

                else:
                    #do nothing
                    if self.VERBOSE > 0:
                        print("Quorum ja encheu. Jogando request fora...")

        elif messageFromServer[Define.status] == Define.error:
            if self.VERBOSE > 0:
                print("Ocorreu algum erro na request")

        elif messageFromServer[Define.request_code] != self.REQUEST_CODE:
            if self.VERBOSE > 0:
                print("Response atrasada.")


    '''
    Gets the timestamp from a server and append in the responses array if possible
    param: server - Server to send the request
    param: request_code - Request ID identifier
    '''
    def readTimestampFromServer(self, server, request_code):
        messageFromServer = self.getTimestampFromServer(server, request_code)

        if messageFromServer[Define.status] == Define.success and messageFromServer[Define.request_code] == self.REQUEST_CODE:
            data = messageFromServer[Define.data]
            serverTimestamp = data[Define.timestamp]

            with self.LOCK:
                if len(self.RESPONSES) < self.QUORUM-1:
                    self.RESPONSES.append(serverTimestamp)

                elif len(self.RESPONSES) == self.QUORUM-1:
                    self.RESPONSES.append(serverTimestamp)
                    self.SEMAPHORE.release()

                else:
                    #do nothing
                    if self.VERBOSE > 0:
                        print("Quorum ja encheu. Jogando request fora...")

        elif messageFromServer[Define.status] == Define.error:
            if self.VERBOSE > 0:
                print("Ocorreu algum erro na request")

        elif messageFromServer[Define.request_code] != self.REQUEST_CODE:
            if self.VERBOSE > 0:
                print("Response atrasada.")


    '''
    Makes the request for getting the value registered at a specific server
    param: server - Server to send the request
    param: request_code - Request ID identifier
    '''
    def getValueFromServer(self, server, request_code):
        request = dict(type=Define.read, request_code =request_code, client_id = self.ID)
        requestJSON = json.dumps(request)
        TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        TCPSocket.connect(server)
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + requestJSON)
        TCPSocket.send(requestJSON.encode('utf-8'))

        messageFromServer, server = TCPSocket.recvfrom(2048)
        messageFromServerJSON = messageFromServer.decode('utf-8')
        if self.VERBOSE == 2:
            print('-----REQUEST CHEGANDO:-----' + messageFromServerJSON)

        return json.loads(messageFromServerJSON)


    '''
    Makes the request for getting the timestamp registered at a specific server
    param: server - Server to send the request
    param: request_code - Request ID identifier
    '''
    def getTimestampFromServer(self, server, request_code):
        request = dict(type=Define.read_timestamp, request_code=request_code, client_id=self.ID)
        requestJSON = json.dumps(request)
        TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        TCPSocket.connect(server)
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + requestJSON)
        TCPSocket.send(requestJSON.encode('utf-8'))

        messageFromServer, server = TCPSocket.recvfrom(2048)
        messageFromServerJSON = messageFromServer.decode('utf-8')
        if self.VERBOSE == 2:
            print('-----REQUEST CHEGANDO:-----' + messageFromServerJSON)

        return json.loads(messageFromServerJSON)


    '''
    Makes the client exit.
    '''
    def terminate(self):
        self.exit = True


    '''
    Increments the timestamp
    param: serverTimestamp - Highest timestamp from a quorum of servers
    return: (int) Incremented timestamp
    '''
    def incrementTimestamp(self, serverTimestamp):
        return serverTimestamp+1


    '''
    Gets the data with the highest timestamp. Maps the outdated servers too.
    param: response - Servers' responses
    return: (tuple) Tuple with the actual data (value, timestamp, signature, writter_id)
    '''
    def analyseResponse(self, responses):
        value = ''
        timestamp = -1
        repeatTimes = 0
        auxServers = []
        data_signature = ''
        client_id = -1

        for (rValue, rTimestamp, data_sign, r_client_id, server) in responses:
            if rTimestamp == -1 or r_client_id == -1:
                #nao ha dado escrito no servidor
                self.OUT_DATED_SERVERS.append(server)

            else:
                if Signature.verifySign(Signature.getPublicKey(-1, r_client_id%2, self.CERT_PATH), data_sign, rValue + str(rTimestamp)):
                    if (rTimestamp == timestamp):
                        repeatTimes = repeatTimes + 1
                        auxServers.append(server)

                    elif (rTimestamp > timestamp):
                        timestamp = rTimestamp
                        repeatTimes = 1
                        value = rValue
                        data_signature = data_sign
                        client_id = r_client_id
                        self.transferObjects(auxServers, self.OUT_DATED_SERVERS)
                        auxServers.append(server)

                    else:
                        self.OUT_DATED_SERVERS.append(server)

                else:
                    # assinatura invalida
                    self.OUT_DATED_SERVERS.append(server)

        return (value, timestamp, data_signature, client_id)


    '''
    Gets the highest timestamp from an array containing various timestamps.
    param: response - Servers' responses
    return: (int) Highest timestamp founded
    '''
    def analyseTimestampResponse(self, responses):
        timestamp = -1
        repeatTimes = 0

        for rTimestamp in responses:
            if (rTimestamp == timestamp):
                repeatTimes = repeatTimes + 1

            elif (rTimestamp > timestamp):
                timestamp = rTimestamp
                repeatTimes = 1

            else:
                pass

        return timestamp


    '''
    Transfer Objects from one array to other
    param: oldArray - Array where objects will leave
    param: newArray - Array where objects will come
    '''
    def transferObjects(self, oldArray, newArray):
        for obj in oldArray:
            oldArray.remove(obj)
            newArray.append(obj)