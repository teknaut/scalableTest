package com.scalable;

import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {


    public static void main(String[] args) {
        try(Scanner scan    = new Scanner(System.in)){
            while (scan.hasNext()) {
                Target target = new Target(scan.nextLine());
                target.run();
            }
        }
    }
}
