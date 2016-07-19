package ch.usi.dag.disl.util;

/**
 * Created by alexandernorth on 19/07/16.
 */
public final class ClassLoaderHelper {
    public static Class<?> forName(final String name, final ClassLoader loader) throws ClassNotFoundException{
        if (loader == null) {
            return Class.forName(name);
        }else{
            return Class.forName(name, true, loader);
        }
    }
}
