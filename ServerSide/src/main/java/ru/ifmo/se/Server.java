package ru.ifmo.se;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server extends Thread {
    //Серверный модуль должен реализовывать все функции управления коллекцией
    //в интерактивном режиме, кроме отображения текста в соответствии с сюжетом предметной области.
    private static ServerSocket serverSocket;
    protected static SortedSet<Person> collec = Collections.synchronizedSortedSet(new TreeSet<Person>());

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(4718,1, InetAddress.getByName("localhost"));
            System.out.println(serverSocket.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Server is now running.");
        try {
            while (true) {
                Socket client = serverSocket.accept();
                Connection connec = new Connection(client);
            }
        } catch (Exception e) {
            System.out.println("Server is not listening.");
            e.printStackTrace();
        }
    }
}

class Connection extends Thread {
    private Socket client;
    private BufferedReader fromClient;
    private static PrintStream toClient;
    private static ObjectInputStream objectFromClient;
    private static ObjectOutputStream objectToClient;
    private static final String filename = System.getenv("FILENAME");
    private static final String currentdir = System.getProperty("user.dir");
    private static String filepath;
    private static File file;
    private static ReentrantLock locker = new ReentrantLock();

    Connection(Socket client){
        Connection.filemaker();
        this.client = client;
        try {
            fromClient = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
            toClient = new PrintStream(client.getOutputStream());
        } catch (IOException e){
            try{
                client.close();
            }catch (IOException ee){
                ee.printStackTrace();
            }
            e.printStackTrace();
        }
        this.start();
    }

    public static void filemaker(){
        if (currentdir.startsWith("/")) {
            filepath = currentdir + "/" + filename;
        } else
            filepath = currentdir + "\\" + filename;
        file = new File(filepath);
    }

    public void run(){
        try {
            this.load();
        } catch (IOException e) {
            System.out.println("Exception while trying to load collection.\n" + e.toString());
        }
        System.out.println("Client " + client.toString() + " has connected to server.");
        toClient.println("You've connected to the server.\n");
        String[] buf;
        Scanner sc = new Scanner(fromClient);
        sc.useDelimiter("\n");
        while(true) {
            try {

                String clientInput = fromClient.readLine();
                System.out.println("Command from client: " + clientInput);
                buf = clientInput.split(" ");
                String command = buf[0];
                String data = "";
                if (buf.length > 1)
                    data = buf[1];
                if (command == null)
                    command = "";
                switch (command) {
                    case "data_request":
                        this.giveCollection();
                        break;
                    case "save":
                        this.save();
                        //toClient.println("Collection has been saved to file.\n");
                        break;
                    case "add":
                        this.addObject(data);
                        break;
                    case "remove_greater":
                        this.remove_greater(data);
                        break;
                    case "clear":
                        this.clear();
                        toClient.println("Collection has been cleared.\n");
                        break;
                    case "quit":
                        this.quit();
                        break;
                    default:
                        toClient.println("Not valid command. Try one of those:\nhelp - get help;\nclear - clear the collection;" +
                                "\nload - load the collection again;\nadd {element} - add new element to collection;" +
                                "\nremove_greater {element} - remove elements greater than given;\n" +
                                "show - show the collection;\nquit - quit;");
                        toClient.println();
                }
            } catch (IOException e) {
                System.out.println("Connection with the client is lost.");
                System.out.println(e.toString());
                /*try {
                    fromClient.close();
                    toClient.close();
                    client.close();
                } catch (IOException ee){
                    System.out.println("Exception while trying to close client's streams.");
                }*/
                return;
            }
        }
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
                toClient.println("File is empty.");
            }
        } catch (FileNotFoundException e) {
            toClient.println("Collection can not be loaded.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
        locker.unlock();
    }

    private void remove_greater(String data) {
        locker.lock();
        try {
            if (JsonConverter.jsonToObject(data, Known.class).getName() != null) {
                Server.collec.removeIf(person -> JsonConverter.jsonToObject(data, Known.class).compareTo(person) > 0);
                toClient.println("Objects greater than given have been removed.\n");
            }
            else toClient.println("Object can not be null.\n");
        } catch (NullPointerException | JsonSyntaxException e) {
            toClient.println("Something went wrong. Check your object and try again. For example of json format see \"help\" command.");
            toClient.println(e.toString());
            toClient.println();
        }
        toClient.flush();
        locker.unlock();
    }

    private void addObject(String data) {
        locker.lock();
        try {
            if ((JsonConverter.jsonToObject(data, Known.class).getName() != null)) {
                Server.collec.add(JsonConverter.jsonToObject(data, Known.class));
                toClient.println("Object " + JsonConverter.jsonToObject(data, Known.class).toString() + " has been added.\n");
            }
            else toClient.println("Object null can not be added.\n");
        } catch (NullPointerException | JsonSyntaxException e) {
            toClient.println("Something went wrong. Check your object and try again. For example of json format see \"help\" command.");
            toClient.println(e.toString());
            toClient.println();
        }
        locker.unlock();
    }

    private void quit() throws IOException {
        fromClient.close();
        //toClient.close();
        client.close();
        System.out.println("Client has disconnected.");
    }

    protected static void save(){
        locker.lock();
        try {
            Writer writer = new FileWriter(file);
            //
            //Server.collec.forEach(person -> writer.write(Connection.objectToJson(person)));
            for (Person person: Server.collec){
                writer.write(JsonConverter.objectToJson(person));
            }
            writer.close();
            System.out.println("Collection has been saved.");
            toClient.println("Collection has been saved.\n");
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
        locker.unlock();
    }

    private void giveCollection(){
        locker.lock();
        try {
            objectToClient = new ObjectOutputStream(toClient);
        } catch (IOException e){
            System.out.println("Can not create ObjectOutputStream.");
            return;
        }
        try {
            //Server.collec.forEach(person -> toClient.writeObject(person));
            for (Person person: Server.collec){
                objectToClient.writeObject(person);
            }
            toClient.println(" Collection copy has been loaded on client.\n");
        } catch (IOException e){
            System.out.println("Can not write collection into stream.");
        }
        try {
            objectToClient.flush();
        } catch (IOException e){
            System.out.println("Connection was lost.");
            System.exit(0);
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
        locker.lock();
        Server.collec.clear();
        locker.unlock();
    }
}