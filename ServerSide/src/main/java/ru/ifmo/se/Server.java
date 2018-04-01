package ru.ifmo.se;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class Server extends Thread {
    //Серверный модуль должен реализовывать все функции управления коллекцией
    //в интерактивном режиме, кроме отображения текста в соответствии с сюжетом предметной области.
    private ServerSocket serverSocket;

    private Server() {
        try {
            serverSocket = new ServerSocket(4718);
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
    //private BufferedWriter toClient;
    private final String filename = System.getenv("FILENAME");
    private final String currentdir = "C:\\files";
    //private final String currentdir = System.getProperty("user.dir");
    private final String filepath = currentdir + "\\" + filename;
    //private final String filepath = currentdir + "/" + filename;
    private Set<Person> collec = new TreeSet<>();
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
        String[] buf;
        //String from;
        while(true) {
            try {
                //StringBuilder clientInput = new StringBuilder();
                //while ((from = fromClient.readLine()) != null) {
                //    clientInput.append(from);
                //}
                //System.out.println(fromClient.toString());
                String clientInput = fromClient.readLine();
                //String data = fromClient.readLine();
                System.out.println("Command from client: " + clientInput);
                buf = clientInput.split(" ");
                String command = buf[0];
                String data = "";
                if (buf.length > 1)
                    data = buf[1];
                switch (command) {
                    case "start":
                        break;
                    case "clear":
                        toClient.println("Clear is made.");
                        System.out.println("Clear is made.");
                        //this.clear();
                        break;
                    case "load":
                        this.load();
                        break;
                    case "add":
                        this.addObject(data);
                        break;
                    case "remove_greater":
                        System.out.println("Remove greater is made.");
                        //this.remove_greater(data);
                        break;
                    case "quit":
                        System.out.println("Quit is made");
                        //this.quit();
                        break;
                    case "show":
                        this.showCollection();
                    default:
                        toClient.println("Not valid command. Try one of those:\nhelp - get help;\nclear - clear the collection;" +
                                "\nload - load the collection again;\nadd {element} - add new element to collection;" +
                                "\nremove_greater {element} - remove elements greater than given;" +
                                "\nquit - quit;\n");
                }
            } catch (IOException e) {
                System.out.println("Something is wrong with the connection, message can not be sent.");
                e.printStackTrace();
                toClient.close();
                try {
                    fromClient.close();
                    client.close();
                } catch (IOException ee){
                    System.out.println("Exception while trying to close.");
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
                toClient.println("Collection has been loaded.\n");
            } catch (NullPointerException e) {
                toClient.println("File is empty.");
            }
        } catch (FileNotFoundException e) {
            toClient.println("Collection can not be loaded.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    /*
    private void quit() throws IOException {
        this.save();
        client.close();
    }
    */

    private void remove_greater(String data) throws IOException {
        Person a = Connection.jsonToObject(data, Known.class);
        this.collec.removeIf(person -> a.compareTo(person) > 0);
        toClient.println("Objects greater than given have been removed.\n");
    }

    private void addObject(String data) throws IOException {
        try {
            this.collec.add(Connection.jsonToObject(data, Known.class));
            toClient.println("Object " + Connection.jsonToObject(data, Known.class).toString() + " has been added.\n");
        } catch (NullPointerException e) {
            toClient.println("Something went wrong. Check your object and try again. For example of json format see \"help\" command.\n");
            e.printStackTrace();
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

    private static <T> T jsonToObject(String tempString, Class<T> classT) {
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
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    public void showCollection() {
        if (this.collec.isEmpty())
            System.out.println("Collection is empty.");
        this.collec.forEach(person -> System.out.println(person.toString()));
        System.out.println();
    }
}