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
        PrintStream toServer = null;
        BufferedReader fromServer = null;
        try {
            clientSocket = new Socket(InetAddress.getByName("localhost"), 4718);
            toServer = new PrintStream(clientSocket.getOutputStream());
            fromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }catch (IOException e){
            e.printStackTrace();
        }

        Scanner sc = new Scanner(System.in);
        sc.useDelimiter("\n");
        while (true){
            String command = sc.next();
            switch (command){
                case "show":
                    //this.show;
                    toServer.println(command);
                    break;
                case "describe_collection":
                    //this.describe;
                    break;
                case "quit":
                    //this.quit;
                    break;
                default:
                    try{
                        toServer.println(command);
                        //toServer.newLine();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
            }
            try{
                String from;
                while (!((from = fromServer.readLine()).equals(""))) {
                    System.out.println(from);
                }
                System.out.println("End of getting from server.");
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
