package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.log4j.Logger;

import util.ComponentFactory;
import util.Config;
import cli.Shell;
import message.Response;
import message.request.LoginRequest;
import message.response.LoginResponse;
import message.response.LoginResponse.Type;
import message.response.MessageResponse;

public class ClientCli implements IClientCli {

	private static final Logger log = Logger.getLogger(ClientCli.class);
	private Thread shellThread;
	private Config configClient;
	private Shell shell;
	private boolean loggedIn;

	private String downloadDir;
	private int port;
	private String host;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

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
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Socket cSocket = new Socket(host, port);
				    PrintWriter out = new PrintWriter(cSocket.getOutputStream(), true);
				    BufferedReader in = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
				    BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
					log.info("Socket established ...");
					
					String userInput;
					while ((userInput = stdIn.readLine()) != null) {
					    out.println(userInput);
					    System.out.println("echo: " + in.readLine());
					}
				    
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
		
		log.info("Client started ...");
	}
	
	@Override
	public LoginResponse login(String username, String password) throws IOException {
		if (loggedIn) {
            shell.writeLine("You are already logged in!");
            return null;
        }
		
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        out.writeObject(new LoginRequest(username, password));
        
        LoginResponse response = null;
        try {
            response = (LoginResponse) in.readObject();
            if (response.getType() == Type.SUCCESS) {
                loggedIn = true;
            }
		    
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return response;
	}

	@Override
	public Response credits() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response buy(long credits) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response download(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse upload(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse logout() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
