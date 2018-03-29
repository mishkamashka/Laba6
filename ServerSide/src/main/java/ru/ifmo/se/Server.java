package ru.ifmo.se;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class Server extends Thread {
    //Серверный модуль должен реализовывать все функции управления коллекцией
    //в интерактивном режиме, кроме отображения текста в соответствии с сюжетом предметной области.
    private ServerSocket serverSocket;

    private Server() {
        try {
            serverSocket = new ServerSocket(4718, 10, InetAddress.getByName("localhost"));
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
        try{
            this.load();
        } catch (IOException e){
            System.out.println("Something is wrong with the connection, message can not be sent.");
            e.printStackTrace();
        }
        String[] buf;
        try{
            while(true){
                String clientInput = fromClient.readLine();
                buf = clientInput.split(" ");
                String command = buf[0];
                String data = "";
                if (buf.length > 2)
                    data = buf[1];
                switch (command) {
                    case "clear":
                        //this.clear();
                        break;
                    case "load":
                        this.load();
                        break;
                    case "add":
                        //this.addObject(data);
                        break;
                    case "remove_greater":
                        //this.remove_greater(data);
                        break;
                    case "quit":
                        //this.quit();
                        break;
                    default:
                        toClient.print("Not valid command. Try one of those:\nhelp - get help;\nclear - clear the collection;" +
                                "\nload - load the collection again;\nadd {element} - add new element to collection;" +
                                "\nremove_greater {element} - remove elements greater than given;" +
                                "\nquit - quit;");
                }
            }
        } catch (IOException e){
            System.out.println("Something is wrong with the connection, message can not be sent.");
            e.printStackTrace();
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
                toClient.println("Collection has been loaded.");
                //toClient.newLine();
            } catch (NullPointerException e) {
                toClient.print("File is empty.");
            }
        } catch (FileNotFoundException e) {
            toClient.print("Collection can not be loaded.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    /*
    private void quit() throws IOException {
        this.save();
        client.close();
    }

    private void remove_greater(String data) throws IOException {
        Person a = Connection.jsonToObject(data, Known.class);
        this.collec.removeIf(person -> a.compareTo(person) > 0);
        toClient.write("Objects greater than given have been removed.");
    }

    private void addObject(String data) throws IOException {
        this.collec.add(Connection.jsonToObject(data, Known.class));
        toClient.write("Object has been added.");
    }

    private void clear() throws IOException {
        if (collec.isEmpty())
            toClient.write("There is nothing to remove, collection is empty.");
        else {
            collec.clear();
            toClient.write("Collection has been cleared.");
        }
    }
    */
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

    private void save(){
        try (Writer writer = new FileWriter(file)) {
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
            for (Person person : this.collec) {
                writer.write(gson.toJson(person));
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

}