/*
 * Created on May 25, 2008
 */

package javang.util;

import java.util.Arrays;
import java.util.LinkedList;

public class ByteQueue
{
   private final LinkedList<byte[]> queue;

   public ByteQueue()
   {
      this.queue = new LinkedList<byte[]>();
   }

   public void enqueue(byte[] data)
   {
      queue.addLast(data);
   }

   public int size()
   {
      // make 'size' attribute

      int sum = 0;
      for (byte[] data : queue)
         sum += data.length;
      return sum;
   }

   public byte get(int index)
   {
      int offset = 0;

      for (byte[] data : queue)
      {
         for (int i = 0; i < data.length; i++)
         {
            if (offset == index)
               return data[i];
            offset++;
         }
      }

      throw new IllegalStateException();
   }

   public int indexOf(byte b)
   {
      int offset = 0;

      for (byte[] data : queue)
      {
         for (int i = 0; i < data.length; i++)
         {
            if (data[i] == b)
               return offset;
            offset++;
         }
      }

      return -1;
   }

   public int indexOf(byte[] buf)
   {
      return this.indexOf(buf, 0, buf.length);
   }

   public int indexOf(byte[] buf, int off, int len)
   {
      int offset = 0;
      int match = 0;

      for (byte[] data : queue)
      {
         for (int i = 0; i < data.length; i++)
         {
            if (data[i] != buf[off + match])
               match = 0;
            else if (++match == len)
               return offset - len + 1;

            offset++;
         }
      }

      return -1;
   }

   public byte[] dequeueAll()
   {
      return this.dequeue(this.size());
   }

   public byte[] dequeue(int bytes)
   {
      int size = this.size();
      if (bytes > size)
         throw new IllegalArgumentException("dequeueing " + bytes + "/" + size + " bytes");

      byte[] dst = new byte[bytes];

      int chunks = 0;
      int outerOffset = 0;
      int innerOffset = 0;

      outer: for (byte[] data : queue)
      {
         for (innerOffset = 0; innerOffset < data.length;)
         {
            if (outerOffset == bytes)
               break outer;
            dst[outerOffset++] = data[innerOffset++];
         }

         chunks++;
      }

      if (outerOffset != bytes)
         throw new IllegalStateException("");

      for (int k = 0; k < chunks; k++)
         queue.removeFirst();

      if (innerOffset != 0 && !queue.isEmpty())
      {
         byte[] part = queue.removeFirst();
         byte[] left = Arrays.copyOfRange(part, innerOffset, part.length);
         queue.addFirst(left);
      }

      return dst;
   }
}