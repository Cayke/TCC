import socket
import json
import threading
import Define
from representedData import RepresentedData
from signature import Signature

class Client ():
    HOST = 'localhost'
    PORTS = [5000,5001,5002]

    ID = 0 # id do cliente. serve para identificar qual chave usar no RSA

    quorum = 2


    LOCK = threading.Lock()
    REQUEST_CODE = 0
    RESPONSES = [] # armazena tuplas (value, timestamp, data_signature, client_id, server)
    OUT_DATED_SERVERS = []


    SEMAPHORE = threading.Semaphore(0)
    LOCK_PRINT = threading.Lock()

    exit = False

    def __init__(self):
        self.run()

    def run(self):
        print('Client running...')
        self.initUserInterface()

    def initUserInterface(self):
        while not self.exit:
            choice = 0
            while choice != '1' and choice != '2' and choice != '3':
                self.cleanScreen()
                with self.LOCK_PRINT:
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
                    self.write(data)

            elif choice == '2':
                self.read()

            elif choice == '3':
                self.terminate()

        print('Adeus')

    def cleanScreen(self):
        pass
        #todo os.system('cls' if os.name == 'nt' else 'clear')

    def write(self, value):
        timestamp = self.read()
        timestamp = self.incrementTimestamp(timestamp)

        data_signature = Signature.signData(Signature.getPrivateKey(-1,self.ID), value+str(timestamp))

        for port in self.PORTS:
            server = (self.HOST, port)
            threading.Thread(target=self.writeOnServer, args=(server, value, timestamp, data_signature, self.ID, self.REQUEST_CODE)).start()

        self.REQUEST_CODE = self.REQUEST_CODE + 1


    def writeBack(self, value, timestamp, data_signature, client_id):
        for server in self.OUT_DATED_SERVERS:
            threading.Thread(target=self.writeOnServer, args=(server, value, timestamp, data_signature, client_id, self.REQUEST_CODE)).start()

        self.REQUEST_CODE = self.REQUEST_CODE + 1


    def read(self):
        with self.LOCK:
            self.RESPONSES = []
            self.OUT_DATED_SERVERS = []

        with self.LOCK_PRINT:
            print("Lendo dados dos servidores....")
        for port in self.PORTS:
            server = (self.HOST, port)
            threading.Thread(target=self.readFromServer, args=(server, self.REQUEST_CODE)).start()

        wasReleased = self.SEMAPHORE.acquire(True, Define.timeout)

        if wasReleased:
            with self.LOCK:
                self.REQUEST_CODE = self.REQUEST_CODE + 1

            if (len(self.RESPONSES) >= self.quorum):
                value, timestamp, data_signature, client_id = self.analyseResponse(self.RESPONSES)

                self.writeBack(value, timestamp, data_signature, client_id)

                data = RepresentedData(value)
                with self.LOCK_PRINT:
                    print('Li o dado do server:')
                    data.showInfo()
                    print ("Timestamp: " + str(timestamp))

                return timestamp #todo criar request que pega apenas o timestamp(nao precisa do timestamp)
            else:
                with self.LOCK_PRINT:
                    print ("EERO NAO ESPERADO!!!!!. Nao foi possivel ler nenhum dado. O semaforo liberou mas nao teve quorum.")

        else:
            with self.LOCK_PRINT:
                print ("Nao foi possivel ler nenhum dado. Timeout da conexao expirado")


    def writeOnServer(self, server, value, timestamp, data_signature, client_id, request_code):
            request = dict(type=Define.write, timestamp=timestamp, variable=value, request_code = request_code, client_id = client_id, data_signature = data_signature)
            requestJSON = json.dumps(request)

            TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            TCPSocket.connect(server)
            TCPSocket.send(requestJSON.encode('utf-8'))

            messageFromServerJSON, server = TCPSocket.recvfrom(2048)
            messageFromServer = json.loads(messageFromServerJSON.decode('utf-8'))
            if messageFromServer[Define.status] == Define.success:
                with self.LOCK_PRINT:
                    print('Variable updated')
            else:
                with self.LOCK_PRINT:
                    print('Error updating')


    def readFromServer(self, server, request_code):
        messageFromServer = self.getValueFromServer(server, request_code)

        if messageFromServer[Define.status] == Define.success and messageFromServer[Define.request_code] == self.REQUEST_CODE:
            data = messageFromServer[Define.data]
            serverTimestamp = data[Define.timestamp]
            serverVariable = data[Define.variable]
            dataSignature = data[Define.data_signature]
            clientID = data[Define.client_id]

            with self.LOCK:
                if len(self.RESPONSES) < self.quorum-1:
                    self.RESPONSES.append((serverVariable, serverTimestamp, dataSignature, clientID, server))

                elif len(self.RESPONSES) == self.quorum-1:
                    self.RESPONSES.append((serverVariable, serverTimestamp, dataSignature, clientID, server))
                    self.SEMAPHORE.release()

                else:
                    #do nothing
                    with self.LOCK_PRINT:
                        print("Quorum ja encheu. Jogando request fora...")

        elif messageFromServer[Define.status] == Define.error:
            with self.LOCK_PRINT:
                print("Ocorreu algum erro na request")

        elif messageFromServer[Define.request_code] != self.REQUEST_CODE:
            with self.LOCK_PRINT:
                print("Response atrasada.")


    def getValueFromServer(self, server, request_code):
        request = dict(type=Define.read, request_code =request_code, client_id = self.ID)
        requestJSON = json.dumps(request)
        TCPSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        TCPSocket.connect(server)
        TCPSocket.send(requestJSON.encode('utf-8'))

        messageFromServerJSON, server = TCPSocket.recvfrom(2048)

        return json.loads(messageFromServerJSON.decode('utf-8'))

    def terminate(self):
        self.exit = True

    def incrementTimestamp(self, serverTimestamp):
        return serverTimestamp+1

    # retorna uma tupla com o dado mais atual e sua quantidade de repeticoes para o quorum atual (value, timestamp)
    def analyseResponse(self, response):
        value = ''
        timestamp = -1
        repeatTimes = 0
        auxServers = []
        data_signature = ''
        client_id = -1

        for (rValue, rTimestamp, data_sign, r_client_id, server) in response:
            if Signature.verify_sign(Signature.getPublicKey(-1, r_client_id), data_sign, rValue + str(rTimestamp)):
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

    # transfere os objetos de um array para outro
    def transferObjects(self, oldArray, newArray):
        for obj in oldArray:
            oldArray.remove(obj)
            newArray.append(obj)