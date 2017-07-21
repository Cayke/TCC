from client import Client
from testClient import TestClient

servers = [('node0.caykequoruns.freestore.emulab.net', 5000),('node2.caykequoruns.freestore.emulab.net', 5000),('node4.caykequoruns.freestore.emulab.net', 5000)]
Client(0, servers)


# servers = [('localhost', 5000),('localhost', 5001),('localhost', 5002), ('localhost', 5003)]
# TestClient(0 , servers)