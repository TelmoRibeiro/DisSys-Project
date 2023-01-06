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

public class Connection implements Runnable {
    int          hostID;
    String       hostAddress;
    String       nextAddress;
    Logger       logger;
    ServerSocket CCSocket;

    public Connection(int hostID, String hostAddress, String nextAddress, Logger logger) throws Exception {
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.nextAddress = nextAddress;
        this.logger      = logger;
        this.CCSocket    = new ServerSocket(12301, 1, InetAddress.getByName(this.hostAddress));
    }

    @Override
    public void run() {
        try {
            logger.info("connection: endpoint running ...\n");

            if (this.hostID == 1) {
                try {
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Press ENTER when all peers are running");
                    String hold = scanner.nextLine();
                    System.out.println();

                    Socket nextSocket = new Socket(InetAddress.getByName(this.nextAddress), 12301);
                    logger.info("connection: new connection to " +   nextSocket.getInetAddress().getHostAddress() + "\n");

                    Socket prevSocket = CCSocket.accept();
                    logger.info("connection: new connection from " + prevSocket.getInetAddress().getHostAddress() + "\n");

                    new Thread(new ConnectionHandler(hostID, hostAddress, prevSocket, nextSocket, logger)).start();
                } catch(Exception exception) { exception.printStackTrace(); }
                return;
            }

            try {
                Socket prevSocket = CCSocket.accept();
                logger.info("connection: new connection from " + prevSocket.getInetAddress().getHostAddress() + "\n");

                Socket nextSocket = new Socket(InetAddress.getByName(this.nextAddress), 12301);
                logger.info("connection: new connection to " +   nextSocket.getInetAddress().getHostAddress() + "\n");

                new Thread(new ConnectionHandler(hostID, hostAddress, prevSocket, nextSocket, logger)).start();
            } catch(Exception exception) { exception.printStackTrace(); }
            return;
        } catch(Exception exception) { exception.printStackTrace(); }
    }
}


class ConnectionHandler implements Runnable {
    int          hostID;
    String       hostAddress;
    Socket       prevSocket;
    Socket       nextSocket;
    Logger       logger;
    ServerSocket server;

    public ConnectionHandler(int hostID, String hostAddress, Socket prevSocket, Socket nextSocket, Logger logger) throws Exception {
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.prevSocket  = prevSocket;
        this.nextSocket  = nextSocket;
        this.logger      = logger;
        this.server      = new ServerSocket(12302, 1, InetAddress.getByName(this.hostAddress));
    }

    @Override
    public void run() {
        try {
            BufferedReader prevIn = new BufferedReader(new InputStreamReader(prevSocket.getInputStream()));
            PrintWriter   nextOut = new PrintWriter(nextSocket.getOutputStream(), true);

            new Thread(new Console(hostAddress, logger)).start();
            Socket CHCSocket = server.accept();
            logger.info("connection_handler: new connection from " + CHCSocket.getInetAddress().getHostAddress() + "\n");

            BufferedReader CHCIn = new BufferedReader(new InputStreamReader(CHCSocket.getInputStream()));

            if (hostID == 1) { Peer.hasToRead = true; }
            while(true) {
                if (Peer.hasToRead) {
                    String command = CHCIn.readLine();
                    logger.info("connection_handler: message from " + CHCSocket.getInetAddress().getHostAddress() + " [command=" + command +"]\n");

                    switch(command) {
                        case "start":
                            int token = 0;
                            System.out.println(this.hostAddress + ": " + "token = " + token + "\n");
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
                    logger.info("connection_handler: message from " + this.prevSocket.getInetAddress().getHostAddress() + " [message=" + message + "]\n");

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
