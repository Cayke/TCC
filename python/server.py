import socket
import threading
import json
import Define
from signature import Signature

class Server(object):
    HOST = Define.ip
    PORT = Define.port

    DATA = ''
    TIMESTAMP = 0
    DATA_SIGNATURE = ''
    CLIENT_ID = -1

    LOCK = threading.Lock();

    def __init__(self):
        print("servidor rodando...")
        self.waitForConnection()

    def waitForConnection(self):
        socketTCP = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        orig = (self.HOST, self.PORT)
        socketTCP.bind(orig)
        socketTCP.listen(1)

        while True:
            con, client = socketTCP.accept()
            threading.Thread(target=self.clientConnected, args=(con,client)).start()

        socketTCP.close()

    def clientConnected(self, socketTCPThread, client):
        print("Novo cliente conectado, nova thread criada")
        while True:
            try:
                data = socketTCPThread.recv(2048)
                request = json.loads(data.decode('utf-8'))
                self.getRequestStatus(request,socketTCPThread,client)
            except socket.error as msg:
                print('Error code: ' + str(msg[0]) + ', Error message: ' + str(msg[1]))
                print('matando thread')
                socketTCPThread.close()
                return False
            except:
                print('matando thread')
                socketTCPThread.close()
                return False

    def getRequestStatus(self, request, socketTCP, client):
        try:
            type = request[Define.type]

            if type == Define.write:
                self.write(request,socketTCP,client)

            elif type == Define.read:
                self.read(request,socketTCP,client)

            elif type == Define.read_timestamp:
                self.readTimestamp(request, socketTCP,client)

            elif type == Define.bye:
                socketTCP.close()
                print('cliente desconectou propositalmente')
                return

            else:
                response = dict(serverID = Define.plataform, status = Define.error, msg = Define.undefined_type)
                responseJSON = json.dumps(response)
                socketTCP.send(responseJSON.encode('utf-8'))

        except:
            response = dict(serverID = Define.plataform, status = Define.error, msg = Define.unknown_error)
            responseJSON = json.dumps(response)
            socketTCP.send(responseJSON.encode('utf-8'))

    def write(self, request, socketTCP, client):
        variable = request[Define.variable]
        timestamp = request[Define.timestamp]
        signature = request[Define.data_signature]
        client_id = request[Define.client_id]

        self.LOCK.acquire()
        if timestamp > self.TIMESTAMP:

            if Signature.verify_sign(Signature.getPublicKey(-1, client_id), signature, variable+str(timestamp)):
                print("Recebido variable = " + variable + " e timestamp " + str(timestamp))

                self.DATA = variable
                self.TIMESTAMP = timestamp
                self.DATA_SIGNATURE = signature
                self.CLIENT_ID = client_id

                response = dict(serverID = Define.plataform, status = Define.success, msg = Define.variable_updated, request_code = request[Define.request_code])
                responseJSON = json.dumps(response)
                self.LOCK.release()

                socketTCP.send(responseJSON.encode('utf-8'))

            else:
                print("Recebido dado com assinatura invalida.")

                response = dict(serverID=Define.plataform, status=Define.error, msg=Define.invalid_signature,
                request_code=request[Define.request_code])
                responseJSON = json.dumps(response)
                self.LOCK.release()

                socketTCP.send(responseJSON.encode('utf-8'))

        else:
            self.LOCK.release()

            response = dict(serverID = Define.plataform, status = Define.error, msg = Define.outdated_timestamp)
            responseJSON = json.dumps(response)
            socketTCP.send(responseJSON.encode('utf-8'))

    def read(self, request, socketTCP, client):
        with self.LOCK:
            dataDict = dict(variable = self.DATA, timestamp = self.TIMESTAMP)
            response = dict(serverID = Define.plataform, status = Define.success, msg = Define.read, data = dataDict, request_code = request[Define.request_code])
            responseJSON = json.dumps(response)

        socketTCP.send(responseJSON.encode('utf-8'))

    def readTimestamp(self, request, socketTCP, client):
        with self.LOCK:
            dataDict = dict(timestamp = self.TIMESTAMP)
            response = dict(serverID = Define.plataform, status = Define.success, msg = Define.read_timestamp, data = dataDict, request_code = request[Define.request_code])
            responseJSON = json.dumps(response)

        socketTCP.send(responseJSON.encode('utf-8'))