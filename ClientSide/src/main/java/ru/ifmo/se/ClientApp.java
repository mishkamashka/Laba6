package ru.ifmo.se;

import com.google.gson.JsonSyntaxException;

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
    private static ObjectInputStream objectFromServer;
    private static DataInput fromServer;
    private static PrintStream toServer;
    private Scanner sc;

    public void main() {
        try {
            clientSocket = new InetSocketAddress(InetAddress.getByName("localhost"), 4718);
            channel = SocketChannel.open(clientSocket);
        } catch (IOException e){
            //e.printStackTrace();
        }
        int i = 0;
        while (channel == null) {
            try {
                Thread.sleep(1000);
                channel = SocketChannel.open(clientSocket);
            } catch (IOException e) {
                if (i++ == 5){
                    System.out.println("Server is not responding for a long time...");
                }
                if (i == 15){
                    System.out.println("Server did not respond for too long. Try again later.");
                    System.exit(0);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            fromServer = new DataInputStream(channel.socket().getInputStream());
            toServer = new PrintStream(new DataOutputStream(channel.socket().getOutputStream()));
        } catch (IOException e){
            System.out.println("Can not create DataInput or DataOutput stream.");
            e.printStackTrace();
        }
        this.gettingResponse();
        toServer.println("data_request");
        this.clear();
        this.load();
        this.gettingResponse();
        sc = new Scanner(System.in);
        sc.useDelimiter("\n");
        String command;
        while (true) {
            command = sc.next();
            switch (command) {
                case "load":
                    toServer.println("data_request");
                    this.clear();
                    this.load();
                    this.gettingResponse();
                    break;
                case "show":
                    //toServer.println("data_request");
                    //this.clear();
                    //this.load();
                    //this.gettingResponse();
                    this.show();
                    break;
                case "describe":
                    //toServer.println("data_request");
                    //this.clear();
                    //this.load();
                    //this.gettingResponse();
                    this.describe();
                    break;
                case "help":
                    this.help();
                    break;
                case "quit":
                    toServer.println(command);
                    this.quit();
                    break;
                case "data_request":
                    System.out.println();
                    break;
                default:
                    try{
                        toServer.println(command);
                        this.gettingResponse();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
            }
        }
    }

    private void load(){
        try{
            objectFromServer = new ObjectInputStream(channel.socket().getInputStream());
        } catch (IOException e){
            System.out.println("Can not create ObjectInputStream.");
            System.out.println(e.toString());
            System.out.println("That's okay, just try again.");
            return;
        }
        Person person;
        try{
            while ((person = (Person)objectFromServer.readObject()) != null){
                this.collec.add(person);
            }
        } catch (IOException e) {
            // выход из цикла через исключение(да, я в курсе, что это нехоршо наверное, хз как по-другому)
            //e.printStackTrace();
        } catch (ClassNotFoundException e){
            System.out.println("Class not found while deserializing.");
        }
    }

    private void show() {
        if (this.collec.isEmpty())
            System.out.println("Collection is empty.");
        this.collec.forEach(person -> System.out.println(person.toString()));
        System.out.println();
    }

    private void describe(){
        this.collec.forEach(person -> person.describe());
        System.out.println();
    }

    private void quit(){
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

    private void gettingResponse(){
        try{
            Scanner sc = new Scanner(fromServer.readLine());
            sc.useDelimiter("\n");
            /*while (!((from = fromServer.readLine()).equals(""))) {
                System.out.println(from);
            }
            System.out.println("End of getting from server.");*/
            while (sc.hasNext()) {
                System.out.println(sc.next());
                sc = new Scanner(fromServer.readLine());
            }
            System.out.println("End of getting from server.");
        } catch (IOException e){
            System.out.println("The connection was lost.");
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println();
    }

    private void clear() {
        if (collec.isEmpty())
            System.out.println("There is nothing to remove, collection is empty.");
        else {
            collec.clear();
            System.out.println("Collection has been cleared.");
        }
        System.out.println();
    }

    private void help(){
        System.out.println("Commands:\nclear - clear the collection;\nload - load the collection again;" +
                "\nshow - show the collection;\ndescribe - show the collection with descriptions;" +
                "\nadd {element} - add new element to collection;\nremove_greater {element} - remove elements greater than given;" +
                "\nsave - save changes;\nq - quit without saving;\nqw - save and quit;\nhelp - get help;");
        System.out.println("\nPattern for object Person input:\n{\"name\":\"Andy\",\"last_name\":\"Killins\",\"age\":45,\"steps_from_door\":0," +
                "\"generalClothes\":[{\"type\":\"Jacket\",\"colour\":\"white\",\"patches\":[\"WHITE_PATCH\",\"BLACK_PATCH\"," +
                "\"NONE\",\"NONE\",\"NONE\"],\"material\":\"NONE\"}],\"shoes\":[],\"accessories\":[],\"state\":\"NEUTRAL\"}");
        System.out.println("\nHow objects are compared:\nObject A is greater than B if it stands further from the door than B does. (That's weird but that's the task.)");
        System.out.println();
    }
}
