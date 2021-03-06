package core;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class represents the game cartridge and contains methods to load the ROM
 * and battery RAM (if necessary) from disk or over the web, and handles
 * emulation of ROM mappers and RAM banking. It is missing emulation of MBC3
 * (this is very rare).
 */

public class Cartucho {
	/**
	 * Translation between ROM size byte contained in the ROM header, and the number
	 * of 16Kb ROM banks the cartridge will contain
	 */
	final int[][] romSizeTable = { { 0, 2 }, { 1, 4 }, { 2, 8 }, { 3, 16 }, { 4, 32 }, { 5, 64 }, { 6, 128 },
			{ 7, 256 }, { 0x52, 72 }, { 0x53, 80 }, { 0x54, 96 } };

	/**
	 * Contains strings of the standard names of the cartridge mapper chips, indexed
	 * by cartridge type
	 */
	final String[] cartTypeTable = { "ROM Only", /* 00 */
			"ROM+MBC1", /* 01 */
			"ROM+MBC1+RAM", /* 02 */
			"ROM+MBC1+RAM+BATTERY", /* 03 */
			"Unknown", /* 04 */
			"ROM+MBC2", /* 05 */
			"ROM+MBC2+BATTERY", /* 06 */
			"Unknown", /* 07 */
			"ROM+RAM", /* 08 */
			"ROM+RAM+BATTERY", /* 09 */
			"Unknown", /* 0A */
			"Unsupported ROM+MMM01", /* 0B */
			"Unsupported ROM+MMM01+SRAM", /* 0C */
			"Unsupported ROM+MMM01+SRAM+BATTERY", /* 0D */
			"Unknown", /* 0E */
			"ROM+MBC3+TIMER+BATTERY", /* 0F */
			"ROM+MBC3+TIMER+RAM+BATTERY", /* 10 */
			"ROM+MBC3", /* 11 */
			"ROM+MBC3+RAM", /* 12 */
			"ROM+MBC3+RAM+BATTERY", /* 13 */
			"Unknown", /* 14 */
			"Unknown", /* 15 */
			"Unknown", /* 16 */
			"Unknown", /* 17 */
			"Unknown", /* 18 */
			"ROM+MBC5", /* 19 */
			"ROM+MBC5+RAM", /* 1A */
			"ROM+MBC5+RAM+BATTERY", /* 1B */
			"ROM+MBC5+RUMBLE", /* 1C */
			"ROM+MBC5+RUMBLE+RAM", /* 1D */
			"ROM+MBC5+RUMBLE+RAM+BATTERY" /* 1E */ };

	/** Compressed file types */
	final byte bNotCompressed = 0;
	final byte bZip = 1;
	final byte bJar = 2;
	final byte bGZip = 3;

	/** RTC Reg names */
	final byte SECONDS = 0;
	final byte MINUTES = 1;
	final byte HOURS = 2;
	final byte DAYS_LO = 3;
	final byte DAYS_HI = 4;

	/** Contains the complete ROM image of the cartridge */
	public byte[] rom;

	/** Contains the RAM on the cartridge */
	public byte[] ram = new byte[0x10000];

	/** Number of 16Kb ROM banks */
	int numBanks;

	/** Cartridge type - index into cartTypeTable[][] */
	int cartType;

	/** Starting address of the ROM bank at 0x4000 in CPU address space */
	int pageStart = 0x4000;

	/** The bank number which is currently mapped at 0x4000 in CPU address space */
	int currentBank = 1;

	/**
	 * The bank which has been saved when the debugger changes the ROM mapping. The
	 * mapping is restored from this register when execution resumes
	 */
	int savedBank = -1;

	/**
	 * The RAM bank number which is currently mapped at 0xA000 in CPU address space
	 */
	int ramBank;
	int ramPageStart;

	boolean mbc1LargeRamMode = false;
	boolean ramEnabled, disposed = false;
	Component applet;

	/** The filename of the currently loaded ROM */
	String romFileName;

	String cartName;

	boolean cartridgeReady = false;

	boolean needsReset = false;

	/** Real time clock registers. Only used on MBC3 */
	int[] RTCReg = new int[5];
	long realTimeStart;
	long lastSecondIncrement;
	String romIntFileName;

	/**
	 * Create a cartridge object, loading ROM and any associated battery RAM from
	 * the cartridge filename given.
	 */
	public Cartucho(String romFileName, Component a) {
		/*
		 * Cartucho recibe como parámetros la ruta del fichero ROM y el componente gráfico en el
		 * que debe dibujar la salida de vídeo.
		 */
		
		applet = a; /* 5823 */
		
		this.romFileName = romFileName;
		InputStream is = null;
		try {
			is = openRom(romFileName, a);
			byte[] firstBank = new byte[0x04000];

			int total = 0x04000;
			do {
				total -= is.read(firstBank, 0x04000 - total, total); // Read the first bank (bank 0)
			} while (total > 0);

			cartType = firstBank[0x0147];

			numBanks = lookUpCartSize(firstBank[0x0148]); // Determine the number of 16kb rom banks

			rom = new byte[0x04000 * numBanks]; // Recreate the ROM array with the correct size

			// Copy first bank into main rom array
			for (int r = 0; r < 0x4000; r++)
				rom[r] = firstBank[r];

			total = 0x04000 * (numBanks - 1); // Calculate total ROM size (first one already loaded)
			do { // Read ROM into memory
				total -= is.read(rom, rom.length - total, total); // Read the entire ROM
			} while (total > 0);
			is.close();

			StaticFunctions.debugLog("Loaded ROM '" + romFileName + "'.  " + numBanks + " banks, " + (numBanks * 16)
					+ "Kb.  " + getNumRAMBanks() + " RAM banks.");
			StaticFunctions
					.debugLog("Type: " + cartTypeTable[cartType] + " (" + StaticFunctions.hexByte(cartType) + ")");

			if (!verifyChecksum() && (a instanceof Frame))
				System.err.println("Invalid Checksum\n");

			loadBatteryRam();

			// Set up the real time clock
			Calendar rightNow = Calendar.getInstance();

			int days = rightNow.get(Calendar.DAY_OF_YEAR);
			int hour = rightNow.get(Calendar.HOUR_OF_DAY);
			int minute = rightNow.get(Calendar.MINUTE);
			int second = rightNow.get(Calendar.SECOND);

			RTCReg[SECONDS] = second;
			RTCReg[MINUTES] = minute;
			RTCReg[HOURS] = hour;
			RTCReg[DAYS_LO] = days & 0x00FF;
			RTCReg[DAYS_HI] = (days & 0x01FF) >> 8;

			realTimeStart = System.currentTimeMillis();
			lastSecondIncrement = realTimeStart;

			cartridgeReady = true;

		} catch (IOException e) {
			System.out.println("Error opening ROM image '" + romFileName + "'!");
		} catch (IndexOutOfBoundsException e) {
		}

	}

	public boolean needsResetEnable() {
		System.out.println("Reset !");
		if (needsReset) {
			needsReset = false;
			System.out.println("Reset requested");
			return true;
		} else 
			return false;
	}

	public void resetSystem() {
		needsReset = true;
	}

	public void update() {
		// Update the realtime clock from the system time
		long millisSinceLastUpdate = System.currentTimeMillis() - lastSecondIncrement;

		while (millisSinceLastUpdate > 1000) {
			millisSinceLastUpdate -= 1000;
			RTCReg[SECONDS]++;
			if (RTCReg[SECONDS] == 60) {
				RTCReg[MINUTES]++;
				RTCReg[SECONDS] = 0;
				if (RTCReg[MINUTES] == 60) {
					RTCReg[HOURS]++;
					RTCReg[MINUTES] = 0;
					if (RTCReg[HOURS] == 24) {
						if (RTCReg[DAYS_LO] == 255) {
							RTCReg[DAYS_LO] = 0;
							RTCReg[DAYS_HI] = 1;
						} else 
							RTCReg[DAYS_LO]++;
						RTCReg[HOURS] = 0;
					}
				}
			}
			lastSecondIncrement = System.currentTimeMillis();
		}
	}

	public InputStream openRom(String romFileName, Component a) {
		byte bFormat;
		boolean bFoundGBROM = false;
		String romName = "None";

		if (romFileName.toUpperCase().indexOf("ZIP") > -1)
			bFormat = bZip;
		else if (romFileName.toUpperCase().indexOf("JAR") > -1)
			bFormat = bZip;
		else if (romFileName.toUpperCase().indexOf("GZ") > -1)
			bFormat = bGZip;
		else
			bFormat = bNotCompressed;

		// Simplest case, open plain gb or gbc file.
		if (bFormat == bNotCompressed) {
			try {
				romIntFileName = StaticFunctions.stripExtention(romFileName);
				return new FileInputStream(new File(romFileName));
			} catch (Exception e) {
				System.out.println("Cant open file");
				return null;
			}
		}

		// Should the ROM be loaded from a ZIP compressed file?
		if (bFormat == bZip) {
			System.out.println("Loading ZIP Compressed ROM");

			ZipInputStream zip;

			try {

				zip = new ZipInputStream(new FileInputStream(romFileName));

				// Check for valid files (GB or GBC ending in filename)
				ZipEntry ze;

				while ((ze = zip.getNextEntry()) != null) {
					String str = ze.getName();
					if (str.toUpperCase().indexOf(".GB") > -1 || str.toUpperCase().indexOf(".GBC") > -1) {
						bFoundGBROM = true;
						romIntFileName = StaticFunctions.stripExtention(str);
						romName = str;
						// Leave loop if a ROM was found.
						break;
					}
				}
				// Show an error if no ROM file was found in the ZIP
				if (!bFoundGBROM) {
					System.err.println("No GBx ROM found!");
					throw new IOException("ERROR");
				}
				System.out.println("Found " + romName);
				return zip;
			} catch (Exception e) {
				System.out.println(e);
				return null;
			}
		}

		if (bFormat == bGZip) {
			System.out.println("Loading GZIP Compressed ROM");
			romIntFileName = StaticFunctions.stripExtention(romFileName);
			try {
				return new GZIPInputStream(new FileInputStream(romFileName));
			} catch (Exception e) {
				System.out.println("Can't open file");
				return null;
			}
		}

		// Will never get here
		return null;
	}

	/**
	 * Returns the byte currently mapped to a CPU address. Addr must be in the range
	 * 0x0000 - 0x4000 or 0xA000 - 0xB000 (for RAM access)
	 */
	public final byte addressRead(int addr) {
		// if (disposed) System.out.println("oh. dodgy cartridge");

		// if (cartType == 0) {
		// return (byte) (rom[addr] & 0x00FF);
		// } else {
		if ((addr >= 0xA000) && (addr <= 0xBFFF)) {
			switch (cartType) {
			case 0x0F:
			case 0x10:
			case 0x11:
			case 0x12:
			case 0x13: { /* MBC3 */
				if (ramBank >= 0x04) {
					// System.out.println("Reading RTC reg " + ramBank + " is " + RTCReg[ramBank -
					// 0x08]);
					return (byte) RTCReg[ramBank - 0x08];
				} else {
					return ram[addr - 0xA000 + ramPageStart];
				}
			}

			default: {
				return ram[addr - 0xA000 + ramPageStart];
			}
			}
		}
		if (addr < 0x4000) {
			return (byte) (rom[addr]);
		} else {
			return (byte) (rom[pageStart + addr - 0x4000]);
		}
		// }
	}

	/** Returns a string summary of the current mapper status */
	public String getMapInfo() {
		String out;
		switch (cartType) {
		case 0 /* No mapper */ :
		case 8:
		case 9:
			return "This ROM has no mapper.";
		case 1 /* MBC1 */ :
			return "MBC1: ROM bank " + StaticFunctions.hexByte(currentBank) + " mapped to " + " 4000 - 7FFFF";
		case 2 /* MBC1+RAM */ :
		case 3 /* MBC1+RAM+BATTERY */ :
			out = "MBC1: ROM bank " + StaticFunctions.hexByte(currentBank) + " mapped to " + " 4000 - 7FFFF.  ";
			if (mbc1LargeRamMode) {
				out = out + "Cartridge is in 16MBit ROM/8KByte RAM Mode.";
			} else {
				out = out + "Cartridge is in 4MBit ROM/32KByte RAM Mode.";
			}
			return out;
		case 5:
		case 6:
			return "MBC2: ROM bank " + StaticFunctions.hexByte(currentBank) + " mapped to 4000 - 7FFF";

		case 0x19:
		case 0x1C:
			return "MBC5: ROM bank " + StaticFunctions.hexByte(currentBank) + " mapped to 4000 - 7FFF";

		case 0x1A:
		case 0x1B:
		case 0x1D:
		case 0x1E:
			return "MBC5: ROM bank " + StaticFunctions.hexByte(currentBank) + " mapped to 4000 - 7FFF";

		}
		return "Unknown mapper.";
	}

	/** Maps a ROM bank into the CPU address space at 0x4000 */
	public void mapRom(int bankNo) {
		// addressWrite(0x2000, bank);
		// if (bankNo == 0) bankNo = 1;
		currentBank = bankNo;
		pageStart = 0x4000 * bankNo;
	}

	public void reset() {
		mapRom(1);
	}

	/** Save the current mapper state */
	public void saveMapping() {
		if ((cartType != 0) && (savedBank == -1))
			savedBank = currentBank;
	}

	/** Restore the saved mapper state */
	public void restoreMapping() {
		if (savedBank != -1) {
			System.out.println("- ROM Mapping restored to bank " + StaticFunctions.hexByte(savedBank));
			addressWrite(0x2000, savedBank);
			savedBank = -1;
		}
	}

	/**
	 * Writes a byte to an address in CPU address space. Identical to addressWrite()
	 * except that writes to ROM do not cause a mapping change, but actually write
	 * to the ROM. This is usefull for patching parts of code. Only used by the
	 * debugger.
	 */
	public void debuggerAddressWrite(int addr, int data) {
		if (cartType == 0)
			rom[addr] = (byte) data;
		else if (addr < 0x4000)
			rom[addr] = (byte) data;
		else
			rom[pageStart + addr - 0x4000] = (byte) data;
	}

	/**
	 * Writes to an address in CPU address space. Writes to ROM may cause a mapping
	 * change.
	 */
	public final void addressWrite(int addr, int data) {
		int ramAddress = 0;

		switch (cartType) {

		case 0: /* ROM Only */
			break;

		case 1: /* MBC1 */
		case 2:
		case 3:
			if ((addr >= 0xA000) && (addr <= 0xBFFF)) {
				if (ramEnabled) {
					ramAddress = addr - 0xA000 + ramPageStart;
					ram[ramAddress] = (byte) data;
				}
			}
			if ((addr >= 0x2000) && (addr <= 0x3FFF)) {
				int bankNo = data & 0x1F;
				if (bankNo == 0)
					bankNo = 1;
				mapRom((currentBank & 0x60) | bankNo);
			} else if ((addr >= 0x6000) && (addr <= 0x7FFF)) {
				if ((data & 1) == 1) {
					mbc1LargeRamMode = true;
					// ram = new byte[0x8000];
				} else {
					mbc1LargeRamMode = false;
					// ram = new byte[0x2000];
				}
			} else if (addr <= 0x1FFF) {
				if ((data & 0x0F) == 0x0A)
					ramEnabled = true;
				else
					ramEnabled = false;

			} else if ((addr <= 0x5FFF) && (addr >= 0x4000)) {
				if (mbc1LargeRamMode) {
					ramBank = (data & 0x03);
					ramPageStart = ramBank * 0x2000;
					// System.out.println("RAM bank " + ramBank + " selected!");
				} else
					mapRom((currentBank & 0x1F) | ((data & 0x03) << 5));

			}
			break;

		case 5:
		case 6:
			if ((addr >= 0x2000) && (addr <= 0x3FFF) && ((addr & 0x0100) != 0)) {
				int bankNo = data & 0x1F;
				if (bankNo == 0)
					bankNo = 1;
				mapRom(bankNo);
			}
			if ((addr >= 0xA000) && (addr <= 0xBFFF))
				if (ramEnabled)
					ram[addr - 0xA000 + ramPageStart] = (byte) data;

			break;

		case 0x0F:
		case 0x10:
		case 0x11:
		case 0x12:
		case 0x13: /* MBC3 */

			// Select ROM bank
			if ((addr >= 0x2000) && (addr <= 0x3FFF)) {
				int bankNo = data & 0x7F;
				if (bankNo == 0)
					bankNo = 1;
				mapRom(bankNo);
			} else if ((addr <= 0x5FFF) && (addr >= 0x4000)) {
				// Select RAM bank
				ramBank = data;

				if (ramBank < 0x04)
					ramPageStart = ramBank * 0x2000;

				// System.out.println("RAM bank " + ramBank + " selected!");
			}
			if ((addr >= 0xA000) && (addr <= 0xBFFF)) {
				// Let the game write to RAM
				if (ramBank <= 0x03)
					ram[addr - 0xA000 + ramPageStart] = (byte) data;
				else {
					// Write to realtime clock registers
					RTCReg[ramBank - 0x08] = data;
					// System.out.println("RTC Reg " + ramBank + " = " + data);
				}

			}
			break;

		case 0x19:
		case 0x1A:
		case 0x1B:
		case 0x1C:
		case 0x1D:
		case 0x1E:

			if ((addr >= 0x2000) && (addr <= 0x2FFF)) {
				int bankNo = (currentBank & 0xFF00) | data;
				mapRom(bankNo);
			}
			if ((addr >= 0x3000) && (addr <= 0x3FFF)) {
				int bankNo = (currentBank & 0x00FF) | ((data & 0x01) << 8);
				mapRom(bankNo);
			}

			if ((addr >= 0x4000) && (addr <= 0x5FFF)) {
				ramBank = (data & 0x07);
				ramPageStart = ramBank * 0x2000;
				// System.out.println("RAM bank " + ramBank + " selected!");
			}
			if ((addr >= 0xA000) && (addr <= 0xBFFF))
				ram[addr - 0xA000 + ramPageStart] = (byte) data;

			break;

		}

	}

	public int getNumRAMBanks() {
		switch (rom[0x149]) {
		case 0: {
			return 0;
		}
		case 1:
		case 2: {
			return 1;
		}
		case 3: {
			return 4;
		}
		case 4: {
			return 16;
		}
		}
		return 0;
	}

	/**
	 * Read an image of battery RAM into memory if the current cartridge mapper
	 * supports it. The filename is the same as the ROM filename, but with a .SAV
	 * extention. # * Files are compatible with VGB-DOS.
	 */
	public void loadBatteryRam() {
		String saveRamFileName = romFileName;
		int numRamBanks;

		try {
			int dotPosition = romFileName.lastIndexOf('.');

			if (dotPosition != -1)
				saveRamFileName = romFileName.substring(0, dotPosition) + ".sav";
			else
				saveRamFileName = romFileName + ".sav";

			/*
			 * if (rom[0x149] == 0x03) { numRamBanks = 4; } else { numRamBanks = 1; }
			 */
			numRamBanks = getNumRAMBanks();

			if ((cartType == 3) || (cartType == 9) || (cartType == 0x1B) || (cartType == 0x1E) || (cartType == 0x10)
					|| (cartType == 0x13)) {
				FileInputStream is = new FileInputStream(new File(saveRamFileName));
				is.read(ram, 0, numRamBanks * 8192);
				is.close();
				System.out.println("Read SRAM from '" + saveRamFileName + "'");
			}
			if (cartType == 6) {
				FileInputStream is = new FileInputStream(new File(saveRamFileName));
				is.read(ram, 0, 512);
				is.close();
				System.out.println("Read SRAM from '" + saveRamFileName + "'");
			}

		} catch (IOException e) {
			System.out.println("Error loading battery RAM from '" + saveRamFileName + "'");
		}
	}

	public int getBatteryRamSize() {
		if (rom[0x149] == 0x06)
			return 512;
		else
			return getNumRAMBanks() * 8192;
	}

	public byte[] getBatteryRam() {
		return ram;
	}

	public boolean canSave() {
		return (cartType == 3) || (cartType == 9) || (cartType == 0x1B) || (cartType == 0x1E) || (cartType == 6)
				|| (cartType == 0x10) || (cartType == 0x13);
	}

	/**
	 * Writes an image of battery RAM to disk, if the current cartridge mapper
	 * supports it.
	 */
	public void saveBatteryRam() {
		String saveRamFileName = romFileName;
		int numRamBanks;

		/*
		 * if (rom[0x149] == 0x03) { numRamBanks = 4; } else { numRamBanks = 1; }
		 */
		numRamBanks = getNumRAMBanks();

		try {
			int dotPosition = romFileName.lastIndexOf('.');

			if (dotPosition != -1)
				saveRamFileName = romFileName.substring(0, dotPosition) + ".sav";
			else
				saveRamFileName = romFileName + ".sav";

			if ((cartType == 3) || (cartType == 9) || (cartType == 0x1B) || (cartType == 0x1E) || (cartType == 0x10)
					|| (cartType == 0x13)) {
				FileOutputStream os = new FileOutputStream(new File(saveRamFileName));
				os.write(ram, 0, numRamBanks * 8192);
				os.close();
				System.out.println("Written SRAM to '" + saveRamFileName + "'");
			}
			if (cartType == 6) {
				FileOutputStream os = new FileOutputStream(new File(saveRamFileName));
				os.write(ram, 0, 512);
				os.close();
				System.out.println("Written SRAM to '" + saveRamFileName + "'");
			}

		} catch (IOException e) {
			System.out.println("Error saving battery RAM to '" + saveRamFileName + "'");
		}
	}

	/** Peforms saving of the battery RAM before the object is discarded */
	public void dispose() {
		saveBatteryRam();
		disposed = true;
	}

	public boolean verifyChecksum() {
		int checkSum = (StaticFunctions.unsign(rom[0x14E]) << 8) + StaticFunctions.unsign(rom[0x14F]);

		int total = 0; // Calculate ROM checksum
		for (int r = 0; r < rom.length; r++)
			if ((r != 0x14E) && (r != 0x14F))
				total = (total + StaticFunctions.unsign(rom[r])) & 0x0000FFFF;

		return checkSum == total;
	}

	/** Gets the cartridge name */
	String getCartName() {
		return cartName;
	}

	String getRomFilename() {
		return romIntFileName;
	}

	/** Outputs information about the loaded cartridge to stdout. */
	public void outputCartInfo() {
		boolean checksumOk;

		cartName = new String(rom, 0x0134, 16);
		// Extract the game name from the cartridge header

		// StaticFunctions.debugLog(rom[0x14F]+ " "+ rom[0x14E]);

		checksumOk = verifyChecksum();

		// Remove NULLs from the end of the cart name
		String s = "";
		for (int r = 0; r < cartName.length(); r++) {
			if (((int) cartName.charAt(r) != 0) && ((int) cartName.charAt(r) >= 32)
					&& ((int) cartName.charAt(r) <= 127))
				s += cartName.charAt(r);

		}
		cartName = s;

		String infoString = "ROM Info: Name = " + cartName + ", Size = " + (numBanks * 128) + "Kbit, ";

		if (checksumOk)
			infoString = infoString + "Checksum Ok.";
		else
			infoString = infoString + "Checksum invalid!";

		StaticFunctions.debugLog(infoString);
	}

	/**
	 * Returns the number of 16Kb banks in a cartridge from the header size byte.
	 */
	public int lookUpCartSize(int sizeByte) {
		int i = 0;
		while ((i < romSizeTable.length) && (romSizeTable[i][0] != sizeByte))
			i++;

		if (romSizeTable[i][0] == sizeByte)
			return romSizeTable[i][1];
		else
			return -1;
	}

}

class NoSaveDataException extends Exception {
	public NoSaveDataException(String s) {
		super(s);
	}
}
