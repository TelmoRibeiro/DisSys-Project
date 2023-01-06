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

public class WordGenerator implements Runnable {
  ArrayList<String> fileList;

  public WordGenerator(ArrayList<String> fileList) {
    this.fileList = fileList;
  }

  public void generate(ArrayList<String> fileList) {
    Random random = new Random();
    String word   = fileList.get(random.nextInt(fileList.size()));
    System.out.println("Word Generated: " + word);
    System.out.println();

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
      System.out.println("t - " + time);
      System.out.println();
      try { Thread.sleep(time); } catch(Exception exception) { exception.printStackTrace(); }
      generate(this.fileList);
    }
  }
}
