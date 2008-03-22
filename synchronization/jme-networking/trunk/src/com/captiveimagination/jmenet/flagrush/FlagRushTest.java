package com.captiveimagination.jmenet.flagrush;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import jmetest.flagrushtut.lesson8.Lesson8;
import jmetest.flagrushtut.lesson9.Vehicle;

import com.captiveimagination.jgn.synchronization.SyncObjectManager;
import com.captiveimagination.jgn.synchronization.message.SynchronizeCreateMessage;
import com.captiveimagination.jgn.synchronization.message.SynchronizeRemoveMessage;
import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.export.binary.BinaryImporter;

public abstract class FlagRushTest implements SyncObjectManager {
	private Node scene;
	
	public void setScene(Node scene) {
		this.scene = scene;
	}
	
	public Object create(SynchronizeCreateMessage scm) {
		System.out.println("CREATING PLAYER!");
		return buildRemotePlayer();
	}

	public boolean remove(SynchronizeRemoveMessage srm, Object obj) {
		return removeRemotePlayer((Vehicle)obj);
	}
	
	private Vehicle buildRemotePlayer() {
		Future<Vehicle> future = GameTaskQueueManager.getManager().update(new Callable<Vehicle> () {
			public Vehicle call() throws Exception {
				Spatial model = null;
		        try {
		            URL bikeFile = Lesson8.class.getClassLoader().getResource("jmetest/data/model/bike.jme");
		            BinaryImporter importer = new BinaryImporter();
		            model = (Spatial)importer.load(bikeFile.openStream());
		            model.setModelBound(new BoundingBox());
		            model.updateModelBound();
		            //scale it to be MUCH smaller than it is originally
		            model.setLocalScale(.0025f);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		        
		        //set the vehicles attributes (these numbers can be thought
		        //of as Unit/Second).
		        Vehicle player = new Vehicle("Player Node", model);
		        player.setAcceleration(15);
		        player.setBraking(15);
		        player.setTurnSpeed(2.5f);
		        player.setWeight(25);
		        player.setMaxSpeed(25);
		        player.setMinSpeed(15);
		        
		        player.setLocalTranslation(new Vector3f(100,0, 100));
		        scene.attachChild(player);
		        scene.updateGeometricState(0, true);
		        player.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
		        scene.updateRenderState();
		        return player;
			}
		});
		try {
			return future.get();
		} catch(Exception exc) {
			exc.printStackTrace();
		}
		return null;
	}
	
	private boolean removeRemotePlayer(Vehicle vehicle) {
		return vehicle.removeFromParent();
	}
}
