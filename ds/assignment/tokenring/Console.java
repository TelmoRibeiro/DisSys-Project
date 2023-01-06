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

public class Console implements Runnable {
    String hostAddress;
    Logger logger;

    public Console(String hostAddress, Logger logger) throws Exception {
        this.hostAddress = hostAddress;
        this.logger      = logger;
    }

    @Override
    public void run() {
        try {
            logger.info("console: endpoint running ...\n");

            Socket CCHSocket = new Socket(InetAddress.getByName(this.hostAddress), 12302);
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
