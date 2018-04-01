package ru.ifmo.se;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class ClientApp {
    //Клиентский модуль должен запрашивать у сервера текущее состояние коллекции,
    //генерировать сюжет, выводить его на консоль и завершать работу.
    Set<Person> collec = new TreeSet<>();
    private static SocketAddress clientSocket;
    private static SocketChannel channel = null;
    private static DataInput fromServer;
    private static PrintStream toServer;
    private Scanner sc;

    public void main() {
        try {
            clientSocket = new InetSocketAddress(InetAddress.getByName("localhost"), 4718);
            channel = SocketChannel.open(clientSocket);
        }catch (IOException e){
            e.printStackTrace();
        }
        try {
            fromServer = new DataInputStream(channel.socket().getInputStream());
            toServer = new PrintStream(new DataOutputStream(channel.socket().getOutputStream()));
        } catch (IOException e){
            System.out.println("Can not create DataInput or DataOutput stream.");
            e.printStackTrace();
        }
        sc = new Scanner(System.in);
        sc.useDelimiter("\n");
        while (true){
            String command = sc.next();
            switch (command){
                case "show":
                    toServer.println(command);
                    this.show();
                    break;
                case "describe":
                    toServer.println("show");
                    this.describe();
                    break;
                case "help":
                    this.help();
                    break;
                case "quit":
                    toServer.println(command);
                    this.quit();
                    break;
                default:
                    try{
                        toServer.println(command);
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

    private void getCollection(){
        ObjectInputStream fromClient;
        try{
            fromClient = new ObjectInputStream(channel.socket().getInputStream());
        } catch (IOException e){
            System.out.println("Can not create ObjectInputStream.");
            return;
        }
        Person person;
        try{
            while ((person = (Person)fromClient.readObject()) != null){
                this.collec.add(person);
            }
        } catch (IOException e) {
            // выход из цикла через исключение(да, я в курсе, что это нехоршо наверное, хз как по-другому)
            //e.printStackTrace();
        } catch (ClassNotFoundException e){
            System.out.println("Class not found while deserializing.");
        }

    }
    private void showCollection() {
        if (this.collec.isEmpty())
            System.out.println("Collection is empty.");
        this.collec.forEach(person -> System.out.println(person.toString()));
        System.out.println();
    }

    private void describe(){
        this.clear();
        this.getCollection();
        this.collec.forEach(person -> person.describe());
    }

    private void show(){
        this.clear();
        this.getCollection();
        this.showCollection();
    }

    private void clear(){
        collec.clear();
    }

    public void quit(){
        sc.close();
        toServer.close();
        try {
            channel.close();
        } catch (IOException e){
            System.out.println("Can not close channel.");
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void help(){
        System.out.println("Commands:\nclear - clear the collection;\nload - load the collection again;" +
                "\nshow - show the collection;\ndescribe - show the collection with descriptions;" +
                "\nadd {element} - add new element to collection;\nremove_greater {element} - remove elements greater than given;" +
                "\nquit - quit;\nhelp - get help;");
        System.out.println("\nPattern for object Person input:\n{\"name\":\"Andy\",\"last_name\":\"Killins\",\"age\":45,\"steps_from_door\":0," +
                "\"generalClothes\":[{\"type\":\"Jacket\",\"colour\":\"white\",\"patches\":[\"WHITE_PATCH\",\"BLACK_PATCH\"," +
                "\"NONE\",\"NONE\",\"NONE\"],\"material\":\"NONE\"}],\"shoes\":[],\"accessories\":[],\"state\":\"NEUTRAL\"}");
        System.out.println("\nHow objects are compared:\nObject A is greater than B if it stands further from the door B does. (That's weird but that's the task.)");
    }
}
