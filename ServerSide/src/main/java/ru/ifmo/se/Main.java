package ru.ifmo.se;

import ru.ifmo.se.enums.Material;
import ru.ifmo.se.enums.Patch;
import ru.ifmo.se.enums.Season;
import ru.ifmo.se.enums.State;

public class Main {
    public static void main(String[] args) {
        Server a = new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(Connection::save));
        a.start();
    }
}
