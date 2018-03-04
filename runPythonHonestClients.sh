#!/bin/bash
clients=4
operations=250
i=0

while [ $i -lt $clients ]
do
	nohup python3 python_dishonest_writers/runClient.py $RANDOM 0 /OneDrive/unb/TCC/git/certs/ $operations write /OneDrive/unb/TCC/git/results/ localhost 5000 localhost 5001 localhost 5002 localhost 5003 &
	i=`expr $i + 1`
done	