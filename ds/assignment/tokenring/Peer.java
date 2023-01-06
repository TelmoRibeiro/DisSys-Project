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
    int            hostID;
    String         hostAddress;
    String         nextAddress;
    Logger         logger;

    public Peer(int hostID, String hostAddress, String nextAddress) {
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
        int     hostID      = Integer.parseInt(args[0]);
        String  hostAddress = args[1];
        String  nextAddress = args[2];
        Peer    currPeer    = new Peer(hostID, hostAddress, nextAddress);
        currPeer.logger.info("peer " + hostID + " @ address = " + hostAddress + "\n");

        new Thread(new Connection(hostID, hostAddress, nextAddress, currPeer.logger)).start();
    }
}
