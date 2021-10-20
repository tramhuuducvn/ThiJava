import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;


public class ServerTCP extends Thread {
	private ServerSocket serverSocket;
	private int port = 9900;
	private HashMap<String, File> fileList;

	public void open(){
		try{
			serverSocket = new ServerSocket(port);
			
			// lay duong dan folder hien tai
			String src = Paths.get("").toAbsolutePath().toString();
			File folder = new File(src);
			File[] ls = folder.listFiles();
			fileList = new HashMap<String, File>();
			for(File f : ls){
				String fname = f.getName();
				String extension = fname.substring(fname.lastIndexOf("."));
				// System.out.println(extension);
				if(extension.equals(".txt")){
					fileList.put(f.getName(),f);
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}

	}

	DataInputStream inFromClient = null;
	DataOutputStream outToClient = null;
	Socket server = null;

	int countClient = 0;
	@Override
	public void run(){			
		while(true){
			try{
				Socket newSlot = serverSocket.accept();
				Reception newRep = new Reception(newSlot);
				newRep.start();
				countClient++;
				System.out.println("Client " + countClient + " connected!");
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	private class Reception extends Thread{// vai trò như lễ tân giao tiếp với mỗi khách hàng
		Socket socket;
		DataInputStream inFromClient = null;
		DataOutputStream outToClient = null;
		public Reception(Socket socket){
			this.socket = socket;
			try{
				inFromClient = new DataInputStream(socket.getInputStream());
				outToClient = new DataOutputStream(socket.getOutputStream());
			}
			catch (IOException e){
				e.printStackTrace();
			}
		}

		public void sendListFileToClient() throws IOException{
			if(fileList != null && outToClient != null){
				Set<String> keys = fileList.keySet();
				for(String fname : keys){
					outToClient.writeUTF("FILENAME_" + fname);
				}
				outToClient.flush();
			}
		}

		@Override
		public void run(){
			try{
				sendListFileToClient();

				String mess;
				while(true){ // listening
					mess = inFromClient.readUTF();
					if(mess.equals("UPLOAD")){
						String filename = inFromClient.readUTF();
						int fileSize = Integer.parseInt(inFromClient.readUTF());
						
						File file = new File(filename);
						FileOutputStream fos = new FileOutputStream(file);
						byte[] buffer = new byte[1024];
						while(fileSize >= 1024){
							inFromClient.read(buffer);
							fos.write(buffer);
							fileSize -= 1024;
						}
						if(fileSize > 0){
							inFromClient.read(buffer);
							fos.write(buffer, 0, fileSize);
						}
					}
					else if(mess.equals("DOWNLOAD")){
						String filename = inFromClient.readUTF();
						File file = fileList.get(filename);
						outToClient.writeUTF("DATAFILE_" + file.length());
						outToClient.flush();

						FileInputStream fis = new FileInputStream(file);
						byte[] buffer = new byte[1024];
						
						while(fis.read(buffer) > 0)	{
							outToClient.write(buffer);
						}


					}
				}
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
		}
	}

	public static void main(String arg[]){
		ServerTCP serverTCP = new ServerTCP();
		serverTCP.open();
		serverTCP.start();
	}
}
