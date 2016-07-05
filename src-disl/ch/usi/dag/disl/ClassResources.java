package ch.usi.dag.disl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.usi.dag.disl.util.Logging;
import ch.usi.dag.util.logging.Logger;


/**
 * Provides access to DiSL and transformer class names discovered from manifest
 * files and system properties.
 *
 * @author Lubomir Bulej
 */
final class ClassResources {

    private static final String __MANIFEST__ = "META-INF/MANIFEST.MF";

    private static final String __ATTR_DISL_CLASSES__ = "DiSL-Classes";
    private static final String __ATTR_DISL_TRANSFORMER__ = "DiSL-Transformer";
    private static final String __ATTR_DISL_TRANSFORMERS__ = "DiSL-Transformers";

    private static final String __PROP_DISL_CLASSES__ = "disl.classes";

    private static final String __SEPARATOR__ = ",";

    //

    private final List <DislManifest> __manifests;

    private ClassResources (final List <DislManifest> manifests) {
        __manifests = manifests;
    }

    //

    /**
     * @return Stream of all distinct resources containing DiSL classes.
     */
    public Stream <URL> instrumentationResources () {
        return __manifests.stream ()
            .filter (DislManifest::isInstrumentation)
            .map (DislManifest::resource)
            .filter (Optional::isPresent) // excludes properties
            .map (Optional::get)
            .distinct ();
    }


    /**
     * @return Stream of all transformer class names extracted from manifest
     *         files and properties used to initialize a {@link DiSL} instance.
     */
    public Stream <String> transformers () {
        return __manifests.stream ().flatMap (DislManifest::transformers);
    }


    /**
     * @return Stream of all distinct DiSL class names extracted from manifest
     *         files and properties used to initialize a {@link DiSL} instance.
     */
    public Stream <String> dislClasses () {
        return __manifests.stream ()
            .flatMap (DislManifest::classes)
            .distinct ();
    }

    //

    /**
     * Discovers DiSL resources. Extracts the names of DiSL and transformer
     * classes from manifest files found in the class path and in properties
     * used to initialize a {@link DiSL} instance.
     */
    public static final ClassResources discover (final Properties properties) {
        final Logger log = Logging.getPackageInstance ();

        // Get class names from manifests in the class path.
        final ManifestLoader ml = new ManifestLoader (log);
        final List <DislManifest> manifests = ml.loadAll ();

        // Get class names from system properties.
        final List <String> dislClasses = __getClassNames (
            Optional.ofNullable (properties.getProperty (__PROP_DISL_CLASSES__))
        );

        manifests.add (new DislManifest (dislClasses));

        return new ClassResources (manifests);
    }

    //


    private static final Pattern __splitter__ = Pattern.compile (__SEPARATOR__);

    private static List <String> __getClassNames (final Optional <String> value) {
        return value.map (v -> __splitter__.splitAsStream (v)
            .map (String::trim)
            .filter (s -> !s.isEmpty ())
            .collect (Collectors.toList ())
        ).orElse (Collections.emptyList ());
    }

    //

    private static final class ManifestLoader {
        private final Logger __log;

        ManifestLoader (final Logger log) {
            __log = log;
        }

        //

        List <DislManifest> loadAll () {
            // Avoid parallel streams to keep order of transformers consistent.
            return __manifestUrlStream ()
                .map (url -> __loadManifest (url))
                .filter (Optional::isPresent)
                .map (Optional::get)
                .filter (m -> !m.isEmpty ())
                .collect (Collectors.toList ());
        }


        private Stream <URL> __manifestUrlStream () {
            final ArrayList <URL> result = new ArrayList <> ();

            try {
                final Enumeration <URL> resources = ClassLoader.getSystemResources (__MANIFEST__);
                while (resources.hasMoreElements()) {
                    result.add (resources.nextElement ());
                }

            } catch (final IOException ioe) {
                __log.warn (ioe, "failed to enumerate manifest resources");
            }

            return result.stream ();
        }

        private Optional <DislManifest> __loadManifest (final URL url) {
            try (
                final InputStream is = url.openStream ();
            ) {
                final Manifest manifest = new Manifest (is);

                //
                // Extract DiSL and transformer class names from the manifest.
                // Merge classes from DiSL-Transformer and DiSL-Transformers.
                //
                final Optional <Attributes> attrs = Optional.ofNullable (manifest.getMainAttributes ());
                final List <String> classes = __getClasses (attrs, __ATTR_DISL_CLASSES__);
                final List <String> transformers = __getClasses (attrs, __ATTR_DISL_TRANSFORMER__);
                transformers.addAll (__getClasses (attrs, __ATTR_DISL_TRANSFORMERS__));

                __log.debug (
                    "loaded %s, classes [%s], transformers [%s]",
                    url, String.join (",", classes), String.join (",", transformers)
                );

                return Optional.of (new DislManifest (url, classes, transformers));

            } catch (final IOException ioe) {
                __log.warn (ioe, "failed to load manifest at %s", url);
            }

            return Optional.empty ();
        }


        private List <String> __getClasses (final Optional <Attributes> attrs, final String attrName) {
            return __getClassNames (attrs.map (a -> a.getValue (attrName)));
        }
    }


    private static final class DislManifest {
        final Optional <URL> __url;
        final List <String> __classes;
        final List <String> __transformers;

        DislManifest (final List <String> classes) {
            this (null, classes, Collections.emptyList ());
        }

        DislManifest (final URL url, final List <String> classes, final List <String> transformers) {
            __url = Optional.ofNullable (url);
            __classes = Objects.requireNonNull (classes);
            __transformers = Objects.requireNonNull (transformers);
        }

        //


        Stream <String> classes () {
            return __classes.stream ();
        }

        Stream <String> transformers () {
            return __transformers.stream ();
        }

        boolean isEmpty () {
            return __classes.isEmpty () && __transformers.isEmpty ();
        }

        boolean isInstrumentation () {
            return !__classes.isEmpty ();
        }

        Optional <URL> resource () {
            return __url;
        }

    }
}
