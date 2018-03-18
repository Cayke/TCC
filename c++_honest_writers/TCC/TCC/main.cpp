//
//  main.cpp
//  TCC
//
//  Created by Cayke Prudente on 23/11/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

#include <iostream>
#include "server.hpp"

bool debug = true;

int main(int argc, const char * argv[]) {
    if (debug) {
        int id = 0;
        server::init(id, "localhost", 5000 + id, 2);
    }
    else {
        if (argc < 4) {
            printf("Numero de argumentos invalidos: %d. Favor ler a documentacao", argc);
            return -1;
        }
        else {
            const char *ip = argv[1];
            int id = atoi(argv[2]);
            int verbose = atoi(argv[3]);
            int port = 5000 + id;
            
            server::init(id, ip, port, verbose);
        }
    }
    
    return 0;
}
