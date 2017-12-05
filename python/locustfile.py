from locust import HttpLocust, TaskSet
from robotClient import RobotClient
from representedData import RepresentedData
from locust.events import request_success, request_failure
import time
import sys


def read(l):
    print('python version: ' + sys.version)
    start_at = time.time()
    servers = [('node0.caykequoruns.freestore.emulab.net', 5000), ('node1.caykequoruns.freestore.emulab.net', 5001), ('node2.caykequoruns.freestore.emulab.net', 5002), ('node3.caykequoruns.freestore.emulab.net', 5003)]
    RobotClient(0, servers, 1, '/OneDrive/unb/TCC/DEV/certs/', 1, 'read', '/OneDrive/unb/TCC/DEV/results/')
    request_success.fire(
        request_type='read',
        name='Client',
        response_time=int((time.time() - start_at) * 1000000)
    )


def write(l):
    print('python version: ' + sys.version)
    start_at = time.time()
    servers = [('node0.caykequoruns.freestore.emulab.net', 5000), ('node1.caykequoruns.freestore.emulab.net', 5001), ('node2.caykequoruns.freestore.emulab.net', 5002), ('node3.caykequoruns.freestore.emulab.net', 5003)]
    RobotClient(1, servers, 1, '/OneDrive/unb/TCC/DEV/certs/', 1, 'write', '/OneDrive/unb/TCC/DEV/results/')
    request_success.fire(
        request_type='write',
        name='Client',
        response_time=int((time.time() - start_at) * 1000000)
    )

class UserBehavior(TaskSet):
    # tasks = {products: 1, stores: 1}
    tasks = {read : 5, write: 1}


class WebsiteUser(HttpLocust):
    task_set = UserBehavior
    min_wait = 10000
    max_wait = 30000
