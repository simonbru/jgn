package com.captiveimagination.jgn.util;

import java.awt.*;
import java.awt.event.*;
import java.net.*;

import javax.swing.*;

import com.captiveimagination.jgn.*;

public class JGNPinger {
	private static boolean ping;
	private static MessageServer server;
	private static UDPMessageServer ums;
	private static TCPMessageServer tms;
	
	private static IP address;
	private static int port;
	private static int timeout;
	
	public static void main(String[] args) {
		JGN.ALWAYS_REBUILD = false;
		
		int udpPort = 999;
		int tcpPort = 1999;
		while ((ums == null) || (tms == null)) {
			try {
				if (ums == null) {
					udpPort++;
					ums = new UDPMessageServer(IP.getLocalHost(), udpPort);
				}
				if (tms == null) {
					tcpPort++;
					tms = new TCPMessageServer(IP.getLocalHost(), tcpPort);
				}
			} catch(Exception exc) {
				exc.printStackTrace();
				System.err.println("Trying another port...");
			}
		}
		ums.startUpdateThread();
		tms.startUpdateThread();
		
		JOptionPane.showMessageDialog(null, "Local UDP: " + udpPort + "\nLocal TCP: " + tcpPort);
		
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("JGN Pinger");
		frame.setSize(400, 170);
		
		Container c = frame.getContentPane();
		c.setLayout(new BorderLayout());
		
		JPanel panel1 = new JPanel();
		panel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel1.setLayout(new GridLayout(4, 2));
		
		JLabel label1 = new JLabel("Address: ");
		label1.setHorizontalAlignment(JLabel.RIGHT);
		panel1.add(label1);
		final JTextField field1 = new JTextField(15);
		panel1.add(field1);
		
		JLabel label2 = new JLabel("Port: ");
		label2.setHorizontalAlignment(JLabel.RIGHT);
		panel1.add(label2);
		final JTextField field2 = new JTextField(15);
		panel1.add(field2);
		
		JLabel label3 = new JLabel("Port: ");
		label3.setHorizontalAlignment(JLabel.RIGHT);
		panel1.add(label3);
		final JComboBox protocol = new JComboBox(new String[] {"UDP", "TCP"});
		panel1.add(protocol);
		
		JLabel label4 = new JLabel("Timeout: ");
		label4.setHorizontalAlignment(JLabel.RIGHT);
		panel1.add(label4);
		final JTextField field4 = new JTextField(15);
		field4.setText("1000");
		panel1.add(field4);
		
		c.add(BorderLayout.CENTER, panel1);
		
		JPanel panel2 = new JPanel();
		panel2.setLayout(new FlowLayout());
		JButton button = new JButton("Ping");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							if ((field1.getText().length() == 0) || (field1.getText().equalsIgnoreCase("localhost"))) {
								address = IP.getLocalHost();
							} else {
								address = IP.fromName(field1.getText());
							}
							port = Integer.parseInt(field2.getText());
							timeout = 1000;
							try {
								timeout = Integer.parseInt(field4.getText());
							} catch(NumberFormatException exc) {
							}
							field4.setText(String.valueOf(timeout));
							if (protocol.getSelectedItem().equals("UDP")) {
								server = ums;
							} else {
								server = tms;
							}
							final JFrame dialog = new JFrame("Ping");
							dialog.setSize(600, 500);
							center(frame, dialog);
							Container c = dialog.getContentPane();
							c.setLayout(new BorderLayout());
							
							final JTextPane text = new JTextPane();
							text.setEditable(false);
							c.add(BorderLayout.CENTER, new JScrollPane(text));
							
							Thread t = new Thread() {
								public void run() {
									ping = true;
									while (ping) {
										text.setText(text.getText() + "\n" + (server.ping(address, port, timeout) * 1000));
									}
								}
							};
							
							JButton button = new JButton("Stop");
							button.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent arg0) {
									ping = false;
								}
							});
							JButton button2 = new JButton("Close");
							button2.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent evt) {
									ping = false;
									dialog.dispose();
								}
							});
							dialog.addWindowListener(new WindowListener() {
								public void windowOpened(WindowEvent arg0) {
								}

								public void windowClosing(WindowEvent arg0) {
									ping = false;
									dialog.dispose();
								}

								public void windowClosed(WindowEvent arg0) {
								}

								public void windowIconified(WindowEvent arg0) {
								}

								public void windowDeiconified(WindowEvent arg0) {
								}

								public void windowActivated(WindowEvent arg0) {
								}

								public void windowDeactivated(WindowEvent arg0) {
								}
								
							});
							JPanel stopPanel = new JPanel();
							stopPanel.setLayout(new FlowLayout());
							stopPanel.add(button);
							stopPanel.add(button2);
							c.add(BorderLayout.SOUTH, stopPanel);
							t.start();
							dialog.setVisible(true);
						} catch(UnknownHostException exc) {
							JOptionPane.showMessageDialog(frame, "Unknown Host: " + field1.getText(), "Invalid Address", JOptionPane.ERROR_MESSAGE);
						} catch(NumberFormatException exc) {
							JOptionPane.showMessageDialog(frame, "Invalid Port Specified: " + field2.getText(), "Invalid Port", JOptionPane.ERROR_MESSAGE);
						}
					}
				});
			}
		});
		panel2.add(button);
		
		c.add(BorderLayout.SOUTH, panel2);
		
		center(null, frame);
		frame.setVisible(true);
	}
	
	public static void center(Component parent, Component window) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowSize = window.getSize();
		double centerWidth = screenSize.width / 2d;
		double centerHeight = screenSize.height / 2d;
		
		Component parentWindow = null;
		Dimension size = null;
		Point point = null;
		if (parent instanceof Dialog) {
		    parentWindow = (Dialog)parent;
		    size = parentWindow.getSize();
		    point = parentWindow.getLocation();
		} else if (parent instanceof Frame) {
			parentWindow = (Frame)parent;
			size = parentWindow.getSize();
			point = parentWindow.getLocation();
		} else if (parent != null) {
		    parentWindow = JOptionPane.getFrameForComponent(parent);
		    size = parentWindow.getSize();
		    point = parentWindow.getLocation();
		}
        if (parentWindow != null) {
            centerWidth = (size.width / 2) + point.x;
            centerHeight = (size.height / 2) + point.y;
        }
        window.setLocation((int)centerWidth - (windowSize.width / 2), (int)centerHeight - (windowSize.height / 2));
    }
}
