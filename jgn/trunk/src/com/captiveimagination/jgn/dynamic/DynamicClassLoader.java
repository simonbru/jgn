/*
 * Created on Dec 19, 2005
 */
package com.captiveimagination.jgn.dynamic;

import java.io.*;
import java.io.File;

/**
 * @author Matthew D. Hicks
 */
public class DynamicClassLoader extends ClassLoader {
    private File searchDirectory;
    
    public DynamicClassLoader(File searchDirectory) {
        this.searchDirectory = searchDirectory;
    }
    
    public Class loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch(ClassNotFoundException exc) {
            try {
                File f = new File(searchDirectory.getPath() + "/" + name + ".class");
                if (f.exists()) {
                    byte[] b = new byte[(int)f.length()];
                    FileInputStream fis = new FileInputStream(f);
                    fis.read(b);
                    
                    return defineClass(name, b, 0, b.length);
                }
            } catch(Exception exc2) {
            }
            throw exc;
        }
    }
}
