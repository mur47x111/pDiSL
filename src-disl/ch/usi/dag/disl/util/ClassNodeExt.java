package ch.usi.dag.disl.util;

import org.objectweb.asm.tree.ClassNode;

import java.net.URL;

/**
 * Created by alexandernorth on 12/07/16.
 */
public class ClassNodeExt extends ClassNode {

    public URL loadUrl;

    public ClassNodeExt(int api){
        super(api);
    }
}
