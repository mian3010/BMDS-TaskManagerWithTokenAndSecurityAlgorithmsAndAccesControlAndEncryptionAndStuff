package taskManagerConcurrent;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import utils.JaxbUtils;

public class UdpServer {
  private static File calendarfile = new File("calendar.xml");
  private TaskList taskList;

  public UdpServer() {
    taskList = JaxbUtils.xmlToTaskList(calendarfile);

    try (DatagramSocket sock = new DatagramSocket(6789)) {
      while (true) { // FUR EVURR
        byte[] buffer = new byte[1000]; // clear buffer each message, otherwise
                                        // old message remains
        // Get next request
        UdpMessage next = getNextMessage(sock, buffer);
        String[] msgParts = next.getMessage().split("\n");
        System.out.println("Received message from " + msgParts[0] + ": "
            + msgParts[1]);
        // Start thread to handle request and send reply
        new Thread(new MessageHandler(next)).start();
      }
    } catch (SocketException e) {

    } catch (IOException e) {

    }
  }

  private class UdpMessage {
    public final InetAddress address;
    public final int port;
    private String message;

    public UdpMessage(InetAddress address, int port, String message) {
      this.address = address;
      this.port = port;
      this.message = message;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

  private class MessageHandler implements Runnable {
    private UdpMessage container;

    public MessageHandler(UdpMessage container) {
      this.container = container;
    }

    public void run() {
      // msgParts[0] = userID
      // msgParts[1] = taskID
      String[] msgParts = container.getMessage().split("\n");
      ArrayList<Task> tasks = (ArrayList<Task>) taskList.getList();
      Task task = taskList.getTask(msgParts[1].trim());

      if (task == null) { // no task found
        container.setMessage("Task not found");
      }if (startTask(task)) {
        setResponses(task);
        endTask(task);
      }

      // Send message back
      try (DatagramSocket sock = new DatagramSocket()) {
        sendMessage(sock, container);
      } catch (SocketException e) {

      } catch (IOException e) {

      }
    }
    
    private boolean startTask(Task task) {
      if(taskList.conditionsCheck(task)){ // conditions met
        synchronized (task) {
          if(task.status.equals("not-executed")){
            task.status = "running";
            return true;
          }else{
            container.setMessage("Task not executable. Task status: " + task.status);
          }
        }
      }else{
        container.setMessage("Task not executable. Conditions not met.");
      }
      return false;
    }

    private void setResponses(Task task) {
      // Send responses
      String resp = task.responses;
      if (resp != null && !resp.isEmpty()) {
        String[] responses = resp.split(",");
        for (String s : responses) {
          s = s.trim();
          Task taskResponse = taskList.getTask(s);
          synchronized (taskResponse) {
            taskResponse.required = true;
          }
        }
      }
    }

    private void endTask(Task task) {
      synchronized (task) {
        task.status = "executed";
        task.required = false;
      }
    }
    
    /**
     * Save taskList to XML
     */
    private void save() {
      JaxbUtils.taskListToXml(taskList);
    }
  }

  public UdpMessage getNextMessage(DatagramSocket sock, byte[] buffer)
      throws IOException {
    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
    sock.receive(request);
    UdpMessage msg = new UdpMessage(request.getAddress(), request.getPort(),
        new String(request.getData()));
    return msg;
  }

  public void sendMessage(DatagramSocket sock, UdpMessage message)
      throws IOException {
    byte[] msg = message.getMessage().getBytes();
    DatagramPacket reply = new DatagramPacket(msg, msg.length, message.address,
        message.port);
    sock.send(reply);
  }

  public static void main(String[] args) {
    new UdpServer();
  }
}
