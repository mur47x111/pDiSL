package ch.usi.dag.disl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.usi.dag.disl.scope.Scope;
import ch.usi.dag.disl.scope.ScopeMatcher;
import ch.usi.dag.disl.util.JavaNames;


/**
 * Helper class to prepare the default exclusion list, which comprises
 * hard-coded exclusions, all classes in the instrumentation JAR files, and
 * user-defined exclusions.
 *
 * @author Lukas Marek
 * @author Lubomir Bulej
 */
abstract class ExclusionSet {

    public static Set <Scope> prepare (final Stream <URL> urlStream) {
        try {
            final Set <Scope> result = __hardCodedExclusions ();
            result.addAll (__instrumentationExclusions (urlStream));
            result.addAll (__userDefinedExclusions ());
            return result;

        } catch (final Exception e) {
            e.printStackTrace ();
            throw new InitializationException (
                e, "failed to initialize exclusion list"
            );
        }
    }

    //

    private static Set <Scope> __hardCodedExclusions () {
        final String [] exclusions = new String [] {
            //
            // Our classes.
            //
            "ch.usi.dag.dislagent.*.*" /* DiSL agent classes */,
            "ch.usi.dag.disl.dynamicbypass.*.*" /* dynamic bypass classes */,
            "ch.usi.dag.dislre.*.*" /* Shadow VM classes */,

            //
            // The following cause trouble when instrumented.
            //
            "sun.instrument.*.*" /* Sun instrumentation classes */,
            "java.lang.Object.finalize" /* Object finalizer */
        };

        return Arrays.stream (exclusions)
            .map (ScopeMatcher::forPattern)
            .collect (Collectors.toCollection (HashSet::new));
    }


    private static Set <Scope> __instrumentationExclusions (
        final Stream <URL> urlStream
    ) {
        //
        // Add all classes from instrumentation jar files, i.e., those that
        // contain any DiSL classes (snippets and argument processors) that
        // are listed in the manifest file.
        //
        return urlStream.unordered ()
            .flatMap (url -> __jarExclusionScopes (url).stream ())
            .collect (Collectors.toSet ());
    }

    private static List <Scope> __jarExclusionScopes (final URL manifestUrl) {
        try {
            final JarFile jarFile = new JarFile (__jarPath (manifestUrl));

            try {
                //
                // For each JAR file entry representing a class file, strip
                // the class file extension, convert the resource name to
                // class name, add a wild card for all methods, and create
                // a scope pattern for it.
                //
                return jarFile.stream ()
                    .filter (e -> !e.isDirectory ())
                    .map (JarEntry::getName)
                    .filter (JavaNames::hasClassFileExtension)
                    .map (entryName -> {
                        final String className = JavaNames.internalToType (
                            JavaNames.stripClassFileExtension (entryName)
                        );

                        // exclude all methods of the class
                        return ScopeMatcher.forPattern (className + ".*");
                    })
                    .collect (Collectors.toList ());

            } finally {
                jarFile.close ();
            }

        } catch (final IOException ioe) {
            throw new InitializationException (
                ioe, "failed to build exclusion list for %s", manifestUrl
            );
        }
    }


    private static String __jarPath (final URL manifestUrl) {
        //
        // Extract JAR file path from the manifest URL.
        //
        final String manifestPath = manifestUrl.getPath ();
        final int jarPathBegin = manifestPath.indexOf ("/");
        final int jarPathEnd = manifestPath.indexOf ("!");

        return manifestPath.substring (jarPathBegin, jarPathEnd);
    }

    //

    private static final String __DISL_EXCLUSION_LIST__ = "disl.exclusionList";

    private static Set <Scope> __userDefinedExclusions ()
    throws FileNotFoundException {
        final String commentPrefix = "#";
        final Set <Scope> result = new HashSet <> ();

        //
        // If a user specified a custom exclusion list, load it line by line,
        // each line representing a single exclusion scope.
        //
        final String fileName = System.getProperty (__DISL_EXCLUSION_LIST__, "");
        if (!fileName.isEmpty ()) {
            final Scanner scanner = new Scanner (new FileInputStream (fileName));
            while (scanner.hasNextLine ()) {
                final String line = scanner.nextLine ().trim ();
                if (!line.isEmpty () && !line.startsWith (commentPrefix)) {
                    result.add (ScopeMatcher.forPattern (line));
                }
            }

            scanner.close ();
        }

        return result;
    }
}
