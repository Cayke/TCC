from client import Client
import sys

DEBUG = True

if DEBUG:
    servers = [('192.168.0.199', 5000), ('192.168.0.199', 5001), ('192.168.0.199', 5002), ('192.168.0.199', 5003)]
    Client(0, servers, 2, '/OneDrive/unb/TCC/DEV/certs/')

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