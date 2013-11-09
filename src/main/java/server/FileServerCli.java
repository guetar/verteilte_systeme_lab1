package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import proxy.ProxyCli;
import util.ComponentFactory;
import util.Config;
import cli.Shell;
import cli.Command;
import message.response.MessageResponse;
import model.FileServerInfo;

public class FileServerCli implements IFileServerCli {

	private static final Logger log = Logger.getLogger(ProxyCli.class);
    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
	private Thread shellThread;
	private Config configFs;
	private Shell shell;

	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	private Thread proxyThread;
	private TimerTask udpTimerTask;
	private Timer udpTimer;
	
	private String fsDir;
	private String fsHost;
	private int fsAlive;
	private int tcpPort;
	private int udpPort;

	public static void main(String[] args) throws Exception {
		
		Config config = new Config("fs1");
		Shell shell = new Shell("fs1", System.out, System.in);
		
		ComponentFactory factory = new ComponentFactory();
		factory.startFileServer(config, shell);
	}

	public FileServerCli(Config config, Shell shell) {
		
		this.configFs = config;
		this.shell = shell;
		log.info(configFs.getString("fileserver"));

		shell.register(this);
		shellThread = new Thread(shell);
		shellThread.start();
		
		try {
			fsAlive = configFs.getInt("fileserver.alive");
			fsDir = configFs.getString("fileserver.dir");
			fsHost = configFs.getString("proxy.host");
			tcpPort = configFs.getInt("tcp.port");
			udpPort = configFs.getInt("udp.port");
			
		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			udpSocket = new DatagramSocket();
			tcpSocket = new ServerSocket(tcpPort);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        udpTimerTask = new TimerTask() {
        	@Override
            public void run() {
            	try {
        			InetAddress IPAddress = InetAddress.getByName(fsHost);
        	    	String message = "!alive " + tcpPort;
        	    	byte[] buffer = new byte[12];
        	    	buffer = message.getBytes();
        	        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, IPAddress, udpPort);
        	        
        	        udpSocket.send(packet);
        	        log.info("sent: " + new String(packet.getData()));
        	        
            	} catch(IOException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
            	}
            }
        };
        
        udpTimer = new Timer();
        udpTimer.schedule(udpTimerTask, fsAlive);
        
		proxyThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						pool.submit(new FileServer(tcpSocket.accept(), fsDir));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						break;
					}
				}
			}
		});
		proxyThread.start();
		
		log.info("Fileserver started ...");
	}

	@Command
	@Override
	public MessageResponse exit() throws IOException {
		proxyThread.interrupt();
        udpTimer.purge();
		pool.shutdown();

        udpSocket.close();
        tcpSocket.close();
        
		return new MessageResponse("exit");
	}

}
