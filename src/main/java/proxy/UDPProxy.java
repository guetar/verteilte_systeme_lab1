package proxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import model.FileServerInfo;

public class UDPProxy extends Thread {
	private static final Logger log = Logger.getLogger(UDPProxy.class);

	private static ConcurrentHashMap<InetAddress, FileServerInfo> servers;
	private DatagramSocket udpSocket;
	private int fsCheckperiod;
	private int fsTimeout;

	public UDPProxy(int udpPort, int fsCheckperiod, int fsTimeout) {
        try {
            this.servers = new ConcurrentHashMap<InetAddress, FileServerInfo>();
			this.udpSocket = new DatagramSocket(udpPort);
			this.fsCheckperiod = fsCheckperiod;
			this.fsTimeout = fsTimeout;
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ConcurrentHashMap<InetAddress, FileServerInfo> getServers() {
		return servers;
	}
	
	@Override
    public void run() {
        try {
            byte[] buffer = new byte[12];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            while(true) {
                udpSocket.receive(packet);
                log.info("received: " + new String(packet.getData()));
                
                int port = Integer.parseInt(new String(packet.getData()).substring(7));
                servers.put(packet.getAddress(), new FileServerInfo(packet.getAddress(), port, 0, true));
                Thread.sleep(fsCheckperiod);
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