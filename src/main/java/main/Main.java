package main;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        if(args.length > 0 ){
            String operationMode = args[0];
            operationMode = operationMode.toLowerCase();
            if(!args[0].equals("master") && !args[0].equals("follower")){
                System.out.println("Invalid argument. Please give argument as either \"master\" or \"follower\"");
                return;
            }
            if(args[0].equals("master")){
                System.out.println("You are running the master mode on this computer...");
                MasterMode masterMode = new MasterMode();
            } else {
                System.out.println("You are running the follower mode on this computer...");
                FollowerMode followerMode = new FollowerMode();
            }
            return;
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("Welcome to DriveCloud application.\nPlease specify the operation mode.\n\n" +
                "If you want to run the project in master mode, type \"master\" and click enter.\n" +
                "If you want to run the project in follower mode, type \"follower\" and click enter.");
        String operationMode = sc.nextLine();
        operationMode = operationMode.toLowerCase();
        while(!operationMode.equals("master") && !operationMode.equals("follower")){
            System.out.println("Please only enter \"master\" or \"follower\"!");
            operationMode = sc.nextLine();
            operationMode = operationMode.toLowerCase();
        }
        if(operationMode.equals("master")){
            System.out.println("You are running the master mode on this computer...");
            MasterMode masterMode = new MasterMode();

        } else {
            System.out.println("You are running the follower mode on this computer...");
            FollowerMode followerMode = new FollowerMode();
        }

    }
}
