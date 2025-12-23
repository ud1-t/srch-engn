package com.search.console;

public final class ConsoleUI {

    private static final int WIDTH = 100;

    public static void header(String title) {
        line();
        System.out.println(center(title));
        line();
    }

    public static void section(String title) {
        System.out.println("\n" + title.toUpperCase());
        System.out.println("-".repeat(title.length()));
    }

    public static void line() {
        System.out.println("=".repeat(WIDTH));
    }

    public static void kv(String key, String value) {
        System.out.printf("%-10s : %s%n", key, value);
    }

    private static String center(String text) {
        int pad = (WIDTH - text.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + text;
    }

    private ConsoleUI() {}
}

