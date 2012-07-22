package it.unitn.disi.distsim.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

public class SimulationPanel extends JFrame {

	private final JTabbedPane fTabbedPane = new JTabbedPane();

	private final JMenuBar fDisconnected;

	private final JMenuBar fConnected;

	private final TextConsole fMessages;

	private final SimulationList fList;

	public SimulationPanel() {
		setLayout(new GridLayout(1, 1));

		fMessages = new TextConsole();
		fList = new SimulationList();

		JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				fTabbedPane, fMessages);
		split1.setResizeWeight(0.8);

		JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fList,
				split1);
		split2.setResizeWeight(0.3);

		add(split2);

		fDisconnected = disconnectedMenu();
		fConnected = connectedMenu();

		setJMenuBar(fDisconnected);
	}

	private JMenuBar connectedMenu() {
		JMenuBar bar = new JMenuBar();
		
		JMenu file = new JMenu("File");
		bar.add(file);

		JMenuItem connect = new JMenuItem("Disconnect");
		file.add(connect);

		bar.add(new JSeparator());
		JMenuItem quit = new JMenuItem("Quit");
		file.add(quit);

		return bar;
	}

	private JMenuBar disconnectedMenu() {
		JMenuBar bar = new JMenuBar();

		JMenu file = new JMenu("File");
		bar.add(file);

		JMenuItem connect = new JMenuItem("Connect");
		file.add(connect);
		connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String host = JOptionPane.showInputDialog("Enter host address and port:",
						"localhost:30530");
				connect(host);
			}
		});

		file.add(new JSeparator());
		JMenuItem quit = new JMenuItem("Quit");
		file.add(quit);

		return bar;
	}

	private void connect(String host) {
		fMessages.print("Connecting to host " + host + "...");
	}
}
