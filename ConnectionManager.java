import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;


public class ConnectionManager implements ActionHandler {

	public static int DEFAULT_PORT = 5000;
	public static long DEFAULT_SERIAL_MESSAGE = 421L;
	public static long DEFAULT_SERIAL_PLAYER = 422L;
	
	private GameUI ui;
	
    	private static ServerSocket server;
   	 private ObjectOutputStream oos;
    	private ObjectInputStream ois;
    
	private Socket socket;
	private InetAddress localhost;
	private boolean connected = false;
	private boolean isServer = false;
	
	private int port;
	
	private Random sharedRand;
	
	private int index;
	private int numberOfClients;
	
	private ArrayList<Message> actionLog;
	private ArrayList<Message> chatLog;
	
	public ConnectionManager(int port) {
		this.port = port;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException exception) {
			localhost = null;
			err("Couldn't resolve hostname");
		}
		index = 0;
		numberOfClients = 0;
		actionLog = new ArrayList<Message>(0);
		chatLog = new ArrayList<Message>(0);
	}
	
	public ConnectionManager(GameUI ui, int port) {
		this(port);
		setGameUI(ui);
	}
	
	public void setGameUI(GameUI ui) {
		this.ui = ui;
	}
	
	public Game connectTo(String host, Player thisPlayer) throws IOException, ClassNotFoundException {
		socket = new Socket(host, port);
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		oos.writeObject(localhost+ " has joined!"); //Offer hand shake
		String message = (String) ois.readObject(); //Confirm hand shake
		response(message);
		long seed = (long) ois.readObject(); //Get matching seed
		index = (int) ois.readObject(); //Read player index
		response("Connected as player "+index);
		oos.writeObject(thisPlayer);
		Player hostPlayer = (Player)ois.readObject();
		int width = (int) ois.readObject();
		int height = (int) ois.readObject();
		int win = (int) ois.readObject();

		sharedRand = new Random(seed);

		connected = true;
		isServer = false;

		return new Game(this, sharedRand, hostPlayer, thisPlayer, width, height, win);
	}
	
	public Game host(Player thisHost, int width, int height, int win) throws IOException, ClassNotFoundException {
		server = new ServerSocket(port);
		socket = server.accept();
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		String message = (String) ois.readObject(); //Accept hand shake
		response(message);
		long seed = new Random().nextLong();
		oos.writeObject("Hello!"); //Confirm hand shake
		oos.writeObject(seed); //Send matching seed
		numberOfClients++;
		oos.writeObject(numberOfClients); //Send player index
		Player newPlayer = (Player) ois.readObject();
		response("Hello "+newPlayer.getName()+"!");
		oos.writeObject(thisHost);
		oos.writeObject(width);
		oos.writeObject(height);
		oos.writeObject(win);

		index = 0;
		sharedRand = new Random(seed);

		connected = true;
		isServer = true;

		return new Game(this, sharedRand, thisHost, newPlayer, width, height, win);
	}
	
	public InetAddress localhost() {
		return localhost;
	}
	
	public boolean isServer() {
		return isServer;
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public int getIndex() {
		return index; //0 is host
	}
	
	public void disconnect() {
		if (!socket.isClosed()) {
			try {
				socket.close();
				connected = false;
			} catch (IOException e) {
				err("IOException closing connection");
			}
		}
	}
	
	public void sendChatMessage(String message) throws IOException {
		ui.chatMsg(index, message);
		Message m = new Message(index, message);
		oos.writeObject(m);
		chatLog.add(m);
	}
	
	public void sendActionMessage(int par, String str) throws IOException {
		ui.actionMsg(index, par, str); //Do the action for this player
		Message m = new Message(index, par, str);
		oos.writeObject(m); //Send the action to other player
		actionLog.add(m);
	}
	
	public Random rand() {
		return sharedRand;
	}
	
	public void response(String str) {
		System.out.println(str);
	}
	
	public void err(String str) {
		System.err.println(str);
	}
	
	//Wait for responses
	public void run() {
		while (connected) {
			try {
				waitForResponse();
			} catch (IOException e) {
				networkError("Fatal IOException accepting response: "+e.getMessage());
				disconnect();
			} catch (ClassNotFoundException e) {
				networkError("Fatal ClassNotFoundException accepting response: "+e.getMessage());
				disconnect();
			}
		}
	}
	
	private void waitForResponse() throws IOException, ClassNotFoundException {
		Message msg = (Message) ois.readObject();
		ui.processing();
		//TODO Process response
		switch (msg.getType()) {
		case CHAT:
			ui.chatMsg(msg.getSource(), msg.getMessage());
			break;
		case ACTION:
			ui.actionMsg(msg.getSource(), msg.getParameter(), msg.getMessage());
			break;
		}
		ui.doneProcessing();
	}
	
	public String actionLog() {
		String str = "";
		for (Message m : actionLog) {
			str = str + m + "\n";
		}
		return str;
	}

	
	public void networkError(String m) {
		ui.chatMsg(index,"ERR: "+m);
	}
}
