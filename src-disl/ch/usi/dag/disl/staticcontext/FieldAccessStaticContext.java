package ch.usi.dag.disl.staticcontext;

import org.objectweb.asm.tree.FieldInsnNode;

import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.util.JavaNames;


/**
 * Represents a field access static context. Provides field's name and type
 * descriptor, along with the internal name of the field's owner class.
 * <p>
 * <b>Note:</b> This context can only be used with {@link BytecodeMarker}
 * triggering on field access instructions, i.e., GETFIELD, PUTFIELD, GETSTATIC,
 * and PUTSTATIC. If you are not sure whether the context can be used, use the
 * {@link #isValid()} method to check if the context is valid.
 *
 * @author Aibek Sarimbekov
 * @author Lubomir Bulej
 */
public final class FieldAccessStaticContext extends AbstractStaticContext {

    public FieldAccessStaticContext () {
        // invoked by DiSL
    }


    /**
     * @return {@code True} if the context is valid.
     */
    public boolean isValid () {
        return staticContextData.getRegionStart () instanceof FieldInsnNode;
    }


    /**
     * @return The field's name.
     */
    public String getName () {
        return __getFieldInsnNode ().name;
    }


    /**
     * @return The field's type descriptor.
     */
    public String getDescriptor () {
        return __getFieldInsnNode ().desc;
    }


    /**
     * Returns the type name of the field's owner class. This is a fully
     * qualified class name, with packages delimited by the '.' character.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use the
     * {@link #getOwnerName()} method instead.
     *
     * @return The type name of the field's owner class.
     */
    @Deprecated
    public String getOwnerClassName () {
        return getOwnerName ();
    }


    /**
     * Returns the type name of the field's owner class. This is a fully
     * qualified class name, with packages delimited by the '.' character.
     *
     * @return The type name of the field's owner class.
     */
    public String getOwnerName () {
        return JavaNames.internalToType (__getFieldInsnNode ().owner);
    }


    /**
     * Returns the internal name of the field's owner class. This is a fully
     * qualified class name, with packages delimited by the '/' character.
     *
     * @return The internal name of the field's owner class. This is
     */
    public String getOwnerInternalName () {
        return __getFieldInsnNode ().owner;
    }

    //

    // TODO Add query methods for access type.

    //

    private FieldInsnNode __getFieldInsnNode () {
        //
        // This will throw an exception when used in a region that does not
        // start with a field access instruction.
        //
        return (FieldInsnNode) staticContextData.getRegionStart ();
    }

}
