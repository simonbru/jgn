package com.captiveimagination.jmenet.flagrush;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.captiveimagination.jgn.synchronization.SynchronizationManager;
import com.captiveimagination.jgn.synchronization.message.SynchronizeCreateMessage;
import com.captiveimagination.jmenet.JMEGraphicalController;
import com.jme.app.AbstractGame.ConfigShowMode;
import com.jme.renderer.pass.ShadowedRenderPass;
import com.jme.scene.Node;

import jmetest.flagrushtut.lesson9.Lesson9;
import jmetest.flagrushtut.lesson9.Vehicle;
import jmetest.renderer.ShadowTweaker;

public class FlagRushTestServer extends FlagRushTest {
	public FlagRushTestServer() throws Exception {
		// Set up the game just like in the lesson
		ShadowedRenderPass shadowPass = new ShadowedRenderPass();
		final FlagRush app = new FlagRush();
		app.setConfigShowMode(ConfigShowMode.AlwaysShow, FlagRushTestClient.class
                .getClassLoader().getResource(
                        "jmetest/data/images/FlagRush.png"));
        new ShadowTweaker(shadowPass).setVisible(true);
        new Thread() {
        	public void run() {
        		app.start();
        	}
        }.start();
        
        // Initialize networking
        InetSocketAddress serverReliable = new InetSocketAddress(InetAddress.getLocalHost(), 9100);
		InetSocketAddress serverFast = new InetSocketAddress(InetAddress.getLocalHost(), 9200);
		JGNServer server = new JGNServer(serverReliable, serverFast);
		server.setConnectionLinking(true);
		
		// Instantiate an instance of a JMEGraphicalController
		JMEGraphicalController controller = new JMEGraphicalController();
		
		// Create SynchronizationManager instance for this server
		SynchronizationManager serverSyncManager = new SynchronizationManager(server, controller);
		serverSyncManager.addSyncObjectManager(this);
		
		JGN.createThread(server).start();

		JGN.createThread(serverSyncManager).start();
		
        // Get the vehicle instance out of the application without making any changes
		Field field = FlagRush.class.getDeclaredField("player");
		field.setAccessible(true);
		Vehicle vehicle = null;
		
		while ((vehicle = (Vehicle)field.get(app)) == null) {
			try {
				Thread.sleep(100);
			} catch(Exception exc) {
				exc.printStackTrace();
			}
		}
		
		// Retrieve the "scene" from the game
		field = FlagRush.class.getDeclaredField("scene");
		field.setAccessible(true);
		Node scene = (Node)field.get(app);
		setScene(scene);
		
		// Register server vehicle
		serverSyncManager.register(vehicle, new SynchronizeCreateMessage(), 50);
	}
	
	public static void main(String[] args) throws Exception {
		new FlagRushTestServer();
	}
}
