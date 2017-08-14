package com.scalable;

import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {

    private static Executor executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try(Scanner scan    = new Scanner(System.in)){
            while (scan.hasNext()) {
                Target target = new Target(scan.nextLine());
                executor.execute(target);
            }
        }
    }
}
