from server import Server
import sys

DEBUG = True

if DEBUG:
    id = 3
    Server(id, 'localhost', 5000 + id, 2)

else:
    if len(sys.argv) < 4:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    elif len(sys.argv) >= 1:
        print (sys.argv)


    ip = sys.argv[1]
    id = int(sys.argv[2])
    verbose = int(sys.argv[3])
    Server(id, ip, 5000+id, verbose)