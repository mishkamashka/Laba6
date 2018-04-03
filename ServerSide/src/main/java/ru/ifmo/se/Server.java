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
    //Серверный модуль содержит внури себя два потока, один слушает запросы клиента на выдачу коллекци.
    //Во стором крутится сканер, выполняется управление коллекцией напрямую с сервера.
    private final String filename = System.getenv("FILENAME");
    private final String currentdir = "C:\\files";
    //private final String currentdir = System.getProperty("user.dir");
    private final String filepath = currentdir + "\\" + filename;
    //private final String filepath = currentdir + "/" + filename;
    protected static SortedSet<Person> collec = Collections.synchronizedSortedSet(new TreeSet<Person>());
    private File file = new File(filepath);
    private Scanner sc;
    private ReentrantLock locker = new ReentrantLock();

    public void run() {
        ClientListening listener = new ClientListening();
        this.load();
        sc = new Scanner(System.in);
        while (true) {
            String localcommand = sc.next();
            String data = sc.next();
            switch (localcommand) {
                case "clear":
                    this.clear();
                    break;
                case "load":
                    this.load();
                    break;
                case "add":
                    this.addObject(data);
                    break;
                case "show":
                    this.showCollection();
                    break;
                case "remove_greater":
                    this.remove_greater(data);
                    break;
                case "quit":
                    this.save();
                    sc.close();
                    break;
                default:
                    System.out.println("Not valid command. Try one of those:\nclear - clear the collection;" +
                            "\nload - load the collection again;\nadd {element} - add new element to collection;" +
                            "\nremove_greater {element} - remove elements greater than given;\n" +
                            "show - show the collection;\nquit - quit;\n");
            }
        }
    }

    public static void main(){
        new Server();
    }

    private void load() {
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
                    Server.collec.add(Server.jsonToObject(jsonObjectAsString, Known.class));
                }
                System.out.println("Connection has been loaded.");
            } catch (NullPointerException e) {
                System.out.println("File is empty.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Collection can not be loaded.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
        locker.unlock();
    }

    private void clear() {
        locker.lock();
        if (collec.isEmpty())
            System.out.println("There is nothing to remove, collection is empty.\n");
        else {
            collec.clear();
            System.out.println("Collection has been cleared.\n");
        }
        locker.unlock();
    }

    private void remove_greater(String data) {
        locker.lock();
        Person a = Server.jsonToObject(data, Known.class);
        System.out.println(a.toString());
        Server.collec.removeIf(person -> a.compareTo(person) > 0);
        System.out.println("Objects greater than given have been removed.");
        locker.unlock();
    }

    private void addObject(String data) {
        locker.lock();
        try {
            Server.collec.add(Server.jsonToObject(data, Known.class));
            System.out.println("Object has been added.");
        }catch (Exception e) {
            System.out.println("Something went wrong. Check your object and try again. For example of json format see \"help\" command.");
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

    private static <T> T jsonToObject(String tempString, Class<T> classT) throws JsonSyntaxException {
        RuntimeTypeAdapterFactory<GeneralClothes> genClothesAdapterFactory =
                RuntimeTypeAdapterFactory.of(GeneralClothes.class, "type")
                        .registerSubtype(Shirt.class, "Shirt")
                        .registerSubtype(Jeans.class, "Jeans")
                        .registerSubtype(Jacket.class, "Jacket")
                        .registerSubtype(Trousers.class, "Trousers");
        RuntimeTypeAdapterFactory<Shoes> shoesAdapterFactory = RuntimeTypeAdapterFactory.of(Shoes.class, "type")
                .registerSubtype(Boots.class, "Shoes")
                .registerSubtype(Trainers.class, "Trainers");
        RuntimeTypeAdapterFactory<Accessories> accessoriesRuntimeTypeAdapterFactory = RuntimeTypeAdapterFactory.of(Accessories.class, "type")
                .registerSubtype(Glasses.class, "Glassess")
                .registerSubtype(Hat.class, "Hat");
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(genClothesAdapterFactory)
                .registerTypeAdapterFactory(shoesAdapterFactory)
                .registerTypeAdapterFactory(accessoriesRuntimeTypeAdapterFactory)
                .create();
        return gson.fromJson(tempString, classT);
    }

    private static String objectToJson (Person person){
        RuntimeTypeAdapterFactory<GeneralClothes> genClothesAdapterFactory = RuntimeTypeAdapterFactory.of(GeneralClothes.class, "type")
                .registerSubtype(Shirt.class, "Shirt")
                .registerSubtype(Jeans.class, "Jeans")
                .registerSubtype(Jacket.class, "Jacket")
                .registerSubtype(Trousers.class, "Trousers");
        RuntimeTypeAdapterFactory<Shoes> shoesAdapterFactory = RuntimeTypeAdapterFactory.of(Shoes.class, "type")
                .registerSubtype(Boots.class, "Shoes")
                .registerSubtype(Trainers.class, "Trainers");
        RuntimeTypeAdapterFactory<Accessories> accessoriesRuntimeTypeAdapterFactory = RuntimeTypeAdapterFactory.of(Accessories.class, "type")
                .registerSubtype(Glasses.class, "Glassess")
                .registerSubtype(Hat.class, "Hat");
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(genClothesAdapterFactory)
                .registerTypeAdapterFactory(shoesAdapterFactory)
                .registerTypeAdapterFactory(accessoriesRuntimeTypeAdapterFactory)
                .create();
        return gson.toJson(person);
    }

    private void save(){
        try {
            Writer writer = new FileWriter(file);
            //this.collec.forEach(person -> writer.write(Connection.objectToJson(person)));
            for (Person person: Server.collec){
                writer.write(Server.objectToJson(person));
            }
            writer.close();
            System.out.println("Collection has been saved.");
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }
}

class ClientListening extends Thread {
    private ServerSocket serverSocket;

    public ClientListening() {
        try {
            serverSocket = new ServerSocket(4718, 1, InetAddress.getByName("localhost"));
            System.out.println(serverSocket.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Server is now running.");
        this.start();
    }

    public void run(){
        System.out.println("Server is listening.");
        while (true) {
            try {
                Socket client = serverSocket.accept();
                Connection connec = new Connection(client);
            } catch (Exception e) {
                System.out.println("Server is not listening.");
                e.printStackTrace();
            }
        }
    }
}

class Connection extends Thread {
    private Socket client;
    private BufferedReader fromClient;
    private PrintStream toClient;
    ReentrantLock locker = new ReentrantLock();

    Connection(Socket client){
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
    public void run(){
        System.out.println("Client " + client.toString() + " has connected to server.");
        toClient.println("You've connected to the server.\nUse commands:\n" +
                "- \"show\" to see what's in the collection;\n- \"describe\" to show the collection with descriptions.\n");
        while(true) {
            try {
                String command = fromClient.readLine();
                System.out.println("Command from client: " + command);
                switch (command) {
                    case "start":
                        toClient.println("\n");
                        break;
                    case "show":
                        this.giveCollection();
                        break;
                    default:
                        toClient.println("\n");
                }
            } catch (IOException e) {
                System.out.println("Connection with the client is lost.");
                System.out.println(e.toString());
                try {
                    this.quit();
                } catch (IOException ee){
                    System.out.println("Exception while trying to close.");
                }
                return;
            }
        }
    }

    private void quit() throws IOException {
        fromClient.close();
        toClient.close();
        client.close();
        System.out.println("Client has disconnected.");
    }

    private void giveCollection(){
        final ObjectOutputStream toClient;
        try {
            toClient = new ObjectOutputStream(this.toClient);
        } catch (IOException e){
            System.out.println("Can not create ObjectOutputStream.");
            return;
        }
        try {
            //this.collec.forEach(person -> toClient.writeObject(person));
            for (Person person: Server.collec){
                toClient.writeObject(person);
            }
            this.toClient.println("\n");
        } catch (IOException e){
            System.out.println("Can not write collection into stream.");
        }
    }
}