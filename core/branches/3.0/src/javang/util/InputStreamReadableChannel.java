/*
 * Created on 30 mei 2008
 */

package javang.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class InputStreamReadableChannel implements ReadableByteChannel
{
   private final InputStream in;
   private final byte[]      buffer;
   private int               lastRead;

   public InputStreamReadableChannel(InputStream in)
   {
      this(in, 8 * 1024);
   }

   public InputStreamReadableChannel(InputStream in, int bufferSize)
   {
      if (in == null)
         throw new NullPointerException();

      this.in = in;
      this.buffer = new byte[bufferSize];
      this.lastRead = 0; // not -1
   }

   @Override
   public int read(ByteBuffer dst) throws IOException
   {
      if (this.lastRead == -1)
         throw new EOFException();

      int bytes = Math.min(buffer.length, dst.remaining());
      int read = this.in.read(buffer, 0, bytes);
      this.lastRead = read;
      if (read == -1)
         return -1;
      dst.put(buffer, 0, bytes);
      return read;
   }

   @Override
   public boolean isOpen()
   {
      return this.lastRead != -1;
   }

   @Override
   public void close() throws IOException
   {
      this.lastRead = -1;
      
      this.in.close();
   }
}
