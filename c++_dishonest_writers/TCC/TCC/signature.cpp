//
//  signature.cpp
//  TCC
//
//  Created by Cayke Prudente on 20/04/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

#include "signature.hpp"

#include <openssl/pem.h>
#include <openssl/rsa.h>
#include "base64.hpp"

//signature after b64 decode.
bool verify(int server_id, const unsigned char *originalMessage, int messageSize, const unsigned char *signature) {
    int result;
    int rc;
    FILE *file;
    
    file        = fopen("server0_public.pem", "r");
    RSA *rsa = PEM_read_RSA_PUBKEY(file, NULL, NULL, NULL);
    fclose(file);
    
    SHA256_CTX sha_ctx = { 0 };
    unsigned char digest[SHA256_DIGEST_LENGTH];
    
    rc = SHA256_Init(&sha_ctx);
    if (1 != rc) {
        exit(-1);
    }
    
    rc = SHA256_Update(&sha_ctx, originalMessage, messageSize);
    if (1 != rc) {
        exit(-1);
    }
    
    rc = SHA256_Final(digest, &sha_ctx);
    if (1 != rc) {
        exit(-1);
    }
    
    result = RSA_verify(NID_sha256, digest, sizeof(digest), signature, RSA_size(rsa), rsa);
    
    if (result == 1)
        return true;
    else
        return false;
}
