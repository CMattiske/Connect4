import java.awt.Color;


public class GamePlayer {

	private Game game;
	private Player player;
	
	private Color color;
	
	public GamePlayer(Game g, Player p) {
		this.player = p;
		this.game = g;
		this.color = Color.BLACK;
	}
	
	public String getName() {
		return player.getName();
	}
	
	public void setColor(Color c) {
		color = c;
	}
	
	public Color getColor() {
		return color;
	}
	
	public Color getColorWin() {
		return color.brighter();
	}

	public void doAction(int par, String str) {
		doMove(par);
	}
	
	private void doMove(int column) {
		game.doMove(column);
	}
	
}
