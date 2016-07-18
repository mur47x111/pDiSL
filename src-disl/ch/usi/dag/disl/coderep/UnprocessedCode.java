package ch.usi.dag.disl.coderep;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.InitializationException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.localvar.AbstractLocalVar;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.AsmHelper.Insns;
import ch.usi.dag.disl.util.CodeTransformer;
import ch.usi.dag.disl.util.JavaNames;
import ch.usi.dag.disl.util.ReflectionHelper;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;


/**
 * Represents a snippet or argument processor code template. This template only
 * contains the original code, without any modifications related, e.g., to the
 * use of thread local variables. A instance of the template containing the
 * modified/processed code can be obtained on demand using the
 * {@link #process(LocalVars) process()} method.
 */
public class UnprocessedCode {

    /** Canonical name of the class where the snippet was defined. */
    private final String __className;

    /** Method node containing the snippet code. */
    private final MethodNode __method;

    private final URLClassLoader __urlClassLoader;

    //

    public UnprocessedCode (
        final String className, final MethodNode method, final URLClassLoader urlClassLoader
    ) {
        __className = className;
        __method = method;
        __urlClassLoader = urlClassLoader;
    }

    //

    public String className () {
        return __className;
    }

    public String methodName () {
        return __method.name;
    }

    public String location () {
        return location (__method.instructions.getFirst ());
    }

    public String location (final AbstractInsnNode insn) {
        return String.format (
            "snippet %s.%s%s", __className, __method.name,
            AsmHelper.formatLineNo (":%d ", insn)
        );
    }

    //

    public Code process (final LocalVars vars) {
        //
        // Analyze code:
        //
        // Determine the kinds of contexts used in the snippet parameters.
        //
        // Collect a set of static context methods invoked by the snippet code.
        // This is done first, because it can result in initialization failure.
        //
        // Then collect the sets of referenced synthetic local and thread local
        // variables, and finally determine if there is any exception handler in
        // the code that handles the exception and does not propagate it.
        //
        final ContextUsage ctxs = ContextUsage.forMethod (__method, __urlClassLoader);
        final Set <StaticContextMethod> scms = __collectStaticContextMethods (
            __method.instructions, ctxs.staticContextTypes ()
        );

        final Set <SyntheticLocalVar> slvs = __collectReferencedVars (
            __method.instructions, vars.getSyntheticLocals ()
        );

        final Set <ThreadLocalVar> tlvs = __collectReferencedVars (
            __method.instructions, vars.getThreadLocals ()
        );

        final boolean handlesExceptions = __handlesExceptionWithoutThrowing (
            __method.instructions, __method.tryCatchBlocks
        );

        //
        // Process code:
        //
        // Clone the method code so that we can transform it, then:
        //
        // - replace all RETURN instructions with a GOTO to the end of a method
        // - rewrite accesses to thread-local variables
        //
        // Finally create an instance of processed code.
        //
        final MethodNode method = AsmHelper.cloneMethod (__method);

        CodeTransformer.apply (method.instructions,
            new ReplaceReturnsWithGotoCodeTransformer (),
            new RewriteThreadLocalVarAccessesCodeTransformer (tlvs)
        );

        return new Code (
            method, slvs, tlvs, scms, handlesExceptions
        );
    }


    /**
     * Collects instances of unique static method invocations from the given
     * list of byte code instructions. Throws an exception if any of the
     * invocations is invalid.
     *
     * @param insns
     *        instructions to analyze
     * @param scTypes
     *        a set of known static context types
     * @return A set of {@link StaticContextMethod} instances.
     * @throws InitializationException
     *         if the static context method invocation is invalid, i.e., it
     *         contains arguments, has an invalid return type, or any of the
     *         referenced classes or methods could not be found via reflection
     */
    private Set <StaticContextMethod> __collectStaticContextMethods (
        final InsnList insns, final Set <Type> scTypes
    ) {
        try {
            final ConcurrentMap <String, Boolean> seen = new ConcurrentHashMap <> ();
            return Insns.asList (insns).parallelStream ().unordered ()
                //
                // Select instructions representing method invocations on known
                // static context classes.
                //
                .filter (insn -> insn instanceof MethodInsnNode)
                .map (insn -> (MethodInsnNode) insn)
                .filter (insn -> scTypes.contains (Type.getObjectType (insn.owner)))
                //
                // Ensure that each static context method invocation is valid.
                // This means that it does not have any parameters and only
                // returns either a primitive type or a String.
                //
                .filter (insn -> {
                    // Throws InvalidStaticContextInvocationException.
                    __ensureInvocationHasNoArguments (insn);
                    __ensureInvocationReturnsAllowedType (insn);
                    return true;
                })
                //
                // Finally create an instance of static method invocation, but
                // only for methods we have not seen so far.
                //
                .filter (insn -> seen.putIfAbsent (__methodId (insn), true) == null)
                .map (insn -> {
                    // Throws InvalidStaticContextInvocationException.
                    final Class <?> ownerClass = __resolveClass (insn);
                    final Method contextMethod = __resolveMethod (insn, ownerClass);
                    return new StaticContextMethod (
                        __methodId (insn), contextMethod, ownerClass
                    );
                })
                .collect (Collectors.toSet ());

        } catch (final InvalidStaticContextInvocationException e) {
            final MethodInsnNode insn = e.insn ();
            throw new InitializationException (
                "%s: invocation of static context method %s.%s: %s",
                location (insn), JavaNames.internalToType (insn.owner),
                insn.name, e.getMessage ()
            );
        }
    }


    private static String __methodId (final MethodInsnNode methodInsn) {
        return JavaNames.methodName (methodInsn.owner, methodInsn.name);
    }


    private void __ensureInvocationHasNoArguments (final MethodInsnNode insn) {
        if (Type.getArgumentTypes (insn.desc).length != 0) {
            throw new InvalidStaticContextInvocationException (
                "arguments found, but NONE allowed", insn
            );
        }
    }


    private void __ensureInvocationReturnsAllowedType (final MethodInsnNode insn) {
        final Type returnType = Type.getReturnType (insn.desc);
        if (! __ALLOWED_RETURN_TYPES__.contains (returnType)) {
            throw new InvalidStaticContextInvocationException (
                "return type MUST be a primitive type or a String", insn
            );
        }
    }


    @SuppressWarnings ("serial")
    private static final Set <Type> __ALLOWED_RETURN_TYPES__ = Collections.unmodifiableSet (
        new HashSet <Type> (9) {{
            add (Type.BOOLEAN_TYPE);
            add (Type.BYTE_TYPE);
            add (Type.CHAR_TYPE);
            add (Type.SHORT_TYPE);
            add (Type.INT_TYPE);
            add (Type.LONG_TYPE);

            add (Type.FLOAT_TYPE);
            add (Type.DOUBLE_TYPE);

            add (Type.getType (String.class));
        }}
    );


    private Class <?> __resolveClass (final MethodInsnNode insn) {
        try {
            return ReflectionHelper.resolveClass (Type.getObjectType (insn.owner), __urlClassLoader);

        } catch (final ReflectionException e) {
            throw new InvalidStaticContextInvocationException (e.getMessage (), insn);
        }
    }


    private Method __resolveMethod (
        final MethodInsnNode insn, final Class <?> ownerClass
    ) {
        try {
            return ReflectionHelper.resolveMethod (ownerClass,  insn.name);

        } catch (final ReflectionException e) {
            throw new InvalidStaticContextInvocationException (e.getMessage (), insn);
        }
    }

    //

    /**
     * Scans the given instruction sequence for field accesses and collects a
     * set of special local variables referenced by instructions in the
     * sequence. The local variables are identified by a fully qualified field
     * name.
     *
     * @param <T> type of the return value
     * @param insn the instruction sequence to scan
     * @param vars mapping between fully qualified field names and variables
     * @return a set of variables references by the code.
     */
    private <T> Set <T> __collectReferencedVars (
        final InsnList insns, final Map <String, T> vars
    ) {
        return Insns.asList (insns).parallelStream ().unordered ()
            .filter (insn -> insn instanceof FieldInsnNode)
            .map (insn -> {
                final FieldInsnNode fi = (FieldInsnNode) insn;
                return Optional.ofNullable (vars.get (
                    AbstractLocalVar.fqFieldNameFor (fi.owner, fi.name)
                ));
            })
            .filter (o -> o.isPresent ())
            .map (o -> o.get ())
            .collect (Collectors.toSet ());
    }

    //

    /**
     * Determines if the code contains an exception handler that handles
     * exceptions and does not propagate them further. This has to be detected
     * because it can cause stack inconsistency that has to be handled in the
     * weaver.
     */
    private boolean __handlesExceptionWithoutThrowing (
        final InsnList insns, final List <TryCatchBlockNode> tcbs
    ) {
        if (tcbs.size () == 0) {
            return false;
        }

        //
        // Create a control flow graph and check if the control flow continues
        // after an exception handler, which indicates that the handler handles
        // the exception.
        //
        final CtrlFlowGraph cfg = new CtrlFlowGraph (insns, tcbs);
        cfg.visit (insns.getFirst ());

        for (int i = tcbs.size () - 1; i >= 0; --i) {
            final TryCatchBlockNode tcb = tcbs.get (i);
            if (cfg.visit (tcb.handler).size () != 0) {
                return true;
            }
        }

        return false;
    }

}
