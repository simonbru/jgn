/*
 * Created on May 30, 2008
 */

package javang.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

/**
 * Utility class to convert <code>NetworkHandler</code> into a class
 * specialized in TCP.<br>
 * <br>
 * <br>
 * The effective NetworkHandler: <br>
 * <code>
 * public void onReceived(Network net, SelectionKey key, byte[] data, InetSocketAddress source);
 * public void onSent(Network net, SelectionKey key, int byteCount);
 * public void onExecute(Network net, SelectionKey key);
 * </code>
 */

public abstract class UDPHandler implements NetworkHandler
{
   public abstract void onReceived(Network net, SelectionKey key, byte[] data, InetSocketAddress source);

   @Override
   public final void onConnected(Network net, SelectionKey key)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public final void onReceivedTCP(Network net, SelectionKey key, byte[] data)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public final void onReceivedUDP(Network net, SelectionKey key, byte[] data, InetSocketAddress source)
   {
      // redirect to onReceived
      this.onReceived(net, key, data, source);
   }

   @Override
   public final void onDisconnected(Network net, SelectionKey key, IOException cause)
   {
      throw new UnsupportedOperationException();
   }
   
   @Override
   public void onExecute(Network net, SelectionKey key)
   {
      // user might want to override this
   }
}
