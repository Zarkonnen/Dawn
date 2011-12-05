import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.JApplet;

public class Main extends JApplet implements Runnable, KeyListener, MouseListener {
	boolean key[] = new boolean[65535];
	MouseEvent click = null;
	BufferStrategy strategy;
	
	static final int TILE_SIZE = 40;
	static final int T_W = 19;
	static final int T_H = 15;
	static final double P_R = 0.3;
	
	static final int[] X_DIRS = { 0, 0, -1, 1};
	static final int[] Y_DIRS = { -1, 1, 0, 0};
	
	// Tile types
	static final byte _ = 0;
	static final byte W = 1;
	
	// b stats
	static final double B_RUN_SPEED = 0.1;
	double b_x = 0;
	double b_y = 0;
	
	// v stats
	static final double V_SPEED = 0.01;
	double v_x = 0;
	double v_y = 0;
	int[][] v_map = new int[T_H][T_W];
	
	// map
	static byte[] T_TO_HP = {
		0, // _
		10 // W
	};
	
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
	byte[][] t_hp = new byte[T_H][T_W];
	
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
		// Setup
		for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
			t_hp[y][x] = T_TO_HP[t_type[y][x]];
		}}
		
		while (true) {
			// Input
			if (key[KeyEvent.VK_W]) {
				b_y -= B_RUN_SPEED;
				// Borders
				b_y = b_y < P_R ? P_R : b_y;
				// Solid things
				b_y = t_hp[(int) b_y][(int) b_x] > 0 ? b_y + B_RUN_SPEED : b_y;
			}
			if (key[KeyEvent.VK_S]) {
				b_y += B_RUN_SPEED;
				// Borders
				b_y = b_y > (T_H - P_R) ? (T_H - P_R) : b_y;
				// Solid things
				b_y = t_hp[(int) b_y][(int) b_x] > 0 ? b_y - B_RUN_SPEED : b_y;
			}
			if (key[KeyEvent.VK_A]) {
				b_x -= B_RUN_SPEED;
				// Borders
				b_x = b_x < P_R ? P_R : b_x;
				// Solid things
				b_x = t_hp[(int) b_y][(int) b_x] > 0 ? b_x + B_RUN_SPEED : b_x;
			}
			if (key[KeyEvent.VK_D]) {
				b_x += B_RUN_SPEED;
				// Borders
				b_x = b_x > (T_W - P_R) ? (T_W - P_R) : b_x;
				// Solid things
				b_x = t_hp[(int) b_y][(int) b_x] > 0 ? b_x - B_RUN_SPEED : b_x;
			}
			
			// V-map update
			for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
				v_map[y][x] = 100000;
			}}
			LinkedList<Point> queue = new LinkedList<Point>();
			v_map[(int) b_y][(int) b_x] = 0;
			queue.add(new Point((int) b_x, (int) b_y));
			while (!queue.isEmpty()) {
				Point p = queue.pop();
				// Up
				if (p.y > 0 && v_map[p.y - 1][p.x] > v_map[p.y][p.x] + 1 + t_hp[p.y - 1][p.x]) {
					v_map[p.y - 1][p.x] = v_map[p.y][p.x] + 1 + t_hp[p.y - 1][p.x];
					queue.add(new Point(p.x, p.y - 1));
				}
				// Down
				if (p.y < T_H - 1 && v_map[p.y + 1][p.x] > v_map[p.y][p.x] + 1 + t_hp[p.y + 1][p.x]) {
					v_map[p.y + 1][p.x] = v_map[p.y][p.x] + 1 + t_hp[p.y + 1][p.x];
					queue.add(new Point(p.x, p.y + 1));
				}
				// Left
				if (p.x > 0 && v_map[p.y][p.x - 1] > v_map[p.y][p.x] + 1 + t_hp[p.y][p.x - 1]) {
					v_map[p.y][p.x - 1] = v_map[p.y][p.x] + 1 + t_hp[p.y][p.x - 1];
					queue.add(new Point(p.x - 1, p.y));
				}
				// Down
				if (p.x < T_W - 1 && v_map[p.y][p.x + 1] > v_map[p.y][p.x] + 1 + t_hp[p.y][p.x + 1]) {
					v_map[p.y][p.x + 1] = v_map[p.y][p.x] + 1 + t_hp[p.y][p.x + 1];
					queue.add(new Point(p.x + 1, p.y));
				}
			}
			
			// V movement
			int dir = -1;
			int least = v_map[((int) v_y)][((int) v_x)];
			for (int i = 0; i < 4; i++) {
				int ny = ((int) v_y) + Y_DIRS[i];
				int nx = ((int) v_x) + X_DIRS[i];
				if (nx < 0 || ny < 0 || nx >= T_W || ny >= T_H) { continue; }
				if (v_map[ny][nx] < least) {
					dir = i;
					least = v_map[ny][nx];
				}
			}
			if (dir != -1) {
				v_y += Y_DIRS[dir] * V_SPEED;
				v_y = v_y < P_R ? P_R : v_y;
				v_y = v_y > (T_H - P_R) ? (T_H - P_R) : v_y;
				v_x += X_DIRS[dir] * V_SPEED;
				v_x = v_x < P_R ? P_R : v_x;
				v_x = v_x > (T_W - P_R) ? (T_W - P_R) : v_x;
			}
			
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
				g.setColor(Color.BLACK);
				g.drawString("" + v_map[y][x], x * TILE_SIZE + 10, y * TILE_SIZE + 20);
			}}
			g.setColor(Color.RED);
			g.fillOval((int) ((v_x - P_R) * TILE_SIZE), (int) ((v_y - P_R) * TILE_SIZE), (int) (2 * P_R * TILE_SIZE), (int) (2 * P_R * TILE_SIZE));
			g.setColor(Color.GREEN);
			g.fillOval((int) ((b_x - P_R) * TILE_SIZE), (int) ((b_y - P_R) * TILE_SIZE), (int) (2 * P_R * TILE_SIZE), (int) (2 * P_R * TILE_SIZE));
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