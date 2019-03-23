import java.io.IOException;
import java.util.Random;


public interface ActionHandler extends Runnable {
	public void setGameUI(GameUI ui);
	public void sendActionMessage(int par, String str) throws IOException;
	public void sendChatMessage(String message) throws IOException;
	public int getIndex();
	public Random rand();
	public void networkError(String message);
}
