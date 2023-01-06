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

public class Client implements Runnable {
  String   message;
  String   hostAddress;
  String[] hostTable;
  Logger   logger;


  public Client(String message, String hostAddress, String[] hostTable, Logger logger) {
    this.message     = message;
    this.hostAddress = hostAddress;
    this.hostTable   = hostTable;
    this.logger      = logger;
  }

  @Override
  public void run() {
    Random random = new Random();
    int    peerID = random.nextInt(7);
    while(peerID == 0 || hostTable[peerID] == null) {
      peerID = random.nextInt(7);
    }
    String serverAddress = hostTable[peerID];
    try {
      Socket P2PSocket = new Socket(InetAddress.getByName(serverAddress), 12302);
      logger.info("client: new connection to " + P2PSocket.getInetAddress().getHostAddress() + "\n");

      PrintWriter P2POut = new PrintWriter(P2PSocket.getOutputStream(), true);

      P2POut.println(this.message);
      P2POut.flush();

      P2PSocket.close();
    } catch(Exception exception) { exception.printStackTrace(); }
  }
}
