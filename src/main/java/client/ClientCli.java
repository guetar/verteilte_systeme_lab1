package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import message.Response;
import message.request.CreditsRequest;
import message.request.LoginRequest;
import message.request.LogoutRequest;
import message.response.CreditsResponse;
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
	private Thread clientThread;

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
		
//		try {
//			socket = new Socket(host, port);
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
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
            socket = new Socket(host, port);
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
	public Response download(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public MessageResponse upload(String filename) throws IOException {
		// TODO Auto-generated method stub
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
            
            socket.shutdownOutput();
            socket.shutdownInput();
            socket.close();
            
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
		// TODO Auto-generated method stub
		return null;
	}

}
