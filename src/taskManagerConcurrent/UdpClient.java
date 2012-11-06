package taskManagerConcurrent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class UdpClient {

  private static class RequestManager implements Runnable {
    private String address, user, task;
    private int port;

    public RequestManager(String address, int port, String user, String task) {
      this.address = address;
      this.user = user;
      this.task = task;
      this.port = port;
    }

    public void run() {
      sendUdpTaskRequest(address, port, user, task);
    }
  }

  public static void main(String[] args) {
    try (Scanner sc = new Scanner(System.in)) {
      System.out.println("Welcome to the TaskManagerConcurrent UDPClient. Please identify.");
      System.out.print("User ID: ");
      String uid = sc.nextLine();
      System.out.println("Hello, " + uid + ". Enter the name of a task to execute it, \"q\" to exit.");
      String task = sc.nextLine();
      while (!task.equals("q")) {
        // System.out.println("Request sent. Reply will be printed upon arrival. Feel free to execute another task.");
        System.out.println("Sending request...");
        new Thread(new RequestManager("127.0.0.1", 6789, uid, task)).start();
        task = sc.nextLine();
      }
    }
  }

  /**
   * Creates a new thread, in which the task is requested and a reply is
   * awaited. Once the reply has been received, it is printed.
   * 
   * @param address
   * @param port
   * @param user
   * @param taskName
   */
  public static void sendUdpTaskRequest(String address, int port, String user,
      String taskName) {
    byte[] msg = (user + "\n" + taskName).getBytes();
    try (DatagramSocket sock = new DatagramSocket()) {
      // Send message
      InetAddress destination = InetAddress.getByName(address);
      DatagramPacket request = new DatagramPacket(msg, msg.length, destination, port);
      sock.send(request);
      // Await reply
      byte[] buffer = new byte[1000];
      DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
      sock.receive(reply);
      System.out.println("Reply received:");
      System.out.println(" " + new String(reply.getData()));
    } catch (SocketException e) {
      throw new RuntimeException("Error with UDP socket.", e);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Host-name (address) in invalid format.", e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to send the message.", e);
    }
  }
}
