/*
 * Created on Feb 16, 2006
 */
package com.captiveimagination.jgn.test.clientServer;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.test.*;

/**
 * @author Matthew D. Hicks
 */
public class TestClient {
    public static void main(String[] args) throws Exception {
        JGN.registerMessage(BasicMessage.class, (short)1);
        
        UDPMessageServer server2a = new UDPMessageServer(null, 1005);
        TCPMessageServer server2b = new TCPMessageServer(null, 1006);
        final NetworkingClient client = new NetworkingClient(server2a, server2b);;
        new Thread(client).start();
        
        System.out.println("Connected: " + client.connectAndWait(null, 1000, 1001, 10000));
        
//        client.disconnect();
//        System.exit(0);
    }
}