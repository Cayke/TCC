from server import Server
import sys

DEBUG = True

if DEBUG:
    Server(0, 'localhost', 5000, 2, '/OneDrive/unb/TCC/DEV/certs/')

else:
    if len(sys.argv) < 5:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    else:
        print (sys.argv)


    ip = sys.argv[1]
    id = sys.argv[2]
    verbose = sys.argv[3]
    cert_path = sys.argv[4]
    Server(id, ip, 5000+id, verbose, cert_path)