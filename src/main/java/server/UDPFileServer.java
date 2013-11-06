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
	
	private ConcurrentHashMap<String, UDPFileServer> sessions;
	private DatagramSocket udpSocket;
	private String fsHost;
	private int udpPort;
	private int fsAlive;

	public UDPFileServer(String fsHost, int udpPort, int fsAlive) {
		try {
			this.udpSocket = new DatagramSocket();
			this.fsHost = fsHost;
			this.udpPort = udpPort;
			this.fsAlive = fsAlive;
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
    public void run() {
        try {
        	InetAddress IPAddress = InetAddress.getByName(fsHost);
        	String message = "isAlive" + udpPort;
        	byte[] buffer = new byte[12];
        	buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IPAddress, udpPort);
            
            while(true) {
                udpSocket.send(packet);
                log.info("sent: " + new String(packet.getData()));
                Thread.sleep(fsAlive);
            }

        } catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }
    }
	
	@Override
    public void interrupt() {
        super.interrupt();
        udpSocket.close();
    }
}
