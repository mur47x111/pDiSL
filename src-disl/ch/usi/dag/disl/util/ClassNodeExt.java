package ch.usi.dag.disl.util;

import org.objectweb.asm.tree.ClassNode;

import java.net.URL;

/**
 * Created by alexandernorth on 12/07/16.
 * Adds a field to ClassNode for the loadUrl: The JAR file's URL to load from.
 * This is used to load the DiSL classes - since we need to locate the class file in the JAR
 */
public class ClassNodeExt extends ClassNode {

    public URL loadUrl;

    public ClassNodeExt(int api){
        super(api);
    }
}
