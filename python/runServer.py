from server import Server
import sys

if len(sys.argv) < 2:
    print("numero de argumentos invalidos")
    exit(-1)
else:
    print (sys.argv)

Server(0, sys.argv[1], 5000)