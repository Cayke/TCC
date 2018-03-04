#!/bin/bash
args=("$@")

clients=args[0]
operations=100
i=args[1]

while [ $i -lt $clients ]
do
	nohup python3 python/runClient.py $i 0 /OneDrive/unb/TCC/DEV/certs/ $operations read /OneDrive/unb/TCC/DEV/results/ node0.caykequoruns.freestore.emulab.net 5000 node1.caykequoruns.freestore.emulab.net 5001 node2.caykequoruns.freestore.emulab.net 5002 node3.caykequoruns.freestore.emulab.net 5003 &
	i=`expr $i + 1`
done	