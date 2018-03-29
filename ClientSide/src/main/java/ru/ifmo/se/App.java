package ru.ifmo.se;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * The App class implements the application which allow user
 * to interact with the collection using console.
 */

public class App {
    private final static String filename = System.getenv("FILENAME");
    private final static String currentdir = System.getProperty("user.dir");
    private final static String filepath = currentdir + "\\" + filename;
    static Set<Person> collec = new TreeSet<>();
    static File file = new File(filepath);

    /**
     * Starts the app.
     */
    public void start() {
        if (filename == null) {
            System.out.println("No file name set or environment variable FILENAME does not exist.");
            System.exit(0);
        }
        this.load();
        while (true) {
            System.out.println();
            Scanner comin = new Scanner(System.in);
            String command = comin.next();
            switch (command) {
                case "clear":
                    this.clear();
                    break;
                case "load":
                    this.load();
                    break;
                case "add":
                    this.addObject(comin);
                    break;
                case "remove_greater":
                    this.remove_greater(comin);
                    break;
                case "quit":
                    this.quit(comin);
                    break;
                case "show":
                    this.showCollection();
                    break;
                case "describe":
                    this.describeCollection();
                    break;
                case "help":
                    this.help();
                    break;
                default:
                    System.out.println("Not valid command. Try one of those:\nhelp - get help;\nclear - clear the collection;" +
                            "\nload - load the collection again;\nadd {element} - add new element to collection;" +
                            "\nshow - show current collection;\ndescribe - show objects from current collection with descriptions;" +
                            "\nremove_greater {element} - remove elements greater than given;" +
                            "\nquit - quit;");
            }
        }
    }

    /**
     * Clears the collection and prints the message about it.
     */
    public void clear() {
        if (collec.isEmpty())
            System.out.println("There is nothing to remove, collection is empty.");
        else {
            collec.clear();
            System.out.println("Collection has been cleared.");
        }
    }

    /**
     * Adds new element given in json format to the collection.
     *
     * @param sc needed to get object which has to be added to the collection.
     */
    public void addObject(Scanner sc) {
        StringBuilder tempString = new StringBuilder();
        tempString.append(sc.next());
        try {
            App.collec.add(App.jsonToObject(tempString.toString(), Known.class));
            System.out.println("Object has been added.");
        } catch (Exception e) {
            System.out.println("Something went wrong. Check your object and try again. For example of json format see \"help\" command.");
            e.printStackTrace();
        }
    }

    /**
     * Loads objects to the collection from the file.
     */
    public static void load() {
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
                    App.collec.add(App.jsonToObject(jsonObjectAsString, Known.class));
                }
                System.out.println("Collection has been loaded.");
            } catch (NullPointerException e) {
                System.out.println("File is empty.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Collection can not be loaded.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    /**
     * Removes all elements which are greater than given in json format one.
     *
     * @param sc needed to get object which objects from th collection have to be compared to.
     */
    public void remove_greater(Scanner sc) {
        StringBuilder tempString = new StringBuilder();
        tempString.append(sc.next());
        try {
            Person a = App.jsonToObject(tempString.toString(), Known.class);
            App.collec.removeIf(person -> a.compareTo(person) > 0);
            System.out.println("Objects greater than given have been removed.");
        } catch (Exception e) {
            System.out.println("Something went wrong. Check your object and try again.\nFor example of json format try \"help\" command.");
            e.printStackTrace();
        }
    }

    /**
     * Saves current collection to the file and quits the app.
     *
     * @param sc needed to be closed.
     */
    public void quit(Scanner sc) {
        sc.close();
        System.exit(0);
    }

    /**
     * Saves current collection to the file.
     */
    public static void save(){
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
            for (Person person : App.collec) {
                writer.write(gson.toJson(person));
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Collection can not be saved.\nFile "+filename+" is not accessible: it does not exist or permission denied.");
            e.printStackTrace();
        }
    }

    /**
     * Converts json string to object of T class.
     *
     * @param tempString string in json format.
     * @param classT class which string is needed to be converted to.
     * @param <T> type of returned object.
     * @return object of T class.
     */
    public static <T> T jsonToObject(String tempString, Class<T> classT) {
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
        //Gson json = new GsonBuilder().create();
        return gson.fromJson(tempString, classT);
    }

    /**
     * Shows the current content of the collection.
     */
    public void showCollection() {
        if (App.collec.isEmpty())
            System.out.println("Collection is empty.");
        for (Person person : App.collec) {
            System.out.println(person.toString());
        }
    }

    /**
     * Shows the current content of the collection with descriptions.
     */
    public void describeCollection() {
        if (App.collec.isEmpty())
            System.out.println("Collection is empty.");
        for (Person person : App.collec) {
            person.describe();
        }
    }

    /**
     * Shows the list of commands and json-pattern for object input.
     */
    public void help() {
        System.out.println("Commands:\nclear - clear the collection;\nload - load the collection again;" +
                "\nadd {element} - add new element to collection;\nremove_greater {element} - remove elements greater than given;" +
                "\nquit - quit;\nhelp - get help;");
        System.out.println("\nPattern for object Person input:\n{\"name\":\"Andy\",\"last_name\":\"Killins\",\"age\":45," +
                "\"generalClothes\":[{\"type\":\"Jacket\",\"colour\":\"white\",\"patches\":[\"WHITE_PATCH\",\"BLACK_PATCH\"," +
                "\"NONE\",\"NONE\",\"NONE\"],\"material\":\"NONE\"}],\"shoes\":[],\"accessories\":[],\"state\":\"NEUTRAL\"}");
        System.out.println("\nHow objects are compared:\nObject A is greater than B if its name and last_name are greater than Bs.");
    }
}
