package ch.usi.dag.disl.util;

import org.objectweb.asm.tree.ClassNode;

import java.net.URLClassLoader;

/**
 * Created by alexandernorth on 12/07/16.
 */
public class ClassNodeExt extends ClassNode {

    public URLClassLoader urlClassLoader;

    public ClassNodeExt(int api){
        super(api);
    }

    public void setUrlClassLoader(URLClassLoader urlClassLoader){
        this.urlClassLoader = urlClassLoader;
    }
}
