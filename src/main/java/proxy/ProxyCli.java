package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import util.ComponentFactory;
import util.Config;
import cli.Shell;
import message.Response;
import message.request.LoginRequest;
import message.response.MessageResponse;

public class ProxyCli extends Proxy implements IProxyCli {
	
	private static final Logger log = Logger.getLogger(ProxyCli.class);
	private Thread shellThread;
	
	private ServerSocket sSocket;
	private DatagramSocket dSocket;
	private Socket sClientSocket;
	private Socket dClientSocket;

	private Thread sThread;
	private Thread dThread;
	
	public static void main(String[] args) throws Exception {
		
		Config config = new Config("proxy");
		Shell shell = new Shell("proxy", System.out, System.in);
		
		ComponentFactory factory = new ComponentFactory();
		factory.startProxy(config, shell);
	}

	public ProxyCli(Config config, Shell shell) throws IOException {

		super(config, shell);
		shell.register(this);
		
		shellThread = new Thread(shell);
		shellThread.start();
		
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					sSocket = new ServerSocket(tcpPort);
//					sClientSocket = sSocket.accept();
//					PrintWriter out = new PrintWriter(sClientSocket.getOutputStream(), true);
//					BufferedReader in = new BufferedReader(new InputStreamReader(sClientSocket.getInputStream()));
//					log.info("Socket established ...");
//					
//					String inputLine;
//					while ((inputLine = in.readLine()) != null) {
//				        out.println(inputLine);
//				    }
//				
//				} catch(IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}).start();
		
		sThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						pool.submit(new TCPHandler(sSocket.accept()));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		dThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						byte[] buffer = new byte[10];
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						dSocket.receive(packet);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		log.info("Proxy started ...");
	}
	
	@Override
	public Response fileservers() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response users() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
