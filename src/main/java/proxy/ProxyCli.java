package proxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import message.Response;
import message.response.FileServerInfoResponse;
import message.response.MessageResponse;
import message.response.UserInfoResponse;
import model.FileServerInfo;
import model.User;

import org.apache.log4j.Logger;

import util.ComponentFactory;
import util.Config;
import cli.Command;
import cli.Shell;

public class ProxyCli implements IProxyCli {
	
	private static final Logger log = Logger.getLogger(ProxyCli.class);
	
    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private static ConcurrentHashMap<Integer, FileServerInfo> servers = new ConcurrentHashMap<Integer, FileServerInfo>();
    private static ConcurrentHashMap<String, User> users = new ConcurrentHashMap<String, User>();
    private static Map<String, String> pws = new HashMap<String, String>();
	
    private Config configProxy;
    private Config configUser;
    private Shell shell;

    private int tcpPort;
    private int udpPort;
    private int fsTimeout;
	private int fsCheckperiod;
	
	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	
	private Timer udpTimer;
	private TimerTask udpTimerTask;

	private Thread shellThread;
	private Thread clientThread;

	private Thread serverThread;
	
	public static void main(String[] args) throws Exception {
		
		Config config = new Config("proxy");
		Shell shell = new Shell("proxy", System.out, System.in);
		
		ComponentFactory factory = new ComponentFactory();
		factory.startProxy(config, shell);
	}

	public ProxyCli(Config config, Shell shell) throws IOException {
		
		this.configProxy = config;
		this.shell = shell;

		shell.register(this);
		shellThread = new Thread(shell);
		shellThread.start();
		
		try {
			tcpPort = configProxy.getInt("tcp.port");
			udpPort = configProxy.getInt("udp.port");
			fsTimeout = configProxy.getInt("fileserver.timeout");
			fsCheckperiod = configProxy.getInt("fileserver.checkPeriod");
			
		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        ResourceBundle bundle = ResourceBundle.getBundle("user");
        Set<String> unsernames = bundle.keySet();
        this.configUser = new Config("user");

        for (String username : unsernames) {
            username = username.substring(0, username.indexOf('.'));
            
            if(!users.containsKey(username)) {
            	String password = configUser.getString(username + ".password");
            	int credits = configUser.getInt(username + ".credits");
                users.put(username, new User(username, credits, false));
                pws.put(username, password);
            }
        }
        
        try {
			udpSocket = new DatagramSocket(udpPort);
			tcpSocket = new ServerSocket(tcpPort);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        class UDPTimerTask extends TimerTask {
        	
        	private int udpPort;
        	
        	public UDPTimerTask(int udpPort) {
        		this.udpPort = udpPort;
        	}
        	
        	@Override
            public void run() {
        		while(true) {
	    	        try {
	                    ProxyCli.setServerOnline(udpPort, false);
	                    
		                byte[] buffer = new byte[12];
		                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	
	                    udpSocket.receive(packet);
	                    log.info("received: " + new String(packet.getData()));
	                    int tcpPort = Integer.parseInt(new String(packet.getData()).substring(7));
	                    ProxyCli.setServerOnline(udpPort, true);
	                    
	                    Thread.sleep(fsTimeout);
	        	        
	    		    } catch (InterruptedException | IOException e) {
	    				// TODO Auto-generated catch block
	    		    	return;
	    		    }
        		}
            }
        };

        serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
	    	        try {
		                byte[] buffer = new byte[12];
		                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	
	                    udpSocket.receive(packet);
	                    log.info("received: " + new String(packet.getData()));
	                    
	                    int port = Integer.parseInt(new String(packet.getData()).substring(7));
	                    ProxyCli.addServer(packet.getPort(), new FileServerInfo(packet.getAddress(), port, 0, true));
	                    udpTimer.schedule(new UDPTimerTask(packet.getPort()), fsTimeout);
	        	        
	    		    } catch (IOException e) {
	    				// TODO Auto-generated catch block
	    		    	return;
	    		    }
        		}
			}
        });
        
		clientThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						pool.submit(new Proxy(tcpSocket.accept()));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						break;
					}
				}
			}
		});
		

        udpTimer = new Timer();
		serverThread.start();
		clientThread.start();
		
		log.info("Proxy started ...");
	}
	
	public static synchronized String getPassword(String username) {
		return pws.get(username);
	}
	
	public static synchronized void setUserOnline(String username, boolean online) {
		User user = users.remove(username);
        users.put(user.getName(), new User(user.getName(), user.getCredits(), online));
	}
	
	public static synchronized long credits(String username) {
		return users.get(username).getCredits();
	}
	
	public static synchronized long updateCredits(String username, long credits) {
		User user = users.remove(username);
		long newCredits = user.getCredits() + credits;
		if (newCredits >= 0) users.put(user.getName(), new User(user.getName(), newCredits, user.isOnline()));
		return newCredits;
	}
	
	public static synchronized void addServer(int udpPort, FileServerInfo info) {
		servers.put(udpPort, info);
	}
	
	public static synchronized void removeServer(int udpPort) {
		servers.remove(udpPort);
	}
	
	public static synchronized void setServerOnline(int udpPort, boolean online) {
		FileServerInfo fs = servers.remove(udpPort);
		servers.put(udpPort, new FileServerInfo(fs.getAddress(), fs.getPort(), fs.getUsage(), online));
	}
	
	public static synchronized List listServers() {
		return new ArrayList<FileServerInfo>(servers.values());
	}
	
	@Command
	@Override
	public Response fileservers() throws IOException {
        return new FileServerInfoResponse(new ArrayList<FileServerInfo>(servers.values()));
	}

	@Command
	@Override
	public Response users() throws IOException {
		log.info("Listing users ...");
        List<User> userNames = new ArrayList<User>(users.values());
        return new UserInfoResponse(userNames);
	}

	@Command
    @Override
    public MessageResponse exit() throws IOException {
    	log.info("Shutting down ...");
        Map<String, Proxy> sessions = Proxy.getSessions();
        
        for (Map.Entry<String, Proxy> session : sessions.entrySet()) {
            session.getValue().logout();
        }

        shellThread.interrupt();
        shell.close();
        System.in.close();
        
        clientThread.interrupt();
        udpTimer.purge();
        pool.shutdown();

        udpSocket.close();
        tcpSocket.close();
        
        return new MessageResponse("exit");
    }
}