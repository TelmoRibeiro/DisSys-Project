package ds.assignment.tokenring;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;


 
public class Peer {
    static Boolean hasToRead = false;   

    static int[]   AddressArray = new int[]{12301, 12302, 12303, 12304, 12305};   // global knowledge about the peers
    static int     nextAddress;                                                   // swap "int" for "String" when hostAddress was changed to IP
    int            hostAddress;                                                   // swap "int" for "String" when hostAddress was changed to IP
    Logger         logger;

    public int getNextAddress(int hostAddress) {
        for (int i = 0; i < 5; i++) {
            if (this.AddressArray[i] == hostAddress) {
                if (i == 4) { return this.AddressArray[0]; }
                return this.AddressArray[i + 1];
            }
        }
        return -1;
    }

    public Peer(int hostAddress) {  // swap "int" for "String" when hostAddress was changed to IP
        this.hostAddress    = hostAddress;
        this.nextAddress    = getNextAddress(this.hostAddress);
        this.logger         = Logger.getLogger("logfile");
        try {
            FileHandler handler = new FileHandler("./" + this.hostAddress + "_peer.log", true);
            this.logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch(Exception exception) { exception.printStackTrace(); }
    }

    public static void main(String[] args) throws Exception {
        Peer peer = new Peer(Integer.parseInt(args[0]));                            // remove "parseInt" when hostAddress was changed to IP
        System.out.printf("new peer @ address = %s\n\n", args[0]);

        new Thread(new Connection(Integer.parseInt(args[0]), peer.logger)).start(); // remove "parseInt" when hostAddress was changed to IP
    }
}


class Connection implements Runnable {
    int          hostAddress;   // swap "int" for "String" when hostAddress was changed to IP
    Logger       logger;
    ServerSocket CCSocket;

    public Connection(int hostAddress, Logger logger) throws Exception {                                // swap "int" for "String" when hostAddress was changed to IP
        this.hostAddress = hostAddress;
        this.logger      = logger;
        this.CCSocket    = new ServerSocket(this.hostAddress, 1, InetAddress.getByName("localhost"));   // swap "localhost" for "this.hostAddress" and "hostAddress" for a Port
    }

    @Override
    public void run() {
        try {
            logger.info("connection: endpoint running at port " + this.hostAddress + " ...\n");           // hostAddress will not be a Port in the end

            Socket prevSocket = null;
            Socket nextSocket = null;

            if (this.hostAddress == 12301) { // TROCAR POR UM ID //
                try {
                    nextSocket = new Socket(InetAddress.getByName("localhost"), Peer.nextAddress);
                    logger.info("connection: new connection to " + nextSocket.getPort() + "\n");

                    prevSocket = CCSocket.accept();
                    logger.info("connection: new connection from " + prevSocket.getPort() + "\n");
                } catch(Exception exception) { exception.printStackTrace(); }
                new Thread(new ConnectionHandler(hostAddress, prevSocket, nextSocket, logger)).start();
            }
            else {
                try {
                    prevSocket = CCSocket.accept();
                    logger.info("connection: new connection from " + prevSocket.getPort() + "\n");
                
                    nextSocket = new Socket(InetAddress.getByName("localhost"), Peer.nextAddress);
                    logger.info("connection: new connection to " + nextSocket.getPort() + "\n");
                } catch(Exception exception) { exception.printStackTrace(); }
                new Thread(new ConnectionHandler(hostAddress, prevSocket, nextSocket, logger)).start();
            }
        } catch(Exception exception) { exception.printStackTrace(); }
    } 
}


class ConnectionHandler implements Runnable {
    int    hostAddress;
    Socket prevSocket;
    Socket nextSocket;
    Logger logger;

    public ConnectionHandler(int hostAddress, Socket prevSocket, Socket nextSocket, Logger logger) {
        this.hostAddress = hostAddress;
        this.prevSocket  = prevSocket;
        this.nextSocket  = nextSocket;
        this.logger      = logger;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(prevSocket.getInputStream()));
            PrintWriter   out = new PrintWriter(nextSocket.getOutputStream(), true);

            ServerSocket server = new ServerSocket(this.hostAddress + 1000, 1, InetAddress.getByName("localhost"));
            new Thread(new Console(hostAddress, logger)).start();

            Socket CHCSocket = null;
            try {
                CHCSocket = server.accept();
                logger.info("connection_handler: new connection from " + CHCSocket.getPort() + "\n");
            } catch(Exception exception) { exception.printStackTrace(); }

            BufferedReader CHCIn = new BufferedReader(new InputStreamReader(CHCSocket.getInputStream()));
            
            if (hostAddress == 12301) { Peer.hasToRead = true; }
            while(true) {
                if (Peer.hasToRead) {
                    String command = CHCIn.readLine();
                    logger.info("connection_handler: message from " + CHCSocket.getPort() + " [command = " + command + "]\n");

                    switch(command) {
                        case "start":
                            int token = 0;
                            System.out.println("token = " + token + " @ port = " + hostAddress + "\n");
                            Peer.hasToRead = false; 
                            out.println(String.valueOf(token));
                            out.flush();
                            break;
                        case "unlock":
                            Peer.hasToRead = false;
                            break;
                        case "lock":
                            Peer.hasToRead = true;
                            break;
                    }
                }
                if (!Peer.hasToRead) {
                    String message = in.readLine();
                    logger.info("connection_handler: message from " + this.prevSocket.getPort() + " [message = " + message + "]\n");

                    int token = Integer.parseInt(message);
                    token++;
                    System.out.println("token = " + token + " @ port = " + hostAddress);
                    Thread.sleep(3000); // maybe remove from here
                    out.println(String.valueOf(token));
                    out.flush();
                }
            }                 
        } catch(Exception exception) { exception.printStackTrace(); }
    }
}


class Console implements Runnable {
    int    hostAddress;
    Logger logger;

    public Console(int hostAddress, Logger logger) throws Exception {
        this.hostAddress = hostAddress;
        this.logger      = logger;
    }

    @Override
    public void run() {
        try { 
            logger.info("console: endpoint running ...\n");

            Socket CCHSocket = new Socket(InetAddress.getByName("localhost"), this.hostAddress + 1000);
            logger.info("console: connected to connection_handler endpoint ...\n");
            
            PrintWriter CCHOut = new PrintWriter(CCHSocket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);
            while(true) {
                String command = scanner.nextLine();
                System.out.println();
                Peer.hasToRead = true;
                CCHOut.println(command);
                CCHOut.flush();
            }
        } catch(Exception exception) { exception.printStackTrace(); }
    }
}