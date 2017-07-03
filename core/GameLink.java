package core;

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