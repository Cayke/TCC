import socket
import threading
import json
import Define
from signature import Signature


class Server(object):
    HOST = ''
    PORT = -1
    ID = -1

    FAULTS = 1  # number of faults system can handle
    QUORUM = 2 * FAULTS + 1

    VARIABLE = ''
    DATA_SIGNATURE = ''
    TIMESTAMP = -1

    LAST_ECHOED_VALUES = [] #contem uma tupla (timestamp,value)

    LOCK = threading.Lock()

    VERBOSE = 0
    CERT_PATH = ''

    '''
    Server constructor.
    param: id - Server id
    param: ip - Server ip
    param: port - Server port
    param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all  
    param: cert_path - Path to certificates
    '''
    def __init__(self, id, ip, port, verbose, cert_path):
        print("servidor " + Define.plataform + " " + str(id) + "rodando...")
        self.ID = id
        self.HOST = ip
        self.PORT = port
        self.VERBOSE = verbose
        self.CERT_PATH = cert_path
        self.waitForConnection()

    '''
    Fuction where server's main thread keeps waiting for a new connection from client.
    Starts a new thread to manipulate new connections.
    '''
    def waitForConnection(self):
        socketTCP = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        orig = (self.HOST, self.PORT)
        socketTCP.bind(orig)
        socketTCP.listen(1)

        while True:
            con, client = socketTCP.accept()
            threading.Thread(target=self.clientConnected, args=[con]).start()

    '''
    Waits for client's message.
    param: socketTCPThread - Socket that has been created for the pair (Server, Client)
    '''
    def clientConnected(self, socketTCPThread):
        if self.VERBOSE > 0:
            print("Novo cliente conectado, nova thread criada")

        try:
            data = socketTCPThread.recv(2048)
            message = data.decode('utf-8')
            if self.VERBOSE == 2:
                print('-----REQUEST CHEGANDO:-----' + message)
            request = json.loads(data.decode('utf-8'))
            self.getRequestStatus(request,socketTCPThread)
        except Exception as msg:
            if self.VERBOSE > 0:
                print('Error on clientConnected, ' + 'Error code: ' + str(msg[0]) + ', Error message: ' + str(msg[1]))

        socketTCPThread.close()
        if self.VERBOSE > 0:
            print('matando thread')


    '''
    Analyse user's message and forwards to the correct handler.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def getRequestStatus(self, request, socketTCP):
        try:
            type = request[Define.type]

            if type == Define.write or type == Define.write_back:
                self.write(request,socketTCP, type)

            elif type == Define.read:
                self.read(request,socketTCP)

            elif type == Define.read_timestamp:
                self.readTimestamp(request, socketTCP)

            elif type == Define.get_echoe:
                self.getEchoe(request, socketTCP)

            else:
                response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.error, msg = Define.undefined_type)
                responseJSON = json.dumps(response)
                socketTCP.send(responseJSON.encode('utf-8'))
                if self.VERBOSE == 2:
                    print('-----REQUEST SAINDO:-----' + responseJSON)

        except:
            response = dict(server_id = self.ID, plataform = Define.plataform, status = Define.error, msg = Define.unknown_error)
            responseJSON = json.dumps(response)
            socketTCP.send(responseJSON.encode('utf-8'))
            if self.VERBOSE == 2:
                print('-----REQUEST SAINDO:-----' + responseJSON)

    '''
    Write data in register if the requirements are followed.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def write(self, request, socketTCP, type):
        variable = request[Define.variable]
        timestamp = request[Define.timestamp]

        echoesArray = request[Define.echoes]
        echoes = []
        for dictionary in echoesArray:
            echoes.append((dictionary[Define.server_id], dictionary[Define.data_signature]))

        self.LOCK.acquire()
        if timestamp > self.TIMESTAMP:
            if self.isEchoValid(echoes, variable, timestamp, type):
                self.VARIABLE = variable
                self.TIMESTAMP = timestamp
                self.DATA_SIGNATURE = Signature.signData(Signature.getPrivateKey(self.ID, -1, self.CERT_PATH), variable + str(timestamp))
                self.LAST_ECHOED_VALUES = []

                self.LOCK.release()

                if self.VERBOSE > 0:
                    print("Recebido variable = " + variable + " e timestamp " + str(timestamp))

                response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.success, msg = Define.variable_updated)
                responseJSON = json.dumps(response)

                socketTCP.send(responseJSON.encode('utf-8'))
                if self.VERBOSE == 2:
                    print('-----REQUEST SAINDO:-----' + responseJSON)

            else:
                self.LOCK.release()

                response = dict(server_id=self.ID, plataform=Define.plataform, request_code=request[Define.request_code], status=Define.error, msg=Define.invalid_echoes)
                responseJSON = json.dumps(response)

                socketTCP.send(responseJSON.encode('utf-8'))
                if self.VERBOSE == 2:
                    print('-----REQUEST SAINDO:-----' + responseJSON)

        else:
            self.LOCK.release()

            response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.error, msg = Define.outdated_timestamp)
            responseJSON = json.dumps(response)

            socketTCP.send(responseJSON.encode('utf-8'))
            if self.VERBOSE == 2:
                print('-----REQUEST SAINDO:-----' + responseJSON)

    '''
    Sends data in register for client.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def read(self, request, socketTCP):
        with self.LOCK:
            dataDict = dict(variable = self.VARIABLE, timestamp = self.TIMESTAMP, data_signature = self.DATA_SIGNATURE)

        response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.success, msg = Define.read, data = dataDict)
        responseJSON = json.dumps(response)

        socketTCP.send(responseJSON.encode('utf-8'))
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + responseJSON)

    '''
    Sends timestamp in register for client.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def readTimestamp(self, request, socketTCP):
        with self.LOCK:
            dataDict = dict(timestamp = self.TIMESTAMP)

        response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.success, msg = Define.read_timestamp, data = dataDict)
        responseJSON = json.dumps(response)

        socketTCP.send(responseJSON.encode('utf-8'))
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + responseJSON)


    '''
    Sends echo for timestamp and value
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def getEchoe(self, request, socketTCP):
        variable = request[Define.variable]
        timestamp = request[Define.timestamp]

        if timestamp < self.TIMESTAMP:
            response = dict(server_id=self.ID, plataform=Define.plataform, request_code=request[Define.request_code], status=Define.error, msg=Define.outdated_timestamp)
            responseJSON = json.dumps(response)

        elif not self.shouldEcho(variable, timestamp):
            response = dict(server_id=self.ID, plataform=Define.plataform, request_code=request[Define.request_code], status=Define.error, msg=Define.timestamp_already_echoed)
            responseJSON = json.dumps(response)

        else:
            data_signature = Signature.signData(Signature.getPrivateKey(self.ID, -1, self.CERT_PATH), variable + str(timestamp))

            with self.LOCK:
                self.LAST_ECHOED_VALUES.append((timestamp, variable))

            dataDict = dict(data_signature = data_signature)
            response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.success, msg = Define.get_echoe, data = dataDict)
            responseJSON = json.dumps(response)

        socketTCP.send(responseJSON.encode('utf-8'))
        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + responseJSON)


    '''
    Check if value was echoed before
    param: value - Variable to sign.
    param: timestamp - Timestamp.
    return: (bool) If server should echo value and timestamp
    '''
    def shouldEcho(self, value, timestamp):
        with self.LOCK:
            for (auxTimestamp, auxValue) in self.LAST_ECHOED_VALUES:
                if timestamp == auxTimestamp and value != auxValue:
                    return False
                elif timestamp == auxTimestamp and value == auxValue:
                    return True

            return True

    '''
    Check if echoes are valid
    param: echoes - Array with tuples(server_id, data_signature)
    param: value - Variable to sign.
    param: timestamp - Timestamp.
    param: type - If is a write or write_back
    return: (bool) If echoes are valid
    '''
    def isEchoValid(self, echoes, value, timestamp, type):
        validEchoes = 0
        for (server_id, data_signature) in echoes:
            try:
                if Signature.verifySign(Signature.getPublicKey(server_id, -1, self.CERT_PATH), data_signature, value + str(timestamp)):
                    validEchoes = validEchoes + 1

            except Exception as msg:
                if self.VERBOSE > 0:
                    print('Error on isEchoValid, ' + 'Error code: ' + str(msg[0]) + ', Error message: ' + str(msg[1]))

        if type == Define.write :
            return validEchoes >= self.QUORUM
        else: #write_back
            return validEchoes >= self.FAULTS + 1