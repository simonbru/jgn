/*
 * Created on 28 mei 2008
 */

package javang.core;

import java.nio.channels.SelectionKey;

public interface NetworkHandlerProvider
{
   public NetworkHandler provide(Network net, SelectionKey key);
}
