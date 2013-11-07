package proxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import model.FileServerInfo;

import org.apache.log4j.Logger;

public class UDPProxy extends Thread {
	private static final Logger log = Logger.getLogger(UDPProxy.class);
	
	private DatagramSocket udpSocket;
	private int fsCheckperiod;
	private int fsTimeout;

	public UDPProxy(int udpPort, int fsCheckperiod, int fsTimeout) {
        try {
			this.udpSocket = new DatagramSocket(udpPort);
			this.fsCheckperiod = fsCheckperiod;
			this.fsTimeout = fsTimeout;
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
    public void run() {
        byte[] buffer = new byte[12];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

	    while(true) {
	        try {
                udpSocket.receive(packet);
                
                int port = Integer.parseInt(new String(packet.getData()).substring(7));
                ProxyCli.addServer(new FileServerInfo(packet.getAddress(), port, 0, true));
                Thread.sleep(fsCheckperiod);
    	        
		    } catch (InterruptedException | IOException e) {
				// TODO Auto-generated catch block
		    	break;
		    }
        }
    }
	
	@Override
    public void interrupt() {
        super.interrupt();
        udpSocket.close();
    }
}