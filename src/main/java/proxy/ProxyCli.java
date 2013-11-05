package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import util.ComponentFactory;
import util.Config;
import cli.Command;
import cli.Shell;
import message.Response;
import message.response.FileServerInfoResponse;
import message.response.MessageResponse;
import message.response.UserInfoResponse;
import model.FileServerInfo;
import model.User;

public class ProxyCli implements IProxyCli {
	
	private static final Logger log = Logger.getLogger(ProxyCli.class);
    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    private static ConcurrentHashMap<String, String> pws;
    private static ConcurrentHashMap<String, User> users;
	
    private Config configProxy;
    private Config configUser;
    private Shell shell;

    private int tcpPort;
    private int udpPort;
    private int fsTimeout;
	private int fsCheckperiod;
	
	private ServerSocket tcpSocket;

	private Thread shellThread;
	private Thread clientThread;
	private UDPProxy udpProxy;
	
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

		users = new ConcurrentHashMap<String, User>();
		pws = new ConcurrentHashMap<String, String>();

        for (String username : unsernames) {
            username = username.substring(0, username.indexOf('.'));
            
            if(!users.containsKey(username)) {
            	String password = configUser.getString(username + ".password");
            	int credits = configUser.getInt(username + ".credits");
                users.put(username, new User(username, credits, false));
                pws.put(username, password);
            }
        }
		
        udpProxy = new UDPProxy(udpPort, fsCheckperiod, fsTimeout);
        udpProxy.start();
        
        try {
	        tcpSocket = new ServerSocket(tcpPort);
	        
        } catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }
        
		clientThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						pool.submit(new TCPProxy(tcpSocket.accept()));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		log.info("Proxy started ...");
	}
	
	public static synchronized String getPassword(String username) {
		return pws.get(username);
	}
	
	public static synchronized void login(String username) {
		User user = users.get(username);
        users.remove(user.getName());
        users.put(user.getName(), new User(user.getName(), user.getCredits(), true));
	}
	
	public static synchronized void logout(String username) {
		User user = users.get(username);
        users.remove(user.getName());
        users.put(user.getName(), new User(user.getName(), user.getCredits(), false));
	}
	
	public static synchronized long credits(String username) {
		return users.get(username).getCredits();
	}
	
	@Command
	@Override
	public Response fileservers() throws IOException {
		log.info("Listing fileservers ...");
		List<FileServerInfo> servers = new ArrayList<FileServerInfo>(udpProxy.getServers().values());
        return new FileServerInfoResponse(servers);
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
        ConcurrentMap<String, TCPProxy> sessions = TCPProxy.getSessions();
        
        for (Map.Entry<String, TCPProxy> session : sessions.entrySet()) {
            session.getValue().logout();
        }

        shellThread.interrupt();
        shell.close();
        System.in.close();
        
        tcpSocket.close();
        udpProxy.interrupt();
        pool.shutdown();

        return new MessageResponse("exit");
    }
}