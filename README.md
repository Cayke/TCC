# Sistema de quoruns bizantinos #

## Execucao servidores ##

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
		
		server.py 192.168.0.199 0 2 /users/cayke
		
		
## Execucao clientes ##

Para executar os clientes deve-se passar como parametros os ip e portas dos servidores. 

Exemplo de execucao:

		client.py node0.caykequoruns.freestore.emulab.net 5000 node1.caykequoruns.freestore.emulab.net 5001 node2.caykequoruns.freestore.emulab.net 5002
		
		