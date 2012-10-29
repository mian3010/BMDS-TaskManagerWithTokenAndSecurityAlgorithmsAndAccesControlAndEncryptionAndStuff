package taskManagerConcurrent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpServer {
	
	public UdpServer() {
		try(DatagramSocket sock = new DatagramSocket(6789)) {
			while(true) { //FUR EVURR
				byte[] buffer = new byte[1000]; //clear buffer each message, otherwise old message remains
				//Get next request
				UdpMessage next = getNextMessage(sock, buffer);
				String[] msgParts = next.getMessage().split("\n");
				System.out.println("Received message from "+msgParts[0]+": "+msgParts[1]);
				//Start thread to handle request and send reply
				new Thread(new MessageHandler(next)).start();
			}
		}
		catch(SocketException e) {
			
		}
		catch(IOException e) {
			
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
			String[] msgParts = container.getMessage().split("\n");
			container.setMessage("hello, timmy. I love you, man"); /*x.runTask(msgParts[1],msgParts[0])*/
			//Send message back
			try(DatagramSocket sock = new DatagramSocket()) {
				sendMessage(sock,container);
			}
			catch(SocketException e) {
				
			}
			catch(IOException e) {
				
			}
		}
	}
	
	public UdpMessage getNextMessage(DatagramSocket sock, byte[] buffer) throws IOException {
		DatagramPacket request = new DatagramPacket(buffer, buffer.length);
		sock.receive(request);
		UdpMessage msg = new UdpMessage(request.getAddress(), request.getPort(), new String(request.getData()));
		return msg;
	}
	
	public void sendMessage(DatagramSocket sock, UdpMessage message) throws IOException {
		byte[] msg = message.getMessage().getBytes();
		DatagramPacket reply = new DatagramPacket(msg,msg.length,message.address,message.port);
		sock.send(reply);
	}
	
	public static void main(String[] args) {
		new UdpServer();
	}
}
