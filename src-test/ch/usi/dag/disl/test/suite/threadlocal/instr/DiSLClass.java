package ch.usi.dag.disl.test.suite.threadlocal.instr;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.marker.BodyMarker;

public class DiSLClass {

    @ThreadLocal
    static boolean booleanF = false;

    @ThreadLocal
    static boolean booleanT = true;


    @ThreadLocal
    static byte byteM1 = -1;

    @ThreadLocal
    static char char127 = 127;

    @ThreadLocal
    static short short32767 = 32767;

    @ThreadLocal
    static int int0 = 0;

    @ThreadLocal
    static int int5 = 5;

    @ThreadLocal
    static int intAny = 2147483647;


    @ThreadLocal
    static long long0 = 0l;

    @ThreadLocal
    static long long1 = 1l;

    @ThreadLocal
    static long longAny = 9223372036854775807l;


    @ThreadLocal
    static float float0 = 0.0f;

    @ThreadLocal
    static float float1 = 1.0f;

    @ThreadLocal
    static float float2 = 2.0f;

    @ThreadLocal
    static float floatAny = 3.4028235E38f;


    @ThreadLocal
    static double double0 = 0.0d;

    @ThreadLocal
    static double double1 = 1.0d;

    @ThreadLocal
    static double doubleAny = 1.7976931348623157E308d;


    @ThreadLocal
    static String stringTlv = "hello";

    //

	@Before(marker = BodyMarker.class, scope = "*.foo*", order=0)
	public static void precondition() {
		System.out.printf (
		    "Thread %s, before method body\n",
		    Thread.currentThread ().getName ()
	    );

        // print the values

        System.out.printf ("\tboolean\t%b\n", booleanF);
        System.out.printf ("\tboolean\t%b\n", booleanT);

        System.out.printf ("\tbyte\t%d\n", byteM1);
        System.out.printf ("\tchar\t%d\n", (int) char127);
        System.out.printf ("\tshort\t%d\n", short32767);

        System.out.printf ("\tint\t%d\n", int0);
        System.out.printf ("\tint\t%d\n", int5);
        System.out.printf ("\tint\t%d\n", intAny);

        System.out.printf ("\tlong\t%d\n", long0);
        System.out.printf ("\tlong\t%d\n", long1);
        System.out.printf ("\tlong\t%d\n", longAny);

        System.out.printf ("\tfloat\t%s\n", float0);
        System.out.printf ("\tfloat\t%s\n", float1);
        System.out.printf ("\tfloat\t%s\n", float2);
        System.out.printf ("\tfloat\t%s\n", floatAny);

        System.out.printf ("\tdouble\t%s\n", double0);
        System.out.printf ("\tdouble\t%s\n", double1);
        System.out.printf ("\tdouble\t%s\n", doubleAny);

        System.out.printf ("\tString\t%s\n", stringTlv);

        // change the values

        booleanF = true;
        booleanT = false;

        byteM1 = 127;
        char127 = 65535;
        short32767 = -32768;

        int0 = 5;
        int5 = 0;
        intAny = -2147483648;

        long0 = 1;
        long1 = 0;
        longAny = -9223372036854775808l;

        float0 = 1.0f;
        float1 = 0.0f;
        floatAny = 1.4E-45f;

        double0 = 1.0d;
        double1 = 0.0d;
        doubleAny = 4.9E-324d;

        stringTlv = "bye";
	}

	@After(marker = BodyMarker.class, scope = "*.foo*", order=0)
	public static void postcondition() {
        System.out.printf (
            "Thread %s, after method body\n",
            Thread.currentThread ().getName ()
        );

        // print the values

        System.out.printf ("\tboolean\t%b\n", booleanF);
        System.out.printf ("\tboolean\t%b\n", booleanT);

        System.out.printf ("\tbyte\t%d\n", byteM1);
        System.out.printf ("\tchar\t%d\n", (int) char127);
        System.out.printf ("\tshort\t%d\n", short32767);

        System.out.printf ("\tint\t%d\n", int0);
        System.out.printf ("\tint\t%d\n", int5);
        System.out.printf ("\tint\t%d\n", intAny);

        System.out.printf ("\tlong\t%d\n", long0);
        System.out.printf ("\tlong\t%d\n", long1);
        System.out.printf ("\tlong\t%d\n", longAny);

        System.out.printf ("\tfloat\t%s\n", float0);
        System.out.printf ("\tfloat\t%s\n", float1);
        System.out.printf ("\tfloat\t%s\n", float2);
        System.out.printf ("\tfloat\t%s\n", floatAny);

        System.out.printf ("\tdouble\t%s\n", double0);
        System.out.printf ("\tdouble\t%s\n", double1);
        System.out.printf ("\tdouble\t%s\n", doubleAny);

        System.out.printf ("\tString\t%s\n", stringTlv);
	}
}
