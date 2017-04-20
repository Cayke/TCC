//
//  signature.hpp
//  TCC
//
//  Created by Cayke Prudente on 20/04/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

#ifndef signature_hpp
#define signature_hpp

#include <stdio.h>

bool verify(int server_id, const unsigned char *originalMessage, int messageSize, const unsigned char *signature);

#endif /* signature_hpp */
