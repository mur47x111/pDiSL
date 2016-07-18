package ch.usi.dag.disl.snippet;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.DiSL.CodeOption;
import ch.usi.dag.disl.coderep.Code;
import ch.usi.dag.disl.coderep.UnprocessedCode;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.marker.Marker;
import ch.usi.dag.disl.processor.ArgProcessor;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorContext;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorMode;
import ch.usi.dag.disl.util.AsmHelper.Insns;
import ch.usi.dag.disl.util.CodeTransformer;


/**
 * Represents a snippet code template. This template contains the original code
 * produced by the Java compiler, potentially wrapped with dynamic bypass
 * control code and an exception handler to catch all exceptions.
 */
public class SnippetUnprocessedCode {
    /**
     * Determines whether the snippet should control dynamic bypass if the
     * dynamic bypass is enabled.
     */
    private final boolean __snippetDynamicBypass;

    /**
     * The code template we are decorating.
     */
    private final UnprocessedCode __template;

    private final URLClassLoader __urlClassLoader;

    //

    /**
     * Initializes a snippet code template with information about source class,
     * source method, usage of context parameters in the template, and whether
     * the snippet requires automatic control of dynamic bypass.
     */
    public SnippetUnprocessedCode (
            final String className, final MethodNode method,
            final boolean snippetDynamicBypass, final URLClassLoader urlClassLoader
            ) {
        __urlClassLoader = urlClassLoader;
        __template = new UnprocessedCode (className, method, __urlClassLoader);
        __snippetDynamicBypass = snippetDynamicBypass;
    }

    //

    public String className () {
        return __template.className ();
    }


    public String methodName () {
        return __template.methodName ();
    }

    //

    /**
     * Processes the stored data and creates snippet code structure.
     */
    public SnippetCode process (
        final LocalVars vars, final Map <Type, ArgProcessor> processors,
        final Marker marker, final Set <CodeOption> options
    ) throws ProcessorException, ReflectionException  {
        //
        // Pre-process code with local variables.
        //
        final Code code = __template.process (vars);

        //
        // Process code:
        //
        // - reclaim local variable slots taken by snippet context parameters
        //
        //   The context parameters to a snippet take up local variable slots.
        //   These are not used at runtime, needlessly increasing the stack
        //   frame. To reclaim these slots, we shift all local variable
        //   accesses by the amount of slots occupied by the context
        //   parameters.
        //
        // - If required, insert dynamic bypass control around the snippet.
        // - If required, catch all exceptions around the snippet.
        //
        // Code processing has to be done before looking for argument processor
        // invocations, otherwise the analysis will produce wrong instruction
        // references.
        //
        final List <CodeTransformer> transformers = new ArrayList <> ();

        transformers.add (
            new ShiftLocalVarSlotCodeTransformer (-code.getParameterSlotCount ())
        );

        if (options.contains (CodeOption.DYNAMIC_BYPASS) && __snippetDynamicBypass) {
            transformers.add (new InsertDynamicBypassControlCodeTransformer ());
        }

        if (options.contains (CodeOption.CATCH_EXCEPTIONS)) {
            transformers.add (
                new InsertExceptionHandlerCodeTransformer (
                    __template.location (), code.getTryCatchBlocks ()
                )
            );
        }

        final InsnList insns = code.getInstructions ();
        CodeTransformer.apply (insns, transformers);

        //
        // Analyze code:
        //
        // Find argument processor invocations so that we can determine the
        // complete set of static context methods invoked within the snippet.
        // This is required later to prepare static context data for all snippet
        // invocations.
        //
        // No other modification should be done to the snippet code before
        // weaving, otherwise the produced instruction references will be
        // invalid.
        //
        // TODO LB: Why do we reference the invocations by bytecode index and
        // not an instruction node reference? Possibly because the index will
        // be still valid after cloning the code.
        //
        final Map <Integer, ProcInvocation> argProcInvocations =
            __collectArgProcInvocations (insns, processors, marker);

        return new SnippetCode (code, argProcInvocations);
    }


    private Map <Integer, ProcInvocation>  __collectArgProcInvocations (
        final InsnList insns, final Map <Type, ArgProcessor> procs, final Marker marker
    ) throws ProcessorException, ReflectionException {
        final Map <Integer, ProcInvocation> result = new HashMap <> ();

        int insnIndex = 0;
        for (final AbstractInsnNode insn : Insns.selectAll (insns)) {
            final ProcessorInfo apInfo = insnInvokesProcessor (
                insn, insnIndex, procs, marker
            );

            if (apInfo != null) {
                result.put (apInfo.insnIndex, apInfo.invocation);
            }

            insnIndex++;
        }

        return result;
    }


    private static class ProcessorInfo {
        final Integer insnIndex;
        final ProcInvocation invocation;

        public ProcessorInfo (final Integer insnIndex, final ProcInvocation invocation) {
            this.insnIndex = insnIndex;
            this.invocation = invocation;
        }
    }


    private ProcessorInfo insnInvokesProcessor (
        final AbstractInsnNode instr, final int i,
        final Map <Type, ArgProcessor> processors, final Marker marker
    ) throws ProcessorException, ReflectionException {
        // check method invocation
        if (!(instr instanceof MethodInsnNode)) {
            return null;
        }

        // check if the invocation is processor invocation
        final MethodInsnNode min = (MethodInsnNode) instr;
        final String apcClassName = Type.getInternalName (ArgumentProcessorContext.class);
        if (!apcClassName.equals (min.owner)) {
            return null;
        }

        if (!"apply".equals (min.name)) {
            return null;
        }

        // resolve load parameter instruction
        final AbstractInsnNode secondParam = Insns.REVERSE.nextRealInsn (instr);
        final AbstractInsnNode firstParam = Insns.REVERSE.nextRealInsn (secondParam);

        // NOTE: object parameter is ignored - will be removed by weaver

        // the first parameter has to be loaded by LDC
        if (firstParam == null || firstParam.getOpcode() != Opcodes.LDC) {
            throw new ProcessorException (
                "%s: pass the first (class) argument to the apply() method "+
                "directly as a class literal", __template.location (min)
            );
        }

        // the second parameter has to be loaded by GETSTATIC
        if (secondParam == null || secondParam.getOpcode() != Opcodes.GETSTATIC) {
            throw new ProcessorException (
                "%s: pass the second (type) argument to the apply() method "+
                "directly as an enum literal", __template.location (min)
            );
        }


        final Object asmType = ((LdcInsnNode) firstParam).cst;
        if (!(asmType instanceof Type)) {
            throw new ProcessorException (
                "%s: unsupported processor type %s",
                __template.location (min), asmType.getClass ().toString ()
            );
        }

        final Type processorType = (Type) asmType;
        final ArgumentProcessorMode procApplyType = ArgumentProcessorMode.valueOf (
            ((FieldInsnNode) secondParam).name
        );

        // if the processor apply type is CALLSITE_ARGS
        // the only allowed marker is BytecodeMarker
        if (ArgumentProcessorMode.CALLSITE_ARGS.equals (procApplyType)
            && marker.getClass () != BytecodeMarker.class
        ) {
            throw new ProcessorException (
                "%s: ArgumentProcessor applied in the CALLSITE_ARGS mode can "+
                "be only used with the BytecodeMarker", __template.location (min)
            );
        }

        final ArgProcessor processor = processors.get (processorType);
        if (processor == null) {
            throw new ProcessorException (
                "%s: unknown processor: %s", __template.location (min),
                processorType.getClassName ()
            );
        }

        //
        // Create an argument processor invocation instance tied to a
        // particular instruction index.
        //
        return new ProcessorInfo (
            i, new ProcInvocation (processor, procApplyType)
        );
    }

}
