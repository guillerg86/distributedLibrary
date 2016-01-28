package Distributed;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Network.Frame;
import Network.Node;
import UtilsLib.GFunctions;

public class RAMutex {
	
	public static final String FRAME_RELEASE = "RELEASE";
	public static final String FRAME_REQUEST = "REQUEST";
	public static String LOGFILE = "RAMutexLog.log";
	
	private LamportClock clock;	
	private Lock lock;
	
	private int myTS;
	private int numOkay;
	private LinkedList<Node> pendingQ;
	private ArrayList<Node> nodes;
	private Node localnode;
	private int numberNodes;
	private RAListener listener;
	private boolean insideCS = false;
	private boolean debug_mode;
	private static final int Infinity = Integer.MIN_VALUE;

	
	
	public RAMutex(Node localnode, ArrayList<Node> nodes,boolean debug_mode) {
		this.debug_mode = debug_mode;
		this.clock = new LamportClock();
		this.pendingQ = new LinkedList<Node>();
		this.myTS = Infinity;
		this.nodes = nodes;
		this.numberNodes = nodes.size() + 1;
		this.localnode = localnode;
		// Iniciamos el Listener para la gestion de Mutex 
		this.listener = new RAListener( localnode.getMutexPort());
		this.listener.start();
		this.lock = new ReentrantLock();
	}
	
	public void calibrate() {
		for (int i=0;i<2;i++) {
			this.requestCS();
			for (int j=0;j<3;j++) {
				
				if (debug_mode) System.out.println("RAMT: Calibration -> Printing "+localnode.getName()+" ["+j+"]");
				try {
					Thread.sleep(1000);
				} catch (Exception e){}
			}
			this.releaseCS();
		}
	}
	
	public synchronized void requestCS() {
		clock.tick();
		myTS = clock.getValue();
		numOkay = 0;
		if (debug_mode) GFunctions.writeToScreen("RAMT: Requesting CSection");
		broadcastMessage();
		if (debug_mode) GFunctions.writeToScreen("RAMT: Requested CSection");
		while ( numOkay < (numberNodes-1) ) {
			this.myWait(1000);
			if (debug_mode)System.out.println("RAMT: Waitting for all releases ["+numOkay+"/"+(numberNodes-1)+"]");
		}
		this.lock.lock();
		this.insideCS = true;
		this.lock.unlock();
	}
	
	public synchronized void releaseCS() {
		myTS = Infinity;
		this.lock.lock();
			insideCS = false;
		this.lock.unlock();
		if (debug_mode)GFunctions.writeToScreen("RAMT: Leaving Critical Section");
		while ( !pendingQ.isEmpty() ) {
			lock.lock();
			Node id = pendingQ.removeFirst();
			lock.unlock();
			sendMessage(id, FRAME_RELEASE,Infinity);
		}
	}
	
	private void myWait(int milis) {
		try {
			Thread.sleep(milis);
		}catch (Exception e) {}
	}
	
	private void broadcastMessage() {
		int requestClockValue = clock.getValue();
		for (Node node: nodes) {
			if (debug_mode)GFunctions.writeToScreen("RAMT: Sending Request signal to "+node.getName()+"-"+node.getHost());
			sendMessage(node,FRAME_REQUEST,requestClockValue);
		}
	}
	
	public void sendMessage(Node destNode,String type, int tstamp) {

		Frame frameTX = new Frame(localnode,destNode,type,tstamp,"");
		try {
			if (debug_mode)GFunctions.writeToScreen("RAMT: Sending message ["+type+"] to: "+destNode.getHost()+":"+destNode.getMutexPort());
			Socket socket = new Socket(destNode.getHost(),destNode.getMutexPort());
			ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream iStream = new ObjectInputStream(socket.getInputStream());
			
			oStream.writeObject(frameTX);
			
			oStream.close();
			iStream.close();
			socket.close();
		} catch (Exception e) {
			GFunctions.writeToLog("RAMT: Error sending "+type+", exception message: ["+e.getMessage()+"]", LOGFILE);
			e.printStackTrace();
		}
	}
	
	
	class RAListener extends Thread {
		private ServerSocket server; 
		private int port;
		
		public RAListener(int port) {
			this.port = port;
		}
		
		@Override
		public void run() {
			if ( startServer() ) {
				while(true) {
					try {
						(new RAClientHandler(server.accept())).start();	
					} catch (Exception e) {
						if (debug_mode) GFunctions.writeToScreen("RAMT: Can't accept connection "+e.getStackTrace());
					}
				}
			}
		}
		
		private boolean startServer() {
			try {
				this.server = new ServerSocket(port);
				if (debug_mode) GFunctions.writeToScreen("RAMT: Server started on port "+this.port);
				return true;
			} catch (Exception e) {
				GFunctions.writeToLog("RAMT: Can't start server "+e.getStackTrace(), LOGFILE);
			}

			return false;
		}
	}
	
	class RAClientHandler extends Thread{
		
		private Socket client;
		ObjectOutputStream oStream = null;
		ObjectInputStream iStream = null;
		
		public RAClientHandler(Socket clientSocket) {
			this.client = clientSocket;
		}
		
		@Override
		public void run() {
			try {
				iStream = new ObjectInputStream(client.getInputStream());
				oStream = new ObjectOutputStream(client.getOutputStream());
				Object readed = iStream.readObject();
				if ( readed instanceof Frame ) {
					Frame frameRX = (Frame) readed;
					handleMessage(frameRX);
				}				
				oStream.close();
				iStream.close();
			} catch (Exception e) {
				
			}
		}
		
		public void handleMessage(Frame frm) {
			if ( frm.frame_type != null ) {
				switch (frm.frame_type) {
				case FRAME_REQUEST:
					String senderID = frm.server_src.getName().toLowerCase();
					String localID = localnode.getName().toLowerCase();
					if (debug_mode)GFunctions.writeToScreen("RAMT: Request received from : "+senderID+"  IDCompare["+senderID.compareTo(localID)+"] LocalTS:"+myTS+" RemoteTS:"+frm.frame_timestamp);
					if ( ( myTS == Infinity || (frm.frame_timestamp < myTS ) || ((frm.frame_timestamp == myTS) && ( senderID.compareTo(localID) == -1 ) ) ) && insideCS == false ) {
						// O bien no nos interesa la zona critica o bien el nodo que nos ha contactado tiene preferencia
						String reason = "";
						if ( myTS == Infinity ) { reason = "Not interested in CS"; } else { reason="LowerTS[L:"+myTS+"-R:"+frm.frame_timestamp+"] or LowerID[L:"+localID+"R:"+senderID+"]"; }
						if (debug_mode)GFunctions.writeToScreen("RAMT: Sending realease to "+frm.server_src.getHost()+" | Reason["+reason+"] ");
						sendMessage(frm.server_src, FRAME_RELEASE, Infinity);
					} else {
						lock.lock();
						pendingQ.add(frm.server_src);
						lock.unlock();
					}
				break;
				case FRAME_RELEASE:
					if (debug_mode) GFunctions.writeToScreen("RAMT: Release received from "+client.getRemoteSocketAddress());
					synchronized (this) {
						numOkay++;
					}
				break;
				}
			}
		}
	}
}
