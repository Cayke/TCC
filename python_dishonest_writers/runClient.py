from client import Client
from robotClient import RobotClient
import sys

DEBUG = True

if DEBUG:
    servers = [('localhost', 5000), ('localhost', 5001), ('localhost', 5002), ('localhost', 5003)]
    Client(0, servers, 2, '/OneDrive/unb/TCC/git/certs/')
    # RobotClient(1, servers, 2, '/OneDrive/unb/TCC/git/certs/', 250, 'read', '/OneDrive/unb/TCC/git/results/')

else:
    if len(sys.argv) < 13:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    else:
        print (sys.argv)


    id = int(sys.argv[1])
    verbose = int(sys.argv[2])
    cert_path = sys.argv[3]
    exec_times = int(sys.argv[4])
    operation = sys.argv[5]
    results_path = sys.argv[6]

    servers = []
    i = 7
    while i < len(sys.argv):
        servers.append((sys.argv[i], int(sys.argv[i+1])))
        i = i+2

    RobotClient(id, servers, verbose, cert_path, exec_times, operation, results_path)