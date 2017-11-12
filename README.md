# Sistema de quoruns bizantinos #

## Execucao servidores ##

### Clientes honestos ###

Para executar os servidores deve-se iniciar o programa passando os parametros na ordem indicada a seguir:

- IP de entrada dos dados 
	- IP da maquina em que esta rodando. Utilizar **ifconfig** para ver o mesmo.
- ID do servidor
	- Identificador do servidor. Por padrao utilizar o numero do node. Exemplo: node0.caykequoruns.... utilizar ID = 0
- Modo verbose
	- 0 para nao printar nada, 1 para printar so os dados principais, 2 para printar dados e todas as requests.	
	
Exemplo de execucao:
		
		server.py 192.168.0.199 0 2


### Clientes desonestos ###

Para executar os servidores deve-se iniciar o programa passando os parametros na ordem indicada a seguir:

- IP de entrada dos dados 
	- IP da maquina em que esta rodando. Utilizar **ifconfig** para ver o mesmo.
- ID do servidor
	- Identificador do servidor. Por padrao utilizar o numero do node. Exemplo: node0.caykequoruns.... utilizar ID = 0
- Modo verbose
	- 0 para nao printar nada, 1 para printar so os dados principais, 2 para printar dados e todas as requests.
- Path dos certificados 
	- Caminho completo para pasta que contem os arquivos dos certificados publicos e privados.
	
Exemplo de execucao:
		
		server.py 192.168.0.199 0 2 /users/cayke/
		
		
## Execucao clientes ##

### Honestos  e Desonestos###

Para executar os clientes deve-se passar como parametros os ip e portas dos servidores. 

- ID do cliente
	- Identificador do cliente.
- Modo verbose
	- 0 para nao printar nada, 1 para printar so os dados principais, 2 para printar dados e todas as requests.
- Path dos certificados 
	- Caminho completo para pasta que contem os arquivos dos certificados publicos e privados.
- IP e porta dos servidores

Exemplo de execucao:

		client.py 0 2 /users/cayke/ node0.caykequoruns.freestore.emulab.net 5000 node1.caykequoruns.freestore.emulab.net 5001 node2.caykequoruns.freestore.emulab.net 5002
		

## Execucao testes (clientes automatizados) ##
Criar a  pasta **results**.
Executar o script de cliente:

		./runPythonHonestClients.sh
		
Apos o termino das execucoes, executar o analisador dos resultados.

		python3 runResultsAnalyser.py  

		
		
		