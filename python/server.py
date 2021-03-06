import socket
import threading
import json
import Define


class Server(object):
    HOST = ''
    PORT = -1
    ID = -1

    VARIABLE = ''
    TIMESTAMP = -1
    DATA_SIGNATURE = ''
    CLIENT_ID = -1

    LOCK = threading.Lock();

    VERBOSE = 0

    '''
    Server constructor.
    param: id - Server id
    param: ip - Server ip
    param: port - Server port
    param: verbose - Verbose level: 0 - no print, 1 - print important, 2 - print all  
    '''
    def __init__(self, id, ip, port, verbose):
        print("servidor " + Define.plataform + " " + str(id) + "rodando...")
        self.ID = id
        self.HOST = ip
        self.PORT = port
        self.VERBOSE = verbose
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

        socketTCP.close()

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
            request = json.loads(message)
            if self.VERBOSE == 2:
                print('-----REQUEST CHEGOU:-----' + message)
            self.getRequestStatus(request,socketTCPThread)
        except socket.error as msg:
            if self.VERBOSE > 0:
                print('Error code: ' + str(msg[0]) + ', Error message: ' + str(msg[1]))

        except Exception as e:
            if self.VERBOSE > 0:
                print('Exception: ' + str(e))

        if self.VERBOSE > 0:
            print('matando thread')
        socketTCPThread.close()

    '''
    Analyse user's message and forwards to the correct handler.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def getRequestStatus(self, request, socketTCP):
        try:
            type = request[Define.type]

            if type == Define.write:
                self.write(request,socketTCP)

            elif type == Define.read:
                self.read(request,socketTCP)

            elif type == Define.read_timestamp:
                self.readTimestamp(request, socketTCP)

            elif type == Define.bye:
                socketTCP.close()
                if self.VERBOSE > 0:
                    print('cliente desconectou propositalmente')
                return

            else:
                response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.error, msg = Define.undefined_type)
                responseJSON = json.dumps(response)
                if self.VERBOSE == 2:
                    print('-----REQUEST SAINDO:-----' + responseJSON)
                socketTCP.send(responseJSON.encode('utf-8'))

        except:
            response = dict(server_id = self.ID, plataform = Define.plataform, status = Define.error, msg = Define.unknown_error)
            responseJSON = json.dumps(response)
            if self.VERBOSE == 2:
                print('-----REQUEST SAINDO:-----' + responseJSON)
            socketTCP.send(responseJSON.encode('utf-8'))

    '''
    Write data in register if the requirements are followed.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def write(self, request, socketTCP):
        variable = request[Define.variable]
        timestamp = request[Define.timestamp]
        signature = request[Define.data_signature]
        client_id = request[Define.client_id]

        self.LOCK.acquire()
        if timestamp > self.TIMESTAMP or (timestamp == self.TIMESTAMP and client_id > self.CLIENT_ID):
            if self.VERBOSE > 0:
                print("Recebido variable = " + variable + " e timestamp " + str(timestamp))

            self.VARIABLE = variable
            self.TIMESTAMP = timestamp
            self.DATA_SIGNATURE = signature
            self.CLIENT_ID = client_id

            response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.success, msg = Define.variable_updated)
            responseJSON = json.dumps(response)
            self.LOCK.release()

            if self.VERBOSE == 2:
                print('-----REQUEST SAINDO:-----' + responseJSON)
            socketTCP.send(responseJSON.encode('utf-8'))

        else:
            self.LOCK.release()

            response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.error, msg = Define.outdated_timestamp)
            responseJSON = json.dumps(response)
            if self.VERBOSE == 2:
                print('-----REQUEST SAINDO:-----' + responseJSON)
            socketTCP.send(responseJSON.encode('utf-8'))

    '''
    Sends data in register for client.
    param: request - A dictionary with client's request data.
    param: socketTCP - Socket that has been created for the pair (Server, Client)
    '''
    def read(self, request, socketTCP):
        with self.LOCK:
            dataDict = dict(variable = self.VARIABLE, timestamp = self.TIMESTAMP, data_signature = self.DATA_SIGNATURE, client_id = self.CLIENT_ID)
            response = dict(server_id = self.ID, plataform = Define.plataform, request_code = request[Define.request_code], status = Define.success, msg = Define.read, data = dataDict)
            responseJSON = json.dumps(response)

        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + responseJSON)
        socketTCP.send(responseJSON.encode('utf-8'))

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

        if self.VERBOSE == 2:
            print('-----REQUEST SAINDO:-----' + responseJSON)
        socketTCP.send(responseJSON.encode('utf-8'))