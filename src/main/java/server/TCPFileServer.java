package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cli.Command;
import proxy.TCPProxy;
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
import message.response.MessageResponse;

public class TCPFileServer extends Thread implements IFileServer {

	private static final Logger log = Logger.getLogger(TCPFileServer.class);
	private static ConcurrentHashMap<String, TCPFileServer> sessions = new ConcurrentHashMap<String, TCPFileServer>();;
	
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	public TCPFileServer(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
			
			while(true) {
				Request request = (Request) in.readObject();
				
				if(request instanceof ListRequest) {
					out.writeObject(list());
				} else if(request instanceof InfoRequest) {
					out.writeObject(info((InfoRequest) request));
				} else if(request instanceof DownloadFileRequest) {
					out.writeObject(download((DownloadFileRequest) request));
				} else if(request instanceof VersionRequest) {
					out.writeObject(version((VersionRequest) request));
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
    
    public static ConcurrentHashMap<String, TCPFileServer> getSessions() {
    	return sessions;
    }

	@Command
	@Override
	public Response list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public Response download(DownloadFileRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public Response info(InfoRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public Response version(VersionRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
