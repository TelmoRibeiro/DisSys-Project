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

// vamos receber os nomes das maquinas
// resolver os seus IPs
// usar os IPs

// Agreements:
// - Ports Inter-Peer 12301
// - Ports Intra-Peer 12302

public class Peer {
    // add word list
    static String[] table;          // x <=> IP(x) where x is a peer 
    int             hostID;
    int             hostAddress;    // swap "int" for "String" when hostAddress is changed to IP //
    Logger          logger;
    

    public Peer(int hostID, int hostAddress) {  // swap "int" for "String" when hostAddress is changed to IP //
        this.table       = new String[6];
        this.hostID      = hostID;
        this.hostAddress = hostAddress;
        this.logger      = Logger.getLogger("logfile");
        try {
            FileHandler handler = new FileHandler("./" + this.hostAddress + "_peer.log", true);
            this.logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch(Exception exception) { exception.printStackTrace(); }
    }

    public static void registerPeers() {
        Scanner scanner = new Scanner(System.in);
        String  command = scanner.next();
        while (!command.equals("end")) {
            switch(command) {
                case "register":
                    int nextAddress = Integer.parseInt(scanner.next()); // swap "int" for "String" and remove "parseInt" when it is changed to IP
                    System.out.println(command + " " + nextAddress);
                    break;

                default: break;
            }
            command = scanner.next();
        }
    }

    public static void main(String[] args) throws Exception {
        int hostID      = Integer.parseInt(args[0]);
        int hostAddress = Integer.parseInt(args[1]);    // remove "parseInt" when hostAddress is changed to IP
        Peer currPeer   = new Peer(hostID, hostAddress);
        currPeer.logger.info("peer " + hostID + " @ address = " + hostAddress + "\n");

        registerPeers();
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