package core;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

/**
 * This class is the master class for implementations of the graphics class. A
 * graphics implementation will subclass from this class. It contains methods
 * for calculating the frame rate.
 */

abstract class GraphicsChip {
	/** GameBoy resolution */
	static final int SCREEN_WIDTH = 160;
	static final int SCREEN_HEIGHT = 144;
	
	/** Tile uses the background palette */
	static final int TILE_BKG = 0;
	/** Tile uses the first sprite palette */
	static final int TILE_OBJ1 = 4;
	/** Tile uses the second sprite palette */
	static final int TILE_OBJ2 = 8;
	/** Tile is flipped horizontally */
	static final int TILE_FLIPX = 1;
	/** Tile is flipped vertically */
	static final int TILE_FLIPY = 2;

	/** The current contents of the video memory, mapped in at 0x8000 - 0x9FFF */
	byte[] videoRam = new byte[0x8000];

	/** The background palette */
	GameboyPalette backgroundPalette;

	/** The first sprite palette */
	GameboyPalette obj1Palette;

	/** The second sprite palette */
	GameboyPalette obj2Palette;
	GameboyPalette[] gbcBackground = new GameboyPalette[8];
	GameboyPalette[] gbcSprite = new GameboyPalette[8];

	boolean spritesEnabled = true;

	boolean bgEnabled = true;
	boolean winEnabled = true;

	/** The image containing the Gameboy screen */
	Image backBuffer;

	/** The current frame skip value */
	int frameSkip = 2;

	/**
	 * The number of frames that have been drawn so far in the current frame
	 * sampling period
	 */
	int framesDrawn = 0;

	/** Image magnification */
	int mag = 2;
	int width = SCREEN_WIDTH * mag;
	int height = SCREEN_HEIGHT * mag;

	/** Amount of time to wait between frames (ms) */
	int frameWaitTime = 0;

	/** The current frame has finished drawing */
	boolean frameDone = false;
	int averageFPS = 0;
	long startTime = 0;

	/** Selection of one of two addresses for the BG and Window tile data areas */
	boolean bgWindowDataSelect = true;

	/** If true, 8x16 sprites are being used. Otherwise, 8x8. */
	boolean doubledSprites = false;

	/** Selection of one of two address for the BG tile map. */
	boolean hiBgTileMapAddress = false;
	Dmgcpu dmgcpu;
	Component applet;
	int tileStart = 0;
	int vidRamStart = 0;

	/** Create a new GraphicsChip connected to the speicfied CPU */
	public GraphicsChip(Component a, Dmgcpu d) {
		dmgcpu = d;

		backgroundPalette = new GameboyPalette(0, 1, 2, 3);
		obj1Palette = new GameboyPalette(0, 1, 2, 3);
		obj2Palette = new GameboyPalette(0, 1, 2, 3);

		for (int r = 0; r < 8; r++) {
			gbcBackground[r] = new GameboyPalette(0, 1, 2, 3);
			gbcSprite[r] = new GameboyPalette(0, 1, 2, 3);
		}

		backBuffer = a.createImage(SCREEN_WIDTH * mag, SCREEN_HEIGHT * mag);
		applet = a;
	}

	/** Set the magnification for the screen */
	public void setMagnify(int m) {
		mag = m;
		width = m * SCREEN_WIDTH;
		height = m * SCREEN_HEIGHT;
		if (backBuffer != null)
			backBuffer.flush();
		backBuffer = applet.createImage(SCREEN_WIDTH * mag, SCREEN_HEIGHT * mag);
	}

	/** Clear up any allocated memory */
	public void dispose() {
		backBuffer.flush();
	}

	/** Calculate the number of frames per second for the current sampling period */
	public void calculateFPS() {
		if (startTime == 0) 
			startTime = System.currentTimeMillis();
		
		if (framesDrawn > 30) {
			long delay = System.currentTimeMillis() - startTime;
			averageFPS = (int) ((framesDrawn) / (delay / 1000f));
			startTime = System.currentTimeMillis();
			int timePerFrame;

			if (averageFPS != 0) 
				timePerFrame = 1000 / averageFPS;
			 else 
				timePerFrame = 100;
			
			frameWaitTime = 17 - timePerFrame + frameWaitTime;
			framesDrawn = 0;
		}
	}

	/**
	 * Return the number of frames per second achieved in the previous sampling
	 * period.
	 */
	public int getFPS() {
		return averageFPS;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	abstract public short addressRead(int addr);

	abstract public void addressWrite(int addr, byte data);

	abstract public void invalidateAll(int attribs);

	abstract public boolean draw(Graphics g, int startX, int startY, Component a);

	abstract public void notifyScanline(int line);

	abstract public void invalidateAll();

	abstract public boolean isFrameReady();
}
