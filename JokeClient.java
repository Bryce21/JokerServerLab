/*
    1. Bryce Reinhard / 9/25/21
    2. Java version
        java version "16.0.2" 2021-07-20
        Java(TM) SE Runtime Environment (build 16.0.2+7-67)
        Java HotSpot(TM) 64-Bit Server VM (build 16.0.2+7-67, mixed mode, sharing)
    3. Command line compilation example for JokeClient:
        javac JokeClient.java
        java JokeClient
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
import java.util.UUID;

public class JokeClient {
    public static void main(String args[]) {
        // clientUID is the unique identifer for this client
        // Passed into JokeServer so it can retrieve this client's state
        String clientUID = UUID.randomUUID().toString();

        // server1 and server2 information.
        // set defaults that are overwritten depending on arguments
        String server1Name = "localhost";
        String server2Name = "default";
        boolean hasServer2 = false;

        // port 1 is Server 1 port
        int port1 = 4545;
        // port 2 is Server 2 port (if it is configured)
        int port2 = 4546;

        // if length 1 user configured server name
        // overwrite default of localhost
        if (args.length == 1) {
            server1Name = args[0];
        } else if (args.length == 2) {
            // if length 2 user configured Server1 and Server2
            // Set server names and set hasServer2 boolean to represent
            // there being a server 2 configured
            server1Name = args[0];
            server2Name = args[1];
            hasServer2 = true;
        }
        // usedServer represents which server the joke client talks to
        // default to server1Name since that is always there
        String usedServer = server1Name;
        int usedPort = port1;
        // this represents which server is being talked to
        // true for firstServer, false for second server.
        boolean firstServer = true;

        // print information about servers
        System.out.println("Server one: " + server1Name + ", port: " + port1);
        if (hasServer2) {
            System.out.println("Server two: " + server2Name + ", port: " + port2);
        }
        // Makes an input reader
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        // get the name of the user to send to JokeServer to put in as part of jokes
        String userName = getUserName(in);
        try {
            // this boolean represents waiting for new connections / the client is alive
            boolean infiniteLoop = true;
            while (infiniteLoop) {
                System.out.println("Press enter to get response");
                if (hasServer2) {
                    System.out.println("Enter \"s\" to toggle used server");
                }
                System.out.flush();

                // read in userInput
                // s or quit have action
                String userInput = in.readLine();
                if (userInput.equals("s")) {
                    // user wants to toggle used server
                    if (hasServer2) {
                        // change server then set usedServer depending
                        // on the boolean that represents which server user wants to use
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
                        // user wanted to toggle used server with no secondary server configured
                        System.out.println("No secondary server being used");
                    }
                } else if (userInput.equals("quit")) {
                    // user wanted kill this client.
                    System.out.println("Exiting");
                    infiniteLoop = false;
                } else {
                    // User has put in input that doesn't mean anything - they want data
                    // query the current set server with userName and client uid
                    queryServer(usedServer, usedPort, userName, clientUID);
                }

            }
        } catch (IOException x) {
            x.printStackTrace();
        }
    }

    /*
     * Reads in the user name from input
     */
    static String getUserName(BufferedReader input) {
        System.out.println("Enter name:");
        try {
            return input.readLine();
        } catch (IOException y) {
            System.out.println("Error reading user name:");
            y.printStackTrace();
            return "defaultUserName";
        }
    }

    /*
     * Function that reaches out to used server. Sends userName and clientUUID to
     * server to set jokes and retrieve state Outputs the response from the server
     */
    static void queryServer(String serverName, int port1, String userName, String clientUUID) {
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String resp = null;

        try {
            sock = new Socket(serverName, port1);
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServer = new PrintStream(sock.getOutputStream());
            // write the userName and clientUUID for server to use to make response
            toServer.println(userName);
            toServer.println(clientUUID);
            toServer.flush();

            // print response
            while ((resp = fromServer.readLine()) != null) {
                System.out.println(resp);
            }
            // Close the socket - request is done
            sock.close();
        } catch (IOException x) {
            System.out.println("Socket error.");
            x.printStackTrace();
        }
    }
}