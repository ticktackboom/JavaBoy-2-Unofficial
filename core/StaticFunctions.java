package core;

public class StaticFunctions {

	private static final String hexChars = "0123456789ABCDEF";

	/** Output a debugger command list to the console */
	public static void displayDebuggerHelp() {
		System.out.println("Enter a command followed by it's parameters (all values in hex):");
		System.out.println("?                     Display this help screen");
		System.out.println("c [script]            Execute _c_ommands from script file [default.scp]");
		System.out.println("s                     Re_s_et CPU");
		System.out.println("r                     Show current register values");
		System.out.println("r reg val             Set value of register reg to value val");
		System.out.println("e addr val [val] ...  Write values to RAM / ROM starting at address addr");
		System.out.println("d addr len            Hex _D_ump len bytes starting at addr");
		System.out.println("i addr len            D_i_sassemble len instructions starting at addr");
		System.out.println("p len                 Disassemble len instructions starting at current PC");
		System.out.println("n                     Show interrupt state");
		System.out.println("n 1|0                 Enable/disable interrupts");
		System.out.println("t [len]               Execute len instructions starting at current PC [1]");
		System.out.println("g                     Execute forever");
		System.out.println("o                     Output Gameboy screen to applet window");
		System.out.println("b addr                Set breakpoint at addr");
		System.out.println("k [keyname]           Toggle Gameboy key");
		System.out.println("m bank                _M_ap to ROM bank");
		System.out.println("m                     Display current ROM mapping");
		System.out.println("q                     Quit debugger interface");
		System.out.println("<CTRL> + C            Quit JavaBoy");
	}
	
	

	/** Returns a string representation of an 16-bit number in hexadecimal */
	static public String hexWord(int w) {
		return new String(hexByte((w & 0x0000FF00) >> 8) + hexByte(w & 0x000000FF));
	}

	/** Outputs a line of debugging information */
	static public void debugLog(String s) {
		System.out.println("Debug: " + s);
	}

	/**
	 * Returns the unsigned value (0 - 255) of a signed byte.
	 * 
	 * Java doesn't have unsigned types for data store, so, we need a function to
	 * make the conversion from an unsigned number to the signed representation.
	 */
	static public short unsign(byte b) {
		return (short) (b & 0xFF);
	}

	/**
	 * Returns the unsigned value (0 - 255) of a signed 8-bit value stored in a
	 * short.
	 * 
	 * Problem detected: Java doesn't detect error when overflow, so, values lower
	 * and higher of a byte value will produce signed results.
	 * 
	 * But this function will never be called with an overflow value, because all
	 * data parsed came from gameboy.
	 */
	static public short unsign(short b) {
		if (b < 0) {
			return (short) (256 + b);
		} else {
			return b;
		}
	}
	
	/** Returns a string representation of an 8-bit number in hexadecimal */
	static public String hexByte(int b) {
		String s = new Character(hexChars.charAt(b >> 4)).toString();
		s = s + new Character(hexChars.charAt(b & 0x0F)).toString();

		return s;
	}

}
