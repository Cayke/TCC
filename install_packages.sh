#!/bin/bash
sudo apt-get update
sudo apt-get install libssl-dev

sudo apt-get install default-jre

sudo apt-get install clang libicu-dev libcurl3 libpython2.7

cd Python-3.5.1
sudo make install
sudo pip3.5 install pycryptodome -U