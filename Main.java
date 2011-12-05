import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import javax.swing.JApplet;

public class Main extends JApplet implements Runnable, KeyListener, MouseListener {
	boolean key[] = new boolean[65535];
	MouseEvent click = null;
	BufferStrategy strategy;
	
	@Override
	public void init() {
		setIgnoreRepaint(true);
		Canvas canvas = new Canvas();
		add(canvas);
		canvas.setBounds(0, 0, 800, 600);
		canvas.createBufferStrategy(2);
		strategy = canvas.getBufferStrategy();
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		new Thread(this).start();
	}

	public void run() {
		while (true) {
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 800, 600);
			if (click != null) {
				g.setColor(Color.RED);
				g.fillRect(click.getX(), click.getY(), 100, 100);
			}
			strategy.show();
		}
	}

	public void keyTyped(KeyEvent e) {}

	public void keyPressed(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = true;
	}

	public void keyReleased(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = false;
	}

	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		click = e;
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}