package core;

import java.awt.Button;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Hashtable;

public class DefineControls extends Frame implements KeyListener, WindowListener, ActionListener {
	/**
	 * Serial ID 
	 */
	private static final long serialVersionUID = 1459483557622330864L;

	TextField[] controlsField = new TextField[8];

	Hashtable<Integer, String> keyNames;

	public DefineControls() {
		super("Define Controls");

		keyNames = new Hashtable<>();
		keyNames.put(new Integer(38), "Up arrow");
		keyNames.put(new Integer(40), "Down arrow");
		keyNames.put(new Integer(37), "Left arrow");
		keyNames.put(new Integer(39), "Right arrow");
		keyNames.put(new Integer(36), "Pad 7");
		keyNames.put(new Integer(33), "Pad 9");
		keyNames.put(new Integer(35), "Pad 1");
		keyNames.put(new Integer(64), "Pad 3");
		keyNames.put(new Integer(12), "Pad 5");
		keyNames.put(new Integer(155), "Insert");
		keyNames.put(new Integer(36), "Home");
		keyNames.put(new Integer(33), "Page up");
		keyNames.put(new Integer(127), "Delete");
		keyNames.put(new Integer(35), "End");
		keyNames.put(new Integer(34), "Page down");
		keyNames.put(new Integer(10), "Return");
		keyNames.put(new Integer(16), "Shift");
		keyNames.put(new Integer(17), "Control");
		keyNames.put(new Integer(18), "Alt");
		keyNames.put(new Integer(32), "Space");
		keyNames.put(new Integer(20), "Caps lock");
		keyNames.put(new Integer(8), "Backspace");

		GridLayout g = new GridLayout(9, 2, 12, 12);

		setLayout(g);

		controlsField[0] = addControlsLine("D-pad up:");
		controlsField[1] = addControlsLine("D-pad down:");
		controlsField[2] = addControlsLine("D-pad left:");
		controlsField[3] = addControlsLine("D-pad right:");
		controlsField[4] = addControlsLine("A button:");
		controlsField[5] = addControlsLine("B button:");
		controlsField[6] = addControlsLine("Start button:");
		controlsField[7] = addControlsLine("Select button:");

		for (int r = 0; r < 8; r++) {
			controlsField[r].setText(
					getKeyDesc(JavaBoyNeo.keyCodes[r], (char) JavaBoyNeo.keyCodes[r]) + " (" + JavaBoyNeo.keyCodes[r] + ")");
		}

		Button cancel = new Button("Close");
		cancel.setActionCommand("Controls close");
		cancel.addActionListener(this);
		add(cancel);

		setSize(230, 300);
		setResizable(false);
		addWindowListener(this);
		setVisible(true);
	}

	public String getKeyDesc(int code, char c) {
		if (keyNames.containsKey(new Integer(code))) 
			return (String) keyNames.get(new Integer(code));
		 else 
			return c + "";
	}

	public TextField addControlsLine(String name) {
		add(new Label(name));
		TextField t = new TextField(4);
		t.addKeyListener(this);
		add(t);
		return t;
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		System.out.println(e.getKeyCode() + ", " + e.getKeyChar());

		for (int r = 0; r < 8; r++) {
			if (e.getSource() == controlsField[r]) {
				controlsField[r].setText(getKeyDesc(e.getKeyCode(), e.getKeyChar()) + " (" + e.getKeyCode() + ")");
				JavaBoyNeo.keyCodes[r] = e.getKeyCode();
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		setVisible(false);
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setVisible(false);
	}

}