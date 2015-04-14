/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.core;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.omnetpp.common.util.ReflectionUtils;

import com.simulcraft.test.gui.access.Access;
import com.simulcraft.test.gui.access.ClickableAccess;


public abstract class GUITestCase
    extends TestCase
{
    private static boolean debug = false;
    private static double retryTimeout = 5;  // seconds

    /*package*/ static boolean paused = false;  // set by ModeSwitcher

    public abstract class Test {
        public abstract void run() throws Exception;
    }

    public static double getRetryTimeout() {
        return retryTimeout;
    }

    public static void setRetryTimeout(double retryTimeout) {
        GUITestCase.retryTimeout = retryTimeout;
    }

    /**
     * Scales ALL time and delay in the test case. For example, setting it to 2 will result
     * in a test case running twice as slow as normal.
     */
    public static void setTimeScale(double timeScale) {
        Access.setTimeScale(timeScale);
    }

    /**
     * The average time between keypresses during typing
     */
    public static void setKeyboardTypeDelay(int delay) {
        KeyPressAnimator.typingDelay = delay;
    }

    /**
     * The time until the shortcut keys are displayed in a tooltip box
     */
    public static void setShortcutDisplayDelay(int delay) {
        KeyPressAnimator.shortcutDisplayDelay = delay;
    }

    /**
     * A little delay before the mouse starts moving
     */
    public static void setDelayBeforeMouseMove(int delay) {
        ClickableAccess.delayBeforeMouseMove = delay;
    }

    /**
     * A delay after the mouse stops moving
     */
    public static void setDelayAfterMouseMove(int delay) {
        ClickableAccess.delayAfterMouseMove = delay;
    }

    /**
     * How long does it take to move the mouse from the start to the end location
     */
    public static void setMouseMoveDuration(int delay) {
        ClickableAccess.mouseMoveDurationMillis = delay;
    }

    /**
     * How long does it take to click with the mouse
     */
    public static void setMouseClickDuration(int delay) {
        ClickableAccess.mouseClickDurationMillis = delay;
    }

    public static void setMouseClickAnimation(boolean animate) {
        MouseClickAnimator.mouseClickAnimation = animate;
    }

    public static class Step {
        public Object runAndReturn() throws Exception {
            return null;
        }

        public void run() throws Exception {
        }
    }

    /**
     * Runs the given test code (the run() method of the Test object passed)
     * in another thread. This is needed to be able to insert UI events
     * into the event queue while the GUI is running a nested readAndDispatch()
     * loop, e.g. inside modal dialogs.
     */
    public void runTest(final Test test) throws Throwable {
        String testName = new Throwable().getStackTrace()[1].getMethodName();

        try {
            Access.log(debug, "Starting test: " + testName);

            // start the background thread which will query the gui and post events
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        test.run();
                    }
                    catch (final Throwable t) {
                        // we caught an exception from the background thread
                        // we need to re-throw the exception from the UI thread, so the same handling takes place
                        try {
                            Display.getDefault().syncExec(new Runnable() {
                                public void run() {
                                    // closing the shells and menus are repeated here on purpose
                                    closeShells();
                                    closeMenus();
                                    throw new TestException(t);
                                }
                            });
                        }
                        catch (Throwable th) {
                            // void, ignore the exception just been thrown and got back here
                            // because we are in the background thread
                        }
                    }
                }
            }, testName);
            // start the background thread immediately
            thread.start();

            // start processing gui events
            while (thread.isAlive())
                Display.getCurrent().readAndDispatch();
        }
        catch (Throwable t) {
            AnimationEffects.displayError(getInterestingCause(t), 3000);
            // closing the shells and menus are repeated here on purpose
            closeShells();
            closeMenus();
            throw t;
        }
        finally {
            Access.log(debug, "Finished test: " + testName);
        }
    }

    private Throwable getInterestingCause(Throwable t) {
        if (t instanceof TestException) {
            Throwable cause = ((TestException)t).getCause();

            if (cause != null)
                return getInterestingCause(cause);
            else
                return t;
        }
        else if (t instanceof SWTException) {
            Throwable cause = ((SWTException)t).throwable;

            if (cause != null)
                return getInterestingCause(cause);
            else
                return t;
        }
        else
            return t;
    }

    private void closeShells() {
        // KLUDGE: close all shells except the workbench window's shell
        // so that there are no hanging windows left open
        // SWT does not close open windows when exceptions pass through the event loop
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();

        if (activeWorkbenchWindow != null) {
            Shell activeShell = activeWorkbenchWindow.getShell();

            for (Shell shell : Display.getCurrent().getShells()) {
                if (shell != activeShell)
                    shell.close();
            }
        }
    }

    private void closeMenus() {
        // KLUDGE: close all menus so that there are no hanging menus left open
        // SWT does not close open menus when exceptions pass through the event loop
        for (Shell shell : Display.getCurrent().getShells())
            closeMenus(shell);
    }

    private void closeMenus(Composite composite) {
        if (composite instanceof Decorations) {
            Menu[] menus = (Menu[])ReflectionUtils.getFieldValue(composite, "menus");

            if (menus != null)
                for (Menu menu : menus)
                    if (menu != null && menu.isVisible())
                        menu.setVisible(false);
        }

        for (Control control : composite.getChildren())
            if (control instanceof Composite)
                closeMenus(((Composite)control));
    }

    public static Object runStep(final Step step) {
        return runStepWithTimeout(retryTimeout, step);
    }

    /**
     * Runs the given runnable in a synchronized way from the event dispatch thread.
     * The idea is to run the runnable at least once and keep trying if there was an exception and
     * there is still some remaining time to run otherwise throw the first exception caught from the runnable.
     *
     * @param timeToRun -1 means run exactly once while positive values mean the runnable may be run multiple times
     * @param step the runnable to be run from the event dispatch thread
     * @return
     */
    public static Object runStepWithTimeout(double timeToRun, final Step step) {
        if (Display.getCurrent() != null) {
            // if we are already in the UI thread, just plain do it
            try {
                step.run();
                return step.runAndReturn();
            }
            catch (Throwable t) {
                throw new TestException(t);
            }
        }

        //System.out.print("sleep 1s to help debugging");
        //Access.sleep(1);

        long begin = System.currentTimeMillis();
        boolean hasBeenRunOnce = false;
        final Object[] result = new Object[1];
        Throwable firstThrowable = null;
        final Throwable[] stepThrowables = new Throwable[1];

        while (!hasBeenRunOnce || System.currentTimeMillis() - begin < timeToRun * 1000) {
            if (hasBeenRunOnce)
                Access.sleep(0.5);

            Access.log(debug, hasBeenRunOnce ? "Rerunning step" : "Running step");
            stepThrowables[0] = null;

            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    try {
                        step.run();
                        result[0] = step.runAndReturn();
                    }
                    catch (Throwable t) {
                        // just store the exception for later use and ignore it for now
                        stepThrowables[0] = t;
                        Access.log(debug, "Caught: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    }
                }
            });

            Access.log(debug, "Waiting for GUI thread to process events");

            waitUntilEventQueueBecomesEmpty();

            // check if step has been run successfully and return
            if (stepThrowables[0] == null)
                return result[0];
            else if (firstThrowable == null)
                firstThrowable = stepThrowables[0];

            hasBeenRunOnce = true;
        }

        Access.log(debug, "Step failed");
        throw new TestException(firstThrowable);
    }

    public static void waitUntilEventQueueBecomesEmpty() {
        Assert.assertTrue("This method must not be called from the UI thread", Display.getCurrent()==null);
        while (paused && hasWorkbenchWindow()) {
            try {Thread.sleep(200);} catch (InterruptedException e) {}
            Thread.yield();
        }

        // note: actually, the next line waits until the last UI event *begins* processing not when it finishes processing
        while (PlatformUtils.hasPendingUIEvents()) {
            doPendingAsyncExecs();
            Thread.yield();
        }
    }

    private static boolean hasWorkbenchWindow() {
        // note: getActiveWorkbenchWindow() may only be called from UI thread...
        final boolean[] result = new boolean[1];
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                result[0] = PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null;
            }
        });
        return result[0];
    }

    private static void doPendingAsyncExecs() {
        // just do an empty syncExec(); it will be placed at the end of Display's queue,
        // so the call will only return if everything before it has been done too
        Display.getDefault().syncExec(new Runnable() { public void run() {} });
    }
}
