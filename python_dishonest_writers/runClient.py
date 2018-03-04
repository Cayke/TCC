from client import Client
from robotClient import RobotClient
import sys

DEBUG = True

if DEBUG:
    servers = [('localhost', 5000), ('localhost', 5001), ('localhost', 5002), ('localhost', 5003)]
    RobotClient(1, servers, 1, '/OneDrive/unb/TCC/git/certs/', 250, 'read', '/OneDrive/unb/TCC/git/results/')

else:
    if len(sys.argv) < 10:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    else:
        print (sys.argv)


    id = int(sys.argv[1])
    verbose = int(sys.argv[2])
    cert_path = sys.argv[3]

    servers = []
    i = 4
    while i < len(sys.argv):
        servers.append((sys.argv[i], int(sys.argv[i+1])))
        i = i+2

    Client(id, servers, verbose, cert_path)