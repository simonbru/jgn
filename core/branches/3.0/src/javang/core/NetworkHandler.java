/*
 * Created on 28 mei 2008
 */

package javang.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

public interface NetworkHandler
{
   public void onConnected(Network net, SelectionKey key);
   
   public void onExecute(Network net, SelectionKey key);

   public void onReceivedTCP(Network net, SelectionKey key, byte[] data);
   
   public void onReceivedUDP(Network net, SelectionKey key, byte[] data, InetSocketAddress source);

   public void onSent(Network net, SelectionKey key, int byteCount);

   public void onDisconnected(Network net, SelectionKey key, IOException cause);
}
