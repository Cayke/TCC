from server import Server
import sys

DEBUG = False

if DEBUG:
    id = 2
    Server(id, 'localhost', 5000 + id, 1, '/OneDrive/unb/TCC/git/certs/')

else:
    if len(sys.argv) < 5:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    else:
        print (sys.argv)


    ip = sys.argv[1]
    id = int(sys.argv[2])
    verbose = int(sys.argv[3])
    cert_path = sys.argv[4]
    Server(id, ip, 5000+id, verbose, cert_path)