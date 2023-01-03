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

// vamos receber os nomes das maquinas
// resolver os seus IPs
// usar os IPs

// Agreements:
// - Ports Inter-Peer 12301
// - Ports Intra-Peer 12302
 
public class Peer {
    static Boolean hasToRead = false;
    int            hostID;
    int            hostAddress;         // swap "int" for "String" when hostAddress is changed to IP //
    int            nextAddress;         // swap "int" for "String" when hostAddress is changed to IP //
    Logger         logger;

    public Peer(int hostID, int hostAddress, int nextAddress) { // swap "int" for "String" when hostAddress is changed to IP //
        this.hostID         = hostID;
        this.hostAddress    = hostAddress;
        this.nextAddress    = nextAddress;
        this.logger         = Logger.getLogger("logfile");
        try {
            FileHandler handler = new FileHandler("./" + this.hostAddress + "_peer.log", true);
            this.logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch(Exception exception) { exception.printStackTrace(); }
    }

    public static void main(String[] args) throws Exception {
        int  hostID      = Integer.parseInt(args[0]);
        int  hostAddress = Integer.parseInt(args[1]);                       // remove "parseInt" when hostAddress is changed to IP
        int  nextAddress = Integer.parseInt(args[2]);                       // remove "parseInt" when hostAddress is changed to IP
        Peer currPeer    = new Peer(hostID, hostAddress, nextAddress);
        currPeer.logger.info("peer " + hostID + " @ address = " + hostAddress + "\n");

        new Thread(new Connection(hostID, hostAddress, nextAddress, currPeer.logger)).start();
    }
}


class Connection implements Runnable {
    int          hostID; 
    int          hostAddress; // swap "int" for "String" when hostAddress is changed to IP
    int          nextAddress; // swap "int" for "String" when hostAddress is changed to IP
    Logger       logger;
    ServerSocket CCSocket;

    public Connection(int hostID, int hostAddress, int nextAddress, Logger logger) throws Exception {   // swap "int" for "String" when hostAddress is changed to IP
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.nextAddress = nextAddress;
        this.logger      = logger;
        this.CCSocket    = new ServerSocket(this.hostAddress, 1, InetAddress.getByName("localhost"));  // swap "localhost" for "this.hostAddress" and "hostAddress" for an agreed Port
    }

    @Override
    public void run() {
        try {
            logger.info("connection: endpoint running at port " + this.hostAddress + " ...\n");         // hostAddress will not be a Port in the end

            if (this.hostID == 1) {
                try {
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Press ENTER when all peers are running");
                    String hold = scanner.nextLine();
                    System.out.println();

                    Socket nextSocket = new Socket(InetAddress.getByName("localhost"), this.nextAddress);
                    logger.info("connection: new connection to " +   nextSocket.getPort() + "\n");

                    Socket prevSocket = CCSocket.accept();
                    logger.info("connection: new connection from " + prevSocket.getPort() + "\n");

                    new Thread(new ConnectionHandler(hostID, hostAddress, prevSocket, nextSocket, logger)).start();
                } catch(Exception exception) { exception.printStackTrace(); }
                return;
            }

            try {
                Socket prevSocket = CCSocket.accept();
                logger.info("connection: new connection from " + prevSocket.getPort() + "\n");
                
                Socket nextSocket = new Socket(InetAddress.getByName("localhost"), this.nextAddress);
                logger.info("connection: new connection to " +   nextSocket.getPort() + "\n");

                new Thread(new ConnectionHandler(hostID, hostAddress, prevSocket, nextSocket, logger)).start();
            } catch(Exception exception) { exception.printStackTrace(); }
            return;
        } catch(Exception exception) { exception.printStackTrace(); }
    }
}


class ConnectionHandler implements Runnable {
    int          hostID;
    int          hostAddress;
    Socket       prevSocket;
    Socket       nextSocket;
    Logger       logger;
    ServerSocket server;

    public ConnectionHandler(int hostID, int hostAddress, Socket prevSocket, Socket nextSocket, Logger logger) throws Exception {    // swap "int" for "String" when hostAddress is changed to IP
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.prevSocket  = prevSocket;
        this.nextSocket  = nextSocket;
        this.logger      = logger;
        this.server      = new ServerSocket(this.hostAddress + 1000, 1, InetAddress.getByName("localhost"));   // swap  Ports and "localhost"
    }

    @Override
    public void run() {
        try {
            BufferedReader prevIn = new BufferedReader(new InputStreamReader(prevSocket.getInputStream()));
            PrintWriter   nextOut = new PrintWriter(nextSocket.getOutputStream(), true);

            new Thread(new Console(hostAddress, logger)).start();
            Socket CHCSocket = server.accept();
            logger.info("connection_handler: new connection from " + CHCSocket.getPort() + "\n");

            BufferedReader CHCIn = new BufferedReader(new InputStreamReader(CHCSocket.getInputStream()));
            
            if (hostID == 1) { Peer.hasToRead = true; }
            while(true) {
                if (Peer.hasToRead) {
                    String command = CHCIn.readLine();
                    logger.info("connection_handler: message from " + CHCSocket.getPort() + " [command=" + command +"]\n");

                    switch(command) {
                        case "start":
                            int token = 0;
                            System.out.println(hostAddress + ": " + "token = " + token + "\n");
                            nextOut.println(String.valueOf(token));
                            nextOut.flush();
                            Peer.hasToRead = false;
                            break;
                        case "unlock":
                            Peer.hasToRead = false;
                            break;
                        case "lock":
                            Peer.hasToRead = true;
                            break;

                        default:
                            break;
                    }
                }
                if (!Peer.hasToRead) {
                    String message = prevIn.readLine();
                    logger.info("connection_handler: message from " + this.prevSocket.getPort() + " [message=" + message + "]\n");

                    int token = Integer.parseInt(message);
                    token++;
                    System.out.println(hostAddress + ": " + "token = " + token + "\n");
                    
                    nextOut.println(String.valueOf(token));
                    nextOut.flush();
                }

                Thread.sleep(3000);
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