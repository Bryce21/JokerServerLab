/*
    1. Bryce Reinhard / 9/25/21
    2. Java version
        java version "16.0.2" 2021-07-20
        Java(TM) SE Runtime Environment (build 16.0.2+7-67)
        Java HotSpot(TM) 64-Bit Server VM (build 16.0.2+7-67, mixed mode, sharing)
    3. Command line compilation example for JokeServer:
        javac JokeServer.java
        java JokeServer
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class JokeServer {
    public static void main(String args[]) throws IOException {
        int q_len = 6;
        int port;
        int adminPort;
        boolean isSecondaryServer;

        // setting up server information from passed in arguments
        // determines if secondary server or not
        // sets ports as expected
        if (args.length == 1) {
            port = 4546;
            adminPort = 5051;
            isSecondaryServer = true;
        } else {
            port = 4545;
            adminPort = 5050;
            isSecondaryServer = false;
        }
        // tracks what mode (Joke/proverb) the server is in
        ModeTracker mode = new ModeTracker(true);

        Socket sock;

        // async spin off admin listener on the admin port
        // AdminListern is in charge of setting mode and uses mode argument to change
        // mode
        AdminListener adminListener = new AdminListener(adminPort, q_len, mode, isSecondaryServer);
        Thread adminThread = new Thread(adminListener);
        adminThread.start();

        ServerSocket servsock = new ServerSocket(port, q_len);

        // ConcurrentHashMap to track state for client depending on user
        /*
         * UID from client : UserTracker class that tracks joke/proverbs and retrieves
         * them
         */
        ConcurrentHashMap<String, UserTracker> stateTracker = new ConcurrentHashMap<>();

        System.out
                .println("B. Reinhard's Joke server starting up, port: " + port + ", admin port: " + adminPort + "\n");

        /*
         * Start an infinite loop to wait for connections.
         */
        while (true) {
            sock = servsock.accept();
            new Worker(sock, stateTracker, mode, isSecondaryServer).start();
        }
    }
}

/*
 * Class that tracks the mode of the server. Uses boolean to represent mode
 */
class ModeTracker {
    // true is default (joke) false is second (proverb)
    static boolean mode;

    ModeTracker(boolean modeParam) {
        mode = modeParam;
    }

    // function to change mode boolean and toggle mode
    public static void toggleMode() {
        mode = !mode;
        if (mode) {
            System.out.println("Changed to joke mode");
        } else {
            System.out.println("Changed to proverb mode");
        }
    }
}

/*
 * AdminListener class that listens on the admin port Switches the mode of the
 * server Async start up above
 */
class AdminListener implements Runnable {
    int adminPort;
    int q_len;
    ModeTracker mode;
    boolean isSecondaryServer;

    AdminListener(int adminPortParam, int q_lenParam, ModeTracker modeParam, boolean isSecondaryServerParam) {
        adminPort = adminPortParam;
        q_len = q_lenParam;
        mode = modeParam;
        isSecondaryServer = isSecondaryServerParam;
    }

    public void run() {
        Socket adminSock;
        try {
            ServerSocket adminServSock = new ServerSocket(adminPort, q_len);
            // listen on the admin port, change mode on request recieved
            while (true) {
                adminSock = adminServSock.accept();
                new AdminWorker(adminSock, mode, isSecondaryServer).start();
            }
        } catch (IOException x) {
            System.out.println(x);
        }

    }
}

/*
 * AdminWorker is in charge of handling request passed in from AdminListener
 * Changes mode of the server
 */
class AdminWorker extends Thread {
    Socket adminSocket;
    ModeTracker mode;
    boolean isSecondaryServer;

    AdminWorker(Socket adminSocketParam, ModeTracker modeParam, boolean isSecondaryServerParam) {
        adminSocket = adminSocketParam;
        mode = modeParam;
        isSecondaryServer = isSecondaryServerParam;
    }

    public void run() {
        try {
            // Change the mode
            mode.toggleMode();

            // Print to AdminClient the mode after change
            PrintStream adminOut = new PrintStream(adminSocket.getOutputStream());
            String changedMode;
            if (mode.mode) {
                changedMode = "Joke";
            } else {
                changedMode = "Proverb";
            }
            if (isSecondaryServer) {
                adminOut.println("<S2> Changed mode to: " + changedMode);
            } else {
                adminOut.println("Changed mode to: " + changedMode);
            }

            adminSocket.close();
        } catch (IOException x) {
            System.out.println(x);
        }
    }
}

/*
 * JokeClient non admin worker. This is what retrieves user state based on
 * client UID and returns joke
 */
class Worker extends Thread {
    Socket sock;
    ConcurrentHashMap<String, UserTracker> stateTracker;
    ModeTracker mode;
    boolean isSecondaryServer;

    Worker(Socket s, ConcurrentHashMap<String, UserTracker> stateTrackerParam, ModeTracker modeParam,
            boolean isSecondaryServerParam) {
        sock = s;
        stateTracker = stateTrackerParam;
        mode = modeParam;
        isSecondaryServer = isSecondaryServerParam;
    }

    // since Worker extends thread it must implement run method. Point of entry
    public void run() {
        // PrintStream to add pretty print output from socket
        PrintStream out = null;
        // BufferedReader to read from socket
        BufferedReader in = null;
        try {
            // Initialize the in read from the socket input stream
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            // Initialize the output stream using the socket input stream
            out = new PrintStream(sock.getOutputStream());
            try {
                // Read user name from client
                String userName = in.readLine();
                // Read client from from client
                String clientUUID = in.readLine();

                // initialize UserTracker if it does not already exist. Ensures existence
                if (!stateTracker.containsKey(clientUUID)) {
                    stateTracker.put(clientUUID, new UserTracker(clientUUID));
                }
                // retrieve users state for joke and proverb retrieval
                UserTracker userInfo = stateTracker.get(clientUUID);

                JokeAndProverb resp;

                // Consume the current mode from the mode tracker.
                // true represents the default (joke)
                // User tracker retrieves the info and is in charge of maintaing the state
                if (mode.mode) {
                    resp = userInfo.getJoke();
                } else {
                    resp = userInfo.getProverb();
                }
                // format the data to respond with
                String jokeResponse = resp.id + " " + userName + ": " + resp.data;
                // Format depending on if is secondary server
                String cleanedJokeResponse = isSecondaryServer ? "<S2> " + jokeResponse : jokeResponse;
                System.out.println(cleanedJokeResponse);
                // respond to client
                out.println(cleanedJokeResponse);
            } catch (IOException x) {
                System.out.println("Server read error");
            }
            sock.close();
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

/*
 * JokeAndProverb class just stores id (JA||JB/PA/PB...etc) and data (joke or
 * proverb)
 */
class JokeAndProverb {
    String id;
    String data;

    JokeAndProverb(String idParam, String dataParam) {
        this.id = idParam;
        this.data = dataParam;
    }
}

class UserTracker {
    // uid identifier. UUID string from client. Not actually consumed anywhere
    // but should be part of user tracker class.
    String uid;
    // counters that keep track of where user is in the cycle
    int jokeIndexCounter;
    int proverbIndexCounter;

    // all the available jokes
    JokeAndProverb[] allJokes = { new JokeAndProverb("JA", "joke1"), new JokeAndProverb("JB", "joke2"),
            new JokeAndProverb("JC", "joke3"), new JokeAndProverb("JD", "joke4") };
    // available jokes - intizialize to all jokes
    JokeAndProverb[] availableJokes = allJokes;

    // proverb version of joke variables above
    JokeAndProverb[] allProverbs = { new JokeAndProverb("PA", "proverb1"), new JokeAndProverb("PB", "proverb2"),
            new JokeAndProverb("PC", "proverb3"), new JokeAndProverb("PD", "proverb4") };
    JokeAndProverb[] availableProverbs = allProverbs;

    UserTracker(String inputUID) {
        uid = inputUID;
        jokeIndexCounter = 0;
        proverbIndexCounter = 0;
    }

    // get a joke and update current state so cycle is correct
    public JokeAndProverb getJoke() {
        if (availableJokes.length == 0) {
            System.out.println("JOKE CYCLE COMPLETED.");
            // client has seen all the jokes, reset, ensures there's always something to
            availableJokes = allJokes;
        }
        // random index between (0, availableJokes.length). Pick a random joke from
        // availableJokes
        int randomJokeIndex = ThreadLocalRandom.current().nextInt(0, availableJokes.length);
        // joke selected
        JokeAndProverb jokeToReturn = availableJokes[randomJokeIndex];

        // todo use the known index instead of filter. Also assumes jokes are unique
        // filter out the current selected joke, decrementing available by one
        // basically an ugly way to do a remove one operation
        availableJokes = Arrays.stream(availableJokes).filter(x -> !x.equals(jokeToReturn))
                .toArray(JokeAndProverb[]::new);

        return jokeToReturn;
    }

    // get a proverb and update current state so cycle is correct
    public JokeAndProverb getProverb() {
        if (availableProverbs.length == 0) {
            System.out.println("PROVERB CYCLE COMPLETED.");
            // client has seen all the proverbs, reset, ensures there's always something to
            availableProverbs = allProverbs;
        }
        int randomProverbIndex = ThreadLocalRandom.current().nextInt(0, availableProverbs.length);
        JokeAndProverb proverbToReturn = availableProverbs[randomProverbIndex];

        // todo use the known index instead of filter. Also assumes proverbs are unique
        // filter out the current selected proverb, decrementing available by one
        // basically an ugly way to do a remove one operation
        availableProverbs = Arrays.stream(availableProverbs).filter(x -> !x.equals(proverbToReturn))
                .toArray(JokeAndProverb[]::new);

        return proverbToReturn;
    }

}
