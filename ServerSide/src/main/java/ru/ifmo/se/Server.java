package ru.ifmo.se;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Thread {
    //Серверный модуль должен реализовывать все функции управления коллекцией
    //в интерактивном режиме, кроме отображения текста в соответствии с сюжетом предметной области.
    private ServerSocket serverSocket;

    private Server() {
        try {
            serverSocket = new ServerSocket(4718,1, InetAddress.getByName("localhost"));
            System.out.println(serverSocket.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Server is now running.");
        this.start();
    }

    public void run() {
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
    public static void main(){
        new Server();
    }
}

class Connection extends Thread {
    private Socket client;
    private BufferedReader fromClient;
    private PrintStream toClient;
    private final String filename = System.getenv("FILENAME");
    private final String currentdir = "C:\\files";
    //private final String currentdir = System.getProperty("user.dir");
    private final String filepath = currentdir + "\\" + filename;
    //private final String filepath = currentdir + "/" + filename;
    private SortedSet<Person> collec = Collections.synchronizedSortedSet(new TreeSet<Person>());
    private File file = new File(filepath);

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
        try {
            this.load();
        } catch (IOException e) {
            System.out.println("Exception while trying to load collection.\n" + e.toString());
        }
        System.out.println("Client " + client.toString() + " has connected to server.");
        toClient.println("You've connected to the server.\n");
        String[] buf;
        while(true) {
            try {
                String clientInput = fromClient.readLine();
                System.out.println("Command from client: " + clientInput);
                buf = clientInput.split(" ");
                String command = buf[0];
                String data = "";
                if (buf.length > 1)
                    data = buf[1];
                switch (command) {
                    case "start":
                        toClient.println();
                        break;
                    case "clear":
                        this.clear();
                        break;
                    case "load":
                        this.load();
                        toClient.println();
                        break;
                    case "add":
                        this.addObject(data);
                        break;
                    case "remove_greater":
                        this.remove_greater(data);
                        break;
                    case "quit":
                        this.quit();
                        break;
                    case "show":
                        this.showCollection();
                        this.giveCollection();
                        break;
                    default:
                        toClient.println("Not valid command. Try one of those:\nhelp - get help;\nclear - clear the collection;" +
                                "\nload - load the collection again;\nadd {element} - add new element to collection;" +
                                "\nremove_greater {element} - remove elements greater than given;\n" +
                                "show - show the collection;\nquit - quit;\n");
                }
            } catch (IOException e) {
                System.out.println("Connection with the client is lost.");
                System.out.println(e.toString());
                try {
                    fromClient.close();
                    toClient.close();
                    client.close();
                    System.out.println("Client has disconnected.");
                } catch (IOException ee){
                    System.out.println("Exception while trying to close client's streams.");
                }
                return;
            }
        }
    }

    private void load() throws IOException {
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
                    this.collec.add(Connection.jsonToObject(jsonObjectAsString, Known.class));
                }
                System.out.println("Connection has been loaded.");
                //toClient.println("Collection has been loaded.\n");
            } catch (NullPointerException e) {
                toClient.println("File is empty.");
            }
        } catch (FileNotFoundException e) {
            toClient.println("Collection can not be loaded.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    private void quit() throws IOException {
        fromClient.close();
        toClient.close();
        client.close();
        System.out.println("Client has disconnected.");
    }

    private void remove_greater(String data) throws IOException {
        Person a = Connection.jsonToObject(data, Known.class);
        System.out.println(a.toString());
        this.collec.removeIf(person -> a.compareTo(person) > 0);
        toClient.println("Objects greater than given have been removed.\n");
    }

    private void addObject(String data) throws IOException {
        try {
            if ((Connection.jsonToObject(data, Known.class).getName() != null)) {
                this.collec.add(Connection.jsonToObject(data, Known.class));
                toClient.println("Object " + Connection.jsonToObject(data, Known.class).toString() + " has been added.\n");
            }
            else toClient.println("Object null can not be added.\n");
        } catch (NullPointerException | JsonSyntaxException e) {
            toClient.println("Something went wrong. Check your object and try again. For example of json format see \"help\" command.\n");
            System.out.println(e.toString());
        }
    }

    private void clear() throws IOException {
        if (collec.isEmpty())
            toClient.println("There is nothing to remove, collection is empty.\n");
        else {
            collec.clear();
            toClient.println("Collection has been cleared.\n");
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
            for (Person person: this.collec){
                writer.write(Connection.objectToJson(person));
            }
            writer.close();
            System.out.println("Collection has been saved.");
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    public void giveCollection(){
        ObjectOutputStream toClient;
        try {
            toClient = new ObjectOutputStream(this.toClient);
        } catch (IOException e){
            System.out.println("Can not create ObjectOutputStream.");
            return;
        }
        try {
            //this.collec.forEach(person -> toClient.writeObject(person));
            for (Person person: this.collec){
                toClient.writeObject(person);
            }
            this.toClient.println("\n");
        } catch (IOException e){
            System.out.println("Can not write collection into stream.");
        }
    }

    public void showCollection() {
        if (this.collec.isEmpty())
            System.out.println("Collection is empty.");
        for (Person person : this.collec) {
            System.out.println(person.toString());
        }
    }
}