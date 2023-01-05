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
    // Only for testing
    for (int i = 1; i < 7; i++) {
      System.out.println("Peer " + i + ": " + currPeer.hostTable[i]);
    }
    // Only for testing
    new Thread(new WordGenerator(currPeer.fileList)).start();
    new Thread(new Server(hostAddress, currPeer.hostTable, currPeer.messageMap, currPeer.logger)).start();
    
    int hostMessages = 0;
    ServerSocket server = new ServerSocket(12303, 1, InetAddress.getByName("localhost")); 
    while (true) {
      try {
        Socket PWGSocket = server.accept();

        BufferedReader PWGIn = new BufferedReader(new InputStreamReader(PWGSocket.getInputStream()));

        String word    = PWGIn.readLine();
        String key     = String.valueOf(hostID) + String.valueOf(hostMessages);
        currPeer.messageMap.put(Integer.parseInt(key), word);

        String message = String.valueOf(hostID) + String.valueOf(hostMessages) + " " + word;
        new Thread(new Client(message, hostAddress, currPeer.hostTable, currPeer.logger)).start();

        hostMessages++;

        PWGSocket.close();
      } catch(Exception exception) { exception.printStackTrace(); }
    }
  }
}


class Client implements Runnable {
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


class Server implements Runnable{
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



class WordGenerator implements Runnable {
  ArrayList<String> fileList;

  public WordGenerator(ArrayList<String> fileList) {
    this.fileList = fileList;
  }

  public void generate(ArrayList<String> fileList) {
    Random random = new Random();
    String word   = fileList.get(random.nextInt(fileList.size()));
    System.out.println("Generated Word: " + word);    // REMOVE WHEN FINISHED //

    try {    
      Socket WGPSocket = new Socket(InetAddress.getByName("localhost"), 12303);

      PrintWriter WGPOut = new PrintWriter(WGPSocket.getOutputStream(), true);

      WGPOut.println(word);
      WGPOut.flush();

      WGPOut.close();
    } catch(Exception exception) { exception.printStackTrace(); } 
  }

  @Override
  public void run() {
    PoissonProcess process = new PoissonProcess(2, new Random());
    while(true) {
      double dt = process.timeForNextEvent() * 60 * 1000;
      long time = (long)dt;
      System.out.println("Poisson Value: " + dt);   // REMOVE WHEN FINISHED //
      System.out.println("t - " + time);            // REMOVE WHEN FINISHED // 
      try { Thread.sleep(time); } catch(Exception exception) { exception.printStackTrace(); }
      generate(this.fileList);
    }
  }
}






class PoissonProcess {

  /**
   * Rate parameter.
   */
  private final double lambda;
  
  /**
   * Random number generator to use.
   */
  private final Random rng;
  
  /**
   * Constructor.
   * @param lambda Rate parameter.
   * @param rng Base random number generator to use.
   */
  public PoissonProcess(double lambda, Random rng) {
    if (lambda <= 0d) {
      throw new IllegalArgumentException("Supplied rate parameter is not positive: " + lambda);
    }
    if (rng == null) {
      throw new IllegalArgumentException("Null RNG argument");
    }
    this.lambda = lambda;
    this.rng = rng;
  }
  
  /**
   * Get rate parameter.
   * @return The RNG in use.
   */
  public double getLambda() {
    return lambda;
  }
  
  /**
   * Get random number generator.
   * @return The RNG in use.
   */
  public Random getRNG() {
    return rng;
  }
  
  /**
   * Get time for next event. 
   * 
   * @return A random inter-arrival time.
   */
  public double timeForNextEvent() {
    // The sequence of inter-arrival times are independent and have an exponential distribution with mean 1/lambda.
    // To generate it we use the recipe in https://en.wikipedia.org/wiki/Exponential_distribution#Generating_exponential_variates
    return - Math.log(1.0 - rng.nextDouble()) / lambda;
  }
  
  /**
   * Get number of events in an unit of time.
   * The call is shorthand for <code>events(1.0)</code>.
   * @return Number of events.
   */
  public int events() {
    return events(1d);
  }
  
  /**
   * Get number of occurrences in time t (assumed to be relative to the unit time).
   * @param time Length of time interval.
   */
  public int events(double time) {
    // The algorithm based on inverse transform sampling is used -- see:
    // https://en.wikipedia.org/wiki/Poisson_distribution#Generating_Poisson-distributed_random_variables
    int n = 0;
    double p = Math.exp(-lambda * time);
    double s = p;
    double u = rng.nextDouble();
    while (u > s) {
      n = n + 1;
      p = p * lambda / n;
      s = s + p;
    }    
    return n;
  }
  
}

class SampleValues {

  private final String id;
  private int count;
  private double sum, sumSq, min, max;
  
  SampleValues(String statsId) {
    id = statsId;
    count = 0;
    sum = 0;
    sumSq = 0;
    min = Double.MAX_VALUE;
    max = Double.MIN_VALUE;

  }
  
  public void add(double v) {
    count++;
    sum += v;
    sumSq += (v * v);
    min = Math.min(v, min);
    max = Math.max(v, max);
  }
  
  public String id() {
    return id;
  }
  
  public double min() { 
    return min; 
  }
  
  public double max() { 
    return max; 
  }
  
  public int count() { 
    return count; 
  }
  
  public double mean() { 
    return sum / count; 
  }
  
  public double stddev() { 
    return Math.sqrt(variance());
  }
   
  public double variance() { 
    double u = mean();
    return u * u + (sumSq - 2 * u * sum)/count;
  }
  
  @Override
  public String toString() {
    return String.format("%s|count=%d|avg=%f|variance=%f|min=%f|max=%f",
                         id(), count(), mean(), variance(), min(), max());
  }
  
  public void mergeWith(SampleValues other) {
    count += other.count;
    sum += other.sum;
    sumSq += other.sumSq;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
  }
  
}