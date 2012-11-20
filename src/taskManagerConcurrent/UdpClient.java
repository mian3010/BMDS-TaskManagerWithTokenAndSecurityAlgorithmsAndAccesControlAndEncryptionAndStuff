package taskManagerConcurrent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import utils.TokenService;

public class UdpClient {

	private static class RequestManager implements Runnable {
		private String address, task;
		byte[] token;
		private int port;

		public RequestManager(String address, int port, byte[] token, String task) {
			this.address = address;
			this.token = token;
			this.task = task;
			this.port = port;
		}

		public void run() {
			sendUdpTaskRequest(address, port, token, task);
		}
	}

	public static void main(String[] args) {
		// Make the symmetric key
		SecretKeySpec desKey = new SecretKeySpec("clitoken".getBytes(), "DES");
		// Create the ciphernben
		Cipher desCipher = null;
		try {
			desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			// Initialize the cipher for encryption
			desCipher.init(Cipher.ENCRYPT_MODE, desKey);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException e) {
			throw new RuntimeException(e.getMessage());
		}
		try (Scanner sc = new Scanner(System.in)) {
			System.out
					.println("Welcome to the TaskManagerConcurrent UDPClient. Please identify.");

			byte[] token = null;
			String username = null;
			// Until valid token is found - display error message and try again:
			while (token == null) {
				System.out.print("Username: ");
				username = sc.nextLine();
				System.out.print("Password: ");
				String password = sc.nextLine();
				// Our cleartextn
				byte[] cleartext = (username + "," + password).getBytes();

				try {
					// Encrypt the cleartext
					byte[] ciphertext = desCipher.doFinal(cleartext);
					token = TokenService.getToken(ciphertext);
					if (token == null) {
						System.out.println("Wrong. Try again");
					}
				} catch (IllegalBlockSizeException | BadPaddingException e) {
					throw new RuntimeException(e.getMessage());
				}
			}
			
			//Do: token = decryptedToken
			Cipher decryptCipher = null;
			try {
				decryptCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
				// Initialize the cipher for encryption
				decryptCipher.init(Cipher.DECRYPT_MODE, desKey);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
					| InvalidKeyException e) {
				throw new RuntimeException(e.getMessage());
			}
			
			try {
				token = decryptCipher.doFinal(token);
			}
			catch(BadPaddingException | IllegalBlockSizeException e) {
				throw new RuntimeException(e.getMessage());
			}
			
			System.out
					.println("Hello, "
							+ username
							+ ". Enter the name of a task to execute it, \"q\" to exit.");
			String task = sc.nextLine();
			while (!task.equals("q")) {
				// System.out.println("Request sent. Reply will be printed upon arrival. Feel free to execute another task.");
				System.out.println("Sending request...");
				new Thread(new RequestManager("127.0.0.1", 6789, token, task)).start();
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
	 * @param token
	 * @param taskName
	 */
	public static void sendUdpTaskRequest(String address, int port,
			byte[] token, String taskName) {
		byte[] msg = (new String(token) + "\n" + taskName).getBytes();
		try (DatagramSocket sock = new DatagramSocket()) {
			// Send message
			InetAddress destination = InetAddress.getByName(address);
			DatagramPacket request = new DatagramPacket(msg, msg.length,
					destination, port);
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
			throw new RuntimeException(
					"Host-name (address) in invalid format.", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to send the message.", e);
		}
	}
}
