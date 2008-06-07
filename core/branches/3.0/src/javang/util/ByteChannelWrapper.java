/*
 * Created on 27 mei 2008
 */

package javang.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ByteChannelWrapper
{
   private final ReadableByteChannel in;
   private boolean           eof;

   public ByteChannelWrapper(ReadableByteChannel in)
   {
      this.in = in;
      this.eof = in == null;
   }

   public boolean isEOF()
   {
      return eof;
   }

   public int fill(ByteBuffer buf)
   {
      if (this.eof)
         return -1;

      try
      {
         int bytes = in.read(buf);
         if (bytes == -1)
            this.eof = true;
         return bytes;
      }
      catch (IOException exc)
      {
         this.eof = true;
         return -1;
      }
   }
}
