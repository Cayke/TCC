import socket
import json
import threading
import Define
from representedData import RepresentedData
from signature import Signature
import math
import time

class RobotClient ():
    SERVERS = [] # contains a tuple of (ip, port)

    FAULTS = 1 #number of faults system can handle
    QUORUM = 2*FAULTS + 1

    ID = -1 # id do cliente. serve para identificar qual chave usar no RSA

    LOCK = threading.Lock()
    REQUEST_CODE = 0
    RESPONSES = [] # armazena tuplas (value, timestamp, data_signature, server_id, server)
    ECHOES = [] # armazena as assinaturas dos servidores (server_id, data_signature)
    OUT_DATED_SERVERS = []

    INCREMENT_TIMESTAMP_BY = 1

    # tratamento de timestamp already echoed por algum server
    TIMESTAMP_ALREADY_ECHOED_BY_ANY_SERVER = False
    TIMESTAMP_ALREADY_ECHOED_POWER = 0

    SEMAPHORE = threading.Semaphore(0)

    VERBOSE = 0
    CERT_PATH = ''
    RESULTS_PATH = ''

    exit = False

    NUMBER_OF_EXECUTIONS = 0
    OPERATION_TIMERS = []  # array with timers of operations
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

        print('Client ' + Define.plataform + " " + str(self.ID) + 'running...')
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
                self.OPERATION_TIMERS.append(final_time - init_time)
                i += 1

            self.FINAL_TIME = time.time()
            self.writeExecutionInfo(self.RESULTS_PATH + 'client_' + str(self.ID) + '_read.txt')

        elif type == 'write':
            while (i < n):
                init_time = time.time()
                data = RepresentedData.getFakeData(200)  # 100kb
                self.write(data)
                final_time = time.time()
                self.OPERATION_TIMERS.append(final_time - init_time)
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

        return count / len(self.OPERATION_TIMERS)


    '''
    Shows user inteface.
    '''
    def initUserInterface(self):
        while not self.exit:
            choice = 0
            while choice != '1' and choice != '2' and choice != '3':
                self.cleanScreen()
                if self.VERBOSE > 0:
                    print ('*********************************\n')
                    print ('O que deseja fazer?\n')
                    print ('1 - Escrever valor na variavel\n')
                    print ('2 - Ler valor da variavel\n')
                    print ('3 - Sair\n')
                    print ('*********************************\n')
                choice = input()

            if choice == '1':
                self.cleanScreen()

                data = RepresentedData.getData()

                if data != '':
                    init_time = time.time()
                    self.write(data)
                    final_time = time.time()
                    print('Write function time: %.2f seconds' % (final_time - init_time))

            elif choice == '2':
                init_time = time.time()
                self.read()
                final_time = time.time()
                print('Read function time: %.2f seconds' % (final_time - init_time))

            elif choice == '3':
                self.terminate()

        if self.VERBOSE > 0:
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
        echoesResult = self.getEchoes(value, timestamp)

        if (echoesResult is not None):
            echoes, last_timestamp = echoesResult
        else:
            echoes = None

        if (echoes is not None):
            for server in self.SERVERS:
                threading.Thread(target=self.writeOnServer, args=(server, value, last_timestamp, echoes, self.REQUEST_CODE, Define.write)).start()

            with self.LOCK:
                self.REQUEST_CODE = self.REQUEST_CODE + 1

        else:
            with self.LOCK:
                self.INCREMENT_TIMESTAMP_BY = self.INCREMENT_TIMESTAMP_BY + 1

            if self.VERBOSE > 0:
                print('Nao foi possivel fazer a escrita')

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
    Obtains echoes from servers.
    param: value - Value to be written in servers (dictionary from RepresentedData class)
    param: timestamp - Timestamp from value
    return: (int) Timestamp value; -1 if cant get timestamp.
    '''
    def getEchoes(self, value, timestamp):
        with self.LOCK:
            self.ECHOES = []
            self.TIMESTAMP_ALREADY_ECHOED_BY_ANY_SERVER = False

        if self.VERBOSE > 0:
            print("Obtendo echos dos servidores....")
        for server in self.SERVERS:
            threading.Thread(target=self.readEchoeFromServer, args=(server, self.REQUEST_CODE, value, timestamp)).start()

        wasReleased = self.SEMAPHORE.acquire(True, Define.timeout)

        if wasReleased:
            with self.LOCK:
                self.REQUEST_CODE = self.REQUEST_CODE + 1

            if self.TIMESTAMP_ALREADY_ECHOED_BY_ANY_SERVER:
                increment = math.pow(2, self.TIMESTAMP_ALREADY_ECHOED_POWER)
                self.TIMESTAMP_ALREADY_ECHOED_POWER = self.TIMESTAMP_ALREADY_ECHOED_POWER + 1
                return self.getEchoes(value, timestamp + increment)


            elif (len(self.ECHOES) >= self.QUORUM):
                self.TIMESTAMP_ALREADY_ECHOED_POWER = 0
                validEchoes = self.analyseEchoes(self.ECHOES, value, timestamp)

                if len(validEchoes) >= self.QUORUM:
                    if self.VERBOSE > 0:
                        print('Li os echos com sucesso')

                    return validEchoes, timestamp
                else:
                    if self.VERBOSE > 0:
                        print('Li os echos, mas nao deu quorum. Algum echo veio errado.')

                    return None

            else:
                if self.VERBOSE > 0:
                    print("ERRO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.")
                return None

        else:
            self.TIMESTAMP_ALREADY_ECHOED_POWER = 0

            if self.VERBOSE > 0:
                print("Nao foi possivel ler nenhum dado. Timeout da conexao expirado")
            return None


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
                result = self.analyseResponse(self.RESPONSES)

                if result is not None:
                    value, timestamp, echoes = result
                    self.writeBack(value, timestamp, echoes)

                    data = RepresentedData(value)
                    if self.VERBOSE > 0:
                        print('Li o dado do server:')
                        data.showInfo()
                        print ("Timestamp: " + str(timestamp))

                else:
                    if self.VERBOSE > 0:
                        print("Nao foi possivel ler nenhum dado. Chegou o quorum de mensagens mas nao havia b+1 iguais.")

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
    '''
    def writeBack(self, value, timestamp, echoes,):
        for server in self.OUT_DATED_SERVERS:
            if timestamp != -1:
                threading.Thread(target=self.writeOnServer, args=(server, value, timestamp, echoes, self.REQUEST_CODE, Define.write_back)).start()

        with self.LOCK:
            self.REQUEST_CODE = self.REQUEST_CODE + 1


    '''
    Makes the request for write the data in a specific server.
    param: server - Server to send the request
    param: value - Value to be written
    param: timestamp - Timestamp from value
    param: echos - Signatures for value+timestamp from servers
    param: request_code - Request ID identifier
    '''
    def writeOnServer(self, server, value, timestamp, echoes, request_code, type):
            echoesArray = []
            for (server_id, signature) in echoes:
                echoesArray.append(dict(server_id = server_id, data_signature = signature))

            request = dict(type=type, timestamp=int(timestamp), variable=value, request_code = request_code, echoes = echoesArray, client_id = self.ID, plataform = Define.plataform)
            requestJSON = json.dumps(request)

            TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            TCPSocket.connect(server)
            TCPSocket.send(requestJSON.encode('utf-8'))
            if self.VERBOSE == 2:
                print('-----REQUEST SAINDO:-----' + requestJSON)

            messageFromServer, server = TCPSocket.recvfrom(2048)
            messageFromServerJSON = messageFromServer.decode('utf-8')
            if self.VERBOSE == 2:
                print('-----REQUEST CHEGANDO:-----' + messageFromServerJSON)

            messageFromServerJSON = json.loads(messageFromServerJSON)
            if messageFromServerJSON[Define.status] == Define.success:
                with self.LOCK:
                    self.INCREMENT_TIMESTAMP_BY = 1

                if self.VERBOSE > 0:
                    print('Variable updated on server ' + str(messageFromServerJSON[Define.server_id]))
            elif messageFromServerJSON[Define.status] == Define.error and messageFromServerJSON[Define.msg] == Define.outdated_timestamp:
                with self.LOCK:
                    self.INCREMENT_TIMESTAMP_BY = 1

                if self.VERBOSE > 0:
                    print('Tried to write, but there is a newest data already')
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
            serverID = messageFromServer[Define.server_id]

            with self.LOCK:
                if len(self.RESPONSES) < self.QUORUM-1:
                    self.RESPONSES.append((serverVariable, serverTimestamp, dataSignature, serverID, server))

                elif len(self.RESPONSES) == self.QUORUM-1:
                    self.RESPONSES.append((serverVariable, serverTimestamp, dataSignature, serverID, server))
                    self.SEMAPHORE.release()

                else:
                    #do nothing
                    if self.VERBOSE > 0:
                        print("Quorum ja encheu. Jogando request fora...")

        elif messageFromServer[Define.status] == Define.error:
            if self.VERBOSE > 0:
                print("Ocorreu algum erro na request")


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
    Gets the echoe from a server and append in the echoes array if possible
    param: server - Server to send the request
    param: request_code - Request ID identifier
    '''
    def readEchoeFromServer(self, server, request_code, value, timestamp):
        messageFromServer = self.getEchoeFromServer(server, request_code, value, timestamp)

        if messageFromServer[Define.status] == Define.success and messageFromServer[Define.request_code] == self.REQUEST_CODE:
            data = messageFromServer[Define.data]
            data_signature = data[Define.data_signature]
            server_id = messageFromServer[Define.server_id]

            with self.LOCK:
                if len(self.ECHOES) < self.QUORUM - 1:
                    self.ECHOES.append((server_id, data_signature))

                elif len(self.ECHOES) == self.QUORUM - 1:
                    self.ECHOES.append((server_id, data_signature))
                    self.SEMAPHORE.release()

                else:
                    # do nothing
                    if self.VERBOSE > 0:
                        print("Quorum ja encheu. Jogando request fora...")

        elif messageFromServer[Define.status] == Define.error and messageFromServer[Define.msg] == Define.timestamp_already_echoed:
            with self.LOCK:
                self.TIMESTAMP_ALREADY_ECHOED_BY_ANY_SERVER = True

                server_id = messageFromServer[Define.server_id]

                if len(self.ECHOES) < self.QUORUM - 1:
                    self.ECHOES.append((server_id, ""))

                elif len(self.ECHOES) == self.QUORUM - 1:
                    self.ECHOES.append((server_id, ""))
                    self.SEMAPHORE.release()

                else:
                    # do nothing
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
        request = dict(type=Define.read, request_code =request_code, client_id = self.ID, plataform = Define.plataform)
        requestJSON = json.dumps(request)
        TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        TCPSocket.connect(server)
        TCPSocket.send(requestJSON.encode('utf-8'))
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + requestJSON)

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
        request = dict(type=Define.read_timestamp, request_code=request_code, client_id = self.ID, plataform = Define.plataform)
        requestJSON = json.dumps(request)
        TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        TCPSocket.connect(server)
        TCPSocket.send(requestJSON.encode('utf-8'))
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + requestJSON)

        messageFromServer, server = TCPSocket.recvfrom(2048)
        messageFromServerJSON = messageFromServer.decode('utf-8')
        if self.VERBOSE == 2:
            print('-----REQUEST CHEGANDO:-----' + messageFromServerJSON)

        return json.loads(messageFromServerJSON)


    '''
    Makes the request for getting the echoe for <v,t> at a specific server
    param: server - Server to send the request
    param: request_code - Request ID identifier
    param: value - Value to be written
    param: timestamp - Timestamp from value
    '''
    def getEchoeFromServer(self, server, request_code, value, timestamp):
        request = dict(type=Define.get_echoe, request_code=request_code, variable = value, timestamp = int(timestamp), client_id = self.ID, plataform = Define.plataform)
        requestJSON = json.dumps(request)
        TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        TCPSocket.connect(server)
        TCPSocket.send(requestJSON.encode('utf-8'))
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + requestJSON)

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
        return serverTimestamp + self.INCREMENT_TIMESTAMP_BY


    '''
    Gets the data with the highest timestamp in b+1 servers. Maps the outdated servers too.
    param: response - Servers' responses
    return: (tuple) Tuple with the actual data (value, timestamp, [echoes]). None if error.
    '''
    def analyseResponse(self, responses):
        array = []

        for (rValue, rTimestamp, data_sign, server_id, server) in responses:
            if rTimestamp == -1:
                #nao ha dado escrito no servidor
                self.OUT_DATED_SERVERS.append(server)
            else:
                if Signature.verifySign(Signature.getPublicKey(server_id, -1, self.CERT_PATH), data_sign, rValue + str(rTimestamp)):
                    self.addResponseToArrayWithRepeatTimes(array, rValue, rTimestamp, data_sign, server_id, server)
                else:
                    # assinatura invalida
                    self.OUT_DATED_SERVERS.append(server)

        return self.getValidResponse(array)


    '''
    Adds a response on an array. If value+timestamp already there, increments value.
    param: array - Array to add timestamp. Contains tuples (repeatTimes, value, timestamp, [(server_id, data_sign)], [server])
    param: value - Value to be added.
    param: timestamp - Timestamp to be added.
    param: data_sign - Data signature.
    param: server_id - Id of the server
    param: server - Server ip and port
    '''
    def addResponseToArrayWithRepeatTimes(self, array, value, timestamp, data_sign, server_id, server):
        wasAdded = False
        for x in range(0,len(array)):
            repeatTimes, tValue, tTimestamp, echoes, servers = array[x]
            if (tTimestamp == timestamp and tValue == value):
                repeatTimes += 1
                echoes.append((server_id, data_sign))
                servers.append(server)
                array[x] = (repeatTimes, tValue, tTimestamp, echoes, servers)
                wasAdded = True

        if (not wasAdded):
            array.append((1, value, timestamp, [(server_id, data_sign)], [server]))


    '''
    Gets the valid response from an array containing various responses.
    param: response - Servers' responses. Tuple (repeatTimes, value, timestamp, [(server_id, data_sign)], [server])
    return: (tuple - (value, timestamp, [echoes])) Valid response founded. None if error.
    '''
    def getValidResponse(self, responses):
        tuple = None

        for (repeatTimes, tValue, tTimestamp, echoes, servers) in responses:
            if repeatTimes >= self.FAULTS + 1:
                tuple = (tValue, tTimestamp,echoes)
            else:
                for server in servers:
                    self.OUT_DATED_SERVERS.append(server)

        return tuple


    '''
    Analyse if echoes are valid.
    param: echoes - Servers' echoes
    param: value - Value signed
    param: timestamp - Timestamp signed
    return: (Array) Valid echoes.
    '''
    def analyseEchoes(self, echoes, value, timestamp):
        validEchoes = []
        for (server_id, data_signature) in echoes:
            if Signature.verifySign(Signature.getPublicKey(server_id, -1, self.CERT_PATH), data_signature, value + str(timestamp)):
                validEchoes.append((server_id, data_signature))

        return validEchoes

    '''
    Gets the highest timestamp from an array containing various timestamps.
    param: response - Servers' responses
    return: (int) Highest timestamp founded. -1 if error.
    '''
    def analyseTimestampResponse(self, responses):
        array = []
        for rTimestamp in responses:
            self.addTimestampOnArray(rTimestamp, array)

        return self.getLastestTimestamp(array)


    '''
    Adds a timestamp on an array. If timestamp already there, increments value.
    param: timestamp - Timestamp to be added.
    param: array - Array to add timestamp. Contains tuples (timestamp, repeatTimes)
    '''
    def addTimestampOnArray(self, timestamp, array):
        wasAdded = False
        for x in range(0, len(array)):
            tStamp, repeatTimes = array[x]
            if (tStamp == timestamp):
                repeatTimes += 1
                array[x] = (tStamp, repeatTimes)
                wasAdded = True

        if (not wasAdded):
            array.append((timestamp, 1))


    '''
    Returns the lastest timestamp that. (that occurs in b + 1 responses)
    param: array - Array to add timestamp. Contains tuples (timestamp, repeatTimes)
    :return: (int) Highest timestamp founded. -1 if error.
    '''
    def getLastestTimestamp(self, array):
        timestamp = -1
        for (tStamp, repeatTimes) in array:
            if (tStamp > timestamp and repeatTimes >= self.FAULTS + 1):
                timestamp = tStamp

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