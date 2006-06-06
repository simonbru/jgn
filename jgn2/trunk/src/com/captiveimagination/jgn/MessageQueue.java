/*
 * Created on 6-jun-2006
 */

package com.captiveimagination.jgn;

import java.util.LinkedList;

public class MessageQueue
{
   LinkedList<Message>[] lists;

   public MessageQueue()
   {
      lists = new LinkedList[5];
      for (int i = 0; i < lists.length; i++)
         lists[i] = new LinkedList<Message>();
   }

   public void add(Message m)
   {
      int p = m.getPriority();

      synchronized (lists[p])
      {
         lists[p].addLast(m);
      }
   }

   public Message poll()
   {
      for (int i = 0; i < lists.length; i++)
      {
         synchronized (lists[i])
         {
            if (lists[i].isEmpty())
               continue;

            return lists[i].getFirst();
         }
      }

      return null;
   }
}
