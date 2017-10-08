from client import Client
import sys

DEBUG = False

if DEBUG:
    servers = [('node0.caykequoruns.freestore.emulab.net', 5000), ('node2.caykequoruns.freestore.emulab.net', 5000), ('node4.caykequoruns.freestore.emulab.net', 5000)]
    Client(0, servers, 2, '/OneDrive/unb/TCC/DEV/certs/')

else:
    if len(sys.argv) < 10:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    else:
        print (sys.argv)


    id = sys.argv[1]
    verbose = sys.argv[2]
    cert_path = sys.argv[3]

    servers = []
    i = 4
    while i <= len(sys.argv):
        servers.append((sys.argv[i], sys.argv[i+1]))
        i = i+2

    Client(id, servers, verbose, cert_path)