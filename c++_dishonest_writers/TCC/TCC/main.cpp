//
//  main.cpp
//  TCC
//
//  Created by Cayke Prudente on 23/11/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

#include <iostream>
#include "server.hpp"
#include <string>
#include "signature.hpp"

int main(int argc, const char * argv[]) {
    // insert code here...
    int id = 0;
    server::init(id, "localhost", 5000 + id);


    return 0;
}
