package com.caykeprudente;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by cayke on 19/03/17.
 */
public class RepresentedData {
    //representar uma pessoa por exemplo
    private String name;
    private int age;
    private String carrer;

    public RepresentedData(String jsonData) {
        if (!jsonData.equals("")) {
            JsonObject data = (JsonObject) new JsonParser().parse(jsonData);
            name = data.get("name").getAsString();
            age = data.get("age").getAsInt();
            carrer = data.get("carrer").getAsString();
        }
    }

    //Prints the object data on scrren
    public void showInfo() {
        System.out.println("*********************************");
        System.out.println("STRUCT PESSOA");
        System.out.println("*********************************");
        System.out.println("Nome = " +  name);
        System.out.println("Idade = " + age);
        System.out.println("Profissao = " + carrer);
        System.out.println("*********************************");
    }

    /*
    Get data from input.
    return: (String) Data represented in json format.
    */
    public static String getData(){
        String name;
        int age;
        String carrer;

        System.out.println("*********************************");
        System.out.println("Cadastro de nova PESSOA");
        System.out.println("*********************************");

        Scanner scanner = new Scanner(System.in);

        System.out.println("Digite seu nome:");
        name = scanner.nextLine();

        System.out.println("Digite sua profissao:");
        carrer = scanner.nextLine();

        System.out.println("Digite sua idade:");
        age = scanner.nextInt();

        Map<String, Object> data = new HashMap<String, Object>();
        data.put( "name", name);
        data.put( "age", age );
        data.put( "carrer", carrer );

        return new Gson().toJson(data);
    }

    /*
    Generates fake data with defined size
    param: size - Data size in bytes
    return: (string) Data
     */
    public static String getFakeData(int size) {
        // 65 bytes is the json message overhead with age 24 and name cayke
        String name = "Cayke";
        int age = 24;
        String carrer = "";

        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final int N = alphabet.length();

        Random r = new Random();

        for (int i = 0; i < size-65; i++) {
            carrer = carrer + (alphabet.charAt(r.nextInt(N)));
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put( "name", name);
        data.put( "age", age );
        data.put( "carrer", carrer );

        return new Gson().toJson(data);
    }
}