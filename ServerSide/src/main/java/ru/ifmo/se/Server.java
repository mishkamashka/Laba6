package ru.ifmo.se;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server extends Thread {
    //Серверный модуль должен реализовывать все функции управления коллекцией
    //в интерактивном режиме, кроме отображения текста в соответствии с сюжетом предметной области.
    private static DatagramSocket serverSocket;
    private static final int sizeOfPacket = 5000;
    static boolean isCollection = false;
    protected static SortedSet<Person> collec = Collections.synchronizedSortedSet(new TreeSet<Person>());

    @Override
    public void run() {
        try {
            serverSocket = new DatagramSocket(4718, InetAddress.getByName("localhost"));
            System.out.println(serverSocket.toString());
            System.out.println(serverSocket.getLocalPort());
            System.out.println("Server is now running.");
            while (true) {
                DatagramPacket fromClient = new DatagramPacket(new byte[sizeOfPacket], sizeOfPacket);
                serverSocket.receive(fromClient);
                new Thread(new Connection(serverSocket, fromClient, isCollection)).start();
            }
        } catch (UnknownHostException | SocketException e){
            System.out.println("Server is not listening.");
            e.printStackTrace();
        } catch (IOException e){
            System.out.println("Can not receive datagramPacket.");
            e.printStackTrace();
        } catch (IllegalThreadStateException e){
            e.printStackTrace();
        }
    }
}

class Connection extends Thread {
    private DatagramSocket client;
    private DatagramPacket packet;
    private InetAddress address;
    private int clientPort;
    private boolean isCollection;
    private final static String filename = System.getenv("FILENAME");
    private final static String currentdir = System.getProperty("user.dir");
    private static String filepath;
    private static File file;
    private ReentrantLock locker = new ReentrantLock();

    Connection(DatagramSocket serverSocket, DatagramPacket packetFromClient, boolean isCollection){
        Connection.filemaker();
        this.packet = packetFromClient;
        this.address = packetFromClient.getAddress();
        this.clientPort = packetFromClient.getPort();
        this.client = serverSocket;
        this.isCollection = isCollection;
    }

    private static void filemaker(){
        if (currentdir.startsWith("/")) {
            filepath = currentdir + "/" + filename;
        } else
            filepath = currentdir + "\\" + filename;
        file = new File(filepath);
    }

    public void run(){
        locker.lock();
        try {
            this.load();
        } catch (IOException e) {
            System.out.println("Exception while trying to load collection.\n" + e.toString());
        }

        if (isCollection) {
            try {
                this.clear();
                this.getCollection();
                Server.isCollection = false;
            } catch (IOException e){
                e.printStackTrace();
            }
            return;
        }

        ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
        int command = byteStream.read();
        System.out.print("Command from client:");
        try {
            switch (command) {
                case 1: //data_request
                    System.out.println(" data_request, client: " + clientPort);
                    this.giveCollection();
                    break;
                case 2: //save
                    System.out.println(" save, client: " + clientPort);
                    Server.isCollection = true;
                    break;
                case 4: //q
                    System.out.println(" q, client: " + clientPort);
                    this.quit();
                    break;
                case 5: //load_file
                    System.out.println(" load_file, client: " + clientPort);
                    this.load();
                    client.send(this.createPacket("\n"));
                    break;
                case 6: //save_file
                    System.out.println(" save_file, client: " + clientPort);
                    this.save();
                    break;
                default:
                    System.out.println(" unknown, client: " + clientPort);
                    client.send(this.createPacket("Not valid command. Try one of those:\nhelp - get help;\nclear - clear the collection;" +
                            "\nload - load the collection again;\nadd {element} - add new element to collection;" +
                            "\nremove_greater {element} - remove elements greater than given;\n" +
                            "show - show the collection;\nquit - quit;\n"));
            }
            byteStream.close();
        } catch (NullPointerException e){
            System.out.println("Null command received.");
        } catch (IOException e) {
            System.out.println("Connection with the client is lost.");
            System.out.println(e.toString());
            client.close();
        }
        locker.unlock();
    }

    private DatagramPacket createPacket(String string){
        ByteArrayOutputStream toClient = new ByteArrayOutputStream();
        try {
            toClient.flush();
            toClient.write(string.getBytes());
            toClient.close();
        } catch (IOException e){
            System.out.println("Can not create packet.");
        }
        DatagramPacket datagramPacket = new DatagramPacket(toClient.toByteArray(), toClient.size(), address, clientPort);
        return datagramPacket;
    }

    private void load() throws IOException {
        locker.lock();
        try (Scanner sc = new Scanner(file)) {
            StringBuilder tempString = new StringBuilder();
            tempString.append('[');
            sc.useDelimiter("}\\{");
            while (sc.hasNext()) {
                tempString.append(sc.next());
                if (sc.hasNext())
                    tempString.append("},{");
            }
            sc.close();
            JSONArray jsonArray = new JSONArray(tempString.append(']').toString());
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String jsonObjectAsString = jsonObject.toString();
                    Server.collec.add(JsonConverter.jsonToObject(jsonObjectAsString, Known.class));
                }
                System.out.println("Connection has been loaded.");
            } catch (NullPointerException e) {
                try {
                    client.send(this.createPacket("File is empty.\n"));
                } catch (IOException ee){
                    System.out.println("Can not send packet.");
                }
            }
        } catch (FileNotFoundException e) {
            try {
                client.send(this.createPacket("Collection can not be loaded.\nFile "+filename+" is not accessible: it does not exist or permission denied.\n"));
            } catch (IOException ee){
                System.out.println("Can not send packet.");
            }
            e.printStackTrace();
        }
        locker.unlock();
    }

    private void getCollection() throws IOException{
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteStream);
            Person person;
            try {
                while ((person = (Person) objectInputStream.readObject()) != null) {
                    Server.collec.add(person);
                }
                System.out.println("Collection has been loaded on server from client " + clientPort + ".");
            } catch (StreamCorruptedException e){
                System.out.println("Collection has been loaded on server from client " + clientPort + ".");
            }
            byteStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void quit() throws IOException {
        System.out.println("Client " + clientPort + " has disconnected.");
    }

    private void save(){
        locker.lock();
        try {
            Writer writer = new FileWriter(file);
            //
            //Server.collec.forEach(person -> writer.write(Connection.objectToJson(person)));
            for (Person person: Server.collec){
                writer.write(JsonConverter.objectToJson(person));
            }
            writer.close();
            System.out.println("Collection has been saved to file.");
            client.send(this.createPacket("Collection has been saved to file.\n"));
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
        locker.unlock();
    }

    public static void saveOnQuit(){
        try {
            Writer writer = new FileWriter(file);
            //
            //Server.collec.forEach(person -> writer.write(Connection.objectToJson(person)));
            for (Person person: Server.collec){
                writer.write(JsonConverter.objectToJson(person));
            }
            writer.close();
            System.out.println("Collection has been saved to file.");
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    private void giveCollection(){
        locker.lock();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            for (Person person : Server.collec) {
                objectOutputStream.writeObject(person);
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, clientPort);
            byteArrayOutputStream.close();
            client.send(packet);
            System.out.println("Collection has been sent to client.");
        } catch (IOException e) {
            System.out.println("Can not send collection to client.");
            e.printStackTrace();
        }
        locker.unlock();
    }

    private void showCollection() {
        if (Server.collec.isEmpty())
            System.out.println("Collection is empty.");
        for (Person person : Server.collec) {
            System.out.println(person.toString());
        }
    }

    private void clear() {
        Server.collec.clear();
    }
}