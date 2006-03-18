/*
 * Created on Feb 4, 2006
 */
package com.captiveimagination.jgn;

import java.util.*;

/**
 * This is for internal use for keeping a list of recently
 * received message ids and rejecting any duplicate ids
 * received.
 * 
 * @author Matthew D. Hicks
 */
public class MessageIdentificationCache {
    private HashSet set;
    private LinkedList list;
    
    private int capacity;
    
    public MessageIdentificationCache(int capacity) {
        this.capacity = capacity;
        
        set = new HashSet(capacity);
        list = new LinkedList();
    }
    
    public void add(long messageId) {
        if (list.size() >= capacity) {
            set.remove(list.get(0));
            list.remove(0);
        }
        Long l = new Long(messageId);
        set.add(l);
        list.add(l);
    }
    
    public void remove(long messageId) {
    	Long l = new Long(messageId);
    	set.remove(l);
    	list.remove(l);
    }
    
    public boolean contains(long messageId) {
        return set.contains(new Long(messageId));
    }
}
