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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MouseInputAdapter;

public class LobbyFrame extends JFrame {

	private final int WINDOW_WIDTH = 600;
	private final int WINDOW_HEIGHT = 400;
	private final int MAX_RANDOM_PLAYERID = 1000;

	private final String dir_images = "images/";
	private InetAddress localhost;
	
	private JButton button_host = new JButton("Host");
	private JLabel label_host = new JLabel("localhost");
	private JButton button_cancel = new JButton("Cancel");
	private JButton button_join = new JButton("Join");
	private JTextField textField_hostname = new JTextField("Chris-PC", 20);
	private JTextField textField_playername = new JTextField(randomName(), 20);
	private JLabel label_playername = new JLabel("Player name:");
	
	private SpinnerModel modelColumns = new SpinnerNumberModel(Game.DEFAULT_WIDTH,1,Game.MAX_COLUMNS,1);
	private SpinnerModel modelRows = new SpinnerNumberModel(Game.DEFAULT_HEIGHT,1,Game.MAX_ROWS,1);
	private SpinnerModel modelWin = new SpinnerNumberModel(Game.DEFAULT_WIN,2,Game.MAX_WIN,1);
	private JSpinner spinnerColumns = new JSpinner(modelColumns);
	private JSpinner spinnerRows = new JSpinner(modelRows);
	private JSpinner spinnerWin = new JSpinner(modelWin);
	private JLabel spinLabel_columns = new JLabel("Width:");
	private JLabel spinLabel_rows = new JLabel("Height:");
	private JLabel spinLabel_win = new JLabel("To win:");


	public LobbyFrame(String title) {
		//Preliminary Stuff
		super(title);
		setSize(WINDOW_WIDTH,WINDOW_HEIGHT);
	    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
	    
	    ButtonListener bl = new ButtonListener();
		button_host.addActionListener(bl);
		button_join.addActionListener(bl);
	    
	    //Initiate components
	    label_host.setLabelFor(button_host);
	    button_cancel.setEnabled(false);
		try {
			localhost = InetAddress.getLocalHost();
		    textField_hostname.setText(""+localhost.getHostName());
			label_host.setText(localhost.getHostName()+"/"+localhost.getHostAddress());
		} catch (UnknownHostException exception) {
			localhost = null;
			label_host.setText("Unkown host");
		}
		textField_hostname.setEditable(true);
		
		JPanel vertPanel = new JPanel();
	    vertPanel.setLayout(new BoxLayout(vertPanel,BoxLayout.Y_AXIS));
	    JPanel hostPanel = new JPanel();
	    hostPanel.add(button_host);
	    hostPanel.add(label_host);
	    hostPanel.add(button_join);
	    hostPanel.add(textField_hostname);
	    JPanel namePanel = new JPanel();
	    namePanel.add(label_playername);
	    namePanel.add(textField_playername);
	    JPanel spinPanel = new JPanel();
	    spinPanel.add(spinLabel_columns);
	    spinPanel.add(spinnerColumns);
	    spinPanel.add(spinLabel_rows);
	    spinPanel.add(spinnerRows);
	    spinPanel.add(spinLabel_win);
	    spinPanel.add(spinnerWin);
	    
	    vertPanel.add(hostPanel);
	    vertPanel.add(namePanel);
	    vertPanel.add(spinPanel);
	    
	    this.add(vertPanel);
	    
	    this.setLocationRelativeTo(null);
		this.setVisible(true);
		
	}
	
	public String randomName() {
		return "p"+new Random().nextInt(MAX_RANDOM_PLAYERID);
	}
	
	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			Object source = e.getSource();
			if (source == button_host) {
				//Get fields from interface elements
				String playername = textField_playername.getText();
				if (playername.length()<1) {
					playername = "Indecisive Client";
				}
				Player host = new Player(playername);
				int width = (int) spinnerColumns.getValue();
				int height = (int) spinnerRows.getValue();
				int win = (int) spinnerWin.getValue();
				
				hostGame(host, width, height, win);
				
			} else if (source == button_join) {
				//Get fields from interface elements
				String playername = textField_playername.getText();
				if (playername.length()<1) {
					playername = "Indecisive Client";
				}
				Player client = new Player(playername);
				
				joinGame(client, textField_hostname.getText());
				
			}
		}
	}
	
	private void hostGame(Player host, int width, int height, int win) {
		log("Hosting at "+localhost);
		String gameName = host.getName()+"'s game";
		ConnectionManager newConnection = new ConnectionManager(ConnectionManager.DEFAULT_PORT);
		try {
			Game newGame = newConnection.host(host, width, height, win);
			GameFrame newFrame = new GameFrame(gameName, newGame);
		} catch (IOException e) {
			err("IOException while attempting to host at "+localhost);
		} catch (ClassNotFoundException e) {
			err("ClassNotFoundException while attempting to host at "+localhost);
		}

	}
	
	private void joinGame(Player client, String hostname) {
		log("Try to join hostname "+hostname);
		String gameName = client.getName()+"'s game";
		ConnectionManager newConnection = new ConnectionManager(ConnectionManager.DEFAULT_PORT);
		try {
			Game newGame = newConnection.connectTo(hostname, client);
			GameFrame newFrame = new GameFrame(gameName, newGame);
		} catch (IOException e) {
			err("IOException while attempting to connect to "+hostname);
		} catch (ClassNotFoundException e) {
			err("ClassNotFoundException while attempting to connect to "+hostname);
		}
	}
	
	public static void log(String m) {
		System.out.println(m);
	}
	
	public static void err(String m) {
		System.err.println(m);
	}
}
