#!/bin/bash
clients=32
operations=500
i=0

while [ $i -lt $clients ]
do
	nohup python3.5 python_dishonest_writers/runClient.py $RANDOM 0 /users/cayke/tcc_executables/mesa/certs/ $operations read /users/cayke/tcc_executables/mesa/results/ node0.caykequoruns.freestore.emulab.net 5000 node1.caykequoruns.freestore.emulab.net 5001 node2.caykequoruns.freestore.emulab.net 5002 node3.caykequoruns.freestore.emulab.net 5003 &
	i=`expr $i + 1`
done	