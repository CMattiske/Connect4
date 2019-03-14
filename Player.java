import java.io.Serializable;

public class Player implements Serializable {
	
	private static final long serialVersionUID = ConnectionManager.DEFAULT_SERIAL_PLAYER;
	
	private String name;
	
	public Player(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
