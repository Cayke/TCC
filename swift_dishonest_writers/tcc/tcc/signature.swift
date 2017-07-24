//
//  signature.swift
//  tcc
//
//  Created by Cayke Prudente on 20/06/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

import Foundation

class signature: NSObject {
    /*
    Returns a private Key. Attribute must be the ID number of the host, and the other must be -1
    param: server - ID from the server (if not server, pass -1)
    param: client - ID from the client (if not client, pass -1)
    return: (String) The private certificate.
    */
    static func getPrivateKey(server : Int, client : Int) -> String? {
        var filePath = ""
        if server != -1 {
            filePath = "server" + String(server) + "_private.pem"
        }
        else {
            filePath = "client" + String(client) + "_private.pem"
        }
        
        let location = NSString(string: "~/Desktop/certs/" + filePath).expandingTildeInPath
        return try? String(contentsOfFile: location, encoding: .utf8)
    }
    
    
    /*
    Returns a public Key. Attribute must be the ID number of the host, and the other must be -1
    param: server - ID from the server (if not server, pass -1)
    param: client - ID from the client (if not client, pass -1)
    return: (String) The public certificate.
    */
    static func getPublicKey(server : Int, client : Int) -> String? {
        var filePath = ""
        if server != -1 {
            filePath = "server" + String(server) + "_public.pem"
        }
        else {
            filePath = "client" + String(client) + "_public.pem"
        }
        
        let location = NSString(string: "~/Desktop/certs/" + filePath).expandingTildeInPath
        return try? String(contentsOfFile: location, encoding: .utf8)
    }
    
    
    /*
     Signs data with a private Certificate.
     param: server_id - Signer's id
     param: message - Data to be signed
     return: (String) base64 encoded signature
     */
    static func signData(server_id : Int, message : String) -> String? {
        guard let privateKeyPEM = getPrivateKey(server: server_id, client: -1) else {
            return nil
        }
        
        guard let privateKeyDER = try? SwKeyConvert.PrivateKey.pemToPKCS1DER(privateKeyPEM) else {
            return nil
        }
        
        let messageData = message.data(using: .utf8)!
        
        guard let sign = try? CC.RSA.sign(messageData, derKey: privateKeyDER, padding: .pkcs15, digest: .sha256, saltLen: 32) else {
            return nil
        }
        
        return sign.base64EncodedString()
    }
    
    
    /*
     Verifies with a public key from whom the data came that it was indeed signed by their private key
     param: server_id - Server's id that signed message
     param: signature - Signature to be verified (in base64 format)
     param: message - Data that was signed.
     return: (Boolean) True if the signature is valid; False otherwise.
     */
    static func verifySignature(server_id : Int, originalMessage : String, signature : String) -> Bool {
        guard let publicKeyPEM = getPublicKey(server: server_id, client: -1) else {
            return false
        }
        
        guard let publicKeyDER = try? SwKeyConvert.PublicKey.pemToPKCS1DER(publicKeyPEM) else {
            return false
        }
        
        let messageData = originalMessage.data(using: .utf8)!
        
        guard let signatureData = Data(base64Encoded: signature, options: .init(rawValue: 0)) else {
            return false
        }
        
        guard let verified = try? CC.RSA.verify(messageData, derKey: publicKeyDER, padding: .pkcs15, digest: .sha256, saltLen: 32, signedData: signatureData as Data) else {
            return false
        }
        
        return verified
    }
    
    static func base64Decode(string : String) -> String? {
        guard let data = Data(base64Encoded: string, options: .init(rawValue: 0)) else {
            return nil
        }
        
        return String(data: data as Data, encoding: .utf8)
    }
    
    static func base64Encode(string : String) -> String {
        return Data(string.utf8).base64EncodedString()
    }
}
