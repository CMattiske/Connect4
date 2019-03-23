import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class Game {
	
	public final static String NAME = "Connect 4";
	
	public static int DEFAULT_WIDTH = 7;
	public static int DEFAULT_HEIGHT = 6;
	public static int DEFAULT_WIN = 4;
	public static int MAX_ROWS = 7;
	public static int MAX_COLUMNS = 15;
	public static int MAX_WIN = 10;
	private Color defaultColor_first = Color.RED;
	private Color defaultColor_second = Color.YELLOW;
	
	public enum Chip {
		NONE, FIRST, SECOND;
	}
	
	public enum Direction {
		HORZ, VERT, DIAGNEG, DIAGPOS;
	}

	private ActionHandler ah;
	
	private Random rand;
	private GamePlayer player1;
	private GamePlayer player2;
	
	private boolean p1turn;
	
	private Chip[][] rack;
	private int width;
	private int height;
	private int winCondition;
	
	private int winner;
	public final static int NOWINNER = 0;
	public final static int WINNER_P1 = 1;
	public final static int WINNER_P2 = 2;
	private boolean gameOver;
	
	private boolean[][] rackHighlight;
	private boolean[] win; //A column turn player can win in
	private boolean[] check; //A column turn player has to block a win
	
	private int[] moveLog;
	private int currentTurn;
	
	public Game(ActionHandler ah, Random rand, Player p1, Player p2) {
		this(ah, rand, p1, p2, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_WIN);
	}
	
	public Game(ActionHandler ah, Random rand, Player p1, Player p2, int w, int h, int winC) {
		this.ah = ah;
		this.rand = rand;
		this.player1 = new GamePlayer(this, p1);
		this.player2 = new GamePlayer(this, p2);
		this.width = w;
		this.height = h;
		this.winCondition = winC;
		rack = new Chip[width][height];
	}
	
	public ActionHandler getActionHandler() {
		return ah;
	}
	
	public void begin(boolean p1starts) {
		rackHighlight = new boolean[width][height];
		win = new boolean[width];
		check = new boolean[width];
		moveLog = new int[width*height];
		emptyRack();
		p1turn = p1starts;
		player1.setColor(p1turn?defaultColor_first:defaultColor_second);
		player2.setColor(p1turn?defaultColor_second:defaultColor_first);
		currentTurn = 0;
		winner = NOWINNER;
		gameOver = false;
	}
	
	public void update() {
		//If a player has won, end the game
		checkForWin();
		if (winner!=NOWINNER) {
			gameOver = true;
		}
		//If the rack is full, it's a draw
		if (rackFull()) {
			gameOver = true;
		}
		//If opponent has a winning move next move, highlight column
		//Create and check hypothetical maps for check
		for (int x=0; x<width; x++) {
			Chip[][] hypotheticalRack = clone(rack);
			dropChip(hypotheticalRack, x, p1turn?Chip.FIRST:Chip.SECOND);
			win[x] = hasWinner(hypotheticalRack);
			hypotheticalRack = clone(rack);
			dropChip(hypotheticalRack, x, p1turn?Chip.SECOND:Chip.FIRST); //Opposite player
			check[x] = hasWinner(hypotheticalRack);

		}
	}
	
	public GamePlayer getPlayer(int index) {
		switch (index) {
		case 0:
			return player1;
		case 1:
			return player2;
		default:
			return null;
		}
	}
	
	public GamePlayer getPlayer1() {
		return player1;
	}
	
	public GamePlayer getPlayer2() {
		return player2;
	}
	
	public int getWidth() {
		return rack.length;
	}
	
	public int getHeight() {
		return rack[0].length;
	}
	
	public int getWinCondition() {
		return winCondition;
	}
	
	public Chip getChip(int x, int y) {
		return rack[x][y];
	}
	
	public int getCurrentTurn() {
		return currentTurn;
	}
	
	public boolean isOver() {
		return gameOver;
	}
	
	public int getWinner() {
		return winner;
	}
	
	public boolean isWinningChip(int x, int y) {
		return rackHighlight[x][y];
	}
	
	public boolean isWin(int x) {
		return win[x];
	}
	
	public boolean isCheck(int x) {
		return check[x];
	}
	
	public boolean p1turn() {
		return p1turn;
	}
	
	public GamePlayer turnPlayer() {
		return p1turn?player1:player2;
	}
	
	private void emptyRack() {
		for (Chip[] x : rack) {
			Arrays.fill(x, Chip.NONE);
		}
	}
	
	public int availableSpace(int x) {
		for (int y = height-1; y>=0; y--) {
			if (rack[x][y]==Chip.NONE) {
				return y;
			}
		}
		return -1; //Error
	}
	
	private int availableSpace(Chip[][] map, int x) {
		for (int y = height-1; y>=0; y--) {
			if (map[x][y]==Chip.NONE) {
				return y;
			}
		}
		return -1; //Error
	}
	
	public int topChip(int x) {
		for (int y = 0; y<height; y++) {
			if (rack[x][y]!=Chip.NONE) {
				return y;
			}
		}
		return -1; //Error
	}
	
	private int topChip(Chip[][] map, int x) {
		for (int y = 0; y<height; y++) {
			if (map[x][y]!=Chip.NONE) {
				return y;
			}
		}
		return -1; //Error
	}
	
	private void dropChip(Chip[][] map, int x, Chip c) {
		int y = availableSpace(map, x);
		if (y>-1) {
			map[x][y]=c;
		}
	}
	

	public void doMove(int x) {
		dropChip(rack, x, p1turn?Chip.FIRST:Chip.SECOND);
		moveLog[currentTurn] = x;
		nextTurn();
	}
	
	private void nextTurn() {
		currentTurn++;
		p1turn = !p1turn;
	}
	
	
	public void undo() {
		if (currentTurn>0) {
			int x = moveLog[currentTurn-1];
			int y = topChip(rack, x);
			rack[x][y]=Chip.NONE;
			p1turn = !p1turn;
			currentTurn--;
		}
	}
	
	public boolean hasWinner(Chip[][] map) {
		//Check horizontal first
		for (int x = 0; x < width-(winCondition-1); x++) {
			for (int y = 0; y < height; y++) {
				if (isWin(map, x, y, Direction.HORZ)) {
					return true;
				}
			}
		}
		//Now check vertical
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height-(winCondition-1); y++) {
				if (isWin(map, x, y, Direction.VERT)) {
					return true;
				}
			}
		}
		//Now check diagonal negative
		for (int x = 0; x < width-(winCondition-1); x++) {
			for (int y = 0; y < height-(winCondition-1); y++) {
				if (isWin(map, x, y, Direction.DIAGNEG)) {
					return true;
				}
			}
		}
		//Now check diagonal positive
		for (int x = 0; x < width-(winCondition-1); x++) {
			for (int y = winCondition-1; y < height; y++) {
				if (isWin(map, x, y, Direction.DIAGPOS)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void checkForWin() {
		//Check horizontal first
		for (int x = 0; x < width-(winCondition-1); x++) {
			for (int y = 0; y < height; y++) {
				if (isWin(rack, x, y, Direction.HORZ)) {
					winner = p1turn?WINNER_P2:WINNER_P1;
					highlightWin(x,y, Direction.HORZ);
				}
			}
		}
		//Now check vertical
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height-(winCondition-1); y++) {
				if (isWin(rack,x, y, Direction.VERT)) {
					winner = p1turn?WINNER_P2:WINNER_P1;
					highlightWin(x,y, Direction.VERT);
				}
			}
		}
		//Now check diagonal negative
		for (int x = 0; x < width-(winCondition-1); x++) {
			for (int y = 0; y < height-(winCondition-1); y++) {
				if (isWin(rack, x, y, Direction.DIAGNEG)) {
					winner = p1turn?WINNER_P2:WINNER_P1;
					highlightWin(x,y, Direction.DIAGNEG);
				}
			}
		}
		//Now check diagonal positive
		for (int x = 0; x < width-(winCondition-1); x++) {
			for (int y = winCondition-1; y < height; y++) {
				if (isWin(rack, x, y, Direction.DIAGPOS)) {
					winner = p1turn?WINNER_P2:WINNER_P1;
					highlightWin(x,y, Direction.DIAGPOS);
				}
			}
		}
	}
	
	private boolean isWin(Chip[][] map, int x, int y, Direction dir) {
		Chip thisChip = map[x][y];
		if (thisChip==Chip.NONE) {
			return false;
		}
		int i=1;
		while (i<winCondition) {
			int checkX = x;
			int checkY = y;
			switch (dir) {
			case HORZ:
				checkX = x+i;
				break;
			case VERT:
				checkY = y+i;
				break;
			case DIAGNEG:
				checkX = x+i;
				checkY = y+i;
				break;
			case DIAGPOS:
				checkX = x+i;
				checkY = y-i;
				break;
			default:
				break;
			}
			if (map[checkX][checkY]==thisChip) {
				i++;
			} else {
				i = winCondition+1; //Stop the loop
			}
		}
		if (i==winCondition) {
			return true;
		}
		return false;
	}
	
	private void highlightWin(int x, int y, Direction dir) {
		for (int i = 0; i<winCondition; i++) {
			int checkX = x;
			int checkY = y;
			switch (dir) {
			case HORZ:
				checkX = x+i;
				break;
			case VERT:
				checkY = y+i;
				break;
			case DIAGNEG:
				checkX = x+i;
				checkY = y+i;
				break;
			case DIAGPOS:
				checkX = x+i;
				checkY = y-i;
				break;
			default:
				break;
			}
			rackHighlight[checkX][checkY] = true;
		}
	}
	
	private Chip[][] clone(Chip[][] map) {
		Chip[][] clone = new Chip[map.length][map[0].length];
		for (int x = 0; x<map.length; x++) {
			for (int y = 0; y<map[0].length; y++) {
				clone[x][y]=map[x][y];
			}
		}
		return clone;
	}
	
	public boolean columnFull(int x) {
		return rack[x][0]!=Chip.NONE;
	}
	
	public boolean rackFull() {
		for (int x = 0; x<width; x++) {
			if (!columnFull(x)) {
				return false;
			}
		}
		return true;
	}
	
	public void nextGame() {
		//Start new game with previous winner going second
		boolean p1start = true;
		switch (winner) {
		case NOWINNER:
			p1start = rand.nextBoolean();
			break;
		case WINNER_P1:
			p1start = false;
			break;
		case WINNER_P2:
			p1start = true;
			break;
		}
		begin(p1start);
		
	}
}
