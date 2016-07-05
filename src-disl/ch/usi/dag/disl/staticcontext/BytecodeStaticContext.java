package ch.usi.dag.disl.staticcontext;

/**
 * Provides static context information about instrumented bytecode.
 * <p>
 * <b>Note:</b> This class is being deprecated. Please use the
 * {@link InstructionStaticContext} class instead.
 */
@Deprecated
public class BytecodeStaticContext extends AbstractStaticContext {

	/**
	 * Returns (ASM) integer number of the instrumented bytecode.
	 */
	public int getBytecodeNumber() {

		return staticContextData.getRegionStart().getOpcode();
	}
}
