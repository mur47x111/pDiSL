package ch.usi.dag.disl.util;

import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by alexandernorth on 08/07/16.
 * This class allows us to use the ClassReader with dynamic JAR paths.
 * It finds the class inside the JAR file, and then creates a new ClassReader using an input stream.
 */
public class ClassReaderOTF {

    public static ClassReader getClassReader(String className, URL jarPath) throws IOException, URISyntaxException{
        ClassReader cr = null;
        if (jarPath == null){
            cr = new ClassReader(className);
        }else{
            URI jarUri = jarPath.toURI();
            JarFile jar = new JarFile(new File(jarUri));
            JarEntry classFile = jar.getJarEntry(className.replace('.', '/') + ".class");

            InputStream is = jar.getInputStream(classFile);
            cr = new ClassReader(is);
            is.close();
        }
        return cr;
    }

}
