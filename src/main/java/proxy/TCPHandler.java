package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import message.Request;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadTicketRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.LoginResponse;
import message.response.MessageResponse;

public class TCPHandler extends Thread implements IProxy {

	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private boolean running;
	private Proxy proxy;

	public TCPHandler(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
			
			while(running) {
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
			
		} catch(ClassNotFoundException e) {
			
		} catch(IOException e) {
			
		}
	}

	@Override
	public LoginResponse login(LoginRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
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
