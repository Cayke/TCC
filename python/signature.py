# INSTALL PYCRYPTODOME BEFORE COMPILING THIS

from Crypto.PublicKey import RSA
from Crypto.Signature import PKCS1_v1_5
from Crypto.Hash import SHA256
from base64 import b64encode, b64decode

class Signature():
    # ATRIBUTOS DEVEM SER O NUMERO DO HOST DESEJADO E O CONTRA -1
    @staticmethod
    def getPrivateKey(server, client):
        filePath = ''
        if server != -1:
            filePath = "server" + str(server) + "_private.pem"
        else:
            filePath = "client" + str(client) + "_private.pem"

        return open(filePath, "r").read()

    # ATRIBUTOS DEVEM SER O NUMERO DO HOST DESEJADO E O CONTRA -1
    @staticmethod
    def getPublicKey(server, client):
        filePath = ''
        if server != -1:
            filePath = "server" + str(server) + "_public.pem"
        else:
            filePath = "client" + str(client) + "_public.pem"

        return open(filePath, "r").read()


    '''
    param: privateKey Your private key
    param: data Data to be signed
    return: String base64 encoded signature
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
    param: public_key_loc Path to public key
    param: signature String signature to be verified
    return: Boolean. True if the signature is valid; False otherwise.
    '''
    @staticmethod
    def verify_sign(publicKey, signature, data):
        RSAKey = RSA.importKey(publicKey)
        signer = PKCS1_v1_5.new(RSAKey)
        digest = SHA256.new()
        digest.update(str.encode(data))
        if signer.verify(digest, b64decode(signature)):
            return True
        else:
            return False