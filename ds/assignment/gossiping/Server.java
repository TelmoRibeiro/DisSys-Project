package ds.assignment.gossiping;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.HashMap;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Server implements Runnable{
  String                    hostAddress;
  String[]                  hostTable;
  Logger                    logger;
  ServerSocket              server;
  ArrayList<Integer>        blacklist;
  HashMap<Integer, String>  messageMap;


  public Server(String hostAddress, String[] hostTable, HashMap<Integer, String> messageMap, Logger logger) throws Exception {
    this.hostAddress = hostAddress;
    this.hostTable   = hostTable;
    this.messageMap  = messageMap;
    this.logger      = logger;
    this.server      = new ServerSocket(12302, 50, InetAddress.getByName(this.hostAddress));
    this.blacklist   = new ArrayList<Integer>();
  }


  @Override
  public void run() {
    while(true) {
      try {
        Socket P2PSocket = server.accept();
        logger.info("server: new connection from " + P2PSocket.getInetAddress().getHostAddress() + "\n");

        BufferedReader P2PIn = new BufferedReader(new InputStreamReader(P2PSocket.getInputStream()));

        String message = P2PIn.readLine();
        logger.info("server: new message from " + P2PSocket.getInetAddress().getHostAddress() + " [message=" + message + "]\n");

        Scanner scanner = new Scanner(message);
        int messageID   = Integer.parseInt(scanner.next());
        String word     = scanner.next();

        if (!messageMap.containsKey(messageID)) {
          messageMap.put(messageID, word);
          // only for testing //
          System.out.println("Word: " + word + " added to the map with ID: " + messageID);
          // only for testing //
          new Thread(new Client(message, this.hostAddress, this.hostTable, this.logger)).start();
        }
        else if (!blacklist.contains(messageID) && Math.random() > 0.20) { new Thread(new Client(message, this.hostAddress, this.hostTable, this.logger)).start(); }
        else                                                            { blacklist.add(messageID); }
        P2PSocket.close();
      } catch(Exception exception) { exception.printStackTrace(); }
    }
  }
}
