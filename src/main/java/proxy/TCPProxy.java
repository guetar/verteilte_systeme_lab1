package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cli.Command;
import message.Request;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadTicketRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.CreditsResponse;
import message.response.LoginResponse;
import message.response.LoginResponse.Type;
import message.response.MessageResponse;

public class TCPProxy extends Thread implements IProxy {
	
	private static final Logger log = Logger.getLogger(TCPProxy.class);
	private static ConcurrentHashMap<String, TCPProxy> sessions = new ConcurrentHashMap<String, TCPProxy>();;
	
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private boolean running;
	private String username;
	private String password;

	public TCPProxy(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
			
			while(true) {
				Request request = (Request) in.readObject();
				
				if(request instanceof LoginRequest) {
					out.writeObject(login((LoginRequest) request));
				} else if(request instanceof CreditsRequest) {
					out.writeObject(credits());
				} else if(request instanceof BuyRequest) {
					out.writeObject(buy((BuyRequest) request));
				} else if(request instanceof DownloadTicketRequest) {
					out.writeObject(download((DownloadTicketRequest) request));
				} else if(request instanceof UploadRequest) {
					out.writeObject(upload((UploadRequest) request));
				}
			}
			
		} catch(ClassNotFoundException  | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    @Override
    public void interrupt() {
        super.interrupt();
        try {
            socket.shutdownOutput();
            socket.close();
            
        } catch (IOException e) {
            log.error("IOException occurred during shutdown");
        }
    }
    
    public static ConcurrentHashMap<String, TCPProxy> getSessions() {
    	return sessions;
    }

	@Command
	@Override
	public LoginResponse login(LoginRequest request) throws IOException {
		
		this.username = request.getUsername();
	    this.password = request.getPassword();
	    
	    if (sessions.containsKey(username)) {
	    	
	        log.info("User already logged in.");
	        interrupt();
	        return new LoginResponse(Type.WRONG_CREDENTIALS);
	        
	    } else if (password.equals(ProxyCli.getPassword(username))) {
	    	
	        sessions.put(username, this);
	        ProxyCli.login(username);
	        return new LoginResponse(Type.SUCCESS);
	        
	    } else {
	    	
	        interrupt();
	        return new LoginResponse(Type.WRONG_CREDENTIALS);
	    }
	}

	@Command
	@Override
	public Response credits() throws IOException {
		long credits = ProxyCli.credits(username);
        return new CreditsResponse(credits);
	}

	@Command
	@Override
	public Response buy(BuyRequest credits) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public Response list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public Response download(DownloadTicketRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public MessageResponse logout() throws IOException {
		if (!sessions.containsValue(this)) return null;

        interrupt();
        ProxyCli.logout(username);
        sessions.remove(username);
        return new MessageResponse("User successfully logged out.");
	}
}
