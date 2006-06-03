/**
 * MethodComparator.java
 *
 * Created: Jun 3, 2006
 */
package com.captiveimagination.jgn.convert;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author Matthew D. Hicks
 */
public class MethodComparator implements Comparator<Method> {
	public int compare(Method method1, Method method2) {
		return method1.getName().compareTo(method2.getName());
	}

}
