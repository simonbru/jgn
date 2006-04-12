/*
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.captiveimagination.jgn;

import java.util.*;

/**
 * Updater is an a thread that can be configured
 * to send messages on a specific schedule particularly
 * useful for sending updates to a remote machine such
 * as positioning information of a character and/or object.
 * 
 * @author Matthew D. Hicks
 */
public class Updater extends Thread {
    private long recycle;
    
    private ArrayList senders;
    private MessageSender[] cycle;
    
    private boolean changed;
    private boolean keepAlive;
    private Comparator senderComparator;
    
    /**
     * @param recycle
     *      The length of time a cycle should take and start a
     *      new cycle.
     */
    public Updater(long recycle) {
        this.recycle = recycle;
        
        senders = new ArrayList();
        
        senderComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                MessageSender ms1 = (MessageSender)o1;
                MessageSender ms2 = (MessageSender)o2;
                Integer i1 = new Integer(ms1.getUpdatesPerCycle());
                Integer i2 = new Integer(ms2.getUpdatesPerCycle());
                return i2.compareTo(i1);
            }
        };
        
        keepAlive = true;
        changed = true;
    }
    
    public void run() {
        long time;
        long delay;
        long wait;
        long frequency = 0;
        
        while (keepAlive) {
            try {
                if (changed) {
                    updateCycle();
                    if (cycle.length == 0) {
                        frequency = recycle;
                    } else {
                        frequency = (recycle / cycle.length);
                    }
                }
                time = System.nanoTime();
                delay = 0;
                int missed = 0;
                for (int i = 0; i < cycle.length; i++) {
                    if ((keepAlive) && (cycle[i].isEnabled())) cycle[i].sendMessage();
                    delay += frequency * 1000000;
                    wait = time - System.nanoTime() + delay;
                    if (wait > 0) {
                        Thread.sleep(wait / 1000000);
                    } else {
                        //System.err.println("WARNING: Updater is not able to keep up (" + wait + " of " + delay + "). Try increasing the recycle.");
                        missed++;
                    }
                }
                //System.out.println("Missed: " + missed + " out of " + cycle.length);
                if (cycle.length == 0) {
                    Thread.sleep(recycle);
                }
            } catch(Throwable t) {
                t.printStackTrace();
                System.err.println("Shutting down updater.");
                keepAlive = false;
            }
        }
    }
    
    /**
     * @param sender
     *      MessageSender to be added to Updater
     *      will pick it up on recycle.
     */
    public void add(MessageSender sender) {
        senders.add(sender);
        changed = true;
    }
    
    /**
     * @param sender
     *      MessageSender to be removed from Updater
     *      will get removed on recycle.
     */
    public void remove(MessageSender sender) {
        senders.remove(sender);
        changed = true;
    }
    
    public ArrayList getMessageSenders() {
        return senders;
    }
    
    private void updateCycle() {
        changed = false;
        int perCycleCount = 0;
        for (int i = 0; i < senders.size(); i++) {
            perCycleCount += ((MessageSender)senders.get(i)).getUpdatesPerCycle();
        }
        
        // Sort by priority descending
        Collections.sort(senders, senderComparator);
        
        cycle = new MessageSender[perCycleCount];
        
        // Disperse senders equally
        float spread;
        float position;
        for (int i = 0; i < senders.size(); i++) {
            spread = (float)cycle.length / (float)((MessageSender)senders.get(i)).getUpdatesPerCycle();
            position = i;
            for (int j = 0; j < ((MessageSender)senders.get(i)).getUpdatesPerCycle(); j++) {
                addToOptimalPosition((MessageSender)senders.get(i), cycle, position);
                position += spread;
            }
        }
    }
    
    private static void addToOptimalPosition(MessageSender sender, MessageSender[] cycle, float position) {
        // Find the closest matching unused position to its perfect position
        int perfect = Math.round(position);
        if (cycle[perfect] == null) {
            cycle[perfect] = sender;
            return;
        }
        int left = perfect - 1;
        int right = perfect + 1;
        while (true) {
            if (left < 0) {
                left = cycle.length - 1;
            }
            if (right >= cycle.length) {
                right = 0;
            }
            if (cycle[left] == null) {
                cycle[left] = sender;
                break;
            } else if (cycle[right] == null) {
                cycle[right] = sender;
                break;
            }
            left--;
            right++;
        }
    }
    
    /**
     * Shutdown the thread sending updates.
     */
    public void shutdown() {
        keepAlive = false;
    }
}
