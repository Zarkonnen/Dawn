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
	
	static final int TILE_SIZE = 40;
	static final int T_W = 19;
	static final int T_H = 15;
	
	// Tile types
	static final byte _ = 0;
	static final byte W = 1;
	
	// b stats
	static final double B_RUN_SPEED = 0.1;
	double b_x = 0;
	double b_y = 0;
	
	// v stats
	double v_x = 0;
	double v_y = 0;
	int[][] v_map = new int[T_H][T_W];
	
	byte[][] t_type = {
		{_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, W, W, W, W, W, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, _, _, _, _, _, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, _, _, _, _, _, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, _, _, _, _, _, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, _, _, _, _, _, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, W, W, W, _, W, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, W, W, W, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, W, _, _, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, W, _, _, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, W, W, _, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _}
	};
	
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

	@Override
	public void run() {
		while (true) {
			// Input
			if (key[KeyEvent.VK_W]) { b_y -= B_RUN_SPEED; b_y = b_y < 0 ? 0 : b_y; }
			if (key[KeyEvent.VK_S]) { b_y += B_RUN_SPEED; b_y = b_y > (T_H - 1) ? (T_H - 1) : b_y; }
			if (key[KeyEvent.VK_A]) { b_x -= B_RUN_SPEED; b_x = b_x < 0 ? 0 : b_x; }
			if (key[KeyEvent.VK_D]) { b_x += B_RUN_SPEED; b_x = b_x > (T_W - 1) ? (T_W - 1) : b_x; }
			
			// Graphics
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
				Color c = null;
				switch (t_type[y][x]) {
					case _: c = Color.LIGHT_GRAY; break;
					case W: c = Color.DARK_GRAY;  break;
				}
				g.setColor(c);
				g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
			}}
			g.setColor(Color.RED);
			g.fillOval((int) (v_x * TILE_SIZE), (int) (v_y * TILE_SIZE), TILE_SIZE, TILE_SIZE);
			g.setColor(Color.GREEN);
			g.fillOval((int) (b_x * TILE_SIZE), (int) (b_y * TILE_SIZE), TILE_SIZE, TILE_SIZE);
			strategy.show();
			try { Thread.sleep(25); } catch (Exception e) {}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = false;
	}

	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {
		click = e;
	}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
}