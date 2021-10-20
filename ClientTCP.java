import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;


public class ClientTCP extends JFrame{
	private InetAddress ip;
	private String serverName = "localhost";
	private Socket client;
	private int port = 9900;
	
	private DataInputStream inFromServer;
	private DataOutputStream outToServer;

	public ClientTCP(){
		prepareUI();
		try {
			ip = InetAddress.getLocalHost();
			serverName = ip.getHostAddress();
			client = new Socket(serverName, port);
			
			outToServer = new DataOutputStream(client.getOutputStream());
			inFromServer = new DataInputStream(client.getInputStream());
			ListenMessage hear = new ListenMessage();
			hear.start();
		}
		catch(UnknownHostException e) {
			e.printStackTrace();
		}
		catch(IOException e1) {
			e1.printStackTrace();
		}
	}

	public void run() {
		this.setVisible(true);
		this.setActionForWidget();
	}

    JTree treeFile;
    JScrollPane fileContainerSP;

    JButton chooseFileBtn;
    JLabel chooseFileLb;

    private JButton downloadBtn;
    private JButton uploadBtn;
    private JButton resetBtn;
    private JProgressBar progressDownloadPb;

    DefaultMutableTreeNode root;

    public void prepareUI(){
    	root = new DefaultMutableTreeNode("List File Server");

        treeFile = new JTree(root);        
        fileContainerSP = new JScrollPane(treeFile);
        
        progressDownloadPb = new JProgressBar(0, 1000);
        progressDownloadPb.setStringPainted(true);
        chooseFileLb = new JLabel("No file hasn't chosen,yet");
        chooseFileBtn = new JButton("Choose...");
        uploadBtn = new JButton("Upload");
        downloadBtn = new JButton("Download");
        resetBtn = new JButton("Reset");

       
        fileContainerSP.setBounds(20, 20, 460, 300);       
        progressDownloadPb.setBounds(20, 340, 460, 30);        
        chooseFileBtn.setBounds(20, 390, 120, 30);
        chooseFileLb.setBounds(160, 390, 340, 30);
        uploadBtn.setBounds(20, 430, 100, 40);
        downloadBtn.setBounds(170, 430, 120, 40);
        resetBtn.setBounds(320, 430, 100, 40);


       	this.add(fileContainerSP);
       	this.add(progressDownloadPb);
       	this.add(chooseFileBtn);
       	this.add(chooseFileLb);
       	this.add(uploadBtn);
       	this.add(downloadBtn);
       	this.add(resetBtn);

        this.setSize(500, 520);
        this.setResizable(false);
        this.setTitle("Client");
		this.setLayout(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private File fileUpload;
    String fileDownloadName;

    public void setActionForWidget(){
    	treeFile.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                fileDownloadName = e.getNewLeadSelectionPath().getLastPathComponent().toString();
            }
        });
    	chooseFileBtn.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseReleased(MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					JFileChooser fileDialog = new JFileChooser();
					int returnValue = fileDialog.showOpenDialog(chooseFileBtn);
					if(returnValue == JFileChooser.APPROVE_OPTION) {
						fileUpload = fileDialog.getSelectedFile();
						chooseFileLb.setText(fileUpload.getName());
					}
				}
			}
		});

    	uploadBtn.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseReleased(MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					try{
						outToServer.writeUTF("UPLOAD");
						outToServer.writeUTF(fileUpload.getName());
						outToServer.writeUTF("" + fileUpload.length());
						System.out.println("" + fileUpload.length());
						outToServer.flush();

						TaskProgressUpLoad tp = new TaskProgressUpLoad();
						tp.start();
					}
					catch(IOException ioe){
						ioe.printStackTrace();
					}
				}
			}
		});

    	downloadBtn.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseReleased(MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					try{
						outToServer.writeUTF("DOWNLOAD");
						outToServer.writeUTF(fileDownloadName);
						outToServer.flush();

					}
					catch(IOException ioe){
						ioe.printStackTrace();
					}	
				}
			}
		});

		resetBtn.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseReleased(MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					progressDownloadPb.setValue(0);
					fileDownloadName = null;
					fileUpload = null;
					chooseFileLb.setText("No file hasn't chosen yet!");

				}
			}
		});
    }

    private class TaskProgressUpLoad extends Thread {
        public TaskProgressUpLoad() {}
 
        public void run() {
        	try {
				int i = 0;
				int progress = 0;
				progressDownloadPb.setMaximum((int)fileUpload.length());
				progressDownloadPb.setValue(progress);

				FileInputStream dis = new FileInputStream(fileUpload);
				byte[] buffer = new byte[1024];

				while( ( i = dis.read(buffer)) > 0){
					outToServer.write(buffer);
					outToServer.flush();	
					progress += i;
					progressDownloadPb.setValue(progress);      
		        }				
			}
			catch(IOException ioe){
				ioe.printStackTrace();
	        }
    	}
    }

    private class ListenMessage extends Thread {
        public ListenMessage() {}
 
        public void run() {
        	try{
        		String mess;
	        	while(true && inFromServer != null){
	        		mess = inFromServer.readUTF();
	        		String[] segs = mess.split("_");
	        		if(segs[0].equals("FILENAME")){
	        			root.add(new DefaultMutableTreeNode(segs[1]));
	        			// System.out.println(segs[1]);
	        		}
	        		else if(segs[0].equals("DATAFILE")){
	        			int fileSize = Integer.parseInt(segs[1]);
	        			File file = new File(fileDownloadName);
	        			FileOutputStream fos = new FileOutputStream(file);
	        			byte[] buffer = new byte[1024];

	        			int progress = 0;
	        			progressDownloadPb.setValue(0);
	        			progressDownloadPb.setMaximum(fileSize);

	        			while(fileSize >= 1024){
	        				inFromServer.read(buffer);
	        				fos.write(buffer);
	        				progress += 1024;
	        				progressDownloadPb.setValue(progress);
	        			}
	        			if(fileSize > 0){
	        				inFromServer.read(buffer);
	        				fos.write(buffer, 0, fileSize);
	        				progressDownloadPb.setValue(progress + fileSize);
	        			}
	        		}
	        	}
	        }
	        catch (IOException ioe) {
	        	ioe.printStackTrace();
	        }
        }
    }

	public static void main(String arg[]) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				ClientTCP clientTCP = new ClientTCP();
				clientTCP.run();
			}
		});
	}
}
