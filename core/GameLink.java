package core;

/**
 * Subclasses of this class implement the serial communcation (Game Link)
 * interface
 * 
 * Clase abstracta que define cómo debe ser una clase relacionada con la
 * comunicación con Cable Link.
 * 
 * Todas las clases que implementen periféricos conectados por GameLink deben
 * heredar de esta clase e implementar los métodos.
 */

abstract class GameLink {
	boolean serverRunning = false;

	abstract void send(byte b);

	abstract void shutDown();

	abstract void setDmgcpu(Dmgcpu d);
}