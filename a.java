import java.awt.Canvas;
import java.awt.Color;
//import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.util.LinkedList;
import java.util.Random;
import javax.swing.JFrame;

public class a extends JFrame implements Runnable, MouseListener, KeyListener, MouseMotionListener {
	@Override
	public void keyTyped(KeyEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mouseDragged(MouseEvent me) {}

	@Override
	public void mouseMoved(MouseEvent me) {
		my = me.getY();
		mx = me.getX();
	}
	@Override
	public void mousePressed(MouseEvent e) {
		click = true;
	}
	@Override
	public void keyPressed(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = true;
	}
	@Override
	public void keyReleased(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = false;
	}
	
	boolean key[] = new boolean[65535];
	boolean click = false;
	int my, mx;
	BufferStrategy strategy;
	
	static final int TILE_SIZE = 40;
	static final int T_W = 19;
	static final int T_H = 15;
	static final double P_R = 0.3;
	
	static final int[] X_DIRS = { -1, 0, 1, 0 , -1, 1, 1,-1};
	static final int[] Y_DIRS = {  0,-1, 0, 1 , -1,-1, 1, 1};
	static final int[] DIR_KEYS = { KeyEvent.VK_A, KeyEvent.VK_W, KeyEvent.VK_D, KeyEvent.VK_S };
	
	// Tile types
	static final byte _ = 0; // floor
	static final byte G = 1; // grass
	static final byte O = 2; // open door
	static final byte R = 3; // rug
	static final byte T = 4; static final int SOLIDS = 4; // table
	static final byte C = 5; // chair
	static final byte B = 6; // closed box
	static final byte X = 7; // open box
	static final byte E = 8; // bed
	static final byte F = 9; // flower pot
	static final byte I = 10; static final int TRANSPARENTS = 10; // window
	static final byte W = 11; // wall
	static final byte D = 12; // closed door
	
	// b stats
	static final double B_RUN_SPEED = 0.09;
	static final double B_WALK_SPEED = 0.025;
	static final int B_COOLDOWN = 20;
	static final int GUN_DMG = 12;
	static final int GUN_V_DMG = 500;
	static final int MAX_FATIGUE = 900;
	static final int WINDED_FATIGUE = 600;
	
	// inventory
	static final int KEY = 0;
	static final int GUN = 1;
	static final int LEATHER_JACKET = 2;
	static final String[] ITEM_NAMES = { "key", "gun", "leather jacket" };
	
	// v stats
	static final double V_SPEED = 0.05;
	static final int V_COOLDOWN = 30;
	static final int[] Y_VANTAGES = {0, 4, 2, 4, 5, 9, 10 ,12, 7 , 5 , 0 , 14}; // 12 vantages
	static final int[] X_VANTAGES = {0, 3, 7, 7, 9, 5, 10, 14, 13, 15, 18, 6 };
	
	// map
	static byte[] T_TO_HP = {
		0, // _
		0, // G
		20,// O
		0, // R
		12,// T
		4, // C
		16,// B
		16,// X
		32,// E
		8, // F
		28,// I
		40,// W
		18,// D
	};
	
	public static void main(String[] args) {
		new a();
	}

	public a() {
		setIgnoreRepaint(true);
		setUndecorated(true);
		Canvas canvas = new Canvas();
		add(canvas);
		canvas.setSize(800, 600);
		GraphicsDevice gd =
				GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		setLayout(null);
		setBackground(Color.BLACK);
		canvas.setBounds(
				gd.getDefaultConfiguration().getBounds().width / 2 - 400,
				gd.getDefaultConfiguration().getBounds().height / 2 - 300,
				800,
				600
				);
		setSize(gd.getDefaultConfiguration().getBounds().width,
				gd.getDefaultConfiguration().getBounds().height);
		setVisible(true);
		canvas.createBufferStrategy(2);
		strategy = canvas.getBufferStrategy();
		canvas.addMouseListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseMotionListener(this);
		addKeyListener(this);
			canvas.setSize(gd.getDefaultConfiguration().getBounds().width,
					gd.getDefaultConfiguration().getBounds().height);
			setSize(gd.getDefaultConfiguration().getBounds().width,
					gd.getDefaultConfiguration().getBounds().height);
			gd.setFullScreenWindow(this);
		new Thread(this).start();
	}

	@Override
	public void run() {
		int lvl = 1;
		game: while(true) {
			int msgWait = -1;
			int tick = -1;
			String msg = "";
			String msg2 = "Press space to start.";
			Random r = new Random();
			boolean playing = false;

			// b stats
			int b_cooldown = 0;
			int b_fatigue = 0;
			int b_exhaustion = 0;
			int b_push = 0;
			double b_x = 0;
			double b_y = 0;
			boolean[] inventory = new boolean[3];// inventory[LEATHER_JACKET] = true;
			int bullets = 6;
			int jacket_hp = 5;

			// v stats
			int v_cooldown = 0;
			int v_dmg = 0;
			double v_x = 0;
			double v_y = 0;
			double v_b_x = 0;
			double v_b_y = 0;
			int[][] v_map = new int[T_H][T_W];
			int vantage_index = 0;
			boolean v_seen = false;

			// age, x, y, dx, dy
			int sprk = 0;
			double[] particles = new double[600];

			// game
			boolean game_over = false;
			boolean dawn = false;
			long nextFrameStart = 0;
			
			// breathing
			int ticksUntilBreath = 0;
			boolean in = true;

			// map
			// y 0, 4, 2, 4, 5, 9, 10, 12, 7 , 5 , 0 , 14
			// x 0, 3, 7, 7, 9, 5, 10, 14, 13, 15, 18, 6
			int[][] t_type = new int[T_H][T_W]; /*{
			//   0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18
				{G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G}, // 0
				{G, W, I, W, I, W, W, W, W, W, W, W, G, G, G, G, G, G, G}, // 1
				{G, W, _, _, _, _, O, _, _, _, B, W, G, G, W, W, W, W, G}, // 2
				{G, O, _, T, _, B, W, W, W, W, W, W, W, G, W, _, _, W, G}, // 3
				{G, W, _, _, _, _, O, _, W, _, _, _, W, G, W, B, _, W, G}, // 4
				{G, W, W, D, W, W, W, _, W, _, _, _, I, G, W, _, _, W, G}, // 5
				{G, W, _, _, _, _, W, _, O, _, E, _, W, G, W, O, W, W, G}, // 6
				{G, I, _, C, _, _, W, _, W, _, T, _, I, G, G, G, G, F, G}, // 7
				{G, W, _, T, _, B, W, _, W, _, _, _, W, G, G, G, G, G, G}, // 8
				{G, W, _, C, R, _, O, _, W, W, D, W, W, G, G, G, G, G, G}, // 9
				{G, I, _, _, _, _, W, _, O, _, _, W, W, W, I, W, G, G, G}, // 10
				{G, W, _, B, _, _, W, R, W, T, _, D, _, _, _, W, F, G, G}, // 11
				{G, W, W, W, I, W, W, O, W, _, _, W, _, B, _, O, G, G, G}, // 12
				{G, G, G, G, G, G, G, G, W, I, W, W, W, W, W, W, G, G, G}, // 13
				{G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G}, // 14
			};*/
			byte[][] t_hp = new byte[T_H][T_W];
			
			String map = "11111111111111111111bababbbbbbb11111111b000020006b11bbbb1120406bbbbbbb1b00b11b000020b000b1b60b11bbcbbb0b000a1b00b11b0000b02080b1b2bb11a0500b0b040a1111911b0406b0b000b1111111b053020bbcbb1111111a0000b0200bbbab1111b0600b3b40c000b9111bbbabb2b00b060211111111111babbbbbb1111111111111111111111";

			// Setup
			for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
				t_type[y][x] = Integer.parseInt(map.substring(y * T_W + x, y * T_W + x + 1), 16);
				t_hp[y][x] = T_TO_HP[t_type[y][x]];
			}}
			
			// Place players
			int off = r.nextInt(8) + 1;
			b_y = Y_VANTAGES[off] + 0.5;
			b_x = X_VANTAGES[off] + 0.5;
			v_b_y = b_y;
			v_b_x = b_x;
			off += r.nextBoolean() ? -1 : 1;
			v_y = Y_VANTAGES[off];
			v_x = X_VANTAGES[off];
			
			while (true) {
				nextFrameStart = System.nanoTime() + 40000000l;
				if (!playing) {
					if (key[KeyEvent.VK_SPACE]) { playing = true; }
				} else {
					if (!game_over) {
						tick++;
						b_cooldown--;
						v_cooldown--;
						if (v_dmg > 0) {
							v_dmg--;
						}
						msg = "";
						
						// breathing
						if (ticksUntilBreath-- == 0) {
							sound.Sound.play(in ? "in" : "out", -1.2f + (b_fatigue + b_exhaustion) * 0.00045f);
							in = !in;
							ticksUntilBreath = 12 + 25 * (MAX_FATIGUE - b_fatigue) / MAX_FATIGUE;
						}
						
						// b movement
						double sp = b_fatigue > WINDED_FATIGUE ? B_WALK_SPEED : B_RUN_SPEED;
						boolean mv = false;
						for (int i = 0; i < 4; i++) {
							if (key[DIR_KEYS[i]] || key[KeyEvent.VK_LEFT + i]) {
								b_y += Y_DIRS[i] * sp;
								b_x += X_DIRS[i] * sp;
								// Borders
								b_y = b_y < P_R ? P_R : b_y;
								b_y = b_y > (T_H - P_R) ? (T_H - P_R) : b_y;
								b_x = b_x < P_R ? P_R : b_x;
								b_x = b_x > (T_W - P_R) ? (T_W - P_R) : b_x;
								// Solid things
								if (t_type[(int) b_y][(int) b_x] >= SOLIDS && t_type[(int) b_y][(int) b_x] < TRANSPARENTS) {
									if (b_push > 12) {
										sound.Sound.play("shift", 0.1f);
										int ny = ((int) b_y) + Y_DIRS[i];
										int nx = ((int) b_x) + X_DIRS[i];
										if (nx >= 0 && ny >= 0 && nx < T_W && ny < T_H && t_type[ny][nx] <= G && !(ny == (int) v_y && nx == (int) v_x)) {
											int t = t_type[ny][nx];
											t_type[ny][nx] = t_type[(int) b_y][(int) b_x];
											t_hp[ny][nx] = t_hp[(int) b_y][(int) b_x];
											t_type[(int) b_y][(int) b_x] = t;
											t_hp[(int) b_y][(int) b_x] = 0;
											b_push = 0;
										}
									} else {
										b_push++;
										msg = "Pushing...";
									}
								} else {
									b_push = 0;
								}
								b_y = t_type[(int) b_y][(int) b_x] >= SOLIDS ? b_y - Y_DIRS[i] * sp : b_y;
								b_x = t_type[(int) b_y][(int) b_x] >= SOLIDS ? b_x - X_DIRS[i] * sp : b_x;
								mv = true;
							}
						}

						if (mv && b_fatigue <= MAX_FATIGUE) {
							b_fatigue++;
							b_exhaustion++;
						}
						if (!mv && b_fatigue > b_exhaustion / 50) {
							b_fatigue--;
						}

						// Interaction
						for (int i = 0; i < 4; i++) {
							int ny = ((int) b_y) + Y_DIRS[i];
							int nx = ((int) b_x) + X_DIRS[i];
							if (nx < 0 || ny < 0 || nx >= T_W || ny >= T_H) { continue; }
							switch (t_type[ny][nx]) {
								case B:
									msg = "Hold down E to search drawers.";
									if (key[KeyEvent.VK_E]) {
										if (b_push >= 60) {
											b_push = 0;
											t_type[ny][nx] = X;
											int found = r.nextInt(3);
											if (r.nextInt(lvl + 4) > 3 || inventory[found]) {
												msg2 = "You found nothing.";
											} else {
												if (found == LEATHER_JACKET) {
													sound.Sound.play("jacketget", 0.0f);
												}
												if (found == KEY) {
													sound.Sound.play("key", 0.0f);
												}
												msg2 = "You found a " + ITEM_NAMES[found] + "!";
												inventory[found] = true;
											}
											msgWait = 100;
										} else {
											if (b_push == 0) {
												sound.Sound.play("drawer", 0.2f);
											}
											msg = "Searching...";
											b_push++;
										}
									}
									break;
								case O:
									if (inventory[KEY]) {
										msg = "Press space to lock door.";
										if (b_cooldown <= 0 && key[KeyEvent.VK_SPACE]) {
											t_type[ny][nx] = D;
											b_cooldown = B_COOLDOWN;
											sound.Sound.play("doorclose", 0.0f);
										}
									}
									break;
								case D:
									if (inventory[KEY]) {
										msg = "Press space to open door.";
										if (b_cooldown <= 0 && key[KeyEvent.VK_SPACE]) {
											t_type[ny][nx] = O;
											b_cooldown = B_COOLDOWN;
											sound.Sound.play("dooropen", 0.0f);
										}
									}
									break;
							}
						}

						// V-map update
						for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
							v_map[y][x] = 100000;
						}}
						LinkedList<Point> queue = new LinkedList<Point>();
						v_map[(int) v_b_y][(int) v_b_x] = 0;
						queue.add(new Point((int) v_b_x, (int) v_b_y));
						while (!queue.isEmpty()) {
							Point p = queue.pop();
							for (int i = 0; i < 4; i++) {
								int py2 = p.y + Y_DIRS[i];
								int px2 = p.x + X_DIRS[i];
								if (
									py2 >= 0 &&
									px2 >= 0 &&
									py2 < T_H &&
									px2 < T_W)
								{
									int newV = v_map[p.y][p.x] + 1 + (t_type[py2][px2] >= SOLIDS ? t_hp[py2][px2] : 0);
									if (newV < v_map[py2][px2]) {
										v_map[py2][px2] = newV;
										queue.add(new Point(px2, py2));
									}
								}
							}
						}

						// V movement
						int dir = -1;
						int least = v_map[((int) v_y)][((int) v_x)];
						//sp = v_dmg > 0 ? V_HURT_SPEED : V_SPEED;
						sp = V_SPEED - v_dmg * v_dmg * 0.00000020;
						for (int i = 0; i < 8; i++) {
							int ny = ((int) v_y) + Y_DIRS[i];
							int nx = ((int) v_x) + X_DIRS[i];
							int ny2 = (int) (v_y + Y_DIRS[i] * sp);
							int nx2 = (int) (v_x + X_DIRS[i] * sp);
							if (nx < 0 || ny < 0 || nx >= T_W || ny >= T_H) { continue; }
							// Prevent slipping through diagonal gaps.
							if (t_type[ny2][nx2] < SOLIDS && t_type[ny2][(int) v_x] >= SOLIDS && t_type[(int) v_y][nx2] >= SOLIDS) {
								continue;
							}

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
						double dist = (b_y - v_y) * (b_y - v_y) + (b_x - v_x) * (b_x - v_x);
						if (dir == -1) {
							if (dist > P_R * P_R) {
								dy = (b_y - v_y) / dist * sp;
								dx = (b_x - v_x) / dist * sp;
							}
							if (!v_seen) {
								// don't know where b is, pick vantage point
								v_b_y = Y_VANTAGES[vantage_index % 12];
								v_b_x = X_VANTAGES[(vantage_index++) % 12];
							}
						} else {
							dy = Y_DIRS[dir] * sp;
							dx = X_DIRS[dir] * sp;
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
								if (t_type[(int) v_y][(int) v_x] == I) {
									sound.Sound.play("window", 0.2f);
								} else {
									sound.Sound.play("punch", 0.0f);
								}
								
								t_hp[(int) v_y][(int) v_x] -= v_dmg > 0 ? 1 : 2;
								for (int i = 20; i < 40; i++) {
									particles[i * 5] = r.nextDouble() * 5;
									particles[i * 5 + 1] = ((int) v_x) * TILE_SIZE + TILE_SIZE / 2;
									particles[i * 5 + 2] = ((int) v_y) * TILE_SIZE + TILE_SIZE / 2;
									particles[i * 5 + 3] = r.nextDouble() * 4 - 2;
									particles[i * 5 + 4] = r.nextDouble() * 4 - 2;
								}
								v_cooldown = V_COOLDOWN;
								if (t_hp[(int) v_y][(int) v_x] <= 0) {
									t_type[(int) v_y][(int) v_x] = _;
								}
							}
							v_y -= dy;
							v_x -= dx;
						}

						if (dist < P_R * 2 && v_cooldown <= 0) {
							v_cooldown = 20;
							off = 80;
							sound.Sound.play("punch", 0.4f);
							if (inventory[LEATHER_JACKET] && jacket_hp > 0) {
								jacket_hp--;
								off = 40;
							} else {
								b_fatigue += v_dmg > 0 ? 50 : 100;
								b_exhaustion += v_dmg > 0 ? 50 : 100;
							}
							for (int i = off; i < off + 40; i++) {
								particles[i * 5] = r.nextDouble() * 4;
								particles[i * 5 + 1] = b_x * TILE_SIZE + 1;
								particles[i * 5 + 2] = b_y * TILE_SIZE - 5;
								particles[i * 5 + 3] = r.nextDouble() * 2 - 1;
								particles[i * 5 + 4] = r.nextDouble() * 3 - 1;
							}
							if (b_fatigue >= MAX_FATIGUE) {
								game_over = true;
								msg2 = "GAME OVER";
								msgWait = 100;
							}
						}

						if (tick % 1500 == 0) {
							msg2 = ((3000 * lvl - tick) / 1500) + " minutes until dawn";
							msgWait = 200;
						}

						if (tick > 3000 * lvl) {
							game_over = true;
							msg2 = "VICTORY!";
							dawn = true;
							msgWait = 300;
							lvl *= 2;
							for (int i = 0; i < 80; i++) {
								particles[i * 5] = r.nextDouble() * 10;
								particles[i * 5 + 1] = v_x * TILE_SIZE;
								particles[i * 5 + 2] = v_y * TILE_SIZE;
								particles[i * 5 + 3] = r.nextDouble() * 10 - 5;
								particles[i * 5 + 4] = r.nextDouble() * 10 - 5;
							}
						}
					}
				}
				
				double ptr = Math.atan2(b_y * TILE_SIZE - my, b_x * TILE_SIZE - mx) + Math.PI;

				// Graphics
				Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
				g.setColor(new Color(0, 0, 30));
				g.fillRect(0, 0, 800, 600);
				Polygon p = new Polygon();
				v_seen = false;
				for (double d = 0.001; d < Math.PI * 2; d += Math.PI / 2000) {
					double y = b_y;
					double x = b_x;
					double d_y = Math.sin(d);
					double d_x = Math.cos(d);
					boolean blocked = false;
					while (true) {
						// v vision
						if ((int) v_x == (int) x && (int) v_y == (int) y) {
							v_b_x = b_x;
							v_b_y = b_y;
							v_seen = true;
							vantage_index = tick;
						}
						double yDist = (d_y < 0 ? Math.ceil(y - 1) : Math.floor(y + 1)) - y;
						double xDist = (d_x < 0 ? Math.ceil(x - 1) : Math.floor(x + 1)) - x;
						if (Math.abs(yDist / d_y) < Math.abs(xDist / d_x)) {
							x += (yDist / d_y * d_x) * 1.001;
							y += yDist * 1.001;
						} else {
							x += xDist * 1.001;
							y += (xDist / d_x * d_y) * 1.001;
						}
						
						if ((int) x < 0 || (int) y < 0 || (int) x >= T_W || (int) y >= T_H) { break; }
						// sparkles & shooting & things
						if ((bullets > 0 && inventory[GUN]) && Math.abs(ptr - d) < Math.PI / 1000 && !blocked) {
							if (r.nextInt(16) == 0) {
								double offset = r.nextDouble() * 40;
								particles[sprk * 5] = 1;
								particles[sprk * 5 + 1] = x * TILE_SIZE + offset * d_x;
								particles[sprk * 5 + 2] = y * TILE_SIZE + offset * d_y;
								particles[sprk * 5 + 3] = 0;
								particles[sprk * 5 + 4] = 0;
								sprk = (sprk + 1) % 20;
							}
							if (inventory[GUN]) {
								if (t_type[(int) y][(int) x] > SOLIDS || (int) y == (int) v_y && (int) x == (int) v_x) {
									blocked = true;
									if (bullets > 0 && click && b_cooldown <= 0) {
										sound.Sound.play("gun", 1.0f);
										b_cooldown = B_COOLDOWN;
										int p_start = 40;
										if (t_type[(int) y][(int) x] > SOLIDS) {
											t_hp[(int) y][(int) x] -= GUN_DMG;
											if (t_hp[(int) y][(int) x] <= 0) {
												t_type[(int) y][(int) x] = t_type[(int) y][(int) x] == F ? G : _;
											}
										}
										if ((int) y == (int) v_y && (int) x == (int) v_x) {
											v_dmg = GUN_V_DMG;
											p_start = 80;
										}
										for (int i = p_start; i < p_start + 40; i++) {
											particles[i * 5] = r.nextDouble() * 5;
											particles[i * 5 + 1] = x * TILE_SIZE + d_x;
											particles[i * 5 + 2] = y * TILE_SIZE + d_y;
											particles[i * 5 + 3] = r.nextDouble() * 4 - 2;
											particles[i * 5 + 4] = r.nextDouble() * 4 - 2;
										}
										bullets--;
									}
								}
							}
						}
						if (t_type[(int) y][(int) x] > TRANSPARENTS) { break; }
					}
					p.addPoint((int) ((x + d_x * 0.2) * TILE_SIZE), (int) ((y + d_y * 0.2) * TILE_SIZE));
				}
				click = false;
				if (!dawn) { g.setClip(p); }
				g.setColor(new Color(37, 59, 29));
				g.fillRect(0, 0, 800, 600);
				/*for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
					if (t_type[y][x] == G || t_type[y][x] == F) {
						g.setColor(new Color(37, 59, 29));
						g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
					}
				} }*/
				g.setColor(new Color(44, 70, 34));
				for (int y = 0; y < T_H; y++) { for (int x = 0; x < T_W; x++) {
					if (t_type[y][x] == I) {
						g.fillOval(x * TILE_SIZE - 100, y * TILE_SIZE - 100, 240, 240);
					}
				}}
				for (int y = 0; y < T_H; y++) { lp: for (int x = 0; x < T_W; x++) {
					Color c = null;
					switch (t_type[y][x]) {
						case O:
						case D:
						case T:
						case C:
						case B:
						case X:
						case E:
						case R:
						case _: c = new Color(59, 39, 29); break;
						case F: break;
						case G: continue lp;
						case I:
						case W: c = new Color(101, 81, 72); break;
					}
					g.setColor(c);
					int yTile = y * TILE_SIZE;
					int xTile = x * TILE_SIZE;
					if (t_type[y][x] != F) { g.fillRect(xTile, yTile, TILE_SIZE, TILE_SIZE); }
					switch (t_type[y][x]) {
						case F:
							g.setColor(new Color(175, 85, 58));
							g.fillPolygon(new int[] {xTile + 15, xTile + 25, xTile + 23, xTile + 17}, new int[] {yTile + 17, yTile + 17, yTile + 31, yTile + 31}, 4);
							//g.fillRect(xTile + 15, yTile + 15, 10, 16);
							g.setColor(Color.RED);
							g.fillOval(xTile + 14, yTile + 13, 5, 5);
							g.fillOval(xTile + 22, yTile + 14, 6, 6);
							g.fillOval(xTile + 18, yTile + 12, 5, 5);
							//g.fillOval(xTile + 17, yTile + 7, 5, 5);
							g.fillOval(xTile + 21, yTile + 9, 5, 5);
							break;
						case R:
							g.setColor(new Color(100, 15, 10));
							g.fillRoundRect(xTile + 2, yTile + 2, 36, 36, 8, 8);
							g.setColor(new Color(20, 25, 150));
							g.fillRect(xTile + 8, yTile + 8, 24, 24);
							g.setColor(new Color(150, 150, 120));
							g.fillPolygon(new int[] {xTile + 20, xTile + 36, xTile + 20, xTile + 4}, new int[] {yTile + 4, yTile + 20, yTile + 36, yTile + 20}, 4);
							break;
						case E:
							g.setColor(Color.WHITE);
							g.fillRect(xTile + 2, yTile + 18, 36, 6);
							g.setColor(new Color(142, 130, 123));
							g.fillRect(xTile + 2, yTile + 10, 2, 20);
							g.fillRect(xTile + 36, yTile + 16, 2, 14);
							break;
						case T:
							g.setColor(new Color(142, 130, 123));
							g.fillRect(xTile + 5, yTile + 10, 30, 2);
							g.fillRect(xTile + 8, yTile + 10, 2, 20);
							g.fillRect(xTile + 30, yTile + 10, 2, 20);
							break;
						case C:
							g.setColor(new Color(142, 130, 123));
							g.fillRect(xTile + 15, yTile + 17, 10, 2);
							g.fillRect(xTile + 15, yTile + 7, 2, 20);
							g.fillRect(xTile + 25, yTile + 17, 2, 10);
							break;
						case D:
							g.setColor(new Color(194, 177, 168));
							g.fillRect(xTile, yTile, TILE_SIZE, TILE_SIZE);
						case O:
							g.setColor(new Color(87, 63, 51));
							g.fillRect(xTile, yTile, 5, TILE_SIZE);
							g.fillRect(xTile + 35, yTile, 5, TILE_SIZE);
							break;
						case I:
							g.setColor(new Color(133, 133, 176));
							g.fillRect(xTile + 2, yTile + 2, 16, 16);
							g.fillRect(xTile + 22, yTile + 2, 16, 16);
							g.fillRect(xTile + 2, yTile + 22, 16, 16);
							g.fillRect(xTile + 22, yTile + 22, 16, 16);
							break;
						case B:
						case X:
							g.setColor(new Color(115, 63, 45));
							g.fillRect(xTile + 2, yTile + 6, 36, 28);
							g.setColor(new Color(57, 32, 22));
							g.fillRect(xTile + 4, yTile + 8, 32, 10);
							g.fillRect(xTile + 4, yTile + 20, 32, 10);
					}
					if (t_type[y][x] == B) {
						g.setColor(new Color(142, 130, 123));
						g.fillRect(xTile + 5, yTile + 9, 30, 8);
						g.fillRect(xTile + 5, yTile + 21, 30, 8);
					}
				}}
				// players
				if (!dawn) {
					g.setColor(Color.BLACK);
					g.fillRect((int) (v_x * TILE_SIZE) - 7, (int) (v_y * TILE_SIZE) - 5, 14, 19);
					g.fillRect((int) (b_x * TILE_SIZE) - 1, (int) (b_y * TILE_SIZE) - 14, 6, 10);
					g.setColor(Color.WHITE);
					g.fillOval((int) (v_x * TILE_SIZE) - 4, (int) (v_y * TILE_SIZE) - 12, 9, 9);
					g.fillOval((int) (b_x * TILE_SIZE) - 3, (int) (b_y * TILE_SIZE) - 12, 7, 6);
					g.fillRect((int) (b_x * TILE_SIZE) - 3, (int) (b_y * TILE_SIZE) - 4, 1, 8);
					g.fillRect((int) (b_x * TILE_SIZE) + 4, (int) (b_y * TILE_SIZE) - 4, 1, 8);
					// body
					g.setColor(Color.RED);
					g.fillRect((int) (b_x * TILE_SIZE) - 2, (int) (b_y * TILE_SIZE) - 3, 6, 18);
					/*g.fillRect((int) (b_x * TILE_SIZE) - 2, (int) (b_y * TILE_SIZE) - 3, 6, 8);
					g.fillRect((int) (b_x * TILE_SIZE) - 2, (int) (b_y * TILE_SIZE) + 2, 2, 10);
					g.fillRect((int) (b_x * TILE_SIZE) + 2, (int) (b_y * TILE_SIZE) + 2, 2, 10);*/
					if (v_dmg > 0) {
						g.fillOval((int) (v_x * TILE_SIZE), (int) (v_y * TILE_SIZE), 3, 7);
					}
					if (inventory[LEATHER_JACKET] && jacket_hp > 0) {
						g.setColor(Color.BLACK);//new Color(145, 92, 54));
						g.fillRect((int) (b_x * TILE_SIZE) - 4, (int) (b_y * TILE_SIZE) - 5, 10, 8);
						/*g.fillRect((int) (b_x * TILE_SIZE) - 1, (int) (b_y * TILE_SIZE) - 6, 4, 2);
						g.fillRect((int) (b_x * TILE_SIZE) - 4, (int) (b_y * TILE_SIZE) - 4, 10, 7);*/
					}
					//g.fillOval((int) ((v_x - P_R) * TILE_SIZE), (int) ((v_y - P_R) * TILE_SIZE), (int) (2 * P_R * TILE_SIZE), (int) (2 * P_R * TILE_SIZE));
					//if (!(v_vs_b && b_fatigue % 19 == 0)) { g.setColor(Color.GREEN); }
					//g.fillOval((int) ((b_x - P_R) * TILE_SIZE), (int) ((b_y - P_R) * TILE_SIZE), (int) (2 * P_R * TILE_SIZE), (int) (2 * P_R * TILE_SIZE));
				}
				if (dawn) {
					g.setColor(new Color(255, 255, 150, 70));
					g.fillRect(0, 0, 800, 600);
				}
				g.setClip(0, 0, 800, 600);
				g.setColor(Color.YELLOW);
				// qqDPS
				//g.drawOval((int) ((v_x - P_R) * TILE_SIZE), (int) ((v_y - P_R) * TILE_SIZE), (int) (2 * P_R * TILE_SIZE), (int) (2 * P_R * TILE_SIZE));
				// particles
				for (int i = 0; i < 120; i++) {
					if (i == 80) { g.setColor(Color.RED); }
					if (particles[i * 5] > 0) {
						g.fillOval((int) particles[i * 5 + 1], (int) particles[i * 5 + 2], (int) particles[i * 5] + 2, (int) particles[i * 5] + 2);
						particles[i * 5 + 1] += particles[i * 5 + 3];
						particles[i * 5 + 2] += particles[i * 5 + 4];
						particles[i * 5]     -= 0.3;
					}
				}
				g.setColor(Color.YELLOW);
				//g.setFont(new Font("Verdana", Font.PLAIN, 20));
				g.fillRect(760, 600 - b_fatigue / 4, 40, b_fatigue / 4);
				g.setColor(Color.ORANGE);
				g.fillRect(760, 375, 40, 1);
				if (b_fatigue > WINDED_FATIGUE) {
					g.fillRect(760, 600 - b_fatigue / 4, 40, (b_fatigue - WINDED_FATIGUE) / 4);
				}
				
				// inventory
				g.setColor(Color.LIGHT_GRAY);
				if (inventory[KEY]) {
					g.fillOval(765, 15, 8, 10);
					g.fillRect(769, 19, 25, 2);
					g.fillRect(786, 19, 2, 6);
					g.fillRect(790, 19, 2, 8);
				}
				// leather jacket
				if (inventory[LEATHER_JACKET] && jacket_hp > 0) {
					g.drawRect(775, 85, 10, 4);
					g.drawRect(770, 89, 20, 24);
					g.drawRect(765, 92, 3, 18);
					g.drawRect(792, 92, 3, 18);
				}
				// gun
				if (inventory[GUN]) {
					g.fillRect(765, 53, 30, 6);
					g.drawString("" + bullets, 782, 76);
					g.setColor(new Color(115, 63, 45));
					g.fillRect(767, 59, 8, 14);
				}
				
				g.setColor(Color.WHITE);
				g.fillRect(760, 600 - b_exhaustion / 50, 40, b_exhaustion / 50);
				g.drawString(msg, 40, 280);
				g.drawString(msg2, 40, 320);
				strategy.show();
				try { Thread.sleep((nextFrameStart - System.nanoTime()) / 1000000); } catch (Exception e) {}
				if (--msgWait == 0) {
					msg2 = "";
					if (game_over) {
						continue game;
					}
				}
			}
		}
	}
}