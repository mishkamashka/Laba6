package ru.ifmo.se;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class ClientApp {
    //Клиентский модуль должен запрашивать у сервера текущее состояние коллекции,
    //генерировать сюжет, выводить его на консоль и завершать работу.
    Set<Person> collec = new TreeSet<Person>();
    private static SocketAddress clientSocket;
    private static SocketChannel channel = null;
    private static DataInput fromServer;
    private static PrintStream toServer;


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
        Scanner sc = new Scanner(System.in);
        sc.useDelimiter("\n");
        while (true){
            String command = sc.next();
            switch (command){
                case "show":
                    toServer.println(command);
                    this.show();
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

    public void getCollection(){
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
            System.out.println("Collection can not be read from stream.");
        } catch (ClassNotFoundException e){
            System.out.println("Class not found while deserializing.");
        }

    }
    public void showCollection() {
        if (this.collec.isEmpty())
            System.out.println("Collection is empty.");
        this.collec.forEach(person -> System.out.println(person.toString()));
        System.out.println();
    }

    public void show(){
        this.clear();
        this.getCollection();
        this.showCollection();
    }

    public void clear(){
        collec.clear();
    }
}
