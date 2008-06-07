/*
 * Created on May 30, 2008
 */

package javang.core;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

/**
 * Utility class to convert <code>NetworkHandler</code> into a class
 * specialized in TCP.<br>
 * <br>
 * <br>
 * The effective NetworkHandler:<br>
 * <code>
 * public void onConnected(Network net, SelectionKey key);
 * public void onReceived(Network net, SelectionKey key, byte[] data);
 * public void onSent(Network net, SelectionKey key, int byteCount);
 * public void onDisconnected(Network net, SelectionKey key);
 * public void onExecute(Network net, SelectionKey key);
 * </code>
 */

public abstract class TCPHandler implements NetworkHandler
{
   public abstract void onReceived(Network net, SelectionKey key, byte[] data);

   @Override
   public final void onReceivedTCP(Network net, SelectionKey key, byte[] data)
   {
      // redirect to onReceived
      this.onReceived(net, key, data);
   }

   @Override
   public final void onReceivedUDP(Network net, SelectionKey key, byte[] data, InetSocketAddress source)
   {
      throw new UnsupportedOperationException();
   }
   
   @Override
   public void onExecute(Network net, SelectionKey key)
   {
      // user might want to override this
   }
}
