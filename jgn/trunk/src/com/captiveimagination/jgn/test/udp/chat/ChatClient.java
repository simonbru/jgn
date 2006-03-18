/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class ChatClient {
    private int serverPort;
    private MessageServer server;
    private String nickname;
    
    private JTextPane textPane;
    private JTextField textField;
    
    public ChatClient(int serverPort, int port, String nickname) throws IOException {
        this.serverPort = serverPort;
        server = new UDPMessageServer(IP.getLocalHost(), port);
        this.nickname = nickname;
    }
    
    public void start() {
        server.addMessageListener(new ClientListener(this));
        // We start the update thread - this is an alternative to calling update() in your game thread
        server.startUpdateThread();
        
        initGUI();
        
        try {
            ConnectMessage message = new ConnectMessage();
            message.setNickname(nickname);
            server.sendMessage(message, IP.getLocalHost(), serverPort);
        } catch(Exception exc) {
            exc.printStackTrace();
        }
    }
    
    private void initGUI() {
        JFrame frame = new JFrame("Client: " + nickname);
        frame.setSize(600, 500);
        Container c = frame.getContentPane();
        c.setLayout(new BorderLayout());
        textPane = new JTextPane();
        textPane.setText("");
        textPane.setEditable(false);
        c.add(BorderLayout.CENTER, textPane);
        textField = new JTextField();
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    BroadcastMessage message = new BroadcastMessage();
                    message.setMessage(textField.getText());
                    server.sendMessage(message, IP.getLocalHost(), serverPort);
                    textField.setText("");
                } catch(Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        c.add(BorderLayout.SOUTH, textField);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    
    public void addMessage(String message) {
        if (textPane.getText().length() == 0) {
            textPane.setText(message);
        } else {
            textPane.setText(textPane.getText() + "\r\n" + message);
        }
    }
}
