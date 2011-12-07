import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
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
	
	static final int[] X_DIRS = { 0, 0, -1, 1 , -1, 1, 1,-1};
	static final int[] Y_DIRS = { -1, 1, 0, 0 , -1,-1, 1, 1};
	
	String msg = "";
	
	// Tile types
	static final byte _ = 0;
	static final byte O = 1; 
	static final byte I = 2; static final int TRANSPARENTS = 2; static final int SOLIDS = 2;
	static final byte W = 3; 
	static final byte D = 4;
	
	// b stats
	static final double B_RUN_SPEED = 0.09;
	static final double B_WALK_SPEED = 0.025;
	static final int B_COOLDOWN = 20;
	int b_cooldown = 0;
	int b_fatigue = 0;
	int b_exhaustion = 0;
	double b_x = 14;
	double b_y = 7;
	
	// v stats
	static final double V_SPEED = 0.05;
	static final int V_COOLDOWN = 30;
	int v_cooldown = 0;
	double v_x = 11;
	double v_y = 9;
	int[][] v_map = new int[T_H][T_W];
	
	// bang
	int bang_tick = 0;
	int bang_x = -1;
	int bang_y = -1;
	
	// game
	boolean v_vs_b = false;
	boolean game_over = false;
	
	// map
	static byte[] T_TO_HP = {
		0, // _
		10,// O
		14,// I
		20,// W
		9,// D
	};
	
	byte[][] t_type = {
		{_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, I, W, I, W, W, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, _, _, _, _, _, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, O, _, _, _, _, _, I, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, _, _, _, _, _, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, _, _, _, _, _, W, _, _, _, _, _, _, _, _, _},
		{_, _, _, W, I, W, W, O, W, W, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, W, _, _, _, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, I, _, _, _, I, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, W, W, D, W, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, W, _, _, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, W, _, _, W, _, _, _, _, _, _, _, _},
		{_, _, _, _, _, _, _, W, W, O, W, _, _, _, _, _, _, _, _},
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
			b_cooldown--;
			v_cooldown--;
			double sp = b_fatigue > 400 ? B_WALK_SPEED : B_RUN_SPEED;
			boolean mv = false;
			// Input
			if (key[KeyEvent.VK_UP]) {
				b_y -= sp;
				// Borders
				b_y = b_y < P_R ? P_R : b_y;
				// Solid things
				b_y = t_type[(int) b_y][(int) b_x] >= SOLIDS ? b_y + sp : b_y;
				mv = true;
			}
			if (key[KeyEvent.VK_DOWN]) {
				b_y += sp;
				// Borders
				b_y = b_y > (T_H - P_R) ? (T_H - P_R) : b_y;
				// Solid things
				b_y = t_type[(int) b_y][(int) b_x] >= SOLIDS ? b_y - sp : b_y;
				mv = true;
			}
			if (key[KeyEvent.VK_LEFT]) {
				b_x -= sp;
				// Borders
				b_x = b_x < P_R ? P_R : b_x;
				// Solid things
				b_x = t_type[(int) b_y][(int) b_x] >= SOLIDS ? b_x + sp : b_x;
				mv = true;
			}
			if (key[KeyEvent.VK_RIGHT]) {
				b_x += sp;
				// Borders
				b_x = b_x > (T_W - P_R) ? (T_W - P_R) : b_x;
				// Solid things
				b_x = t_type[(int) b_y][(int) b_x] >= SOLIDS ? b_x - sp : b_x;
				mv = true;
			}
			
			if (mv && b_fatigue <= 600) {
				b_fatigue++;
				b_exhaustion++;
			}
			if (!mv && b_fatigue > b_exhaustion / 50) {
				b_fatigue--;
			}
			
			// Interaction
			msg = "";

			for (int i = 0; i < 4; i++) {
				int ny = ((int) b_y) + Y_DIRS[i];
				int nx = ((int) b_x) + X_DIRS[i];
				if (nx < 0 || ny < 0 || nx >= T_W || ny >= T_H) { continue; }
				switch (t_type[ny][nx]) {
					case O:
						msg = "Press space to lock door.";
						if (b_cooldown <= 0 && key[KeyEvent.VK_SPACE]) {
							t_type[ny][nx] = D;
							b_cooldown = B_COOLDOWN;
						}
						break;
					case D:
						msg = "Press space to open door.";
						if (b_cooldown <= 0 && key[KeyEvent.VK_SPACE]) {
							t_type[ny][nx] = O;
							b_cooldown = B_COOLDOWN;
						}
						break;
				}
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
					v_map[p.y - 1][p.x] = v_map[p.y][p.x] + 1 + (t_type[p.y - 1][p.x] >= SOLIDS ? t_hp[p.y - 1][p.x] * 3 : 0);
					queue.add(new Point(p.x, p.y - 1));
				}
				// Down
				if (p.y < T_H - 1 && v_map[p.y + 1][p.x] > v_map[p.y][p.x] + 1 + t_hp[p.y + 1][p.x]) {
					v_map[p.y + 1][p.x] = v_map[p.y][p.x] + 1 + (t_type[p.y + 1][p.x] >= SOLIDS ? t_hp[p.y + 1][p.x] * 3 : 0);
					queue.add(new Point(p.x, p.y + 1));
				}
				// Left
				if (p.x > 0 && v_map[p.y][p.x - 1] > v_map[p.y][p.x] + 1 + t_hp[p.y][p.x - 1]) {
					v_map[p.y][p.x - 1] = v_map[p.y][p.x] + 1 + (t_type[p.y][p.x - 1] >= SOLIDS ? t_hp[p.y][p.x - 1] * 3 : 0);
					queue.add(new Point(p.x - 1, p.y));
				}
				// Down
				if (p.x < T_W - 1 && v_map[p.y][p.x + 1] > v_map[p.y][p.x] + 1 + t_hp[p.y][p.x + 1]) {
					v_map[p.y][p.x + 1] = v_map[p.y][p.x] + 1 + (t_type[p.y][p.x + 1] >= SOLIDS ? t_hp[p.y][p.x + 1] * 3 : 0);
					queue.add(new Point(p.x + 1, p.y));
				}
			}
			
			// V movement
			int dir = -1;
			int least = v_map[((int) v_y)][((int) v_x)];
			for (int i = 0; i < 8; i++) {
				int ny = ((int) v_y) + Y_DIRS[i];
				int nx = ((int) v_x) + X_DIRS[i];
				int ny2 = (int) (v_y + Y_DIRS[i] * V_SPEED);
				int nx2 = (int) (v_x + X_DIRS[i] * V_SPEED);
				if (nx < 0 || ny < 0 || nx >= T_W || ny >= T_H) { continue; }
				int value = v_map[ny][nx];
				if (ny2 != (int) v_y || nx2 != (int) v_x) {
					value = Math.max(value, v_map[ny2][nx2]);
				}
				if (value < least) {
					dir = i;
					least = value;
				}
			}
			double dy = 0;
			double dx = 0;
			double dist = Math.sqrt((b_y - v_y) * (b_y - v_y) + (b_x - v_x) * (b_x - v_x));
			if (dir == -1) {
				if (dist > P_R) {
					dy = (b_y - v_y) / dist * V_SPEED;
					dx = (b_x - v_x) / dist * V_SPEED;
				}
			} else {
				dy = Y_DIRS[dir] * V_SPEED;
				dx = X_DIRS[dir] * V_SPEED;;
			}
			v_y += dy;
			v_y = v_y < P_R ? P_R : v_y;
			v_y = v_y > (T_H - P_R) ? (T_H - P_R) : v_y;
			v_x += dx;
			v_x = v_x < P_R ? P_R : v_x;
			v_x = v_x > (T_W - P_R) ? (T_W - P_R) : v_x;
			// Walls
			if (t_type[(int) v_y][(int) v_x] >= SOLIDS) {
				if (v_cooldown <= 0) {
					t_hp[(int) v_y][(int) v_x]--;
					bang_tick = 10;
					bang_y = (int) v_y;
					bang_x = (int) v_x;
					v_cooldown = V_COOLDOWN;
					if (t_hp[(int) v_y][(int) v_x] == 0) {
						t_type[(int) v_y][(int) v_x] = _;
					}
				}
				v_y -= dy;
				v_x -= dx;
			}
			
			v_vs_b = false;
			if (dist < P_R * 2) {
				b_fatigue += 7;
				v_vs_b = true;
				if (b_fatigue >= 600) {
					game_over = true;
					msg = "GAME OVER";
				}
			}
			
			// Graphics
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 800, 600);
			if (!game_over) {
				for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
					Color c = null;
					switch (t_type[y][x]) {
						case _: c = Color.LIGHT_GRAY; break;
						case W: c = Color.DARK_GRAY;  break;
						case O: c = Color.GRAY;       break;
						case D: c = new Color(127, 127, 0); break;
						case I: c = new Color(127, 127, 255); break;
					}
					g.setColor(c);
					g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
					/*g.setColor(Color.BLACK);
					g.drawString("" + v_map[y][x], x * TILE_SIZE + 10, y * TILE_SIZE + 20);*/
				}}
			}
			g.setColor(Color.RED);
			g.fillOval((int) ((v_x - P_R) * TILE_SIZE), (int) ((v_y - P_R) * TILE_SIZE), (int) (2 * P_R * TILE_SIZE), (int) (2 * P_R * TILE_SIZE));
			if (!(v_vs_b && b_fatigue % 19 == 0)) { g.setColor(Color.GREEN); }
			g.fillOval((int) ((b_x - P_R) * TILE_SIZE), (int) ((b_y - P_R) * TILE_SIZE), (int) (2 * P_R * TILE_SIZE), (int) (2 * P_R * TILE_SIZE));
			g.setColor(Color.YELLOW);
			if (bang_tick-- > 0) {
				g.fillOval(bang_x * TILE_SIZE, bang_y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
			}
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(10));
			for (double d = 0; d < Math.PI * 2; d += Math.PI / 1000) {
				double y = b_y;
				double x = b_x;
				while (true) {
					if ((int) x < 0 || (int) y < 0 || (int) x >= T_W || (int) y >= T_H) { break; }
					if (t_type[(int) y][(int) x] > TRANSPARENTS) { break; }
					y += Math.sin(d) * 0.1;
					x += Math.cos(d) * 0.1;
				}
				y += Math.sin(d) * 0.3;
				x += Math.cos(d) * 0.3;
				g.drawLine((int) (x * TILE_SIZE), (int) (y * TILE_SIZE), (int) (b_x * TILE_SIZE + Math.cos(d) * 1000), (int) (b_y * TILE_SIZE + Math.sin(d) * 1000));
			}
			g.setColor(Color.YELLOW);
			g.fillRect(760, 600 - b_fatigue / 4, 40, b_fatigue / 4);
			if (b_fatigue > 400) {
				g.setColor(Color.ORANGE);
				g.fillRect(760, 600 - b_fatigue / 4, 40, (b_fatigue - 400) / 4);
			}
			g.setColor(Color.WHITE);
			g.fillRect(760, 600 - b_exhaustion / 50, 40, b_exhaustion / 50);
			g.setFont(new Font("Verdana", Font.PLAIN, 20));
			g.drawString(msg, 40, 300);
			strategy.show();
			try { Thread.sleep(10); } catch (Exception e) {}
			if (game_over) {
				return;
			}
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