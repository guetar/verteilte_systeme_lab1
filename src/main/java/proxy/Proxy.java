package proxy;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import message.Response;
import message.request.BuyRequest;
import message.request.DownloadTicketRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.LoginResponse;
import message.response.LoginResponse.Type;
import message.response.MessageResponse;
import model.User;

import org.apache.log4j.Logger;

import util.Config;
import cli.Shell;

public class Proxy implements IProxy {
	
	private static final Logger log = Logger.getLogger(Proxy.class);
    protected static ExecutorService pool;
    
	protected static ConcurrentHashMap<String, String> pws;
	protected static ConcurrentHashMap<String, User> users;
	protected static ConcurrentHashMap<String, Proxy> sessions;
	
	protected Config configProxy;
	protected Config configUser;
	protected Shell shell;

	protected int tcpPort;
	protected int udpPort;
	protected int fileserverTimeout;
	protected int fileserverCheckPeriod;
	
	public Proxy(Config config, Shell shell) {
		
		this.configProxy = config;
		this.shell = shell;
		
		try {
			tcpPort = configProxy.getInt("tcp.port");
			tcpPort = configProxy.getInt("tcp.port");
			fileserverTimeout = configProxy.getInt("udp.port");
			fileserverCheckPeriod = configProxy.getInt("fileserver.timeout");
			
		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        ResourceBundle bundle = ResourceBundle.getBundle("user");
        Set<String> unsernames = bundle.keySet();
        this.configUser = new Config("user");

		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
	}

	@Override
	public LoginResponse login(LoginRequest request) throws IOException {
		
		String username = request.getUsername();
	    String password = request.getPassword();
	    
	    if (sessions.containsKey(username)) {
	        log.info("User already logged in.");
	        return new LoginResponse(Type.WRONG_CREDENTIALS);
	    } else if (password.equals(pws.get(username))) {
	        sessions.put(username, this);
	        return new LoginResponse(Type.SUCCESS);
	    } else {
	        return new LoginResponse(Type.WRONG_CREDENTIALS);
	    }
	}

	@Override
	public Response credits() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response buy(BuyRequest credits) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response download(DownloadTicketRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse logout() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
