package core;

import java.awt.Button;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class implements a dialog box with some text and an OK button. It is
 * used for error messages and confirmation messages.
 */

interface DialogListener {
	void yesPressed();

	void noPressed();
}

class ModalDialog implements ActionListener {
	Frame dialog;
	boolean yesNo;
	boolean result;
	boolean finished;
	DialogListener listener;

	/** Create a dialog box with the specified title and body text */
	public ModalDialog(Frame parent, String title, String text, String text2) {
		this.yesNo = false;
		if (parent == null) {
			System.out.println(text);
			System.out.println(text2);
		}
		dialog = new Frame(title);
		dialog.add(new Label(text), "North");
		dialog.add(new Label(text2), "Center");
		Button b = new Button("Ok");
		b.setActionCommand("Close dialog");
		b.addActionListener(this);
		dialog.add(b, "South");
		dialog.setSize(350, 110);
		dialog.setVisible(true);

		finished = false;

	}

	public ModalDialog(Frame parent, String title, String text, DialogListener listener) {
		this.yesNo = true;
		this.listener = listener;

		if (parent == null)
			System.out.println(text);

		dialog = new Frame(title);
		dialog.setLayout(new GridLayout(3, 1));

		dialog.add(new Label(text));

		Button b = new Button("Yes");
		b.setActionCommand("Yes");
		b.addActionListener(this);
		dialog.add(b);

		b = new Button("No");
		b.setActionCommand("No");
		b.addActionListener(this);
		dialog.add(b);

		dialog.setSize(350, 110);
		dialog.setVisible(true);

		finished = false;

	}

	public boolean getResult() {
		return result;
	}

	public boolean getFinished() {
		return finished;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		result = !e.getActionCommand().equals("No");
		if (yesNo) {
			if (result)
				listener.yesPressed();
			else
				listener.noPressed();
		}

		dialog.setVisible(false);
		dialog = null;
		finished = true;
	}
}