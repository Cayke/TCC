# INSTALL PYCRYPTODOME BEFORE COMPILING THIS

from Crypto.PublicKey import RSA
from Crypto.Signature import PKCS1_v1_5
from Crypto.Hash import SHA256
from base64 import b64encode, b64decode


class Signature():

    '''
    Returns a private Key. Attribute must be the ID number of the host, and the other must be -1
    param: server - ID from the server (if not server, pass -1)
    param: client - ID from the client (if not client, pass -1)
    param: filePath - Path for certs folder
    return: (String) The private certificate.
    '''
    @staticmethod
    def getPrivateKey(server, client, filePath):
        if server != -1:
            filePath = filePath + "server" + str(server) + "_private.pem"
        else:
            filePath = filePath + "client" + str(client) + "_private.pem"

        return open(filePath, "r").read()

    '''
    Returns a public Key. Attribute must be the ID number of the host, and the other must be -1
    param: server - ID from the server (if not server, pass -1)
    param: client - ID from the client (if not client, pass -1)
    param: filePath - Path for certs folder
    return: (String) The public certificate.
    '''
    @staticmethod
    def getPublicKey(server, client, filePath):
        if server != -1:
            filePath = filePath + "server" + str(server) + "_public.pem"
        else:
            filePath = filePath + "client" + str(client) + "_public.pem"

        return open(filePath, "r").read()


    '''
    Signs data with a private Certificate.
    param: privateKey - Your private key
    param: data - Data to be signed
    return: (String) base64 encoded signature
    '''
    @staticmethod
    def signData(privateKey, data):
        RSAKey = RSA.importKey(privateKey)
        signer = PKCS1_v1_5.new(RSAKey)
        digest = SHA256.new()
        digest.update(str.encode(data))
        signature = signer.sign(digest)
        return b64encode(signature).decode("utf-8")


    '''
    Verifies with a public key from whom the data came that it was indeed
    signed by their private key
    param: publicKey - Public Key from signer
    param: signature - Signature to be verified
    param: data - Data that was signed.
    return: (Boolean) True if the signature is valid; False otherwise.
    '''
    @staticmethod
    def verifySign(publicKey, signature, data):
        RSAKey = RSA.importKey(publicKey)
        signer = PKCS1_v1_5.new(RSAKey)
        digest = SHA256.new()
        digest.update(str.encode(data))
        if signer.verify(digest, b64decode(signature)):
            return True
        else:
            return False