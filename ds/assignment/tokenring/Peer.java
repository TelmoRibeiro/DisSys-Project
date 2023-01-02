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
    static int[] portAddress = new int[]{12301, 12302, 12303, 12304, 12305};    // we can access the nth portAddress through Peer.portAddress[n] in any part of the code
    static int    hostPort;                                                            // this peer port
    Logger logger;                                                              //

    public Peer(int hostPort) {
        this.hostPort    = hostPort;
        this.logger      = Logger.getLogger("logfile");
        try {
            FileHandler handler = new FileHandler("./" + this.hostPort + "_peer.log", true);
            logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch(Exception exception) { exception.printStackTrace(); }
    }

    public static void main(String[] args) throws Exception {
        Peer peer = new Peer(Integer.parseInt(args[0]));
        System.out.printf("new peer @ port=%s\n", args[0]);

        Scanner scanner = new Scanner(System.in);
        new Thread(new ConnectionHandler(Integer.parseInt(args[0]), peer.logger)).start();
        new Thread(new Console(hostPort, peer.logger)).start();
    }
}

class ConnectionHandler implements Runnable {
    int          hostPort;
    ServerSocket server;
    Logger       logger;

    public ConnectionHandler(int hostPort, Logger logger) throws Exception {
        this.hostPort    = hostPort;
        this.logger      = logger;
        this.server      = new ServerSocket(this.hostPort, 1, InetAddress.getByName("localhost"));  // trocar localhost por this.hostAddress no futuro 
    }

    public int getNextPort(int currentPort) {
        int index = -1;
        for (int i = 0; i < 5; i++) {
            if (Peer.portAddress[i] == currentPort && i < 4)       { index = i + 1; break; }
            else if (Peer.portAddress[i] == currentPort && i == 4) { index = 0; break; }  
        }
        return Peer.portAddress[index];
    }

    @Override
    public void run() {
        try {
            logger.info("ConnectionHandler: endpoint running at port " + this.hostPort + " ...");
            Socket prevSocket = null;
            Socket nextSocket = null;
            if (this.hostPort == 12301) { // se sou o primeiro
                try {
                    nextSocket = new Socket(InetAddress.getByName("localhost"), getNextPort(this.hostPort));
                    logger.info("ConnectionHandler: new connection to " + nextSocket.getPort());
                } catch(Exception exception) { /* exception.printStackTrace(); */ }
                while(true) {
                    try {
                        prevSocket = server.accept();
                        logger.info("ConnectionHandler: new connection from " + prevSocket.getPort());
                        break;
                    }
                    catch(Exception exception) { exception.printStackTrace(); }
                }
                new Thread(new Connection(hostPort, prevSocket, nextSocket, logger)).start();
            }
            else {
                while(true) {
                    try {
                        prevSocket = server.accept();
                        logger.info("ConnectionHandler: new connection from " + prevSocket.getPort());
                        break;
                    } catch(Exception exception) { exception.printStackTrace(); }
                }
                try {
                    nextSocket = new Socket(InetAddress.getByName("localhost"), getNextPort(this.hostPort));
                    logger.info("ConnectionHandler: new connection to " + nextSocket.getPort());
                } catch(Exception exception) { /* exception.printStackTrace(); */ }
                new Thread(new Connection(hostPort, prevSocket, nextSocket, logger)).start();
            }
        } catch(Exception exception) { exception.printStackTrace(); }
    } 
}

class Connection implements Runnable {
    int    hostPort;
    Socket prevSocket;
    Socket nextSocket;
    Logger logger;

    public Connection(int hostPort, Socket prevSocket, Socket nextSocket, Logger logger) {
        this.hostPort   = hostPort;
        this.prevSocket = prevSocket;
        this.nextSocket = nextSocket;
        this.logger     = logger;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(prevSocket.getInputStream()));
            PrintWriter   out = new PrintWriter(nextSocket.getOutputStream(), true);

            ServerSocket server = new ServerSocket(this.hostPort + 1000, 1, InetAddress.getByName("localhost"));
            Socket console = null;
            Boolean locked = false;
            
            try {
                console = server.accept();
                logger.info("Connection: new connection from " + console.getPort());
            } catch(Exception exception) { exception.printStackTrace(); }

            BufferedReader inConsole = new BufferedReader(new InputStreamReader(console.getInputStream()));
            PrintWriter   outConsole = new PrintWriter(console.getOutputStream(), true);
            
            if (hostPort == 12301) { locked = true; }
            while (true) {
                while (locked) {
                    String command;
                    command = inConsole.readLine();
                    logger.info("Connection: message from " + console.getPort() + " [message = " + command + "]");
                    if (command.equals("start")) {
                        locked = false;
                        int token = 0;
                        System.out.println("token = " + token + " @ port = " + hostPort);
                        out.println(String.valueOf(token));
                        out.flush();
                    }
                }
                String message;
                message = in.readLine();
                logger.info("ConnectionHandler: message from " + this.prevSocket.getPort() + " [message = " + message + "]");
                int token;
                token = Integer.parseInt(message);
                token++;
                System.out.println("token = " + token + " @ port = " + hostPort);
                Thread.sleep(3000);

                out.println(String.valueOf(token));
                out.flush();
            }
        } catch(Exception exception) { exception.printStackTrace(); }
    }
}

class Console implements Runnable {
    int    hostPort;
    Logger logger;

    public Console(int hostPort, Logger logger) throws Exception {
        this.hostPort = hostPort;
        this.logger   = logger;
    }

    @Override
    public void run() {
        try { 
            logger.info("Console: endpoint running ...\n");

            Scanner scanner = new Scanner(System.in);
            System.out.println("Type Y");
            String hold     = scanner.nextLine();
            System.out.println("Confirmed with: " + hold);

            Socket console = new Socket(InetAddress.getByName("localhost"), this.hostPort + 1000);
            logger.info("Console: connected to Connection endpoint ...\n");

            BufferedReader inConsole = new BufferedReader(new InputStreamReader(console.getInputStream()));
            PrintWriter   outConsole = new PrintWriter(console.getOutputStream(), true);

            String command;
            while(true) {
                command = scanner.nextLine();
                if (command.equals("quit")) { return; }
                outConsole.println(command);
                outConsole.flush();
            }
        } catch(Exception exception) { exception.printStackTrace(); }
    }
}

/*
2 Threads -> Console and ConnectionHandler
Console:
- start
- lock
- unlock
ConnectionHandler:
- keeps connection OPEN with 2 machines (for testing localhost but different port)
- DURING the development each peer knows the next port, LATER it will be switched to IPs
-
*/