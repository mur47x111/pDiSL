package ch.usi.dag.disl;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.JavaNames;
import ch.usi.dag.disl.util.ReflectionHelper;


final class TLVInserter extends ClassVisitor {

    private static final Type __threadType__ = Type.getType (Thread.class);
    private static final String __currentThreadName__ = "currentThread";
    private static final Type __currentThreadType__ = Type.getType (
        ReflectionHelper.getMethod (Thread.class, __currentThreadName__)
    );

    //

    private final Set <ThreadLocalVar> __threadLocals__;

    //

    public TLVInserter (final ClassVisitor cv, final Set <ThreadLocalVar> tlvs) {
        super (Opcodes.ASM5, cv);
        __threadLocals__ = tlvs;
    }


    @Override
    public void visit (
        final int version, final int access, final String name,
        final String signature, final String superName, final String [] interfaces
    ) {
        super.visit (version, access, name, signature, superName, interfaces);
        assert __threadType__.getInternalName().equals (name);
    }


    @Override
    public MethodVisitor visitMethod (
        final int access, final String name, final String desc,
        final String sig, final String [] exceptions
    ) {
        final MethodVisitor mv = super.visitMethod (
            access, name, desc, sig, exceptions
        );

        if (JavaNames.isConstructorName (name)) {
            // Add field initialization code to the constructor.
            return new TLVInitializer (mv, access, name, desc);
        } else {
            return mv;
        }
    }

    @Override
    public void visitEnd () {
        // Add instance fields to the class.
        for (final ThreadLocalVar tlv : __threadLocals__) {
            super.visitField (
                Opcodes.ACC_PUBLIC, tlv.getName (), tlv.getDescriptor (),
                null /* no generic signature */, null /* no static value */
            );
        }

        super.visitEnd ();
    }

    //

    private class TLVInitializer extends AdviceAdapter {

        private TLVInitializer (
            final MethodVisitor mv,
            final int access, final String name, final String desc
        ) {
            super (Opcodes.ASM5, mv, access, name, desc);
            assert JavaNames.isConstructorName (name);
        }

        //

        @Override
        protected void onMethodEnter () {
            //
            // Insert initialization code for each thread local variable.
            // The code for inheritable variables has the following structure:
            //
            //   if (Thread.currentThread() != null) {
            //     this.value = Thread.currentThread().value;
            //   } else {
            //     this.value = <predefined-value> | <type-specific default>
            //   }
            //
            // The code for initialized variables has the following structure:
            //     this.value = <predefined-value> | <type-specific default>
            //
            for (final ThreadLocalVar tlv : __threadLocals__) {
                // Load "this" on the stack, for the final PUTFIELD.
                __loadThis ();

                final Label setValueLabel = new Label();
                if (tlv.isInheritable()) {
                    //
                    // Get initial value from the current thread. If the
                    // current thread is invalid, use the type-specific
                    // default value.
                    //
                    final Label getDefaultValueLable = new Label();

                    __loadCurrentThread ();
                    __jumpIfNull (getDefaultValueLable);

                    __loadCurrentThread ();
                    __getThreadField (tlv);
                    __jump (setValueLabel);

                    __setJumpTarget (getDefaultValueLable);
                }

                //
                // Load the initial value on the stack. If there is no
                // predefined initial value, a type-specific default is used.
                //
                __loadInitialValue (tlv);

                //
                // Store the value into the corresponding field. This stores
                // either the "inherited" or the predefined/default value.
                //
                __setJumpTarget (setValueLabel);
                __putThreadField (tlv);
            }
        }

        private void __loadInitialValue (final ThreadLocalVar tlv) {
            if (tlv.getInitialValue () != null) {
                __loadConstant (tlv.getInitialValue ());
            } else {
                __loadDefault (tlv.getType ());
            }
        }

        //

        private void __jump (final Label target) {
            visitJumpInsn (GOTO, target);
        }


        private void __jumpIfNull (final Label target) {
            visitJumpInsn (IFNULL, target);
        }

        private void __setJumpTarget (final Label target) {
            visitLabel (target);
        }

        //

        private void __loadThis () {
            AsmHelper.loadThis ().accept (this);
        }

        private void __loadDefault (final Type type) {
            AsmHelper.loadDefault (type).accept (this);
        }

        private void __loadConstant (final Object value) {
            AsmHelper.loadConst (value).accept (this);
        }

        private void __loadCurrentThread () {
            AsmHelper.invokeStatic (
                __threadType__, __currentThreadName__, __currentThreadType__
            ).accept (this);
        }

        //

        private void __getThreadField (final ThreadLocalVar tlv) {
            AsmHelper.getField (
                __threadType__, tlv.getName (), tlv.getDescriptor ()
            ).accept (this);
        }

        private void __putThreadField (final ThreadLocalVar tlv) {
            AsmHelper.putField (
                __threadType__, tlv.getName (), tlv.getDescriptor ()
            ).accept (this);
        }

    }
}
