import sys
import os

import Define

DEBUG = True


def getInfoFromClients(path):
    operations = 0
    clients = 0
    average_sum = 0
    init_time = 9999999999
    final_time = 0

    for filename in os.listdir(path):
        if 'txt' in filename and not 'results' in filename:
            file = open(path + filename, 'r')
            file.readline() # ignore header
            file_content = file.readline()

            data = file_content.split(';')

            operations +=  int(data[0])
            average_sum += float(data[1])
            clients += 1
            init_time = min(init_time, float(data[2]))
            final_time = max(final_time, float(data[3]))

    results_file = open(path + 'results.txt', 'w')
    # 'number_of_executions_per_client;clients;latency;throughput\n'
    results_file.write(Define.results_file_header)
    results_file.write(str(operations/clients) + ';' + str(clients) + ';' + str(average_sum/clients) + ';' + str(operations / (final_time - init_time)))

#### MAIN FUNCTION ####
if DEBUG:
    path = '/OneDrive/unb/TCC/git/results/'
    getInfoFromClients(path)


else:
    if len(sys.argv) < 2:
        print("numero de argumentos invalidos. favor ler a documentacao")
        exit(-1)
    else:
        path = sys.argv[1]
        getInfoFromClients(path)