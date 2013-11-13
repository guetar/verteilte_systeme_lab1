package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import proxy.Proxy;
import proxy.ProxyCli;
import util.ComponentFactory;
import util.Config;
import cli.Shell;
import cli.Command;
import message.response.MessageResponse;
import model.FileServerInfo;

public class FileServerCli implements IFileServerCli {

	private static final Logger log = Logger.getLogger(ProxyCli.class);
    private static ExecutorService pool;
    
	private Thread shellThread;
	private Config configFs;
	private Shell shell;

	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	private Thread proxyThread;
	private Thread aliveThread;
	
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
		
        pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
		
        aliveThread = new Thread(new Runnable() {
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
        	        Thread.sleep(fsAlive);
        	        
            	} catch(InterruptedException | IOException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
            	}
            }
        });
        
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
		aliveThread.start();
		
		log.info("Fileserver " + configFs.getString("fileserver") + " started ...");
	}

	@Command
	@Override
	public MessageResponse exit() throws IOException {
    	log.info("Shutting down Fileserver ...");
    	
        shellThread.interrupt();
        shell.close();
        System.in.close();
        
		proxyThread.interrupt();
		aliveThread.interrupt();
		pool.shutdown();

        udpSocket.close();
        tcpSocket.close();
        
        try {
            // Wait a while for existing tasks to terminate.
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                log.info("forcing termination with pool.shutdownNow()");
                pool.shutdownNow();  // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled.
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.info("pool did not terminate");
                }
            }
        } catch (InterruptedException ex) {
            // (Re-)Cancel if current thread also interrupted.
            pool.shutdownNow();
            // Preserve interrupt status.
            Thread.currentThread().interrupt();
        }
        
		return new MessageResponse("exit");
	}

}
