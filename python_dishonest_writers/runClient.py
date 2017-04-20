from client import Client
from testClient import TestClient

# servers = [('localhost', 5000),('localhost', 5001),('localhost', 5002), ('localhost', 5003)]
# Client(0, servers)


servers = [('localhost', 5000),('localhost', 5001)]
TestClient(0 , servers)