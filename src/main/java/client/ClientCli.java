package client;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadTicketRequest;
import message.request.ListRequest;
import message.request.LoginRequest;
import message.request.LogoutRequest;
import message.request.UploadRequest;
import message.response.BuyResponse;
import message.response.CreditsResponse;
import message.response.DownloadFileResponse;
import message.response.ListResponse;
import message.response.LoginResponse;
import message.response.LoginResponse.Type;
import message.response.MessageResponse;

import org.apache.log4j.Logger;

import util.ComponentFactory;
import util.Config;
import cli.Command;
import cli.Shell;

public class ClientCli implements IClientCli {

	private static final Logger log = Logger.getLogger(ClientCli.class);
	private Thread shellThread;
	private Config configClient;
	private Shell shell;
	private boolean loggedIn;

	private String downloadDir;
	private int port;
	private String host;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Socket socket;

	public static void main(String[] args) throws Exception {
		
		Config config = new Config("client");
		Shell shell = new Shell("client", System.out, System.in);
		
		ComponentFactory factory = new ComponentFactory();
		factory.startClient(config, shell);

	}
	
	public ClientCli(Config config, Shell shell) {
		
		this.configClient = config;
		this.shell = shell;
		
		try {
			downloadDir = configClient.getString("download.dir");
			host = configClient.getString("proxy.host");
			port = configClient.getInt("proxy.tcp.port");

		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		shell.register(this);
		shellThread = new Thread(shell);
		shellThread.start();
		
		try {
			socket = new Socket(host, port);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Client started ...");
	}
	
	@Command
	@Override
	public LoginResponse login(String username, String password) throws IOException {
		if (loggedIn) {
            log.info("User is already logged in");
            return null;
        }
        
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(new LoginRequest(username, password));
            
            LoginResponse response = (LoginResponse) in.readObject();
            if (response.getType() == Type.SUCCESS) loggedIn = true;
            
            return response;
            
        } catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException in ClientCli.login()");
        }
        return null;
	}

	@Command
	@Override
	public Response credits() throws IOException {
		if (!loggedIn) {
            log.info("User is not logged in.");
            return null;
        }

	    try {
	        out.writeObject(new CreditsRequest());
	        return (CreditsResponse) in.readObject();

        } catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException in ClientCli.credits()");
        }
        return null;
	}

	@Command
	@Override
	public Response buy(long credits) throws IOException {
		if (!loggedIn) {
            log.info("User is not logged in.");
            return null;
        }
		
		try {
			out.writeObject(new BuyRequest(credits));
			return (BuyResponse) in.readObject();
			
		} catch(ClassNotFoundException e) {
            log.error("ClassNotFoundException in ClientCli.buy()");
		}
		return null;
	}

	@Command
	@Override
	public Response list() throws IOException {
		if (!loggedIn) {
            log.info("User is not logged in.");
            return null;
        }
		
		try {
			out.writeObject(new ListRequest());
			return (ListResponse) in.readObject();
			
		} catch(ClassNotFoundException e) {
            log.error("ClassNotFoundException in ClientCli.list()");
		}
		return null;
	}

	@Command
	@Override
	public Response download(String filename) throws IOException {
		if (!loggedIn) {
            log.info("User is not logged in.");
            return null;
        }
		
		try {
			out.writeObject(new DownloadTicketRequest(filename));
			DownloadFileResponse response = (DownloadFileResponse) in.readObject();
			
			FileOutputStream fos = new FileOutputStream(downloadDir + "/" + response.getTicket().getFilename());
			fos.write(response.getContent());
			fos.close();
			
			return response;
			
		} catch(ClassNotFoundException e) {
            log.error("ClassNotFoundException in ClientCli.list()");
		}
		return null;
	}

	@Command
	@Override
	public MessageResponse upload(String filename) throws IOException {
		if (!loggedIn) {
            log.info("User is not logged in.");
            return null;
        }
		
		try {
			File file = new File(downloadDir + "/" + filename);
			byte[] fileBytes = new byte[(int) file.length()];
			
			FileInputStream fis = new FileInputStream(file);
		    fis.read(fileBytes);
		    fis.close();
		    
			out.writeObject(new UploadRequest(filename, 1, fileBytes));
			return (MessageResponse) in.readObject();
			
		} catch(ClassNotFoundException e) {
            log.error("ClassNotFoundException in ClientCli.list()");
		}
		return null;
	}

	@Command
	@Override
	public MessageResponse logout() throws IOException {
		if (!loggedIn) {
            log.info("User is not logged in.");
            return null;
        }
		
        try {
            out.writeObject(new LogoutRequest());
            MessageResponse response = (MessageResponse) in.readObject();
            
            loggedIn = false;
            return response;
        	
        } catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException in ClientCli.logout()");
        }
        return null;
	}

	@Command
	@Override
	public MessageResponse exit() throws IOException {
		
        socket.shutdownOutput();
        socket.shutdownInput();
        socket.close();
        
        return new MessageResponse("exit");
	}

}
