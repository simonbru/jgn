package com.captiveimagination.jgn.util;

import java.awt.*;

import javax.swing.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class MessageServerMonitor extends Thread {
    // Statistics information
    private long receivedCount;
    private long sentCount;
    
	private MessageServer messageServer;
	private long updateFrequency;
	private boolean keepAlive;
	
	private boolean guiLoaded;
	private JLabel sentMessagesLabel;
	private JLabel receivedMessagesLabel;
    private JLabel receivedQueueLabel;
    private JLabel receivedCertifiedQueueLabel;
	
	public MessageServerMonitor(MessageServer messageServer, long updateFrequency) {
		this.messageServer = messageServer;
		this.updateFrequency = updateFrequency;
        init();
	}
    
    private void init() {
        receivedCount = 0;
        messageServer.addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
                receivedCount++;
            }

            public int getListenerMode() {
                return MessageListener.BASIC;
            }
        });
        
        sentCount = 0;
        messageServer.addMessageSentListener(new MessageSentListener() {
            public void messageSent(Message message, MessageServer server) {
                sentCount++;
            }

            public int getListenerMode() {
                return MessageSentListener.BASIC;
            }
        });
    }
	
	public void run() {
		keepAlive = true;
		while (keepAlive) {
			try {
				Thread.sleep(updateFrequency);
			} catch(InterruptedException exc) {
				exc.printStackTrace();
			}
			if (guiLoaded) {
				sentMessagesLabel.setText(String.valueOf(sentCount));
				receivedMessagesLabel.setText(String.valueOf(receivedCount));
                receivedQueueLabel.setText(String.valueOf(messageServer.getMessageQueue().size()));
                if (messageServer instanceof UDPMessageServer) {
                    receivedCertifiedQueueLabel.setText(String.valueOf(((UDPMessageServer)messageServer).getMessageCertifier().size()));
                }
			}
		}
	}
	
	public JFrame createGUI(String title) {
		JFrame frame = new JFrame();
		frame.setTitle(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		
		Container c = frame.getContentPane();
		c.setLayout(new GridLayout(4, 4));
        
		c.add(createLabel("Messages Sent:"));
		sentMessagesLabel = new JLabel("");
        c.add(sentMessagesLabel);
        
		c.add(createLabel("Messages Received:"));
		receivedMessagesLabel = new JLabel("");
		c.add(receivedMessagesLabel);
        
        c.add(createLabel("Received Queue:"));
        receivedQueueLabel = new JLabel("");
        c.add(receivedQueueLabel);
        
        c.add(createLabel("Received Certified Queue:"));
        receivedCertifiedQueueLabel = new JLabel("");
        c.add(receivedCertifiedQueueLabel);
		
		frame.setVisible(true);
		
		guiLoaded = true;
        
        return frame;
	}
	
	private static final JLabel createLabel(String text) {
		JLabel label = new JLabel(text);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		return label;
	}
}
