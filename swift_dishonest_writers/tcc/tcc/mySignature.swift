//
//  mySignature.swift
//  tcc
//
//  Created by Cayke Prudente on 17/04/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

import Foundation

/*
 To generate a new RSA keypair with OpenSSL:
 openssl genrsa -out private_key.pem 512
 openssl rsa -pubout -in private_key.pem -out public_key.pem
 */

class MySignature: NSObject {
    
    /*
     Returns a private Key. Attribute must be the ID number of the host, and the other must be -1
     param: server - ID from the server (if not server, pass -1)
     param: client - ID from the client (if not client, pass -1)
     return: (String) The private certificate.
     */
    static func getPrivateKey(server : Int, client : Int) -> String?{
        var filePath = ""
        if server != -1 {
            filePath = "server" + String(server) + "_private";
        }
        else {
            filePath = "client" + String(client) + "_private";
        }
        
        if let key = getContentOfFile(fileName: filePath, format: "pem") {
            return key
        }
        else {
            return nil
        }
    }
    
    
    /*
     Returns a public Key. Attribute must be the ID number of the host, and the other must be -1
     param: server - ID from the server (if not server, pass -1)
     param: client - ID from the client (if not client, pass -1)
     return: (String) The public certificate.
     */
    static func getPublicKey(server : Int, client : Int) -> String?{
        var filePath = ""
        if server != -1 {
            filePath = "server" + String(server) + "_public";
        }
        else {
            filePath = "client" + String(client) + "_public";
        }
        
        if let key = getContentOfFile(fileName: filePath, format: "pem") {
            return key
        }
        else {
            return nil
        }
    }
    
    
    /*
     Returns the value from file.
     param: file - File's name
     param: format - File's format
     return: (String) The content of file
     */
    static func getContentOfFile(fileName : String, format : String) -> String? {
        if let dir = Bundle.main.path(forResource: fileName, ofType: format) {
            var content = ""
            do {
                content = try String(contentsOfFile: dir)
            } catch {
                print("Failed reading: \(fileName), Error: " + error.localizedDescription)
            }
            return content
        }
        
        print ("nao foi possivel achar o arquivo pem")
        
        return nil
    }
    
    
    /*
     Signs data with a private Certificate.
     param: privateKey - Your private key
     param: data - Data to be signed
     return: (String) base64 encoded signature
     */
    static func signData(privateKey : String, data : String) -> String? {
        let myData = data.data(using: .utf8)
        
        do {
            let signature = data.sha256(key: privateKey)
            return signature
        }
        catch {
            return nil
        }
    }
    
    
    /*
     Verifies with a public key from whom the data came that it was indeed
     signed by their private key
     param: publicKey - Public Key from signer
     param: signature - Signature to be verified in base64 format
     param: data - Data that was signed.
     return: (Boolean) True if the signature is valid; False otherwise.
     */
    static func verifySign(publicKey : String, signature_b64 : String, data : String) -> Bool {
//        let cryptor = CCMCryptor()
//        let signature = CCMBase64.data(fromBase64String: signature_b64)
//        
//        do {
//            let decryptedData = try cryptor.decryptData(signature, with: publicKey)
//            
//            return true
//        }
//        catch {
//            return false
//        }
        return false
    }
}
