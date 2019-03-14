import java.io.Serializable;

//What gets sent over the network

public class Message implements Serializable {
	
	private static final long serialVersionUID = ConnectionManager.DEFAULT_SERIAL_MESSAGE;
	
	public static final String TARGET_SEPARATOR = "-";
	public static final String FIELD_SEPARATOR = ",";
	
	public enum MessageType {
		CHAT, ACTION;
	}
	
	private MessageType type; //The type of message
	private String message; //The content of the message. The string if it's a chat message, additional paramters if it's an action
	private int sourcePlayer; //The index of the player who sent the message
	private int parameter; //The value to apply the action to (eg which card in hand to play for PLAYCARD)
	
	public Message(int source, MessageType type, int par, String str) {
		this.sourcePlayer = source;
		this.type = type;
		this.parameter = par;
		this.message = str;
	}
	
	//Used for chat messages as well as actions with a string for parameters
	public Message(int source, String str) {
		this(source, MessageType.CHAT, 0, str);
	}
	
	public Message(int source) {
		this(source, MessageType.ACTION, 0, "");
	}
	
	//A message which is simply an action and a parameter
	public Message(int source, int par) {
		this(source, MessageType.ACTION, par, "");
	}
	
	public Message(int source, int par, String str) {
		this(source, MessageType.ACTION, par, str);
	}
	
	public int getSource() {
		return sourcePlayer;
	}
	
	public MessageType getType() {
		return type;
	}
	
	
	public int getParameter() {
		return parameter;
	}
	
	public String getMessage() {
		return message;
	}
}