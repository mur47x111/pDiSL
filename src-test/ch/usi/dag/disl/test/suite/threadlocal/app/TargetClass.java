package ch.usi.dag.disl.test.suite.threadlocal.app;

public class TargetClass {

	public void foo() {
	    // to be instrumented
	}

	public static void main(final String[] args) {
        Thread.currentThread().setName ("primary");
		new TargetClass().foo();

		new Thread() {
			@Override
            public void run() {
			    setName ("secondary");
				new TargetClass().foo();
			}
		}.start ();
	}
}
