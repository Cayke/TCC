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
#include "string.h"
#include "signature.hpp"
#include "base64.hpp"
#include <openssl/bio.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>

//int main(int argc, const char * argv[]) {
//    // insert code here...
//    server::init(0, "localhost", 5000);
//
//
//    return 0;
//}

void test();

int main(int, char **) {
    test();
    
    std::string message = "{\"carrer\": \"ios dev\", \"age\": 23, \"name\": \"cayke\"}0";
    std::string sign64 = "dC19qf5WbZ/ugx76wvf1eUvNWxrHiaiYoHcmntBdxtN/pX/WNbmpyr8ALPUJusKfQb0gTcRqwC4ztO4MPVQeNXWgSBWUrH0MQRZB9sT7cfcpwrPE8mbbxHwqHRoY4F/vHQU75odzouS0YJNEuSsPX6j8wI90b/GJPAdATsq6NXI=";
    
    const unsigned char *originalMessage = (const unsigned char *)message.c_str();
    int size = strlen(message.c_str());
    
    const unsigned char *signature = (const unsigned char *)base64decode(sign64.c_str(), strlen(sign64.c_str()));
    
    bool isValidSignature = verify(0, originalMessage, size, signature);
    
    return 0;
}

void test() {
    int result;
    int rc;
    FILE *file;
    
    std::string message = "{\"carrer\": \"ios dev\", \"age\": 23, \"name\": \"cayke\"}0";
    const unsigned char *originalMessage = (const unsigned char *)message.c_str();
    int size = strlen(message.c_str());
    
    file        = fopen("server0_private_pcks8.pem", "r");
    RSA *rsa = PEM_read_RSAPrivateKey(file, NULL, NULL, NULL);
    fclose(file);
    
    SHA256_CTX sha_ctx = { 0 };
    unsigned char digest[SHA256_DIGEST_LENGTH];
    
    rc = SHA256_Init(&sha_ctx);
    if (1 != rc) {
        exit(-1);
    }
    
    rc = SHA256_Update(&sha_ctx, originalMessage, size);
    if (1 != rc) {
        exit(-1);
    }
    
    rc = SHA256_Final(digest, &sha_ctx);
    if (1 != rc) {
        exit(-1);
    }
    
    unsigned char *sig = (unsigned char *) malloc(RSA_size(rsa));
    unsigned int sig_len = 0;
    rc = RSA_sign(NID_sha256, digest, sizeof(digest), sig, &sig_len, rsa);
    //rc = RSA_sign(NID_sha256, digest, sizeof(digest), sig, &sig_len, rsa);
    if (1 != rc) {
        exit(-1);
    }
    
    std::string AEPORRA = base64encode(sig, RSA_size(rsa));
}
