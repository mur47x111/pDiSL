package ch.usi.dag.disl.classparser;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.DiSL.CodeOption;
import ch.usi.dag.disl.InitializationException;
import ch.usi.dag.disl.annotation.ArgumentProcessor;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.processor.ArgProcessor;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.util.ClassNodeHelper;


/**
 * Parser for DiSL classes containing either snippets or method argument
 * processors. This class is not meant for public use.
 */
public final class DislClasses {

    private final SnippetParser __snippetParser;

    //

    private DislClasses (final SnippetParser snippetParser) {
        // not to be instantiated from outside
        __snippetParser = snippetParser;
    }

    //

    public static DislClasses load (
        final Set <CodeOption> options, final Stream <String> classNames
    ) throws ParserException, StaticContextGenException, ReflectionException,
    ProcessorException {
        //
        // Get an ASM tree representation of the DiSL classes first, then
        // parse them as snippets or argument processors depending on the
        // annotations associated with each class.
        //
        final List <ClassNode> classNodes = classNames
            .map (className -> __createClassNode (className))
            .collect (Collectors.toList ());

        if (classNodes.isEmpty ()) {
            throw new InitializationException (
                "No DiSL classes found. They must be explicitly specified "+
                "using the disl.classes system property or the DiSL-Classes "+
                "attribute in the instrumentation JAR manifest."
            );
        }

        //

        final SnippetParser sp = new SnippetParser ();
        final ArgProcessorParser app = new ArgProcessorParser ();

        for (final ClassNode classNode : classNodes) {
            if (__isArgumentProcessor (classNode)) {
                app.parse (classNode);
            } else {
                sp.parse (classNode);
            }
        }

        //
        // Collect all local variables and initialize the argument processor
        // and snippets.
        //
        final LocalVars localVars = LocalVars.merge (
            sp.getAllLocalVars (), app.getAllLocalVars ()
        );

        // TODO LB: Move the loop to the ArgProcessorParser class
        for (final ArgProcessor processor : app.getProcessors ().values()) {
            processor.init (localVars);
        }

        // TODO LB: Consider whether we need to create the argument processor
        // invocation map now -- we basically discard the argument processors
        // and keep an invocation map keyed to instruction indices! :-(

        // TODO LB: Move the loop to the SnippetParser class
        for (final Snippet snippet : sp.getSnippets ()) {
            snippet.init (localVars, app.getProcessors (), options);
        }

        return new DislClasses (sp);
    }


    private static ClassNode __createClassNode (final String className) {
        //
        // Parse input stream into a class node. Include debug information so
        // that we can report line numbers in case of problems in DiSL classes.
        // Re-throw any exceptions as DiSL initialization exceptions.
        //
        try {
            return ClassNodeHelper.SNIPPET.load (className);

        } catch (final Exception e) {
            throw new InitializationException (
                e, "failed to load DiSL class %s", className
            );
        }
    }


    private static boolean __isArgumentProcessor (final ClassNode classNode) {
        //
        // An argument processor must have an @ArgumentProcessor annotation
        // associated with the class. DiSL instrumentation classes may have
        // an @Instrumentation annotation. DiSL classes without annotations
        // are by default considered to be instrumentation classes.
        //
        if (classNode.invisibleAnnotations != null) {
            final Type apType = Type.getType (ArgumentProcessor.class);

            for (final AnnotationNode annotation : classNode.invisibleAnnotations) {
                final Type annotationType = Type.getType (annotation.desc);
                if (apType.equals (annotationType)) {
                    return true;
                }
            }
        }

        // default: not an argument processor
        return false;
    }

    //

    public List <Snippet> getSnippets () {
        return __snippetParser.getSnippets ();
    }

}
