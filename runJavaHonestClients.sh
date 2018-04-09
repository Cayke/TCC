#!/bin/bash
clients=4
operations=250
i=0

while [ $i -lt $clients ]
do
	nohup java -cp java_honest_writers_client.jar:javac.jar com.caykeprudente.Main $RANDOM 0 /users/cayke/tcc_executables/mesa/certs/ $operations write /users/cayke/tcc_executables/mesa/results/ node0.caykequoruns.freestore.emulab.net 5000 node1.caykequoruns.freestore.emulab.net 5001 node2.caykequoruns.freestore.emulab.net 5002 node3.caykequoruns.freestore.emulab.net 5003 &
	i=`expr $i + 1`
done	