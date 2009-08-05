package org.omnetpp.test.gui.access;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.omnetpp.common.util.ReflectionUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.test.gui.InUIThread;

public class Access {
	protected final static boolean debug = true;

	public final static int LEFT_MOUSE_BUTTON = 1;
	public final static int MIDDLE_MOUSE_BUTTON = 2;
	public final static int RIGHT_MOUSE_BUTTON = 3;

	@InUIThread
	public static IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}

	@InUIThread
	public static Display getDisplay() {
		Display display = Display.getCurrent();

		if (display != null)
			return display;
		else
			return Display.getDefault();
	}

	@InUIThread
	public static Shell getActiveShell() {
		return getDisplay().getActiveShell();
	}

	@InUIThread
	public static void postEvent(Event event) {
		if (debug)
			System.out.println("Posting event: " + event);

		getDisplay().post(event);
	}

	@InUIThread
	public static void processEvents() {
		if (debug)
			System.out.println("Processing events started");

		while (getDisplay().readAndDispatch())
			if (debug)
				System.out.println("Processed an event");

		if (debug)
			System.out.println("Processing events finished");
	}

	@InUIThread
	public static void sleep(double seconds) {
		try {
			Thread.sleep((long)(seconds * 1000));
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected Event newEvent(int type) {
		Event event = new Event();
		event.type = type;
		event.display = getDisplay();

		return event;
	}

	@InUIThread
	public void pressKey(int keyCode) {
		pressKey(keyCode, SWT.None);
	}

	@InUIThread
	public void pressKey(int keyCode, int modifierKeys) {
		pressKey((char)0, keyCode, modifierKeys);
	}

	@InUIThread
	public void pressKey(char character) {
		pressKey(character, SWT.None);
	}

	@InUIThread
	public void pressKey(char character, int modifierKeys) {
		pressKey(character, 0, modifierKeys);
	}

	@InUIThread
	public void pressKey(char character, int keyCode, int modifierKeys) {
		Event event;

		if (modifierKeys != 0) {
			if ((modifierKeys & SWT.SHIFT) != 0) {
				event = newEvent(SWT.KeyDown);
				event.keyCode = SWT.SHIFT;
				postEvent(event);
			}

			if ((modifierKeys & SWT.CONTROL) != 0) {
				event = newEvent(SWT.KeyDown);
				event.keyCode = SWT.CONTROL;
				postEvent(event);
			}

			if ((modifierKeys & SWT.ALT) != 0) {
				event = newEvent(SWT.KeyDown);
				event.keyCode = SWT.ALT;
				postEvent(event);
			}
		}

		event = newEvent(SWT.KeyDown);
		event.character = character;
		event.keyCode = keyCode;
		postEvent(event);

		event = newEvent(SWT.KeyUp);
		event.keyCode = keyCode;
		event.character = (char)keyCode;
		postEvent(event);

		if (modifierKeys != 0) {
			if ((modifierKeys & SWT.SHIFT) != 0) {
				event = newEvent(SWT.KeyUp);
				event.keyCode = SWT.SHIFT;
				postEvent(event);
			}

			if ((modifierKeys & SWT.CONTROL) != 0) {
				event = newEvent(SWT.KeyUp);
				event.keyCode = SWT.CONTROL;
				postEvent(event);
			}

			if ((modifierKeys & SWT.ALT) != 0) {
				event = newEvent(SWT.KeyUp);
				event.keyCode = SWT.ALT;
				postEvent(event);
			}
		}
	}

	@InUIThread
	public void typeIn(String text) {
		for (int i = 0; i < text.length(); i++)
			pressKey(text.charAt(i));
	}

	protected void postMouseEvent(int type, int button, int x, int y) {
		Event event = newEvent(type); // e.g. SWT.MouseMove
		event.button = button;
		event.x = x;
		event.y = y;
		event.count = 1;
		postEvent(event);
	}

	@InUIThread
	public void click(int button, int x, int y) {
		postMouseEvent(SWT.MouseMove, button, x, y);
		postMouseEvent(SWT.MouseDown, button, x, y);
		postMouseEvent(SWT.MouseUp, button, x, y);
	}

	@InUIThread
	public void doubleClick(int button, int x, int y) {
		click(button, x, y);
		postMouseEvent(SWT.MouseDown, button, x, y);
		postMouseEvent(SWT.MouseDoubleClick, button, x, y);
		postMouseEvent(SWT.MouseUp, button, x, y);
	}

	@InUIThread
	public void click(int button, Point point) {
		click(button, point.x, point.y);
	}

	@InUIThread
	public void doubleClick(int button, Point point) {
		doubleClick(button, point.x, point.y);
	}

	@InUIThread
	public void clickCenter(int button, Rectangle rectangle) {
		click(button, getCenter(rectangle));
	}

	@InUIThread
	public void doubleClickCenter(int button, Rectangle rectangle) {
		doubleClick(button, getCenter(rectangle));
	}

	@InUIThread
	public void clickCTabItem(CTabItem cTabItem) {
		click(LEFT_MOUSE_BUTTON, cTabItem.getParent().toDisplay(getCenter(cTabItem.getBounds())));
	}

	@InUIThread
	public static Object findObject(Object[] objects, IPredicate predicate) {
		return theOnlyObject(collectObjects(objects, predicate));
	}

	@InUIThread
	public static List<Object> collectObjects(Object[] objects, IPredicate predicate) {
		ArrayList<Object> resultObjects = new ArrayList<Object>();

		for (Object object : objects)
			if (predicate.matches(object))
				resultObjects.add(object);

		return resultObjects;
	}

	@InUIThread
	public static Control findDescendantControl(Composite composite, final Class<? extends Control> clazz) {
		return theOnlyControl(collectDescendantControls(composite, new IPredicate() {
			public boolean matches(Object control) {
				return clazz.isInstance(control);
			}
		}));
	}

	@InUIThread
	public static Control findDescendantControl(Composite composite, IPredicate predicate) {
		return theOnlyControl(collectDescendantControls(composite, predicate));
	}

	@InUIThread
	public static List<Control> collectDescendantControls(Composite composite, IPredicate predicate) {
		ArrayList<Control> controls = new ArrayList<Control>();
		collectDescendantControls(composite, predicate, controls);
		return controls;
	}

	protected static void collectDescendantControls(Composite composite, IPredicate predicate, List<Control> controls) {
		for (Control control : composite.getChildren()) {
			if (control instanceof Composite)
				collectDescendantControls((Composite)control, predicate, controls);

			if (predicate.matches(control))
				controls.add(control);
		}
	}

	protected static CTabItem findDescendantCTabItemByLabel(Composite composite, final String label) {
		CTabFolder cTabFolder = (CTabFolder)findDescendantControl(composite, new IPredicate() {
			public boolean matches(Object object) {
				if (object instanceof CTabFolder) {
					CTabFolder cTabFolder = (CTabFolder)object;
					List<Object> cTabItems = collectObjects(cTabFolder.getItems(), new IPredicate() {
						public boolean matches(Object object) {
							return ((CTabItem)object).getText().matches(label);
						}
					});

					Assert.assertTrue(cTabItems.size() < 2);

					if (cTabItems.size() == 0)
						return false;
					else
						return true;
				}
				return false;
			}
		});

		return (CTabItem)findObject(cTabFolder.getItems(), new IPredicate() {
			public boolean matches(Object object) {
				return ((CTabItem)object).getText().matches(label);
			}
		});
	}

	protected static Object theOnlyObject(List<? extends Object> objects) {
		Assert.assertTrue("Found more than one objects when exactly one is expected", objects.size() < 2);
		Assert.assertTrue("Found zero object when exactly one is expected", objects.size() > 0);
		return objects.get(0);
	}

	protected static Widget theOnlyWidget(List<? extends Widget> widgets) {
		return (Widget)theOnlyObject(widgets);
	}

	protected static Control theOnlyControl(List<? extends Control> controls) {
		return (Control)theOnlyWidget(controls);
	}

	protected static Point getCenter(Rectangle rectangle) {
		return new Point(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height / 2);
	}

	/**
	 * A failed attempt to dump all active popup menus, using the private fields of Display.
	 * Unfortunately, it's all nulls in the arrays. (This is due to runPopups() running
	 * before all syncExec()/asyncExec() code...)
	 */
	@InUIThread
	public static void dumpCurrentMenus() {
		Display display = Display.getDefault();
		Menu[] bars = (Menu[]) ReflectionUtils.getFieldValue(display, "bars");
		Menu[] popups = (Menu[]) ReflectionUtils.getFieldValue(display, "popups");
		MenuItem[] items = (MenuItem[]) ReflectionUtils.getFieldValue(display, "items");
		if (bars == null) bars = new Menu[0];
		if (popups == null) popups = new Menu[0];
		if (items == null) items = new MenuItem[0];

		System.out.println(bars.length + " menubars, "+popups.length + " popup menus, " + items.length + " menu items");
		for (Menu bar : bars) {
			System.out.println("  menubar: " + bar);
			if (bar != null)
				for (MenuItem item : bar.getItems())
					System.out.println("    " + item.toString());
		}
		for (Menu popup : popups) {
			System.out.println("  popup: " + popup);
			if (popup != null)
				for (MenuItem item : popup.getItems())
					System.out.println("    " + item.toString());
		}
	}

	/**
	 * Dumps all widgets of all known shells. Note: active popup menus are
	 * sadly not listed (not returned by Display.getShells()).
	 */
	@InUIThread
	public static void dumpWidgetHierarchy() {
		System.out.println("display-root");
		for (Shell shell : Display.getDefault().getShells())
			dumpWidgetHierarchy(shell, 1);
	}

	@InUIThread
	public static void dumpWidgetHierarchy(Control control) {
		dumpWidgetHierarchy(control, 0);
	}

	protected static void dumpWidgetHierarchy(Control control, int level) {
		System.out.println(StringUtils.repeat("  ", level) + control.toString());
		if (control instanceof Composite)
			for (Control child : ((Composite)control).getChildren())
				dumpWidgetHierarchy(child, level+1);
	}
}
