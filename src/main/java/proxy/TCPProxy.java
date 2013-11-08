package proxy;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import util.ChecksumUtils;
import cli.Command;
import message.Request;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadFileRequest;
import message.request.DownloadTicketRequest;
import message.request.InfoRequest;
import message.request.ListRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.request.VersionRequest;
import message.response.BuyResponse;
import message.response.CreditsResponse;
import message.response.DownloadFileResponse;
import message.response.DownloadTicketResponse;
import message.response.InfoResponse;
import message.response.ListResponse;
import message.response.LoginResponse;
import message.response.LoginResponse.Type;
import message.response.MessageResponse;
import message.response.VersionResponse;
import model.DownloadTicket;
import model.FileServerInfo;

public class TCPProxy extends Thread implements IProxy {
	
	private static final Logger log = Logger.getLogger(TCPProxy.class);
	private static Map<String, TCPProxy> sessions = Collections.synchronizedMap(new HashMap<String, TCPProxy>());
	
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
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		
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
					
				} catch(SocketException | EOFException e) {
					interrupt();
					break;
				}
			}
			
		} catch(ClassNotFoundException | IOException e) {
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
            log.error("IOException occurred during shutdown ...");
        }
    }
    
    public static Map<String, TCPProxy> getSessions() {
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
		long newCredits = ProxyCli.updateCredits(username, credits.getCredits());
		return new BuyResponse(newCredits);
	}

	@Command
	@Override
	public Response list() throws IOException {
		List<FileServerInfo> servers = ProxyCli.listServers();
		Set<String> files = new HashSet<String>();
		
		for(FileServerInfo fs : servers) {
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
		List<FileServerInfo> servers = ProxyCli.listServers();
		
		for(FileServerInfo fs : servers) {
			try {
				Socket socket = new Socket(fs.getAddress(), fs.getPort());
				ObjectInputStream i = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream o = new ObjectOutputStream(socket.getOutputStream());
				
				o.writeObject(new InfoRequest(request.getFilename()));
				InfoResponse info = (InfoResponse) i.readObject();
				
				o.writeObject(new VersionRequest(request.getFilename()));
				VersionResponse version = (VersionResponse) i.readObject();
				
				String checksum = ChecksumUtils.generateChecksum(username, info.getFilename(), version.getVersion(), info.getSize());
				DownloadTicket ticket = new DownloadTicket(username, info.getFilename(), checksum, fs.getAddress(), fs.getPort());
				
				o.writeObject(new DownloadFileRequest(ticket));
	            DownloadFileResponse fileResponse = (DownloadFileResponse) i.readObject();
	            
	            socket.shutdownOutput();
	            socket.close();
	            
	            return fileResponse;
	            
	        } catch (ClassNotFoundException e) {
	            log.error("ClassNotFoundException in ClientCli.login()");
	        }
		}
		return null;
	}

	@Command
	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		List<FileServerInfo> servers = ProxyCli.listServers();
	
		for(FileServerInfo fs : servers) {
			try {
				Socket socket = new Socket(fs.getAddress(), fs.getPort());
				ObjectInputStream i = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream o = new ObjectOutputStream(socket.getOutputStream());
				
				o.writeObject(request);
				MessageResponse serverResponse = (MessageResponse) i.readObject();
				long newCredits = ProxyCli.updateCredits(username, request.getContent().length);
				MessageResponse response = new MessageResponse(serverResponse.getMessage() + "\nYou now have " + newCredits + " credits.");
	            
	            socket.shutdownOutput();
	            socket.close();
	            
	            return response;
	            
	        } catch (ClassNotFoundException e) {
	            log.error("ClassNotFoundException in ClientCli.login()");
	        }
		}
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
