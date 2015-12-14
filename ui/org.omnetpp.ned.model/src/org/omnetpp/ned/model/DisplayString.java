/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import org.eclipse.core.runtime.Assert;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.omnetpp.common.Debug;
import org.omnetpp.common.displaymodel.IDisplayString;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.model.ex.NedElementUtilEx;
import org.omnetpp.ned.model.interfaces.IHasDisplayString;


/**
 * The default implementation of IDisplayString. It is strongly tied to the NedElement
 * model tree, and keeps its contents in sync with the LiteralElement in a display property.
 *
 * IMPORTANT: DisplayString doesn't support a listener list, and please do NOT add one!
 * If you want to get notified about display string changes, listen on the IHasDisplayString
 * NED element. This is the only way to get notified about changes that were made
 * directly on the tree, bypassing the DisplayString object.
 *
 * @author rhornig, andras (cleanup)
 */
//XXX use of the Point and Dimension classes force this plugin to depend on draw2d!
public class DisplayString implements IDisplayString {

    // contains the default fallback values for the different tags if a variable is used in that position
    public static final DisplayString VARIABLE_DEFAULTS = new DisplayString(VARIABLE_DEFAULTS_STR);

    // contains the default fallback values for the different tags if it is empty
    public static final DisplayString EMPTY_DEFAULTS = new DisplayString(EMPTY_DEFAULTS_STR);

    // special value used to remove (neutralize) an inherited value
    public static final String ANTIVALUE = "-";

    // display string used for fallback (i.e. display string of the ancestor type)
    protected DisplayString fallbackDisplayString = null;

    // the owner of the display string
    protected IHasDisplayString owner = null;

    // cached return value of toString(); null means not calculated.
    // Every public method that modifies the object's contents must call invalidate()
    // to null out this field.
    protected String cachedDisplayString = null;

    // map that stores the currently available tag instances
    private Map<String, TagInstance> tagMap = new LinkedHashMap<String, TagInstance>();

    public static class TagInstance {
        private String name = "";
        private Vector<String> args = new Vector<String>(1);

        /**
         * Creates a tag by parsing the given string
         */
        public TagInstance(String tagString) {
            if (tagString != null)
                parseTag(tagString);
        }

        /**
         * Returns the tag name
         */
        public String getName() {
            return name;
        }

        public Tag getTag() {
            return Tag.valueOf(name);
        }

        public int getArgSize() {
            return args.size();
        }

        /**
         * Returns a tag argument at the given position, or an empty string if it does not exist
         */
        public String getArg(int pos) {
            if (pos < 0 || pos >= args.size() || args.get(pos) == null)
                return "";
            return args.get(pos);
        }

        /**
         * Sets the value at a given position.
         */
        public void setArg(int pos, String newValue) {
            // extend the vector if necessary
            args.setSize(Math.max(args.size(), pos+1));
            args.set(pos, newValue);
        }

        /**
         * Check if the tag's value is default. Returns true if all tag values are
         * missing or empty.
         */
        public boolean isEmpty() {
            for (String value : args)
                if (value != null && !value.equals(""))
                    return false;
            return true;
        }

        /**
         * Returns the values part of the tag
         */
        public String getArgString() {
            StringBuffer sb = new StringBuffer(20);

            // check for the last non-default value
            int endPos;
            for (endPos = args.size() - 1; endPos>0; endPos--)
                if (args.get(endPos) != null && !"".equals(args.get(endPos)))
                    break;

            // if there are unnecessary default values at the end, throw them away
            if (endPos < args.size() - 1)
                args.setSize(endPos + 1);

            boolean firstArg = true;
            for (String val : args) {
                if (firstArg)
                    firstArg = false;
                else
                    sb.append(',');

                if (val != null)
                    sb.append(val);
            }
            return sb.toString();
        }

        /**
         * Returns the string representation of the tag, or the empty string
         * if all the args are empty
         */
        @Override
        public String toString() {
            // return an empty string if all the values are default
            if (isEmpty())
                return "";
            return getName() + "=" + getArgString();
        }

        // parse a single tag and its values into a string vector
        private void parseTag(String tagStr) {
            args = new Vector<String>(2);
            Scanner scr = new Scanner(tagStr);

            // TODO string literal and escaping must be correctly handled
            // StreamParser can handle comments too, maybe it would be a better choice
            scr.useDelimiter("=|,");

            // parse for the tag name
            if (scr.hasNext())
                name = scr.next().trim();

            // parse for the tag values with a new tokenizer
            while (scr.hasNext())
                args.add(scr.next().trim());
        }
    }


    /**
     * Create a display string tokenizer class only derived classes allowed to be created
     * @param owner owner of the display string object; its "@display" property will be kept up-to-date
     * @param value The string to be parsed
     */
    public DisplayString(IHasDisplayString owner, String value) {
        this(value);
        this.owner = owner;
    }

    public DisplayString(String value) {
        if (value != null) {
            set(value);
            cachedDisplayString = value;
        }
    }

    /**
     * Owner of this this display string, or null if it does not have one.
     */
    public IHasDisplayString getOwner() {
        return owner;
    }

    /**
     * Returns the currently set fallback display string
     */
    public DisplayString getFallbackDisplayString() {
        return fallbackDisplayString;
    }

    /**
     * Sets the fallback display string used if the current object has empty or null value for a
     * requested property (points to the type or ancestor display string).
     */
    public void setFallbackDisplayString(DisplayString defaults) {
        this.fallbackDisplayString = defaults;
    }

    /**
     * Returns the value of the given tag on the given position
     * @param tag Tag to be checked
     * @param pos Position (0-based)
     * @return TagInstance arg's value, the empty string if empty,  or
     * <code>null</code> if the tag does not exist at all
     */
    protected String getTagArg(Tag tag, int pos) {
        TagInstance tagInst = getTag(tag);

        // if tag doesn't exist, return null
        if (tagInst == null)
            return null;

        // check for the value itself
        String value = tagInst.getArg(pos);
        return value == null ? "" :  value;
    }

    /**
     * Returns TagInstance arg's value or <code>""</code> if empty and no default is defined.
     * If a default display string was provided with <code>setFallbackDisplayString</code>,
     * it tries to look up the property from there.
     */
    protected String getTagArgUsingDefaults(Tag tagName, int pos, boolean useEmptyDefaults, boolean useVariableDefaults) {
        TagInstance tag = getTag(tagName);
        String value = (tag == null) ? "" : tag.getArg(pos);
        Assert.isNotNull(value);

        if (value.startsWith("$") && useVariableDefaults)
            return VARIABLE_DEFAULTS.getTagArg(tagName, pos);
        else if (value.equals(ANTIVALUE))  // antivalue disables inherited values, so we return the default
            return useEmptyDefaults ? EMPTY_DEFAULTS.getTagArg(tagName, pos) : "";
        else if (value.length() > 0)
            return value;
        else if (getFallbackDisplayString() != null)
            return getFallbackDisplayString().getTagArgUsingDefaults(tagName, pos, useEmptyDefaults, useVariableDefaults);
        else
            return useEmptyDefaults ? EMPTY_DEFAULTS.getTagArg(tagName, pos) : "";
    }

    public boolean containsProperty(Prop prop) {
        TagInstance tag = getTag(prop.getTag());
        String value = (tag == null) ? "" : tag.getArg(prop.getPos());
        Assert.isNotNull(value);

        if (!value.equals(""))
            return true;
        else if (getFallbackDisplayString() != null)
            return getFallbackDisplayString().containsProperty(prop);
        else
            return false;
    }

    public boolean containsTag(Tag tagName) {
        TagInstance tag = getTag(tagName);
        if (tag != null)
            return true;
        else if (getFallbackDisplayString() != null)
            return getFallbackDisplayString().containsTag(tagName);
        else
            return false;
    }

    /**
     * Sets the value of a tag at a given position. Extends the tag vector if necessary
     * @param tag Name of the tag
     * @param pos Position to be updated (0 based)
     * @param newValue New value to be stored
     */
    protected void setTagArg(Tag tag, int pos, String newValue) {
        TagInstance tagInstance = getTag(tag);

        // if the value is empty or null and the tag does not exist, do nothing
        if (tagInstance == null && (newValue == null || "".equals(newValue)))
            return;

        // if the tag does not exist add it to the map
        if (tagInstance == null) {
            tagInstance = new TagInstance(tag.name());
            tagMap.put(tag.name(), tagInstance);
        }
        tagInstance.setArg(pos, newValue);

        // remove the tag if every value is empty
        if (tagInstance.isEmpty())
            tagMap.remove(tag.name());
    }

    /**
     * Returns the tag with a given name
     * @param tag requested tag name
     * @return The requested tag or <code>NULL</code> if does not exist
     */
    protected TagInstance getTag(Tag tag) {
        return tagMap.get(tag.name());
    }

    /**
     * Parse the given string and store its contents.
     */
    public void set(String newValue) {
        tagMap = parseTags(newValue);
        cachedDisplayString = newValue;
        updateNedElement();
    }

    public static LinkedHashMap<String, TagInstance> parseTags(String text) {
        LinkedHashMap<String, TagInstance> tagMap = new LinkedHashMap<String, TagInstance>();
        tagMap.clear();
        if (text != null) {
            // parse the display string into tags along ";"
            Scanner scr = new Scanner(text);
            scr.useDelimiter(";");
            while (scr.hasNext()) {
                TagInstance parsedTag = new TagInstance(scr.next().trim());
                tagMap.put(parsedTag.getName(), parsedTag);  //FIXME must resolve escaped ";" and "," ??? --Andras
            }
        }
        return tagMap;
    }

    protected void updateNedElement() {
        if (owner != null) {
            // Change the underlying NedElement tree.
            // This could be optimized somewhat by remembering the LiteralElement, and quickly
            // checking here if that's still where we have to change the display string
            NedElementUtilEx.setDisplayStringLiteral(owner, toString());
        }
    }

    /**
     * Returns the local display string, i.e. without inherited tags.
     */
    @Override
    public String toString() {
        if (cachedDisplayString == null) {
            StringBuffer sb = new StringBuffer(50);
            boolean firstTag = true;
            for (TagInstance tag : tagMap.values()) {
                String tagVal = tag.toString();
                if (!tagVal.equals("")) {
                    if (firstTag) firstTag = false;
                    else sb.append(';');

                    sb.append(tagVal); //FIXME quoting??? string may contain ";" or ","  --Andras
                }
            }
            cachedDisplayString = sb.toString();
        }
        return cachedDisplayString;
    }

    public int cumulativeHashCode() {
        int hash = 0;

        for (DisplayString ds = this; ds != null; ds = ds.getFallbackDisplayString())
            hash += ds.toString().hashCode();

        return hash;
    }

    /**
     * The property value. NULL if tag does not exist, "" if the value
     * is empty.
     */
    public String getAsStringLocal(Prop property) {
        return getTagArg(property.getTag(), property.getPos());
    }

    public String getAsString(Prop property, boolean useEmptyDefaults, boolean useVariableDefaults) {
        return getTagArgUsingDefaults(property.getTag(), property.getPos(), useEmptyDefaults, useVariableDefaults);
    }

    public String getAsString(Prop property) {
        return getTagArgUsingDefaults(property.getTag(), property.getPos(), true, true);
    }

    public int getAsInt(Prop propName, int defaultValue) {
        try {
            String propValue = getAsString(propName);
            return propValue == null ? defaultValue : Integer.valueOf(propValue);
        } catch (NumberFormatException e) { }
        return defaultValue;
    }

    public float getAsFloat(Prop propName, float defaultValue) {
        try {
            String propValue = getAsString(propName);
            return StringUtils.isEmpty(propValue) ? defaultValue : Float.valueOf(propValue);
        } catch (NumberFormatException e) { }
        return defaultValue;
    }

    /**
     * Sets the specified property to the given value in the display string
     */
    public void set(Prop property, String newValue) {
        setTagArg(property.getTag(), property.getPos(), newValue);
        cachedDisplayString = null;
        updateNedElement();
    }

    public float getScale() {
        return 1.0f; //FIXME
    }

    // helper functions for setting and getting the location and size properties
    public final float pixelToUnit(int pixel, Float overrideScale) {
        if (overrideScale != null)
            return pixel / overrideScale;

        return  pixel / getScale();
    }

    public final int unitToPixel(float unit, Float overrideScale) {
        if (overrideScale != null)
            return (int)(unit * overrideScale);

        return (int)(unit * getScale());
    }

    public int getRange(Float scale) {
        float floatvalue = getAsFloat(DisplayString.Prop.RANGE, -1.0f);
        return (floatvalue <= 0) ? -1 : unitToPixel(floatvalue, scale);
    }

    public Point getLocation(Float scale) {
        // return NaN to signal that the property is missing
        Float x = getAsFloat(Prop.X, Float.NaN);
        Float y = getAsFloat(Prop.Y, Float.NaN);

        // if it's unspecified in any direction, we should return a NULL constraint
        if (x.equals(Float.NaN) || y.equals(Float.NaN))
            return null;

        return new Point (unitToPixel(x, scale), unitToPixel(y, scale));
    }

    /**
     * Sets the location properties (P tag). If NULL is given both location property is cleared
     * meaning the module is unpinned and can be freely moved by the layouter.
     * It fires a single property change notification for Prop.X
     */
    public void setLocation(Point location, Float scale) {
        // if location is not specified, remove the constraint from the display string
        if (location == null) {
            set(Prop.X, null);
            set(Prop.Y, null);
        }
        else {
            set(Prop.X, floatToString(pixelToUnit(location.x, scale)));
            set(Prop.Y, floatToString(pixelToUnit(location.y, scale)));
        }
    }

    public Dimension getSize(Float scale) {
        int width = unitToPixel(getAsFloat(Prop.SHAPE_WIDTH, -1.0f), scale);
        width = width > 0 ? width : -1;
        int height = unitToPixel(getAsFloat(Prop.SHAPE_HEIGHT, -1.0f), scale);
        height = height > 0 ? height : -1;
        return new Dimension(width, height);
    }

    /**
     * Converts the size given in pixels to unit based size and sets the size of the element (B tag).
     * If negative number is given as width or height the corresponding property is deleted.
     * It fires property change notification for Prop.SHAPE_WIDTH
     */
    public void setSize(Dimension size, Float scale) {
        // if the size is unspecified, remove the size constraint from the model
        if (size == null || size.width < 0 )
            set(Prop.SHAPE_WIDTH, null);
        else
            set(Prop.SHAPE_WIDTH, floatToString(pixelToUnit(size.width, scale)));

        // if the size is unspecified, remove the size constraint from the model
        if (size == null || size.height < 0)
            set(Prop.SHAPE_HEIGHT, null);
        else
            set(Prop.SHAPE_HEIGHT, floatToString(pixelToUnit(size.height, scale)));
    }

    public Dimension getCompoundSize(Float scale) {
        int width = unitToPixel(getAsFloat(Prop.MODULE_WIDTH, -1.0f), scale);
        width = width > 0 ? width : -1;
        int height = unitToPixel(getAsFloat(Prop.MODULE_HEIGHT, -1.0f), scale);
        height = height > 0 ? height : -1;

        return new Dimension(width, height);
    }

    /**
     * Converts the size given in pixels to unit based size and
     * sets the size of the element (BGB tag). If negative number is given as width or height
     * the corresponding property is deleted.
     * It fires property change notification for Prop.MODULE_WIDTH
     */
    public void setCompoundSize(Dimension size, Float scale) {
        // if the size is unspecified, remove the size constraint from the model
        if (size == null || size.width < 0 )
            set(Prop.MODULE_WIDTH, null);
        else
            set(Prop.MODULE_WIDTH, floatToString(pixelToUnit(size.width, scale)));

        // if the size is unspecified, remove the size constraint from the model
        if (size == null || size.height < 0)
            set(Prop.MODULE_HEIGHT, null);
        else
            set(Prop.MODULE_HEIGHT, floatToString(pixelToUnit(size.height, scale)));
    }

    /**
     * Sets both the size and the location (B and P tag), but sends out only a SINGLE X position change property change notification.
     * It fires property change notification for Prop.X
     */
    public void setConstraint(Point loc, Dimension size, Float scale) {
        setLocation(loc, scale);
        setSize(size, scale);
    }

    /**
     * Converts a float number to string (without trailing ".0" if it's an integer)
     */
    private static String floatToString(float value) {
        return StringUtils.chomp(String.valueOf(value), ".0");
    }

    /**
     * Dump all supported tags
     */
    public static void dumpSupportedTags() {
        for (Prop p : Prop.values()) {
            Debug.println(p.getTag()+"\t "+p.getPos()+"\t "+p.getGroup()+"\t "+p.getName()
                    +"\t "+p.getDefaultDesc()+"\t "+p.getEnumDesc()+"\t "+p.getDesc());
        }
    }

    /**
     * Dump all supported tags in laTex
     */
    public static void dumpSupportedTagsInTex() {
        Debug.println("\\begin{longtable}{|p{6cm}|p{8cm}|}");
        Debug.println("\\hline");
        for (Prop prop : Prop.values()) {
            if (prop.getTag() == null)
                continue;
            Debug.println("\\tbf{"+prop.getTag()+"}["+prop.getPos()+"] - "+prop.getName());
            Debug.println("&");

            String enumDesc = prop.getEnumDesc();
            String defaultValue = prop.getDefaultDesc();
            String desc = prop.getShortDesc().replace("#","\\#");
            desc += (StringUtils.isNotEmpty(enumDesc) ?  ". Values: " + enumDesc : "");
            desc += (StringUtils.isNotEmpty(defaultValue) ? ". Default: "+defaultValue : "");
            Debug.println(desc);
            Debug.println("\\\\ \n \\hline");
        }
        Debug.println("\\end{longtable}");
    }

//    static {
//      dumpSupportedTagsInTex();
//    };
}


