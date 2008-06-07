/*
 * Created on 27 mei 2008
 */

package javang.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

public class ByteArrayReadableChannel implements ReadableByteChannel
{
   private byte[] data;
   private int    read;

   public ByteArrayReadableChannel(byte[] data)
   {
      if (data == null)
         throw new NullPointerException();
      this.data = data;
      this.read = 0;
   }

   @Override
   public int read(ByteBuffer dst) throws IOException
   {
      if (!this.isOpen())
         throw new ClosedChannelException();

      if (read == data.length)
         return -1;

      int max = Math.min(dst.remaining(), data.length - read);
      dst.put(data, read, max);
      this.read += max;
      return max;
   }

   @Override
   public boolean isOpen()
   {
      return data != null;
   }

   @Override
   public void close() throws IOException
   {
      if (data == null)
         throw new ClosedChannelException();
      data = null;
   }
}
