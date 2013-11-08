package server;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cli.Command;
import proxy.TCPProxy;
import util.ChecksumUtils;
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
import message.response.DownloadFileResponse;
import message.response.InfoResponse;
import message.response.ListResponse;
import message.response.MessageResponse;
import message.response.VersionResponse;
import model.DownloadTicket;

public class TCPFileServer extends Thread implements IFileServer {

	private static final Logger log = Logger.getLogger(TCPFileServer.class);
	
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private String fsDir;

	public TCPFileServer(Socket socket, String fsDir) {
		this.socket = socket;
		this.fsDir = fsDir;
	}
	
	@Override
	public void run() {
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			
			while(true) {
				try {
					Request request = (Request) in.readObject();
					
					if(request instanceof ListRequest) {
						out.writeObject(list());
					} else if(request instanceof DownloadFileRequest) {
						out.writeObject(download((DownloadFileRequest) request));
					} else if(request instanceof InfoRequest) {
						out.writeObject(info((InfoRequest) request));
					} else if(request instanceof VersionRequest) {
						out.writeObject(version((VersionRequest) request));
					} else if(request instanceof UploadRequest) {
						out.writeObject(upload((UploadRequest) request));
					}
					
				} catch(SocketException | EOFException e) {
					interrupt();
					break;
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
            log.error("IOException occurred during shutdown ...");
        }
    }
    
    public static int getVersion(String filename) {
    	return 1;
    }

	@Command
	@Override
	public Response list() throws IOException {
		File folder = new File(fsDir);
		Set<String> files = new HashSet<String>();
		
		for (File f : folder.listFiles()) {
			if (f.isFile()) {
				files.add(f.getName());
			}
		}
		return new ListResponse(files);
	}

	@Command
	@Override
	public Response download(DownloadFileRequest request) throws IOException {
		DownloadTicket ticket = request.getTicket();
		
		File file = new File(fsDir + "/" + ticket.getFilename());
		InfoResponse info = (InfoResponse) info(new InfoRequest(ticket.getFilename()));
		VersionResponse version = (VersionResponse) version(new VersionRequest(ticket.getFilename()));
		String checksum = ChecksumUtils.generateChecksum(ticket.getUsername(), ticket.getFilename(), version.getVersion(), info.getSize());
		
		if(ChecksumUtils.verifyChecksum(ticket.getUsername(), file, version.getVersion(), checksum)) {
			byte[] fileBytes = new byte[(int) file.length()];
			
			FileInputStream fis = new FileInputStream(file);
		    fis.read(fileBytes);
		    fis.close();
		    
			return new DownloadFileResponse(ticket, fileBytes);
		} else {
			log.info("Checksums do not match: " + ticket.getChecksum() + " | " + checksum);
		}
		return null;
	}

	@Command
	@Override
	public Response info(InfoRequest request) throws IOException {
		File file = new File(fsDir + "/" + request.getFilename());
		return new InfoResponse(request.getFilename(), file.length());
	}

	@Command
	@Override
	public Response version(VersionRequest request) throws IOException {
		return new VersionResponse(request.getFilename(), 1);
	}

	@Command
	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		
		FileOutputStream fos = new FileOutputStream(fsDir + "/" + request.getFilename());
		fos.write(request.getContent());
		fos.close();
		
		return new MessageResponse("File successfully uploaded.");
	}

}
