package org.omnetpp.inifile.editor.model;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.inifile.editor.InifileEditorPlugin;
import org.omnetpp.ned.model.notification.INEDChangeListener;
import org.omnetpp.ned.model.notification.NEDModelEvent;
import org.omnetpp.ned.resources.NEDResourcesPlugin;

/**
 * Standard implementation of IInifileDocument. Setters change the 
 * underlying text document (IDocument). Parsing is lazy: changes on the 
 * text document cause a "changed" flag to be set here, and getters 
 * automatically reparse the text document if it's out of date.
 * 
 * @author Andras
 */
//XXX thread safety: make the entire class synchronized ?
//XXX validate new keys (after add/rename)! must not contain "=", "#", ";", whitespace, etc...  
//XXX validate section names (after add/rename)! must not contain "[", "]", "#", ";", newline, tab,...
//XXX ^^^ see InifileUtils.validateParameterKey too
public class InifileDocument implements IInifileDocument {
	public static final String INIFILEPROBLEM_MARKER_ID = InifileEditorPlugin.PLUGIN_ID + ".inifileproblem";

	private IDocument document; // the document we are manipulating
	private IFile documentFile; // the file of the document
	private boolean changed; // whether changed since last parsed

	static class Line {
		IFile file;
		int lineNumber; // 1-based
		int numLines;  // ==1 unless line continues on other lines (trailing backslash)
		String comment;
		Object data;
	};
	static class SectionHeadingLine extends Line {
		String sectionName;
		int lastLine; // last line of section contents
	}
	static class KeyValueLine extends Line {
		String key;
		String value;
	}
	static class IncludeLine extends Line {
		String includedFile;
	}

	static class Section {
		ArrayList<SectionHeadingLine> headingLines = new ArrayList<SectionHeadingLine>();
		LinkedHashMap<String,KeyValueLine> entries = new LinkedHashMap<String, KeyValueLine>();
		Object data;
	}

	// primary data structure: sections, keys
	private LinkedHashMap<String,Section> sections = new LinkedHashMap<String,Section>();

	// reverse (linenumber-to-section/key) mapping
	private ArrayList<SectionHeadingLine> mainFileSectionHeadingLines = new ArrayList<SectionHeadingLine>(); 
	private ArrayList<KeyValueLine> mainFileKeyValueLines = new ArrayList<KeyValueLine>();

	// include directives
	private ArrayList<IncludeLine> topIncludes = new ArrayList<IncludeLine>();
	private ArrayList<IncludeLine> bottomIncludes = new ArrayList<IncludeLine>();

	// listeners
	private IDocumentListener documentListener; // we listen on IDocument
	private IResourceChangeListener resourceChangeListener; // we listen on the workspace
	private INEDChangeListener nedChangeListener; // we listen on NED changes
	private InifileChangeListenerList listeners = new InifileChangeListenerList(); // clients that listen on us


	public InifileDocument(IDocument document, IFile documentFile) {
		this.document = document;
		this.documentFile = documentFile;
		this.changed = true;

		// listen on changes so we know when we need to re-parse
		hookListeners();
	}

	protected void hookListeners() {
		// listen on text editor changes
		documentListener = new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {}
			public void documentChanged(DocumentEvent event) {
				markAsChanged();				
			}
		};
		document.addDocumentListener(documentListener);

		// listen of workspace changes (we need to invalidate the doc when an included inifile has changed)
		resourceChangeListener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
					IResource resource = event.getResource();
					if (resource instanceof IFile && resource.getFileExtension().equals("ini")) {
						markAsChanged();
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
		
		// listen on NED changes as well
		nedChangeListener = new INEDChangeListener() {
			public void modelChanged(NEDModelEvent event) {
				markAsChanged();
			}
		};
		NEDResourcesPlugin.getNEDResources().getNEDModelChangeListenerList().add(nedChangeListener);
	}

	public synchronized void markAsChanged() {
		changed = true;
		fireModelChanged();
	}

	/** 
	 * To be called from the editor!
	 */ 
	public void dispose() {
		unhookListeners();
	}

	protected void unhookListeners() {
		document.removeDocumentListener(documentListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		NEDResourcesPlugin.getNEDResources().getNEDModelChangeListenerList().remove(nedChangeListener);
	}

	synchronized public void parseIfChanged() {
		if (changed)
			parse();
	}

	synchronized public void parse() {
		sections.clear();
		mainFileKeyValueLines.clear();
		mainFileSectionHeadingLines.clear();
		topIncludes.clear();
		bottomIncludes.clear();
		long startTime = System.currentTimeMillis();
		Reader streamReader = new StringReader(document.get());

		try {
			documentFile.deleteMarkers(INIFILEPROBLEM_MARKER_ID, true, IResource.DEPTH_ZERO);
			//XXX remove from included files too?
		} catch (CoreException e1) {
			InifileEditorPlugin.logError(e1);
		}

		class Callback implements InifileParser.ParserCallback {
			Section currentSection = null;
			SectionHeadingLine currentSectionHeading = null;
			IFile currentFile; 

			public Callback(IFile file) {
				this.currentFile = file;
			}

			public void blankOrCommentLine(int lineNumber, int numLines, String rawLine, String comment) {
				// ignore
			}

			public void sectionHeadingLine(int lineNumber, int numLines, String rawLine, String sectionName, String comment) {
				// add if such section not yet exists
				Section section = sections.get(sectionName);
				if (section == null) {
					section = new Section();
					sections.put(sectionName, section);
				}

				// add line
				SectionHeadingLine line = new SectionHeadingLine();
				line.file = currentFile;
				line.lineNumber = lineNumber;
				line.numLines = numLines;
				line.comment = comment;
				line.sectionName = sectionName;
				line.lastLine = line.lineNumber + line.numLines - 1;
				section.headingLines.add(line);
				mainFileSectionHeadingLines.add(line);
				currentSection = section;
				currentSectionHeading = line;
			}

			public void keyValueLine(int lineNumber, int numLines, String rawLine, String key, String value, String comment) {
				if (currentSection==null)
					addError(currentFile, lineNumber, "Entry occurs before first section heading");
				else if (currentSection.entries.containsKey(key)) {
					KeyValueLine line = currentSection.entries.get(key);
					String location = (line.file==currentFile ? "" : line.file.getName()+" ") + "line " + line.lineNumber;
					addWarning(currentFile, lineNumber, "Duplicate key, ignored (see "+location+")");
				}
				else {
					KeyValueLine line = new KeyValueLine();
					line.file = currentFile;
					line.lineNumber = lineNumber;
					line.numLines = numLines;
					line.comment = comment;
					line.key = key;
					line.value = value;
					mainFileKeyValueLines.add(line);
					currentSection.entries.put(key, line);
					currentSectionHeading.lastLine = line.lineNumber + line.numLines - 1;
				}
			}

			public void directiveLine(int lineNumber, int numLines, String rawLine, String directive, String args, String comment) {
				if (!directive.equals("include"))
					addError(currentFile, lineNumber, "Unknown directive");
				else { 
					IncludeLine line = new IncludeLine();
					line.file = currentFile;
					line.lineNumber = lineNumber;
					line.numLines = numLines;
					line.comment = comment;
					line.includedFile = args;

					// recursively parse the included file
					try {
						IFile file = currentFile.getParent().getFile(new Path(line.includedFile));
						new InifileParser().parse(file, new Callback(file));
					} 
					catch (ParseException e) {
						addError(currentFile, e.getLineNumber(), e.getMessage());
					} catch (IOException e) {
						addError(currentFile, lineNumber, e.getMessage());
					} catch (CoreException e) {
						addError(currentFile, lineNumber, e.getMessage());
					}
				}
			}

			public void parseError(int lineNumber, int numLines, String message) {
				addError(currentFile, lineNumber, message);
			}
		}

		try {
			new InifileParser().parse(streamReader, new Callback(documentFile));
		} 
		catch (IOException e) {
			// cannot happen with string input
		} 
		catch (ParseException e) {
			addError(documentFile, e.getLineNumber(), e.getMessage());
		}
		System.out.println("Inifile parsing: "+(System.currentTimeMillis()-startTime)+"ms");

		// mark data structure as up to date (even if there was an error, because 
		// we don't want to keep re-parsing again and again)
		changed = false;

		// NOTE: notify listeners (fireModelChanged()) is NOT done here! It is done 
		// when the underlying text document (IDocument) changes, just after we set
		// changed=true.
	}

	protected void addError(IFile file, int line, String message) {
		addMarker(file, INIFILEPROBLEM_MARKER_ID, IMarker.SEVERITY_ERROR, message, line); 
	}

	protected void addWarning(IFile file, int line, String message) {
		addMarker(file, INIFILEPROBLEM_MARKER_ID, IMarker.SEVERITY_WARNING, message, line); 
	}

	@SuppressWarnings("unchecked")
	private static void addMarker(final IFile file, final String type, int severity, String message, int line) {
		try {
			HashMap map = new HashMap();
			MarkerUtilities.setMessage(map, message);
			MarkerUtilities.setLineNumber(map, line);
			map.put(IMarker.SEVERITY, severity);
			MarkerUtilities.createMarker(file, map, type);
		} catch (CoreException e) {
			InifileEditorPlugin.logError(e);
		}
	}

	public void dump() {
		for (String sectionName : sections.keySet()) {
			System.out.println("Section "+sectionName);
			Section section = sections.get(sectionName);
			for (SectionHeadingLine line : section.headingLines) {
				System.out.println("  headingLine: line="+line.lineNumber+"  sectionName="+line.sectionName);
			}
			for (String key : section.entries.keySet()) {
				KeyValueLine line = section.entries.get(key);
				System.out.println("  keyValueLine: line="+line.lineNumber+"  key="+line.key+" value="+line.value);
			}
		}
		System.out.println("Includes:");
		for (IncludeLine line : topIncludes) {
			System.out.println("  topInclude: line="+line.lineNumber+"  file="+line.includedFile);
		}
		for (IncludeLine line : bottomIncludes) {
			System.out.println("  bottomInclude: line="+line.lineNumber+"  file="+line.includedFile);
		}
		System.out.println("num section heading lines: "+mainFileSectionHeadingLines.size());
		System.out.println("num key-value lines: "+mainFileKeyValueLines.size());
	}

	protected boolean isEditable(Line line) {
		return line.file == documentFile;
	}

	protected static boolean nullSafeEquals(String first, String second) {
		return first==null ? second == null : first.equals(second);
	}

	/**
	 * Adds a line to IDocument at the given lineNumber (1-based). Existing line
	 * at lineNumber will be shifted down. Line text is to be specified without
	 * the trailing newline.
	 */
	synchronized protected void addLineAt(int lineNumber, String text) {
		try {
			if (lineNumber==document.getNumberOfLines()+1) {
				// adding a line at the bottom
				document.replace(document.getLength(), 0, "\n");  // XXX doing this, we sometime create two blank lines
			}
			int offset = document.getLineOffset(lineNumber-1); //IDocument is 0-based
			document.replace(offset, 0, text+"\n");
		} 
		catch (BadLocationException e) {
			throw new RuntimeException("Cannot insert line: bad location: "+e.getMessage());
		}
	}

	/**
	 * Replaces line content in IDocument, or if text==null, deletes the line.
	 * Returns false if the line numbers have not changed; in that case, the caller
	 * may opt for suppressing re-parsing by manually setting the "changed" flag to false.
	 */
	synchronized protected boolean replaceLine(Line line, String text) {
		try {
			int offset = document.getLineOffset(line.lineNumber-1);
			int length = document.getLineOffset(line.lineNumber-1+line.numLines) - offset;
			document.replace(offset, length, text==null ? "" : text+"\n");

			boolean lineNumberChange = (text==null) || (line.numLines != StringUtils.countNewLines(text)+1);
			return lineNumberChange;
		} 
		catch (BadLocationException e) {
			throw new RuntimeException("Cannot set value: bad location: "+e.getMessage());
		}
	}

	public boolean containsKey(String section, String key) {
		return lookupEntry(section, key) != null;
	}

	/**
	 * Returns the given entry, or null if it does not exist.
	 */
	protected KeyValueLine lookupEntry(String sectionName, String key) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		return section == null ? null : section.entries.get(key);
	}

	/**
	 * Returns the given entry; throws exception if entry does not exist.
	 */
	protected KeyValueLine getEntry(String sectionName, String key) {
		KeyValueLine line = lookupEntry(sectionName, key);
		if (line == null)
			throw new IllegalArgumentException("No such entry: ["+sectionName+"] "+key);
		return line;
	}

	/**
	 * Returns the given entry; throws exception if entry does not exist, or it is in an included file.
	 */
	protected KeyValueLine getEditableEntry(String sectionName, String key) {
		KeyValueLine line = getEntry(sectionName, key);
		if (!isEditable(line))
			throw new IllegalArgumentException("Entry is in an included file which cannot be edited: ["+sectionName+"] "+key);
		return line;
	}

	public String getValue(String section, String key) {
		KeyValueLine line = lookupEntry(section, key);
		return line == null ? null : line.value;
	}

	public void setValue(String section, String key, String value) {
		KeyValueLine line = getEditableEntry(section, key);
		if (!nullSafeEquals(line.value, value)) {
			line.value = value; 
			String text = line.key + " = " + line.value + (line.comment == null ? "" : " "+line.comment);
			if (!replaceLine(line, text))
				changed = false; // suppress re-parsing
		}
	}

	public void addEntry(String section, String key, String value, String comment, String beforeKey) {
		if (lookupEntry(section, key) != null)
			throw new IllegalArgumentException("Key "+key+" already exists in section ["+section+"]");

		// modify IDocument
		int atLine = beforeKey==null ? getFirstEditableSectionHeading(section).lastLine+1 : getEditableEntry(section, beforeKey).lineNumber;
		String text = key + " = " + value + (comment == null ? "" : " "+comment);
		addLineAt(atLine, text);
	}

	public LineInfo getEntryLineDetails(String section, String key) {
		KeyValueLine line = lookupEntry(section, key);
		return line==null ? null : new LineInfo(line.file, line.lineNumber, !isEditable(line));
	} 

	public String getComment(String section, String key) {
		return getEntry(section, key).comment;
	}

	public void setComment(String section, String key, String comment) {
		KeyValueLine line = getEditableEntry(section, key);
		if (!nullSafeEquals(line.comment, comment)) {
			line.comment = comment; 
			String text = line.key + " = " + line.value + (line.comment == null ? "" : " "+line.comment);
			if (!replaceLine(line, text))
				changed = false;  // suppress re-parsing
		}
	}

	public void changeKey(String section, String oldKey, String newKey) {
		KeyValueLine line = getEditableEntry(section, oldKey);
		if (!nullSafeEquals(line.key, newKey)) {
			if (lookupEntry(section, newKey) != null)
				throw new IllegalArgumentException("Key "+newKey+" already exists in section ["+section+"]");
			line.key = newKey; 
			String text = line.key + " = " + line.value + (line.comment == null ? "" : " "+line.comment);
			replaceLine(line, text);
		}
	}

	public void removeKey(String section, String key) {
		KeyValueLine line = getEditableEntry(section, key);
		replaceLine(line, null);
	}

	public void moveKey(String section, String key, String beforeKey) {
		KeyValueLine line = getEditableEntry(section, key);
		if (beforeKey != null) {
			if (beforeKey.equals(key))
				return; // moving it before itself == nop
			getEditableEntry(section, beforeKey); // just probe it, to make sure it's editable
		}
		removeKey(section, key);
		addEntry(section, key, line.value, line.comment, beforeKey);
	}

	public String[] getKeys(String sectionName) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		return section == null ? null : section.entries.keySet().toArray(new String[0]);
	}

	public String[] getMatchingKeys(String sectionName, String regex) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		if (section == null)
			return null;
		ArrayList<String> list = new ArrayList<String>();
		for (String key : section.entries.keySet())
			if (regex.matches(key))
				list.add(key);
		return list.toArray(new String[list.size()]);
	}

	public String[] getSectionNames() {
		parseIfChanged();
		return sections.keySet().toArray(new String[0]);
	}

	public boolean containsSection(String section) {
		parseIfChanged();
		return sections.containsKey(section);
	}

	protected Section lookupSection(String sectionName) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		if (section == null)
			throw new IllegalArgumentException("Section does not exist: ["+sectionName+"]");
		return section;
	}

	/**
	 * Returns the first editable section heading, or if none are editable, the first one.
	 */
	protected SectionHeadingLine lookupPreferredSectionHeading(String sectionName) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		if (section == null)
			throw new IllegalArgumentException("Section does not exist: ["+sectionName+"]");
		for (SectionHeadingLine line : section.headingLines)
			if (isEditable(line))
				return line;
		return section.headingLines.get(0);
	}

	protected SectionHeadingLine getFirstEditableSectionHeading(String sectionName) {
		SectionHeadingLine line = lookupPreferredSectionHeading(sectionName);
		if (!isEditable(line))
			throw new IllegalArgumentException("Section is in an included file: ["+sectionName+"]");
		return line;
	}

	public void removeSection(String sectionName) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		if (section != null) {
			// section might be disconnected (ie more then one heading), so we have to
			// go deleting in reverse order, otherwise we mess up line numbers as we go
			SectionHeadingLine[] lines = section.headingLines.toArray(new SectionHeadingLine[]{});
			ArrayUtils.reverse(lines);
			boolean hasUndeletableParts = false;
			boolean deletedSomething = false;
			for (SectionHeadingLine line : lines) {
				if (!isEditable(line))
					hasUndeletableParts = true;
				else {
					try {
						int offset = document.getLineOffset(line.lineNumber-1);
						int length = document.getLineOffset(line.lastLine-1+line.numLines) - offset;
						document.replace(offset, length, "");
						deletedSomething = true;
					} 
					catch (BadLocationException e) {
						throw new RuntimeException("Cannot delete section: bad location: "+e.getMessage());
					}
				}
			}
			if (hasUndeletableParts) {
				if (deletedSomething)
					throw new IllegalArgumentException("Section ["+sectionName+"] could only be partially deleted, because part of it was defined in an included file");
				else
					throw new IllegalArgumentException("Section ["+sectionName+"] cannot be deleted, because it is defined in an included file");
			}
		}
	}

	public void renameSection(String sectionName, String newName) {
		Section section = lookupSection(sectionName);
		for (SectionHeadingLine line : section.headingLines)
			if (!isEditable(line))
				throw new IllegalArgumentException("Cannot rename section ["+sectionName+"], because it (or part of it) is in an included file");
		for (SectionHeadingLine line : section.headingLines) {
			//XXX big problem if line numbers change as the result of replacing!!!!! ie original section name was on two lines using backslash...
			replaceLine(line, "[" + newName + "]" + (line.comment == null ? "" : " "+line.comment));
		}
	}

	public void addSection(String sectionName, String beforeSectionName) {
		parseIfChanged();
		if (sections.get(sectionName) != null)
			throw new IllegalArgumentException("Section already exists: ["+sectionName+"]");

		// find insertion point
		int lineNumber;
		if (beforeSectionName==null)                                       
			lineNumber = bottomIncludes.isEmpty() ? document.getNumberOfLines()+1 : bottomIncludes.get(0).lineNumber;
			else
				lineNumber = getFirstEditableSectionHeading(beforeSectionName).lineNumber;

		// modify IDocument
		String text = "[" + sectionName + "]";
		addLineAt(lineNumber, "");  // leave blank
		addLineAt(lineNumber, text);
	}

	public LineInfo getSectionLineDetails(String sectionName) {
		SectionHeadingLine line = lookupPreferredSectionHeading(sectionName);
		return new LineInfo(line.file, line.lineNumber, !isEditable(line));
	} 

	public String getSectionComment(String sectionName) {
		return lookupPreferredSectionHeading(sectionName).comment;
	}

	public void setSectionComment(String sectionName, String comment) {
		SectionHeadingLine line = getFirstEditableSectionHeading(sectionName);
		if (!nullSafeEquals(line.comment, comment)) {
			line.comment = comment; 
			String text = "[" + line.sectionName + "]" + (line.comment == null ? "" : " "+line.comment);
			if (!replaceLine(line, text))
				changed = false; // suppress re-parsing
		}
	}

	public String getSectionForLine(int lineNumber) {
		SectionHeadingLine line = findSectionHeadingLine(lineNumber);
		return line == null ? null : line.sectionName;
	}

	private SectionHeadingLine findSectionHeadingLine(int lineNumber) {
		int i = -1;
		while (i+1 < mainFileSectionHeadingLines.size() && lineNumber >= mainFileSectionHeadingLines.get(i+1).lineNumber)
			i++;
		return i==-1 ? null : mainFileSectionHeadingLines.get(i);
	}

	public String getKeyForLine(int lineNumber) {
		SectionHeadingLine sectionHeadingLine = findSectionHeadingLine(lineNumber);
		if (sectionHeadingLine == null)
			return null;

		// find key in that section (note: linear search, optimize if needed)
		for (KeyValueLine line : mainFileKeyValueLines)
			if (line.lineNumber==lineNumber)
				return line.key;
		return null;
	}

	public String[] getTopIncludes() {
		parseIfChanged();
		return null; //TODO
	}

	public void addTopInclude(String include, String before) {
		parseIfChanged();
		//TODO
	}

	public void removeTopInclude(String include) {
		parseIfChanged();
		//TODO
	}

	public String[] getBottomIncludes() {
		parseIfChanged();
		return null; //TODO
	}

	public void addBottomInclude(String include, String before) {
		parseIfChanged();
		//TODO
	}

	public void removeBottomInclude(String include) {
		parseIfChanged();
		//TODO
	}

	/**
	 * Adds a listener to this document 
	 */
	public void addInifileChangeListener(IInifileChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Adds a listener to this document 
	 */
	public void removeInifileChangeListener(IInifileChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Fires a model change event by notifying listeners.
	 */
	public void fireModelChanged() {
		if (listeners != null && listeners.isEnabled())
			listeners.fireModelChanged();
	}

	public Object getKeyData(String section, String key) {
		KeyValueLine line = lookupEntry(section, key);
		if (line == null)
			throw new IllegalArgumentException("No such entry: ["+section+"] "+key);
		return line.data;
	}

	public void setKeyData(String section, String key, Object data) {
		KeyValueLine line = lookupEntry(section, key);
		if (line == null)
			throw new IllegalArgumentException("No such entry: ["+section+"] "+key);
		line.data = data;
	}

	public Object getSectionData(String sectionName) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		if (section == null)
			throw new IllegalArgumentException("No such section: ["+sectionName+"]");
		return section.data;
	}

	public void setSectionData(String sectionName, Object data) {
		parseIfChanged();
		Section section = sections.get(sectionName);
		if (section == null)
			throw new IllegalArgumentException("No such section: ["+sectionName+"]");
		section.data = data;
	}

	public IFile getDocumentFile() {
		return documentFile;
	}
}
