package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import proxy.ProxyCli;
import util.ComponentFactory;
import util.Config;
import cli.Shell;
import cli.Command;
import message.response.MessageResponse;

public class FileServerCli implements IFileServerCli {

	private static final Logger log = Logger.getLogger(ProxyCli.class);
    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
	private Thread shellThread;
	private Config configFs;
	private Shell shell;

	private UDPFileServer udpFileServer;
	private ServerSocket tcpSocket;
	private Thread proxyThread;
	
	private String fsDir;
	private String fsHost;
	private int fsAlive;
	private int tcpPort;
	private int udpPort;

	public static void main(String[] args) throws Exception {
		
		for(String arg : args) {
			log.info(arg);
		}
		Config config = new Config("fs1");
		Shell shell = new Shell("fs1", System.out, System.in);
		
		ComponentFactory factory = new ComponentFactory();
		factory.startFileServer(config, shell);
	}

	public FileServerCli(Config config, Shell shell) {
		
		this.configFs = config;
		this.shell = shell;

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
		
		udpFileServer = new UDPFileServer(fsHost, udpPort, tcpPort, fsAlive);
		udpFileServer.start();
		
        try {
			tcpSocket = new ServerSocket(tcpPort);
	        
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
		proxyThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						pool.submit(new TCPFileServer(tcpSocket.accept(), fsDir));
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
		tcpSocket.close();
		pool.shutdown();
		return new MessageResponse("exit");
	}

}
