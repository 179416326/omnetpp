/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package com.simulcraft.test.gui.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.omnetpp.common.util.FileUtils;
import org.omnetpp.common.util.StringUtils;

import com.simulcraft.test.gui.Activator;
import com.simulcraft.test.gui.access.Access;

/**
 * Experimentally detects keyboard layout. Not needed, at least on Windows, because 
 * Win32 has a VkKeyScan API call.
 * @author Andras
 */
public class KeyboardLayout {
    private Map<Character,KeyStroke> mapping = null;
    private char receivedChar; // used during testing the keyboard
    
    public KeyboardLayout() {
    }

    public KeyStroke getKeyFor(char character) {
        Assert.isTrue(mapping!=null, "keyboard layout not tested yet");
        Assert.isTrue(mapping.containsKey(character), "don't know how to produce '"+character+"' on the keyboard");
        return mapping.get(character);
    }

    public Map<Character,KeyStroke> getMapping() {
        return mapping;
    }
    
    public boolean isEmpty() {
        return mapping == null || mapping.isEmpty();
    }

    public void testKeyboard() {
        Assert.isTrue(Display.getCurrent() != null, "must be in the UI thread");
        
        final Shell shell = new Shell(SWT.TOOL | SWT.ON_TOP | SWT.APPLICATION_MODAL);
        shell.setSize(200, 20);
        Control text = new Text(shell, SWT.SINGLE | SWT.BORDER);
        shell.setLayout(new FillLayout());
        shell.layout();
        shell.open();
        text.setFocus();
        
        text.addListener(SWT.KeyDown, new Listener() {
            public void handleEvent(Event e) {
                if (receivedChar != 0)
                    MessageDialog.openWarning(shell, "Warning", "Please do NOT press that key again! Or any other key.\nThat's interfering with keyboard layout detection.");
                receivedChar = e.character;
            }
        });
        
        long startTime = System.currentTimeMillis();
        mapping = new HashMap<Character, KeyStroke>();
        for (char naturalKey = 32; naturalKey < 127; naturalKey++) {
            if (naturalKey>='A' && naturalKey<='Z') 
                continue;  // we test letter keys on the keyboard in lowercase only 
            testKey(naturalKey, 0);
            testKey(naturalKey, SWT.SHIFT);
        }
        System.out.println("Keyboard tested in " + (System.currentTimeMillis()-startTime) + " millis");

        shell.dispose();
    }

    protected void testKey(char naturalKey, int modifier) {
        new Access().pressKey(naturalKey, modifier);
        while (Display.getCurrent().readAndDispatch());
        registerKeyStroke(naturalKey, modifier, receivedChar);
        receivedChar = 0;
    }

    protected void registerKeyStroke(char naturalKey, int modifier, char ch) {
        KeyStroke keyStroke = KeyStroke.getInstance(modifier, naturalKey);
        //System.out.println(keyStroke.format() + " ==> " + receivedChar);
        if (!mapping.containsKey(ch))
            mapping.put(ch, keyStroke);
        else {
            // only overwrite if new one is better
            KeyStroke existingKeyStroke = mapping.get(ch);
            if (existingKeyStroke.getModifierKeys()==0 && existingKeyStroke.getNaturalKey() != ch) // already good enough
                if (modifier==0 || (naturalKey>='a' && naturalKey<='z') || (naturalKey>='0' && naturalKey<='9')) // prefer keys without SHIFT, and letter and number keys
                    mapping.put(ch, keyStroke);
        }
    }
    
    public void saveMapping(String filename) {
        try {
            String contents = "";
            for (char ch : mapping.keySet())
                contents += ch + " " + mapping.get(ch).format() + "\n";
            FileUtils.copy(new ByteArrayInputStream(contents.getBytes()), new File(filename));
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void loadMapping(String filename) {
        try {
            mapping = null;
            HashMap<Character, KeyStroke> tmp = new HashMap<Character, KeyStroke>();
            String contents = FileUtils.readTextFile(filename);
            for (String line : StringUtils.splitToLines(contents)) {
                line = line.replace("\n", "");
                if (line.length()<3)
                    throw new ParseException("line too short in " + filename);
                char ch = line.charAt(0);
                String key = line.substring(2);
                tmp.put(ch, KeyStroke.getInstance(key));
            }
            mapping = tmp; // only install if loaded without error
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void printMapping() {
        System.out.println("Keyboard mapping:");
        Character[] characters = mapping.keySet().toArray(new Character[]{});
        Arrays.sort(characters);
        for (Character character : characters)
            System.out.println(" for "+character+" press "+mapping.get(character));
    }
    
    
    public void loadOrTestKeyboardLayout() {
        String filename = Activator.getDefault().getStateLocation().append("keyboard-layout").toOSString();
        try {
            // exceptions will cause keyboardLayout to remain (or become) empty
            loadMapping(filename);
        }
        catch (RuntimeException e) {
            Activator.logError(e); // could not load table
        }

        // if we couldn't load it, re-test keyboard
        if (isEmpty()) {
            testKeyboard();

            try {
                saveMapping(filename);
            }
            catch (RuntimeException e) {
                Activator.logError(e); // could not save the result -- will have to re-test it next time as well
            }
        }
    }
    

}
