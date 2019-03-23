import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;


public class GameFrame extends JFrame implements GameUI {
	
	private final int WINDOW_WIDTH = 1200;
	private final int WINDOW_HEIGHT = 1000;
	private final int CHAT_HEIGHT = 150;
	
	private Thread waitingThread;
	private boolean tempDisable;
	
	private Game game;
	private GamePlayer me;
	
	private GamePanel gamePanel = new GamePanel();
	private ChatPanel chatPanel = new ChatPanel();
	
	//These MUST be negative
	private final int ACTION_UNDO = -1;
	private final int ACTION_UNDOREQUEST = -2;
	private final int ACTION_NEWGAME = -3;
	private final int ACTION_NEWGAMEREQUEST = -4;
	
	public GameFrame(String title, Game game) {
		//Preliminary Stuff
		super(title);
		setSize(WINDOW_WIDTH,WINDOW_HEIGHT);
	    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
	    
	    this.game = game;
	    game.getActionHandler().setGameUI(this);
		gamePanel.initialise();
	    
	    //Set up network stuff
		waitingThread = new Thread(game.getActionHandler());
	    
	    gamePanel.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT-CHAT_HEIGHT));
	    chatPanel.setPreferredSize(new Dimension(WINDOW_WIDTH, CHAT_HEIGHT));
		
		JPanel vertPanel = new JPanel();
	    vertPanel.setLayout(new BoxLayout(vertPanel,BoxLayout.Y_AXIS));
	    vertPanel.add(gamePanel);
	    vertPanel.add(chatPanel);
	    this.add(vertPanel);
	    
	    this.setLocationRelativeTo(null);
		this.setVisible(true);
		
		startGame();

	}
	
	private class ChatPanel extends JPanel {
		
		private JTextArea chat = new JTextArea();
		private JScrollPane chatScroll = new JScrollPane(chat);
		private JTextField textField_chat = new JTextField(15);
		private JButton button_send = new JButton("Send");
		
		public ChatPanel() {
			
			ChatListener listener = new ChatListener();
			
			chat.setEditable(false);
			chat.setLineWrap(true);
			button_send.addActionListener(listener);
			textField_chat.addKeyListener(new EnterListener());
			
			JPanel sendBar = new JPanel();
			sendBar.add(textField_chat);
			sendBar.add(button_send);
			
			this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			this.add(chatScroll);
			this.add(sendBar);
			
			chatScroll.setEnabled(false);
			textField_chat.setEnabled(false);
			button_send.setEnabled(false);
			
		}
		
		public void append(String m) {
			chat.append(m+"\n");
		}
		
		private class ChatListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				if (source==button_send && textField_chat.getText().length()>0) {
					if (game!=null) {
						try {
							game.getActionHandler().sendChatMessage(textField_chat.getText());
							textField_chat.setText("");
						} catch (IOException exception) {
							game.getActionHandler().networkError("IOException while sending message");
						}
					}
				}
			}
		}
		
		private class EnterListener implements KeyListener {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					button_send.doClick();
				}
				
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		}

		public void enableAll() {
			chatScroll.setEnabled(true);
			textField_chat.setEnabled(true);
			button_send.setEnabled(true);
			
		}
	}
	
	private class GamePanel extends JPanel {

		private final Color BG_COLOR = Color.BLACK;
		private final Color RACK_COLOR = Color.WHITE;
		private final Color CHECK_COLOR = Color.GRAY;
		private final Color WIN_COLOR = Color.LIGHT_GRAY;
		private final Color DRAW_COLOR = Color.ORANGE;
		private final int RACK_X = 10;
		private final int RACK_Y = 74;
		private int grid_size = 64;
		private int chip_border = 2;
		private int chip_size = grid_size-chip_border*2;
		
		private Font turnFont = new Font("Turn",Font.BOLD,36);
		private final int TURN_GAP = 32;
		
		private boolean checkEnabled;
		
		private boolean undoRequested;
		private boolean newGameRequested;
		private boolean iRequested; //If this player requested new game
		
		private ArrayList<ButtonComponent> buttons;
		
		private final int BUTTON_PARAM_UNDO = -1;
		private final int BUTTON_PARAM_UNDOREQUEST = -2;
		private final int BUTTON_PARAM_NEWGAME = -3;
		private final int BUTTON_PARAM_NEWGAMEREQUEST = -4;

		private final Color BUTTON_COLOR_UNDO = Color.CYAN;
		private final Color BUTTON_COLOR_UNDOREQUEST = Color.BLUE;
		private final Color BUTTON_COLOR_NEWGAME = Color.GREEN;
		private final Color BUTTON_COLOR_NEWGAMEREQUEST = Color.BLUE;
		private final int BUTTON_WIDTH = 128;
		private final int BUTTON_HEIGHT = 64;
		
		private int ghostColumn = -1; //Where to show a "ghost chip" while hovering mouse
		
		public GamePanel() {
			super();
			setBackground(BG_COLOR);
			setLayout(null);
		}
		
		public void initialise() {
			MyMouseListener ml = new MyMouseListener();
			
			this.addMouseListener(ml);
			this.addMouseMotionListener(ml);
			
			checkEnabled = true;
			undoRequested = false;
			newGameRequested = false;
			iRequested = false;
			update();
		}
		
		public void update() {
			buttons = new ArrayList<ButtonComponent>(0);
			int button_undo_x = RACK_X;
			int button_undo_y = RACK_Y + game.getHeight()*grid_size + TURN_GAP*4;
			int button_newgame_x = RACK_X + game.getWidth()*grid_size;
			buttons.add(new ButtonComponent(BUTTON_PARAM_UNDOREQUEST, button_undo_x, button_undo_y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_COLOR_UNDOREQUEST));
			buttons.add(new ButtonComponent(BUTTON_PARAM_NEWGAMEREQUEST, button_newgame_x, button_undo_y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_COLOR_NEWGAME));
			repaint();
		}
		
		public void toggleCheckEnabled() {
			checkEnabled = !checkEnabled;
		}
		
		public void requestUndo() {
			undoRequested = true;
		}
		
		public void rejectUndo() {
			undoRequested = false;
		}
		
		public void requestNew() {
			newGameRequested = true;
		}
		public void rejectNew() {
			newGameRequested = false;
			iRequested = false;
		}
		
		private class MyMouseListener extends MouseInputAdapter {
			
			@Override
			public void mousePressed(MouseEvent e) {
				//Check if in bounds of rack
				if (e.getX()>=RACK_X && e.getY()>=RACK_Y && e.getX()<RACK_X+game.getWidth()*grid_size && e.getY()<RACK_Y+game.getHeight()*grid_size) {
					int column = (e.getX()-RACK_X)/grid_size;
					if (myTurn() && !game.columnFull(column) && !tempDisable) {
						try {
							game.getActionHandler().sendActionMessage(column, "");
						} catch (IOException exception) {
							game.getActionHandler().networkError("IOException while sending action message");
						}
					}
				}
				
				int index = -1;
				for (int i=0; i<buttons.size(); i++) {
					if (buttons.get(i).isEnabled() && buttons.get(i).inBounds(e.getX(), e.getY())) {
						index = i;
					}
				}
				if (index>-1) {
					switch (buttons.get(index).getParameter()) {
					case BUTTON_PARAM_UNDO:
						try {
							game.getActionHandler().sendActionMessage(ACTION_UNDO, "");
						} catch (IOException exception) {
							game.getActionHandler().networkError("IOException sending undo");
						}
						break;
					case BUTTON_PARAM_UNDOREQUEST:
						try {
							game.getActionHandler().sendActionMessage(ACTION_UNDOREQUEST, "");
						} catch (IOException exception) {
							game.getActionHandler().networkError("IOException sending undo request");
						}
						break;
					case BUTTON_PARAM_NEWGAME:
						try {
							game.getActionHandler().sendActionMessage(ACTION_NEWGAME, "");
						} catch (IOException exception) {
							game.getActionHandler().networkError("IOException sending new game request");
						}
						break;
					case BUTTON_PARAM_NEWGAMEREQUEST:
						try {
							game.getActionHandler().sendActionMessage(ACTION_NEWGAMEREQUEST, "");
							iRequested = true;
						} catch (IOException exception) {
							game.getActionHandler().networkError("IOException sending new game request");
						}
					}
					update();
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if (e.getX()>=RACK_X && e.getY()>=RACK_Y && e.getX()<RACK_X+game.getWidth()*grid_size && e.getY()<RACK_Y+game.getHeight()*grid_size) {
					int column = (e.getX()-RACK_X)/grid_size;
					ghostColumn = column;
				} else {
					ghostColumn = -1;
				}
				repaint();
			}
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (game!=null) {
				//Draw entire rack
				int rackWidth = game.getWidth()*grid_size;
				int rackHeight = game.getHeight()*grid_size;
				g.setColor(RACK_COLOR);
				g.fillRect(RACK_X, RACK_Y, rackWidth, rackHeight);
				//Draw win or check columns
				if (checkEnabled && myTurn()) {
					boolean canWin = false;
					for (int x = 0; x<game.getWidth(); x++) {
						if (game.isWin(x)) {
							g.setColor(WIN_COLOR);
							g.fillRect(RACK_X+grid_size*x, RACK_Y, grid_size, rackHeight);
							canWin = true;
						}

					}
					for (int x = 0; !canWin && x<game.getWidth(); x++) {
						if (game.isCheck(x)) {
							g.setColor(CHECK_COLOR);
							g.fillRect(RACK_X+grid_size*x, RACK_Y, grid_size, rackHeight);
						}
					}
				}
				//Draw each chip (& column "headings")
				for (int x = 0; x<game.getWidth(); x++) {
					g.setColor(myTurn() && ghostColumn==x?me.getColor():RACK_COLOR);
					drawCenteredString(g, ""+(x+1), new Rectangle(RACK_X+grid_size*x,RACK_Y-grid_size, grid_size, grid_size), turnFont);
					for (int y = 0; y<game.getHeight(); y++) {
						switch (game.getChip(x, y)) {
						case NONE:
							g.setColor(BG_COLOR);
							break;
						case FIRST:
							g.setColor(game.isWinningChip(x,y)?game.getPlayer(0).getColorWin():game.getPlayer(0).getColor());
							break;
						case SECOND:
							g.setColor(game.isWinningChip(x,y)?game.getPlayer(1).getColorWin():game.getPlayer(1).getColor());
							break;
						}
						g.fillOval(RACK_X+grid_size*x+chip_border, RACK_Y+grid_size*y+chip_border, chip_size, chip_size);
						if (game.isWinningChip(x,y)) {
							g.setColor(BG_COLOR);
							g.fillOval(RACK_X+grid_size*x+chip_border+chip_size/4, RACK_Y+grid_size*y+chip_border+chip_size/4, chip_size/2, chip_size/2);
						}
					}
				}
				//Draw "ghost chip"
				if (myTurn()) {
					if (ghostColumn>=0) {
						int x = ghostColumn;
						int y = game.availableSpace(x);
						if (y>=0 && y<game.getHeight()) {
							g.setColor(me.getColor());
							g.drawOval(RACK_X+grid_size*x+chip_border*4, RACK_Y+grid_size*y+chip_border*4, chip_size-chip_border*6, chip_size-chip_border*6);
						}
					}
				}
				//Draw turn player
				int turnX = RACK_X+chip_size+TURN_GAP;
				int turnY = RACK_Y+rackHeight+TURN_GAP;
				g.setColor(me.getColor());
				g.fillOval(RACK_X, turnY, chip_size, chip_size);

				g.setFont(turnFont);
				String text = "";
				if (game.isOver()) {
					switch (game.getWinner()) {
					case Game.NOWINNER:
						g.setColor(DRAW_COLOR);
						text = "Draw!";
						break;
					case Game.WINNER_P1:
						g.setColor(game.getPlayer1().getColor());
						text = game.getActionHandler().getIndex()==0?"You win!":"You lose!";
						break;
					case Game.WINNER_P2:
						g.setColor(game.getPlayer2().getColor());
						text = game.getActionHandler().getIndex()==1?"You win!":"You lose!";
						break;
					}
				} else {
					g.setColor(game.turnPlayer().getColor());
					text = myTurn()?"Your turn":"Opponent's turn";
				}
				g.drawString(text, turnX, turnY+turnFont.getSize());
				
				//Draw buttons
				for (ButtonComponent b : buttons) {
					b.paintThis(g);
				}
			}
		}
		
		private class ButtonComponent {

			private final Color TEXT_COLOR = Color.WHITE;	

			private int parameter;
			private Font button_font;
			private int button_width = 64;
			private int button_height = 48;
			private Color color;
			private int x;
			private int y;
			private boolean enabled;
			
			public ButtonComponent(int param, int x, int y, int width, int height, Color c) {
				this.parameter = param;
				this.button_font = new Font("Button", Font.BOLD, 18);
				this.x = x;
				this.y = y;
				this.button_width = width;
				this.button_height = height;
				this.color = c;
				updateEnabled();
			}
			
			public int getWidth() {
				return button_width;
			}
			
			public int getHeight() {
				return button_height;
			}
			
			public boolean inBounds(int mouseX, int mouseY) {
				if (mouseX<x) {
					return false;
				}
				if (mouseX>x+getWidth()) {
					return false;
				}
				if (mouseY<y) {
					return false;
				}
				if (mouseY>y+getHeight()) {
					return false;
				}
				return true;
			}
			
			public boolean isEnabled() {
				return enabled;
			}

			public int getParameter() {
				return parameter;
			}
			
			public void updateEnabled() {
				//Since the undo button is the same button, toggle it appropriately
				if (parameter==BUTTON_PARAM_UNDO || parameter==BUTTON_PARAM_UNDOREQUEST) {
					if (undoRequested) {
						parameter = BUTTON_PARAM_UNDO;
					} else {
						parameter = BUTTON_PARAM_UNDOREQUEST;
					}
				}
				if (parameter==BUTTON_PARAM_NEWGAME || parameter==BUTTON_PARAM_NEWGAMEREQUEST) {
					if (newGameRequested || game.isOver()) {
						parameter = BUTTON_PARAM_NEWGAME;
					} else {
						parameter = BUTTON_PARAM_NEWGAMEREQUEST;
					}
				}
				switch (parameter) {
				case BUTTON_PARAM_UNDO:
					enabled = game.getCurrentTurn()>0 && myTurn() && undoRequested;
					break;
				case BUTTON_PARAM_UNDOREQUEST:
					enabled = game.getCurrentTurn()>0 && !myTurn() && !undoRequested;
					break;
				case BUTTON_PARAM_NEWGAME:
					enabled = (newGameRequested && !iRequested) || game.isOver();
					break;
				case BUTTON_PARAM_NEWGAMEREQUEST:
					enabled = true;
					break;
				}
			}
			
			public String getButtonText() {
				switch (parameter) {
				case BUTTON_PARAM_UNDO:
					return "UNDO";
				case BUTTON_PARAM_UNDOREQUEST:
					return "Request undo";
				case BUTTON_PARAM_NEWGAME:
					return "New game";
				case BUTTON_PARAM_NEWGAMEREQUEST:
					return "Request new";
				}
				return "";
			}
			
			public void paintThis(Graphics g) {
				switch (parameter) {
				case BUTTON_PARAM_UNDO:
					color = BUTTON_COLOR_UNDO;
					break;
				case BUTTON_PARAM_UNDOREQUEST:
					color = BUTTON_COLOR_UNDOREQUEST;
					break;
				case BUTTON_PARAM_NEWGAME:
					color = BUTTON_COLOR_NEWGAME;
					break;
				case BUTTON_PARAM_NEWGAMEREQUEST:
					color = BUTTON_COLOR_NEWGAMEREQUEST;
					break;
				default:
				}
				g.setColor(enabled?color:color.darker());
				g.fillRect(x, y, button_width, button_height);
				g.setColor(RACK_COLOR);
				g.drawRect(x, y, button_width, button_height);
				g.setColor(enabled?TEXT_COLOR:TEXT_COLOR.darker());
				drawCenteredString(g, getButtonText(), new Rectangle(x, y, button_width, button_height), button_font);
			}
		}


	}
	
	public void updateAll() {
		game.update();
		gamePanel.update();
		repaint();
	}
	
	@Override
	public void processing() {
		tempDisable = true;
	}
	
	@Override
	public void doneProcessing() {
		tempDisable = false;
	}
	
	@Override
	public void chatMsg(int sourcePlayer, String m) {
		chatPanel.append(game.getPlayer(sourcePlayer).getName()+": "+m);
	}
	
	@Override
	public void actionMsg(int sourcePlayer, int par, String str) {
		if (par<0) {
			switch (par) {
			case ACTION_UNDO: //Undo
				game.undo();
				break;
			case ACTION_UNDOREQUEST: //Request undo
				gamePanel.requestUndo();
				break;
			case ACTION_NEWGAME:
				game.nextGame();
				break;
			case ACTION_NEWGAMEREQUEST:
				gamePanel.requestNew();
			default:
				
			}
		} else if (par>=0 && par<game.getWidth()) {
			game.getPlayer(sourcePlayer).doAction(par, str);
			gamePanel.rejectUndo();
			gamePanel.rejectNew();
		}
		game.update();
		updateAll();
	}
	
	private boolean myTurn() {
		if (game.p1turn()) {
			if (game.getActionHandler().getIndex()==0) {
				return true;
			}
		} else {
			if (game.getActionHandler().getIndex()==1) {
				return true;
			}
		}
		return false;
	}
	
	private void startGame() {
		chatPanel.enableAll();
		me = game.getPlayer(game.getActionHandler().getIndex());
		game.begin(game.getActionHandler().rand().nextBoolean());
		waitingThread.start();
	}
	
	//Credit: Daniel Kvst (StackOverflow) https://stackoverflow.com/a/27740330
	public void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
	    // Get the FontMetrics
	    FontMetrics metrics = g.getFontMetrics(font);
	    // Determine the X coordinate for the text
	    int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
	    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
	    int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
	    // Set the font
	    g.setFont(font);
	    // Draw the String
	    g.drawString(text, x, y);
	}
}
