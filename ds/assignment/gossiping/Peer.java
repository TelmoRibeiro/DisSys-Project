package ds.assignment.gossiping;

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
    // add word list
    static int[] table;          // x <=> IP(x) where x is a peer // swap for "String" 
    int             hostID;
    String          hostAddress;
    int             hostPort;
    Logger          logger;
    

    public Peer(int hostID, String hostAddress, int hostPort) {
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

        new Thread(new Server(hostID, hostAddress, hostPort, currPeer.logger)).start();
        new Thread(new Client(hostID, hostAddress, hostPort, currPeer.logger)).start();
    }
}

class Client implements Runnable {
    int    hostID;
    String hostAddress;
    int    hostPort;
    Logger logger;

    public Client(int hostID, String hostAddress, int hostPort, Logger logger) {
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.hostPort    = hostPort;
        this.logger      = logger;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String  command = scanner.next();
        while(!command.equals("end")) {
            switch(command) {
                case "register":
                    try {
                        int nextPort    = Integer.parseInt(scanner.next());                                 // needs to be swaped for nextAddress
                        Socket PPSocket = new Socket(InetAddress.getByName("localhost"), nextPort);    // localhost needs to be swaped for nextAddress and this port becomes a agreed upon one
                        logger.info("client: new connection to " + PPSocket.getInetAddress().getHostAddress() + "\n");

                        BufferedReader PPIn = new BufferedReader(new InputStreamReader(PPSocket.getInputStream()));
                        PrintWriter   PPOut = new PrintWriter(PPSocket.getOutputStream(), true);

                        String message = PPIn.readLine();
                        logger.info("client: new message from " + PPSocket.getInetAddress().getHostAddress() + " [message=" + message + "]\n");

                        int nextID = Integer.parseInt(message);
                        Peer.table[nextID] = nextPort;                                                      // needs to be swaped for nextAddress;
                        
                        PPOut.println(this.hostID);
                        PPOut.flush();
                        
                        // TO DO
                    } catch(Exception exception) { exception.printStackTrace(); }
                    break;
                default: break;
            }
            command = scanner.next();
        }
        // Only for testing
        for (int i = 1; i < 7; i++) {
            System.out.println("Peer " + i + ": " + Peer.table[i]);
        }
        // Only for testing
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
        server           = new ServerSocket(this.hostPort, 1, InetAddress.getByName(this.hostAddress));
    }

    @Override
    public void run() {
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
            // TO DO
        } catch(Exception exception) { exception.printStackTrace(); }
    }
}

/*
ligar os 6 peers perviamente

Peer:
    possuo uma estrutura de tabela onde guardo a conexao entre um PeerX e IPX
    executo "register _" para armazenar informacoes nessa tabela
    mantenho sockets abertas com todos na tabela? sockets novas ou aproveito as da comunicacao?
    possuo uma estrutura de lista onde guardo a lista de palavras que o Peer conhece
    de 30 em 30 seg uma palavra e adicionada a lista por poisson
    cada peer tem acesso a um ficheiro na sua maquina com palavras (dicionario da Web) onde saca as palavras aleatoreamente
    quando um peer gera uma palavra faz gossiping para os outros



exemplo de register:
se no Peer1 executo: "register ip2" entao:
- a tabela do Peer1 fica com Peer2 IP2
- a tabela do Peer2 fica com Peer1 IP1
possivel implementacao:
-> estando no Peer1
-> abro um socket para o IP passado no register
-> Peer2 envia o seu ID
-> nova entrada no Peer1 com as informacoes do Peer2
-> envio o meu ID
-> nova entrada no Peer2 com as informacoes do Peer1

para o segundo : 
sabendo que e o peer que esta no IP2 abro socket para ele a partir do Peer1 
e este da os seus dados
*/

/*
uma thread recebe conexoes
outra envia
*/