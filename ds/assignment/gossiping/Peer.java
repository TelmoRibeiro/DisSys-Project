package ds.assignment.gossiping;

import java.io.File;
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
import java.util.*; // almost for sure it is Arrays



public class Peer {
    static Boolean           hasNewWord;
    static ArrayList<String> fileList;
    static ArrayList<String> list;
    static int[]             table;          // x <=> IP(x) where x is a peer // swap for "String" 
    int                      hostID;
    String                   hostAddress;
    int                      hostPort;
    Logger                   logger;
    

    public Peer(int hostID, String hostAddress, int hostPort) {
        this.hasNewWord  = false;
        this.fileList    = new ArrayList<String>();
        this.list        = new ArrayList<String>();
        this.table       = new int[7];  // swap for "String"
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.hostPort    = hostPort; 
        this.logger      = Logger.getLogger("logfile");
        try {
            FileHandler handler = new FileHandler("./" + this.hostAddress + "_peer.log", true);
            this.logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch(Exception exception) { exception.printStackTrace(); }
    }

    public static void main(String[] args) throws Exception {
        int    hostID      = Integer.parseInt(args[0]);
        String hostAddress = args[1];
        int    hostPort    = Integer.parseInt(args[2]); 
        Peer   currPeer    = new Peer(hostID, hostAddress, hostPort);
        currPeer.logger.info("peer " + hostID + " @ address = " + hostAddress + "\n");

        Scanner fileScanner = new Scanner(new File("wordlist.txt"));
        while(fileScanner.hasNextLine()){
            currPeer.fileList.add(fileScanner.nextLine());
        }
        fileScanner.close();

        new Thread(new Server(hostID, hostAddress, hostPort, currPeer.logger)).start();
        
        Scanner scanner = new Scanner(System.in);
        String  command = scanner.next();
        while(!command.equals("end")) {
            switch(command) {
                case "register":
                    try {
                        int nextPort    = Integer.parseInt(scanner.next());                            // needs to be swaped for nextAddress
                        Socket PPSocket = new Socket(InetAddress.getByName("localhost"), nextPort);    // localhost needs to be swaped for nextAddress and this port becomes a agreed upon one
                        currPeer.logger.info("client: new connection to " + PPSocket.getInetAddress().getHostAddress() + "\n");

                        BufferedReader PPIn = new BufferedReader(new InputStreamReader(PPSocket.getInputStream()));
                        PrintWriter   PPOut = new PrintWriter(PPSocket.getOutputStream(), true);

                        String message = PPIn.readLine();
                        currPeer.logger.info("client: new message from " + PPSocket.getInetAddress().getHostAddress() + " [message=" + message + "]\n");

                        int nextID = Integer.parseInt(message);
                        Peer.table[nextID] = nextPort;                                                      // needs to be swaped for nextAddress;
                        
                        PPOut.println(currPeer.hostID);
                        PPOut.flush();
                    } catch(Exception exception) { exception.printStackTrace(); }
                    break;
                default: break;
            }
            command = scanner.next();
        }
        System.out.println();
        // Only for testing
        for (int i = 1; i < 7; i++) {
            System.out.println("Peer " + i + ": " + Peer.table[i]);
        }
        // Only for testing

        new Thread(new wordGenerator()).start();

        while(true) {
            if (Peer.hasNewWord) {
                // read and writer already setted
                // spreads the word through gossiping //
                Peer.hasNewWord = false;
            }
        }
    }
}

class Server implements Runnable {
    int          hostID;
    String       hostAddress;
    int          hostPort;
    Logger       logger;
    ServerSocket server;

    Server(int hostID, String hostAddress, int hostPort, Logger logger) throws Exception {
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.hostPort    = hostPort;
        this.logger      = logger;
        server           = new ServerSocket(this.hostPort, 5, InetAddress.getByName(this.hostAddress));
    }

    @Override
    public void run() {
        while(true) {
            try {
                Socket PPSocket = server.accept();
                logger.info("server: new connection from " + PPSocket.getInetAddress().getHostAddress() + "\n");

                BufferedReader PPIn = new BufferedReader(new InputStreamReader(PPSocket.getInputStream()));
                PrintWriter   PPOut = new PrintWriter(PPSocket.getOutputStream(), true);

                PPOut.println(String.valueOf(this.hostID));
                PPOut.flush();

                String message = PPIn.readLine();
                logger.info("server: new message from " + PPSocket.getInetAddress().getHostAddress() + " [message=" + message + "]\n");

                int nextID = Integer.parseInt(message);
                Peer.table[nextID] = PPSocket.getPort();   // swap getPort for getInetAddress().getHostAddress()
                
                // escerve a socket que descobriu num array global de sockets
                // pode terminar e comecar a conexao ou reaproveitar e nao precisar do array de sockets
            } catch(Exception exception) { exception.printStackTrace(); }
        }   
    }
}

class wordGenerator implements Runnable {
    public void generate() {
        Random random = new Random();
        String word   = Peer.fileList.get(random.nextInt(Peer.fileList.size()));
        System.out.println("new word: " + word);    // remove when it is finished
        Peer.list.add(word);
        Peer.hasNewWord = true;
    }

    @Override
    public void run() {
        PoissonProcess process = new PoissonProcess(2, new Random());
        while(true) {
            double dt = process.timeForNextEvent() * 60 * 1000;
            long time = (long)dt;
            System.out.println("Poisson Value: " + dt); // remove when it is finished
            System.out.println("t - " + time);          // remove when it is finished
            try {
                Thread.sleep(time);
            } catch(Exception exception) { exception.printStackTrace(); }
            generate();
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