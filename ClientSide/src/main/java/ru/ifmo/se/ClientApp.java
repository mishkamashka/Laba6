package ru.ifmo.se;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class ClientApp {
    //Клиентский модуль должен запрашивать у сервера текущее состояние коллекции,
    //генерировать сюжет, выводить его на консоль и завершать работу.
    Set<Person> collec = new TreeSet<Person>();

    public static void main() {
        Socket clientSocket;
        BufferedWriter toServer = null;
        BufferedReader fromServer = null;
        try {
            clientSocket = new Socket(InetAddress.getByName("localhost"), 4718);
            toServer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            fromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }catch (IOException e){
            e.printStackTrace();
        }

        Scanner sc = new Scanner(System.in);
        while (true){

            String command = sc.next();
            switch (command){
                case "show":
                    //this.show;
                    break;
                case "describe_collection":
                    //this.describe;
                    break;
                case "quit":
                    //this.quit;
                    break;
                default:
                    try{
                        toServer.write(command);
                        toServer.newLine();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
            }
            try{
                String from = fromServer.readLine();
                System.out.println(from);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
