/*
 * Created on Feb 16, 2006
 */
package com.captiveimagination.jgn.test.clientServer;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.server.*;
import com.captiveimagination.jgn.test.*;

public class TestServer {
    public static void main(String[] args) throws Exception {
        JGN.registerMessage(BasicMessage.class, (short)1);
        
        UDPMessageServer server1a = new UDPMessageServer(null, 1000);
        TCPMessageServer server1b = new TCPMessageServer(null, 1001);
        final NetworkingServer server = new NetworkingServer(server1a, server1b);
        new Thread(server).start();
    }
}
