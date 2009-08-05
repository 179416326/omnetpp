package org.omnetpp.test.gui;

import org.eclipse.swt.widgets.Display;

/**
 * Aspect for manipulating the GUI from JUnit test methods running 
 * in a background thread.
 * 
 * @author Andras
 */
public aspect UIThread {
	private static final double RETRY_TIMEOUT = 5;  // seconds
	
	/**
	 * Surround methods marked with @InUIThread with Display.syncExec() 
	 * calls. These methods usually operate on SWT widgets, the UI event 
	 * queue, etc, which would cause Illegal Thread Access when done from 
	 * a background thread.
	 *
	 * If the call fails (throws an exception), we'll keep retrying
	 * for 5 seconds until we report failure by re-throwing the exception
	 * 
	 * When the code is already executing in the UI thread, just
	 * proceed with the call.
	 */
	Object around(): execution(@InUIThread public * *(..)) {
	    String method = thisJoinPointStaticPart.getSignature().getDeclaringType().getName() + "." + thisJoinPointStaticPart.getSignature().getName();
	    if (Display.getCurrent() != null) {
	    	System.out.println("AJ: doing " + method + " (already in UI thread)");
	    	return proceed();
	    }
	    else {
	    	System.out.println("AJ: doing in UI thread: " + method);
	    	return GUITestCase.runStepWithTimeout(RETRY_TIMEOUT, new GUITestCase.Step() {
	    		public Object runAndReturn() {
	    			return proceed();
	    		}
	    	});
	    }
	}

	/**
	 * JUnit test case methods should be run in a background thread so that
	 * they can run independent of the UI.
	 */
	void around(final GUITestCase t): target(t) && (execution(public void test*()) || 
			execution(void setUp()) || execution(void tearDown())) {
	    String method = thisJoinPointStaticPart.getSignature().getDeclaringType().getName() + "." + thisJoinPointStaticPart.getSignature().getName();
	    System.out.println("AJ: running test case: " + method);
	    try {
	    	t.runTest(t.new Test() {
	    		public void run() throws Exception {
	    			proceed(t);
	    		}
	    	});
	    } 
	    catch (Throwable e) {
	    	throw new TestException(e);
	    }
	}
	
	
}
