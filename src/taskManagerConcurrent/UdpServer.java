package taskManagerConcurrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import utils.JaxbUtils;

public class UdpServer {
  private static File calendarfile = new File("calendar.xml");
  private TaskList taskList = null;
  private final SecretKeySpec desKey = new SecretKeySpec("toserver".getBytes(), "DES");

  /**
   * Constructor. Runs the server, listening for UPD requests.
   * Creates a new thread for executing tasks when requests are recieved
   */
  public UdpServer() {
    try {
      taskList = JaxbUtils.xmlToTaskList(calendarfile);
    } catch (FileNotFoundException e1) {
      try {
        System.out.println("File not found. Attempting to create...");
        calendarfile.createNewFile();
      } catch (IOException e) {
        System.out.println("Could not create file");
        System.exit(-1);
      }
    }

    try (DatagramSocket sock = new DatagramSocket(6789)) {
      System.out.println("Server running...");
      while (true) { // FUR EVURR
        byte[] buffer = new byte[1000]; // clear buffer each message, otherwise
                                        // old message remains
        // Get next request
        UdpMessage next = getNextMessage(sock, buffer);
        // Start thread to handle request and send reply
        new Thread(new MessageHandler(next)).start();
      }
    } catch (SocketException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Containing the UDP message
   */
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

  /**
   * Handling the execution of a task
   */
  private class MessageHandler implements Runnable {
    private UdpMessage container;

    public MessageHandler(UdpMessage container) {
      this.container = container;
    }
    /**
     * The methods that executes the task by checking that it exists,
     * and that conditions have been met.
     * It also sets the required flag on responses
     */
    public void run() {
      // msgParts[0] = userID
      // msgParts[1] = taskID
      String[] msgParts = container.getMessage().split("\n");
      byte[] token = msgParts[0].getBytes();
      //validate token
      //Do: token = decryptedToken
		Cipher decryptCipher = null;
		try {
			decryptCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			// Initialize the cipher for encryption
			decryptCipher.init(Cipher.DECRYPT_MODE, desKey);
			token = decryptCipher.doFinal(token);
		}
		catch(BadPaddingException | IllegalBlockSizeException |
				NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			token = null;
		}
		
		String role = "invalidRole";
	  if(token != null) {
		  String tokenString = new String(token);
		  String[] tokenParts = tokenString.split(",");
		  role = tokenParts[0];
		  long timestamp = Long.valueOf(tokenParts[1]);
		  if(timestamp > (System.currentTimeMillis() + 120000)) { //2 minute old tokens are allowed
			  token = null;
		  }
		  
	  }
      
	  Task task = taskList.getTask(msgParts[1].trim());
	  if(token != null && task.role.toLowerCase().trim().equals(role.toLowerCase().trim())) {
	      
	      if (task == null) { // no task found
	        container.setMessage("Task not found");
	      } else if (startTask(task)) {
	        setResponses(task);
	        endTask(task);
	        container.setMessage("Task " + task.id + " has been executed");
	      }
	  }
	  else {
		  container.setMessage("Invalid token! U HAZ SUX HAXORR (or maybe it is more than 2 mins old or you are not allowed access to the task)");
	  }

      // Send message back
      try (DatagramSocket sock = new DatagramSocket()) {
        sendMessage(sock, container);
      } catch (SocketException e) {

      } catch (IOException e) {

      }
    }
    /**
     * First step in executing a task. Check conditions and set status to running
     * @param task The task in question
     * @return Whether or not conditions have been met
     */
    private boolean startTask(Task task) {
      if (taskList.conditionsCheck(task)) { // conditions met
        synchronized (task) {
          if (task.status.equals("not-executed")) {
            task.status = "running";
            return true;
          } else {
            container.setMessage("Task not executable. Task status: "
                + task.status);
          }
        }
      } else {
        container.setMessage("Task not executable. Conditions not met.");
      }
      return false;
    }

    /**
     * Set the required flag on responses
     * @param task The task in question
     */
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
            taskResponse.status = "not-executed";
          }
        }
      }
    }

    /**
     * The final step in executing a task. Set required and status
     * @param task The tash in question
     */
    private void endTask(Task task) {
      synchronized (task) {
        task.status = "executed";
        task.required = false;
      }
      save();
    }

    /**
     * Save taskList to XML
     */
    private void save() {
      synchronized (taskList) {
        synchronized (calendarfile) {
          JaxbUtils.taskListToXml(taskList, calendarfile);
        }
      }
    }
  }

  /**
   * Return the next message from socket
   * @param sock The socket
   * @param buffer The buffer
   * @return The next message
   * @throws IOException
   */
  public UdpMessage getNextMessage(DatagramSocket sock, byte[] buffer)
      throws IOException {
    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
    sock.receive(request);
    UdpMessage msg = new UdpMessage(request.getAddress(), request.getPort(),
        new String(request.getData()));
    return msg;
  }

  /**
   * Sends a message over socket
   * @param sock The socket
   * @param message The message
   * @throws IOException
   */
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
