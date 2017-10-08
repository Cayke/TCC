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
#include <string>

bool verify(int server_id, std::string message, std::string signature, std::string cert_path);
std::string signData(int server_id, std::string message, std::string cert_path);

char *base64encode (const void *b64_encode_this, int encode_this_many_bytes);
char *base64decode (const void *b64_decode_this, int decode_this_many_bytes);


#endif /* signature_hpp */
