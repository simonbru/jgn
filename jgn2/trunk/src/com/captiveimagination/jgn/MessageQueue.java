/*
 * Created on 6-jun-2006
 */

package com.captiveimagination.jgn;

import java.util.LinkedList;

public class MessageQueue {
   LinkedList<Message>[] lists;

   public MessageQueue() {
      lists = new LinkedList[5];
      for (int i = 0; i < lists.length; i++)
         lists[i] = new LinkedList<Message>();
   }

   private volatile int size = 0;

   public void add(Message m) {
      int p = m.getPriority();

      if (p < Message.PRIORITY_TRIVIAL)
         throw new IllegalStateException();
      if (p > Message.PRIORITY_CRITICAL)
         throw new IllegalStateException();

      synchronized (lists[p]) {
         lists[p].addLast(m);
      }

      size++;
   }

   public Message poll() {
      if (isEmpty())
         return null;

      for (int i = lists.length - 1; i >= 0; i--) {
         synchronized (lists[i]) {
            if (lists[i].isEmpty())
               continue;

            Message m = lists[i].getFirst();
            size--;
            return m;
         }
      }

      return null;
   }

   public boolean isEmpty() {
      return size == 0;
   }
}