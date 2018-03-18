package com.caykeprudente;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by cayke on 27/03/17.
 */
public class MySignature {

    /*
    Returns a private Key. Attribute must be the ID number of the host, and the other must be -1
    param: server - ID from the server (if not server, pass -1)
    param: client - ID from the client (if not client, pass -1)
    param: filePath - Path for certs folder
    return: (String) The private certificate.
    */
    public static String getPrivateKey(int server, int client, String filePath) {
        //no java o certificado privado tem que estar no formato pcks8, e o gerado no site e pcks1, executar o comando abaixo para poder usar o cert aqui
        //openssl pkcs8 -topk8 -inform PEM -outform PEM -in client0_private.pem -out client0_private_pcks8.pem -nocrypt

        if (server != -1)
            filePath = filePath + "server" + server + "_private_pcks8.pem";
        else
            filePath = filePath + "client" + client + "_private_pcks8.pem";

        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            String everything = IOUtils.toString(inputStream);
            inputStream.close();
            return  everything;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /*
    Returns a public Key. Attribute must be the ID number of the host, and the other must be -1
    param: server - ID from the server (if not server, pass -1)
    param: client - ID from the client (if not client, pass -1)
    param: filePath - Path for certs folder
    return: (String) The public certificate.
    */
    public static String getPublicKey(int server, int client, String filePath) {
        if (server != -1)
            filePath = filePath + "server" + server + "_public.pem";
        else
            filePath = filePath +"client" + client + "_public.pem";

        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            String everything = IOUtils.toString(inputStream);
            inputStream.close();
            return  everything;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /*
    Signs data with a private Certificate.
    param: privateKey - Your private key
    param: data - Data to be signed
    return: (String) base64 encoded signature
    */
    public static String signData(String privateKey, String data) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(getPrivateKey(privateKey));
            signature.update(data.getBytes());

            return Base64.encode(signature.sign());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
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
    public static boolean verifySign(String publicKey, String signature_b64, String data) {
        try {
            Signature signature1 = Signature.getInstance("SHA256withRSA");
            signature1.initVerify(getPublicKey(publicKey));
            signature1.update(data.getBytes());
            return signature1.verify(Base64.decode(signature_b64));
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /*
    Get PrivateKey Class
     */
    private static PrivateKey getPrivateKey(String privateKey) {
        try {
            // Remove the "BEGIN" and "END" lines, as well as any whitespace
            privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----", "");
            privateKey = privateKey.replace("-----END PRIVATE KEY-----", "");
            privateKey = privateKey.replaceAll("\\s+", "");

            // Base64 decode the result
            byte [] pkcs8EncodedBytes = Base64.decode(privateKey);

            // extract the private key
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /*
    Get PublicKey Class
     */
    private static PublicKey getPublicKey(String publicKey) {
        try {
            // Remove the "BEGIN" and "END" lines, as well as any whitespace
            publicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----", "");
            publicKey = publicKey.replace("-----END PUBLIC KEY-----", "");
            publicKey = publicKey.replaceAll("\\s+", "");

            // Base64 decode the result
            byte [] pkcs8EncodedBytes = Base64.decode(publicKey);

            // extract the private key
            X509EncodedKeySpec spec = new X509EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePublic(spec);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
