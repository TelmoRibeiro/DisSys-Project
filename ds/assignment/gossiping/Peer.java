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

public class Peer {
  ArrayList<String>        fileList;
  HashMap<Integer, String> messageMap;
  int                      hostID;
  String                   hostAddress;
  String[]                 hostTable;
  Logger                   logger;


  public Peer(int hostID, String hostAddress) {
    this.fileList    = new ArrayList<String>();
    this.messageMap  = new HashMap<Integer, String>();
    this.hostID      = hostID;
    this.hostAddress = hostAddress;
    this.hostTable   = new String[7];
    this.logger      = Logger.getLogger("logfile");
    try {
      FileHandler handler = new FileHandler("./" + this.hostAddress + "_peer.log", true);
      this.logger.addHandler(handler);
      SimpleFormatter formatter = new SimpleFormatter();
      handler.setFormatter(formatter);
    } catch(Exception exception) { exception.printStackTrace(); }
  }


  static private void readCommands(int hostID, String[] hostTable, Logger logger) {
    Scanner scanner = new Scanner(System.in);
    String  command = scanner.next();
    while(!command.equals("end")) {
      switch(command) {
        case "register":
          String serverAddress = scanner.next();
          System.out.println();
          handshakeClient(serverAddress, hostID, hostTable, logger);
          break;
        default: break;
      }
      command = scanner.next();
    }
  }


  static private void handshakeClient(String serverAddress, int hostID, String[] hostTable, Logger logger) {
    try {
      Socket P2PSocket = new Socket(InetAddress.getByName(serverAddress), 12301);
      logger.info("handshake_client: new connection to " + P2PSocket.getInetAddress().getHostAddress() + "\n");

      BufferedReader P2PIn = new BufferedReader(new InputStreamReader(P2PSocket.getInputStream()));
      PrintWriter   P2POut = new PrintWriter(P2PSocket.getOutputStream(), true);

      String message = P2PIn.readLine();
      logger.info("handshake_client: new message from " + P2PSocket.getInetAddress().getHostAddress() + " [message=" + message + "]\n");

      int serverID = Integer.parseInt(message);
      hostTable[serverID] = serverAddress;

      P2POut.println(hostID);
      logger.info("handshake_client: new message to " + P2PSocket.getInetAddress().getHostAddress() + " [message=" + hostID + "]\n");
      P2POut.flush();

      P2PSocket.close();
    } catch(Exception exception) { exception.printStackTrace(); }
  }


  public static void main(String[] args) throws Exception {
    int    hostID      = Integer.parseInt(args[0]);
    String hostAddress = args[1];
    Peer   currPeer    = new Peer(hostID, hostAddress);
    currPeer.logger.info("peer " + hostID + " @ address = " + hostAddress + "\n");

    Scanner scanner = new Scanner(new File("wordlist.txt"));
    while(scanner.hasNextLine()){
      currPeer.fileList.add(scanner.nextLine());
    }
    scanner.close();

    new Thread(new HandshakeServer(hostID, hostAddress, currPeer.hostTable, currPeer.logger)).start();
    readCommands(hostID, currPeer.hostTable, currPeer.logger);
    
    System.out.println("Table Status:");
    for (int i = 1; i < 7; i++) {
      System.out.println("Peer " + i + ": " + currPeer.hostTable[i]);
    }
    System.out.println();

    new Thread(new WordGenerator(currPeer.fileList)).start();
    new Thread(new Server(hostAddress, currPeer.hostTable, currPeer.messageMap, currPeer.logger)).start();

    int hostMessages = 1;
    ServerSocket server = new ServerSocket(12303, 1, InetAddress.getByName("localhost"));
    while (true) {
      try {
        Socket PWGSocket = server.accept();

        BufferedReader PWGIn = new BufferedReader(new InputStreamReader(PWGSocket.getInputStream()));

        String word    = PWGIn.readLine();
        String key     = String.valueOf(hostID) + String.valueOf(hostMessages);
        currPeer.messageMap.put(Integer.parseInt(key), word);

        System.out.println("Map Status:");
        for (HashMap.Entry<Integer, String> entry: currPeer.messageMap.entrySet()) {
          System.out.println(entry.getKey() + ":" + entry.getValue().toString());
        }
        System.out.println();

        String message = String.valueOf(hostID) + String.valueOf(hostMessages) + " " + word;
        new Thread(new Client(message, hostAddress, currPeer.hostTable, currPeer.logger)).start();

        hostMessages++;

        PWGSocket.close();
      } catch(Exception exception) { exception.printStackTrace(); }
    }
  }
}

class HandshakeServer implements Runnable {
  int          hostID;
  String       hostAddress;
  String[]     hostTable;
  Logger       logger;
  ServerSocket handshakeServer;


  public HandshakeServer(int hostID, String hostAddress, String[] hostTable, Logger logger) throws Exception {
    this.hostID          = hostID;
    this.hostAddress     = hostAddress;
    this.hostTable       = hostTable;
    this.logger          = logger;
    this.handshakeServer = new ServerSocket(12301, 5, InetAddress.getByName(this.hostAddress));
  }


  @Override
  public void run() {
    while(true) {
      try {
        Socket P2PSocket = handshakeServer.accept();
        logger.info("handshake_server: new connection from " + P2PSocket.getInetAddress().getHostAddress() + "\n");

        BufferedReader P2PIn = new BufferedReader(new InputStreamReader(P2PSocket.getInputStream()));
        PrintWriter   P2POut = new PrintWriter(P2PSocket.getOutputStream(), true);

        P2POut.println(String.valueOf(this.hostID));
        logger.info("handshake_server: new message to " + P2PSocket.getInetAddress().getHostAddress() + " [message=" + this.hostID + "]\n");
        P2POut.flush();

        String message = P2PIn.readLine();
        logger.info("handshake_server: new message from " + P2PSocket.getInetAddress().getHostAddress() + " [message=" + message + "]\n");

        int clientID = Integer.parseInt(message);
        hostTable[clientID] = P2PSocket.getInetAddress().getHostAddress();

        P2PSocket.close();
      } catch(Exception exception) { exception.printStackTrace(); }
    }
  }
}
