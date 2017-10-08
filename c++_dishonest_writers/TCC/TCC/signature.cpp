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
#include <string>
#include "string.h"


/*
Verifies with a public key from whom the data came that it was indeed signed by their private key
param: server_id - Server's id that signed message
param: signature - Signature to be verified (in base64 format)
param: message - Data that was signed.
param: cert_path - Path to certificates
return: (Boolean) True if the signature is valid; False otherwise.
*/
bool verify(int server_id, std::string message, std::string signature, std::string cert_path) {
    int result;
    
    const unsigned char *originalMessage = (const unsigned char *)message.c_str();
    int messageSize = (int)strlen(message.c_str());
    
    const unsigned char *sign = (const unsigned char *)base64decode(signature.c_str(), (int)strlen(signature.c_str()));
    
    std::string path = cert_path + "server" + std::to_string(server_id) + "_public.pem";
    FILE *file = fopen(path.c_str(), "r");
    if (file == NULL)
        return false;
    
    RSA *rsa = PEM_read_RSA_PUBKEY(file, NULL, NULL, NULL);
    fclose(file);
    
    SHA256_CTX sha_ctx = { 0 };
    unsigned char digest[SHA256_DIGEST_LENGTH];
    
    result = SHA256_Init(&sha_ctx);
    if (1 != result)
        return false;
    
    result = SHA256_Update(&sha_ctx, originalMessage, messageSize);
    if (1 != result)
        return false;
    
    result = SHA256_Final(digest, &sha_ctx);
    if (1 != result)
        return false;
    
    result = RSA_verify(NID_sha256, digest, sizeof(digest), sign, RSA_size(rsa), rsa);
    
    if (result == 1)
        return true;
    else
        return false;
}


/*
Signs data with a private Certificate.
param: server_id - Signer's id
param: message - Data to be signed
param: cert_path - Path to certificates
return: (String) base64 encoded signature
*/
std::string signData(int server_id, std::string message, std::string cert_path) {
    int result;
    
    const unsigned char *originalMessage = (const unsigned char *)message.c_str();
    int messageSize = (int) strlen(message.c_str());
    
    std::string path = cert_path + "server" + std::to_string(server_id) + "_private.pem";
    FILE *file = fopen(path.c_str(), "r");
    if (file == NULL)
        return NULL;

    RSA *rsa = PEM_read_RSAPrivateKey(file, NULL, NULL, NULL);
    fclose(file);
    
    SHA256_CTX sha_ctx = { 0 };
    unsigned char digest[SHA256_DIGEST_LENGTH];
    
    result = SHA256_Init(&sha_ctx);
    if (1 != result)
        return NULL;
    
    result = SHA256_Update(&sha_ctx, originalMessage, messageSize);
    if (1 != result)
        return NULL;
    
    result = SHA256_Final(digest, &sha_ctx);
    if (1 != result)
        return NULL;
    
    unsigned char *sig = (unsigned char *) malloc(RSA_size(rsa));
    unsigned int sig_len = 0;
    result = RSA_sign(NID_sha256, digest, sizeof(digest), sig, &sig_len, rsa);
    if (1 != result)
        return NULL;
    
    std::string sign_b64 = base64encode(sig, RSA_size(rsa));
    
    return sign_b64;
}



//---------------------------- FOUND ON WEB -----------------------------
char *base64encode (const void *b64_encode_this, int encode_this_many_bytes){
    BIO *b64_bio, *mem_bio;      //Declares two OpenSSL BIOs: a base64 filter and a memory BIO.
    BUF_MEM *mem_bio_mem_ptr;    //Pointer to a "memory BIO" structure holding our base64 data.
    b64_bio = BIO_new(BIO_f_base64());                      //Initialize our base64 filter BIO.
    mem_bio = BIO_new(BIO_s_mem());                           //Initialize our memory sink BIO.
    BIO_push(b64_bio, mem_bio);            //Link the BIOs by creating a filter-sink BIO chain.
    BIO_set_flags(b64_bio, BIO_FLAGS_BASE64_NO_NL);  //No newlines every 64 characters or less.
    BIO_write(b64_bio, b64_encode_this, encode_this_many_bytes); //Records base64 encoded data.
    BIO_flush(b64_bio);   //Flush data.  Necessary for b64 encoding, because of pad characters.
    BIO_get_mem_ptr(mem_bio, &mem_bio_mem_ptr);  //Store address of mem_bio's memory structure.
    BIO_set_close(mem_bio, BIO_NOCLOSE);   //Permit access to mem_ptr after BIOs are destroyed.
    BIO_free_all(b64_bio);  //Destroys all BIOs in chain, starting with b64 (i.e. the 1st one).
    BUF_MEM_grow(mem_bio_mem_ptr, (*mem_bio_mem_ptr).length + 1);   //Makes space for end null.
    (*mem_bio_mem_ptr).data[(*mem_bio_mem_ptr).length] = '\0';  //Adds null-terminator to tail.
    return (*mem_bio_mem_ptr).data; //Returns base-64 encoded data. (See: "buf_mem_st" struct).
}

char *base64decode (const void *b64_decode_this, int decode_this_many_bytes){
    BIO *b64_bio, *mem_bio;      //Declares two OpenSSL BIOs: a base64 filter and a memory BIO.
    char *base64_decoded = (char *) calloc((decode_this_many_bytes*3)/4 + 1, sizeof(char)); //+1 = null.
    b64_bio = BIO_new(BIO_f_base64());                      //Initialize our base64 filter BIO.
    mem_bio = BIO_new(BIO_s_mem());                         //Initialize our memory source BIO.
    BIO_write(mem_bio, b64_decode_this, decode_this_many_bytes); //Base64 data saved in source.
    BIO_push(b64_bio, mem_bio);          //Link the BIOs by creating a filter-source BIO chain.
    BIO_set_flags(b64_bio, BIO_FLAGS_BASE64_NO_NL);          //Don't require trailing newlines.
    int decoded_byte_index = 0;   //Index where the next base64_decoded byte should be written.
    while ( 0 < BIO_read(b64_bio, base64_decoded+decoded_byte_index, 1) ){ //Read byte-by-byte.
        decoded_byte_index++; //Increment the index until read of BIO decoded data is complete.
    } //Once we're done reading decoded data, BIO_read returns -1 even though there's no error.
    BIO_free_all(b64_bio);  //Destroys all BIOs in chain, starting with b64 (i.e. the 1st one).
    return base64_decoded;        //Returns base-64 decoded data with trailing null terminator.
}
