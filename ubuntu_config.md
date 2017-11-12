# CONFIG FILE FOR UBUNTU 14.04 # 

## INSTALL OPENSLL FOR C++ ##

        $ sudo apt-get update
        $ sudo apt-get install libssl-dev
        
        
Add on project linker -lcrypto -lssl
Add on project compiler -std=c++11

## INSTALL JAVA ##
Install default jre

        $ sudo apt-get update
        $ sudo apt-get install default-jre


## INSTALL SWIFT ##
Download swift 3.1.1

        $ wget https://swift.org/builds/swift-3.1.1-release/ubuntu1404/swift-3.1.1-RELEASE/swift-3.1.1-RELEASE-ubuntu14.04.tar.gz
        $ tar -xzf swift-3.1.1-RELEASE-ubuntu14.04.tar.gz
        $ cd swift-3.1.1-RELEASE-ubuntu14.04/
        $ cd usr/bin
        $ pwd

Get result from pwd as "path_to_swift_usr_bin" 


        $ export PATH=path_to_swift_usr_bin/:"${PATH}"

Install other must libraries
 
        $ sudo apt-get update
        $ sudo apt-get install clang libicu-dev libcurl3 libpython2.7


## INSTALL PYTHON ##
Download and install python 3.5.1

        $ wget --no-check-certificate https://www.python.org/ftp/python/3.5.1/Python-3.5.1.tgz
        $ tar -xzf Python-3.5.1.tgz
        $ cd Python-3.5.1.tgz
        $ ./configure
        $ make
        $ make test
        $ sudo make install

install PYCRYPTODOME

        $ sudo pip3 install pycryptodome
        $ python3 -m Crypto.SelfTest

OBS: la no emulab tem que executar o python com "$sudo su" antes, por causa da pasta de instalacao do pycryptodomex



## EXECUCAO ## 
    - C: gera executavel
    - PYTHON: rodar a classe no python via terminal
    - SWIFT: gerar executavel na minha maquina. Nao foi possivel gerar no servidor(deu erros). OBS: o swfit deve estar na pasta /users/cayke
        - transformar .c em .o com “$gcc -c arquivo.c”
        - compilar tudo usando “swiftc arquivo1.swfit arquivo2.swift …. arquivoc.o -lcrypto”
	- compilar usando o swiftc na pasta /users/cayke, utilizar sudo su para isso
	- $ chown cayke main (dar permissao)
    - JAVA: roda no java via terminal
        - gerar o .class usando o intellij - path no mac /OneDrive/unb/TCC/Dev/java_honest_writers/target/classes/com/caykeprudente
        - rodar usando $ java -cp meuprograma.jar:dependecia.jar “com.caykeprudente.Main”
