#!/bin/bash
clients=64
operations=100
i=0

while [ $i -lt $clients ]
do
	nohup python3 python/runClient.py $i 0 /OneDrive/unb/TCC/DEV/certs/ $operations read /OneDrive/unb/TCC/DEV/results/ node0.caykequoruns.freestore.emulab.net 5000 node1.caykequoruns.freestore.emulab.net 5001 node2.caykequoruns.freestore.emulab.net 5002 node3.caykequoruns.freestore.emulab.net 5003 &
	i=`expr $i + 1`
done	