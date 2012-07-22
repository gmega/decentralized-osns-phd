package it.unitn.disi.distsim.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TextConsole extends JComponent {

	private final JTextArea fArea;
	
	private final JButton fClear;

	public TextConsole() {
		this.setLayout(new BorderLayout());
		fArea = new JTextArea();
		this.add(new JScrollPane(fArea), BorderLayout.CENTER);
		
		fClear = new JButton("Clear buffer");
		JComponent container = new JPanel();
		container.setLayout(new GridLayout(1, 3));
		container.add(fClear);
		container.add(new JPanel());
		container.add(new JPanel());
		this.add(container, BorderLayout.PAGE_START);
		
		fClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fArea.setText("");
			}
		});
	}

	public void print(String str) {
		fArea.append(str + "\n");
	}

}
