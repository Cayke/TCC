import json

class RepresentedData():
    # representar uma pessoa por exemplo
    Name = ''
    Age = 0
    Carrer = ''

    def __init__(self, jsonData):
        if(jsonData != ''):
            data = json.loads(jsonData)
            self.Name = data['name']
            self.Age = data['age']
            self.Carrer = data['carrer']

    # prints the object data on screen
    def showInfo(self):
        print('*********************************')
        print('STRUCT PESSOA')
        print('*********************************')
        print('Nome = ' + self.Name)
        print('Idade = ' + str(self.Age))
        print('Profissao = ' + self.Carrer)
        print('*********************************')

    # returns a str
    # data represented in json format
    @staticmethod
    def getData():
        print('*********************************')
        print('Cadastro de nova PESSOA')
        print('*********************************')

        print('Digite seu nome:\n')
        try:
            value = input()
            name = str(value)
        except:
            print("Erro. Nao foi recebido um nome.")
            return ''

        print('Digite sua idade:\n')
        try:
            value = input()
            age = int(value)
        except:
            print("Erro. Nao foi recebido uma idade.")
            return ''

        print('Digite sua profissao:\n')
        try:
            value = input()
            carrer = str(value)
        except:
            print("Erro. Nao foi recebido uma profissao.")
            return ''

        data = dict(name = name, age = age, carrer = carrer)
        return json.dumps(data)