//
//  signature.hpp
//  tcc
//
//  Created by Cayke Prudente on 21/04/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

#if __cplusplus
extern "C" {
#endif
#include <stdio.h>
    int verifySignature (const char* server_id, const unsigned char * message, int messageSize, const char * signature);
    const char * signData(const char * server_id, const char * message, int messageSize);
    char *base64encode (const void *b64_encode_this, int encode_this_many_bytes);
    char *base64decode (const void *b64_decode_this, int decode_this_many_bytes);
    
#ifdef __cplusplus
}
#endif
