package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cli.Command;
import message.Request;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadTicketRequest;
import message.request.InfoRequest;
import message.request.ListRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.BuyResponse;
import message.response.CreditsResponse;
import message.response.ListResponse;
import message.response.LoginResponse;
import message.response.LoginResponse.Type;
import message.response.MessageResponse;
import model.FileServerInfo;

public class TCPProxy extends Thread implements IProxy {
	
	private static final Logger log = Logger.getLogger(TCPProxy.class);
	private static ConcurrentHashMap<String, TCPProxy> sessions = new ConcurrentHashMap<String, TCPProxy>();;
	
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
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

		} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(true) {
			try {
				Request request = (Request) in.readObject();
				
				if(request instanceof LoginRequest) {
					out.writeObject(login((LoginRequest) request));
				} else if(request instanceof CreditsRequest) {
					out.writeObject(credits());
				} else if(request instanceof BuyRequest) {
					out.writeObject(buy((BuyRequest) request));
				} else if(request instanceof ListRequest) {
					out.writeObject(list());
				} else if(request instanceof DownloadTicketRequest) {
					out.writeObject(download((DownloadTicketRequest) request));
				} else if(request instanceof UploadRequest) {
					out.writeObject(upload((UploadRequest) request));
				}
				
			} catch(ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				break;
			}
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
		long newCredits = ProxyCli.buy(username, credits.getCredits());
		return new BuyResponse(newCredits);
	}

	@Command
	@Override
	public Response list() throws IOException {
		log.info("proxy");
		List<FileServerInfo> servers = ProxyCli.listFileservers();
		Set<String> files = new HashSet<String>();
		
		for(FileServerInfo fs : servers) {
			log.info("blah");
			try {
				Socket socket = new Socket(fs.getAddress(), fs.getPort());
				ObjectInputStream i = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream o = new ObjectOutputStream(socket.getOutputStream());
	            o.writeObject(new ListRequest());
	            
	            ListResponse response = (ListResponse) i.readObject();
	            for(String filename : response.getFileNames()) {
		            files.add(filename);
	            }
	            socket.shutdownOutput();
	            socket.close();
	            
	        } catch (ClassNotFoundException e) {
	            log.error("ClassNotFoundException in ClientCli.login()");
	        }
		}
		return new ListResponse(files);
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
