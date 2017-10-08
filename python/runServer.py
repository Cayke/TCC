from server import Server
import sys

DEBUG = False

if DEBUG:
    Server(0, 'localhost', 5000, 2)

else:
    if len(sys.argv) < 4:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    elif sys.argv >= 1:
        print (sys.argv)


    ip = sys.argv[1]
    id = sys.argv[2]
    verbose = sys.argv[3]
    Server(id, ip, 5000+id, verbose)