package core;

import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import java.io.*;
import java.applet.*;
import java.net.*;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.util.StringTokenizer;
import javax.sound.sampled.*;

/**
 * Subclasses of this class implement the serial communcation (Game Link)
 * interface
 */

abstract class GameLink {
	boolean serverRunning = false;

	abstract void send(byte b);

	abstract void shutDown();

	abstract void setDmgcpu(Dmgcpu d);
}