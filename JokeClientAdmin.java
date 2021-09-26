
/*
    1. Bryce Reinhard / 9/25/21
    2. Java version
        java version "16.0.2" 2021-07-20
        Java(TM) SE Runtime Environment (build 16.0.2+7-67)
        Java HotSpot(TM) 64-Bit Server VM (build 16.0.2+7-67, mixed mode, sharing)
    3. Command line compilation example for JokeClientAdmin:
        javac JokeClientAdmin.java
        java JokeClientAdmin
    4. Instructions to run program:
        In seperate shell windows run these commands after compiling the classes:
        java JokeServer
        java JokeClient
        java JokeClientAdmin

        There are a number of optional params you can pass in to change behavior.

        JokeServer can take a parameter that marks it as a secondary server.
        IE: java JokeServer secondary

        JokeClient can be configured to point to two servers.
        Some options:
        java JokeClient server1Name
        java JokeClient server1Name server2Name
        java JokeClient (in this case it assumes localhost as server1Name)

        JokeClientAdmin follows the same logic as JokeClient above.

    5. Necessary to run program.
        JokeServer.java
        JokeClient.java
        JokeClientAdmin.java
    
    6. Notes.
        Going to copy this header comment to each java file. All similar
*/
import java.io.*;
import java.net.*;

public class JokeClientAdmin {
    public static void main(String args[]) {
        // set server informatinon depending on user arguments.
        // Similar logic to JokeClient
        // Set defaults then overwrite depending on user inputs
        String server1Name = "localhost";
        String server2Name = "default";
        boolean hasServer2 = false;
        int port1 = 5050;
        int port2 = 5051;
        if (args.length == 1) {
            // Server1 name
            server1Name = args[0];
        } else if (args.length == 2) {
            // Server1 and Server2 name
            server1Name = args[0];
            server2Name = args[1];
            hasServer2 = true;
        }

        // default to first server
        // usedServer/usedPort point to used server information
        String usedServer = server1Name;
        int usedPort = port1;
        // boolean representing which server (1 or 2) is used. Defaults to 1
        boolean firstServer = true;

        System.out.println("Server one: " + server1Name + ", port: " + port1);
        if (hasServer2) {
            System.out.println("Server two: " + server2Name + ", port: " + port2);
        }

        // Makes an input reader
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        boolean infiniteLoop = true;
        try {
            while (infiniteLoop) {

                System.out.println("Press enter to toggle joke/proverb");
                if (hasServer2) {
                    System.out.println("Enter \"s\" to toggle used server");
                }
                System.out.flush();

                String userInput = in.readLine();
                // read in user input and change mode, quit, or get data depending on user input
                if (userInput.equals("s")) {
                    if (hasServer2) {
                        // Change the mode then set server information based on that
                        firstServer = !firstServer;
                        if (firstServer) {
                            usedServer = server1Name;
                            usedPort = port1;
                        } else {
                            usedServer = server2Name;
                            usedPort = port2;
                        }
                        System.out.println("Now communnicating with: " + usedServer + " port: " + usedPort);
                    } else {
                        System.out.println("No secondary server being used");
                    }
                } else if (userInput.equals("quit")) {
                    System.out.println("Exiting");
                    infiniteLoop = false;
                } else {
                    queryServer(usedServer, usedPort);
                }
            }
        } catch (IOException x) {
            x.printStackTrace();
        }
    }

    /*
     * Function that queries server. Does not need to send any information to work
     */
    static void queryServer(String serverName, int port1) {
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;

        try {
            sock = new Socket(serverName, port1);
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServer = new PrintStream(sock.getOutputStream());
            toServer.println();
            toServer.flush();
            // should only get 1 line of response from server -
            // what mode is currently being read
            for (int i = 0; i < 1; i++) {
                String textFromServer = fromServer.readLine();
                if (textFromServer != null) {
                    System.out.println(textFromServer);
                }
            }
            // Close the socket - request is done
            sock.close();
        } catch (IOException x) {
            System.out.println("Socket error.");
            x.printStackTrace();
        }
    }
}