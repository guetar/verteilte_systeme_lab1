package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

import model.FileServerInfo;

import org.apache.log4j.Logger;

public class UDPFileServer extends Thread {
	private static final Logger log = Logger.getLogger(UDPFileServer.class);
	
	private DatagramSocket udpSocket;
	private int udpPort;
	private int tcpPort;
	private String fsHost;
	private int fsAlive;
	private boolean stopped;


	public UDPFileServer(String fsHost, int udpPort, int tcpPort, int fsAlive) {
		try {
			this.udpSocket = new DatagramSocket();
			this.udpPort = udpPort;
			this.tcpPort = tcpPort;
			this.fsHost = fsHost;
			this.fsAlive = fsAlive;
			this.stopped = false;
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
    public void run() {
    	try {
			InetAddress IPAddress = InetAddress.getByName(fsHost);
	    	String message = "isAlive" + tcpPort;
	    	byte[] buffer = new byte[12];
	    	buffer = message.getBytes();
	        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IPAddress, udpPort);
	        
	        while(!stopped) {
	            try {
	                udpSocket.send(packet);
	                Thread.sleep(fsAlive);
	
	            } catch (InterruptedException e) {
	    			// TODO Auto-generated catch block
	            	return;
	            }
	        }
	        
    	} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
    	}
    }
	
	@Override
    public void interrupt() {
        super.interrupt();
        stopped = true;
        udpSocket.close();
    }
}
