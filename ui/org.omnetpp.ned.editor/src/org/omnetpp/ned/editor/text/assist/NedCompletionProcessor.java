/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.text.assist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.omnetpp.common.editor.text.NedCompletionHelper;
import org.omnetpp.common.editor.text.SyntaxHighlightHelper;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.core.NEDResources;
import org.omnetpp.ned.core.NEDResourcesPlugin;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeLookupContext;
import org.omnetpp.ned.model.interfaces.INEDTypeResolver.IPredicate;
import org.omnetpp.ned.model.pojo.SubmoduleElement;

// TODO completion within inner types
// TODO a better structure is needed for storing the completion proposals
// TODO context help can be supported, to show the documentation of the proposed keyword
// TODO F4 "Open Declaration"
// TODO completion for imports
/**
 * NED completion processor.
 *
 * @author andras
 */
public class NedCompletionProcessor extends NedTemplateCompletionProcessor {

	/**
	 * Simple content assist tip closer. The tip is valid in a range
	 * of 5 characters around its popup location.
     * TODO implement correctly the context information
	 */
	protected static class Validator implements IContextInformationValidator, IContextInformationPresenter {
		protected int fInstallOffset;

		/*
		 * @see IContextInformationValidator#isContextInformationValid(int)
		 */
		public boolean isContextInformationValid(int offset) {
			return Math.abs(fInstallOffset - offset) < 5;
		}

		/*
		 * @see IContextInformationValidator#install(IContextInformation, ITextViewer, int)
		 */
		public void install(IContextInformation info, ITextViewer viewer, int offset) {
			fInstallOffset= offset;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformationPresenter#updatePresentation(int, TextPresentation)
		 */
		public boolean updatePresentation(int documentPosition, TextPresentation presentation) {
			return false;
		}
	}

	// for CompletionInfo.sectionType
	protected static final int SECT_GLOBAL = 0;
	protected static final int SECT_PARAMETERS = 1;
	protected static final int SECT_GATES = 2;
	protected static final int SECT_TYPES = 3;
	protected static final int SECT_CONNECTIONS = 4;
	protected static final int SECT_SUBMODULES = 5;
	protected static final int SECT_SUBMODULE_PARAMETERS = 6;
	protected static final int SECT_SUBMODULE_GATES = 7;

	protected static class CompletionInfo {
		public String linePrefix; // relevant line (lines) just before the insertion point
		public String linePrefixTrimmed; // like linePrefix, but last identifier (which the user is currently typing) chopped
		public String enclosingNedTypeName;
		public String nedTypeName;       // the type name
		public int sectionType; // SECT_xxx
		public String submoduleTypeName;
	}

	protected IContextInformationValidator fValidator = new Validator();
	protected ITextEditor editor;

    public NedCompletionProcessor(ITextEditor editor) {
    	this.editor = editor;
	}

	@Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		// long startMillis = System.currentTimeMillis(); // measure time

		List<ICompletionProposal> result = new ArrayList<ICompletionProposal>();

		// find out where we are: in which module, submodule, which section etc.
		CompletionInfo info = computeCompletionInfo(viewer, documentOffset);
		// if the position is invalid return no proposals
		if (info == null || info.linePrefix == null || info.linePrefixTrimmed == null)
		    return new ICompletionProposal[0];
	
		NEDResources res = NEDResourcesPlugin.getNEDResources();
		IFile file = ((IFileEditorInput)editor.getEditorInput()).getFile();
		NedFileElementEx nedFileElement = res.getNedFileElement(file);
		IProject project = file.getProject();

		String line = info.linePrefixTrimmed;

		// calculate the lookup context used in nedresource calls
		INedTypeLookupContext context = nedFileElement;
		INEDTypeInfo nedTypeInfo = null;
		if (info.nedTypeName!=null) {
		    nedTypeInfo = res.lookupNedType(info.nedTypeName, context);
		    if (nedTypeInfo != null && nedTypeInfo.getNEDElement() instanceof CompoundModuleElementEx)
		        context = (CompoundModuleElementEx)nedTypeInfo.getNEDElement();
		}
	
		INEDTypeInfo nedEnclosingTypeInfo = null;
		if (info.enclosingNedTypeName != null) { // we are inside an inner type
		    nedEnclosingTypeInfo = res.lookupNedType(info.enclosingNedTypeName, nedFileElement);
            if (nedEnclosingTypeInfo != null && nedEnclosingTypeInfo.getNEDElement() instanceof CompoundModuleElementEx)
                context = (CompoundModuleElementEx)nedEnclosingTypeInfo.getNEDElement();
		}

		INEDTypeInfo submoduleType = null;
		if (info.submoduleTypeName!=null) {
			submoduleType = res.lookupNedType(info.submoduleTypeName, context);
		}

		if (info.sectionType==SECT_GLOBAL || info.sectionType==SECT_TYPES)
		{
			// Debug.println("testing proposals for GLOBAL and TYPES scope");

			// match various "extends" and "like" clauses and offer component types
			if (line.matches(".*\\bsimple .* extends"))
			    addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.SIMPLE_MODULE_FILTER);
			else if (line.matches(".*\\b(module|network) .* extends"))
                addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.COMPOUND_MODULE_FILTER);
			else if (line.matches(".*\\bchannel .* extends"))
                addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.CHANNEL_FILTER);
			else if (line.matches(".*\\bmoduleinterface .* extends"))
                addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.MODULEINTERFACE_FILTER);
			else if (line.matches(".*\\bchannelinterface .* extends"))
                addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.CHANNELINTERFACE_FILTER);

			// match "like" clauses
			if (line.matches(".*\\bsimple .* like") || line.matches(".*\\bsimple .* like .*,"))
                addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.MODULEINTERFACE_FILTER);
			else if (line.matches(".*\\b(module|network) .* like") || line.matches(".*\\b(module|network) .* like .*,"))
                addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.MODULEINTERFACE_FILTER);
			else if (line.matches(".*\\bchannel .* like") || line.matches(".*\\bchannel .* like .*,"))
                addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.CHANNELINTERFACE_FILTER);

			if (!line.equals("") && !line.matches(".*\\b(like|extends)\\b.*") && line.matches(".*\\b(simple|module|network|channel|interface|channelinterface)\\b [_A-Za-z0-9]+"))
				addProposals(viewer, documentOffset, result, new String[]{"extends "}, "keyword");

            if (!line.equals("") && line.matches(".*\\b(simple|module|network|channel)\\b [_A-Za-z0-9]+( extends [_A-Za-z0-9]+)?"))
                addProposals(viewer, documentOffset, result, new String[]{"like "}, "keyword");
		}

		// propose line start: param names, gate names, keywords
		if (line.equals("")) {
			// offer param and gate names
			if (info.sectionType == SECT_PARAMETERS && nedTypeInfo!=null)
				addProposals(viewer, documentOffset, result, nedTypeInfo.getParamDeclarations().keySet(), "parameter");
			if (info.sectionType == SECT_GATES && nedTypeInfo!=null)
				addProposals(viewer, documentOffset, result, nedTypeInfo.getGateDeclarations().keySet(), "gate");
			if (info.sectionType == SECT_SUBMODULE_PARAMETERS && submoduleType!=null)
				addProposals(viewer, documentOffset, result, submoduleType.getParamDeclarations().keySet(), "parameter");
			if (info.sectionType == SECT_SUBMODULE_GATES && submoduleType!=null)
				addProposals(viewer, documentOffset, result, submoduleType.getGateDeclarations().keySet(), "gate");

			// offer param and gate type name keywords
			if (info.sectionType == SECT_PARAMETERS)
				addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedParamTypes, "parameter type");
			else if (info.sectionType == SECT_GATES)
				addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedGateTypes, "gate type");

			// provide global start keywords and section names
            if (info.sectionType==SECT_GLOBAL) {
                addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedTopLevelKeywords, "keyword (top level)");
                addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedTypeDefinerKeywords, "keyword");
            }
            else if (info.sectionType==SECT_PARAMETERS) {
                addProposals(viewer, documentOffset, result, new String[]{"connections:", "connections allowunconnected:", "gates:", "parameters:", "submodules:", "types:"}, "section");
            }
            else if (info.sectionType==SECT_GATES) {
                addProposals(viewer, documentOffset, result, new String[]{"connections:", "connections allowunconnected:", "submodules:", "types:"}, "section");
            }
            else if (info.sectionType==SECT_TYPES) {
				addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedTypeDefinerKeywords, "keyword");
                addProposals(viewer, documentOffset, result, new String[]{"connections:", "connections allowunconnected:", "submodules:"}, "section");
	    	}
	    	else if (info.sectionType==SECT_SUBMODULES) {
                addProposals(viewer, documentOffset, result, new String[]{"connections:", "connections allowunconnected:"}, "section");
            }
            else if (info.sectionType==SECT_SUBMODULE_PARAMETERS) {
	    		addProposals(viewer, documentOffset, result, new String[]{"gates:"}, "section");
	    	}

	    	// offer templates
	    	if (info.sectionType==SECT_GLOBAL || info.sectionType==SECT_TYPES) {
			    addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedGlobalTempl);
	    	}
	    	else if (info.sectionType==SECT_SUBMODULES) {
	    		addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedSubmoduleTempl);
	    	}
		}

		// offer double/int/string/xml after "volatile"
		if (line.equals("volatile")) {
			addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedBaseParamTypes, "parameter type");
		}

		// expressions: after "=", opening "[", "if" or "for"
		if (line.contains("=") || line.matches(".*\\b(if|for)\\b.*") || containsOpenBracket(line)) {
			// Debug.println("proposals for expressions");

			// offer parameter names, gate names, types,...
			if (line.matches(".*\\bthis *\\.")) {
				if (submoduleType!=null)
					addProposals(viewer, documentOffset, result, submoduleType.getParamDeclarations().keySet(), "parameter");
			}
			else if (line.matches(".*\\bsizeof *\\(")) {
				if (nedTypeInfo!=null) {
                    // FIXME add only vector submodules and vectors
					addProposals(viewer, documentOffset, result, nedTypeInfo.getGateDeclarations().keySet(), "gate");
					addProposals(viewer, documentOffset, result, nedTypeInfo.getSubmodules().keySet(), "submodule");
				}
			}
	    	else if (line.endsWith(".")) {
	    		// after dot: offer params (and after sizeof(), gates too) of given submodule
	    		if (nedTypeInfo!=null) {
					String submodTypeName = extractSubmoduleTypeName(line, nedTypeInfo);
					// Debug.println(" offering params of type "+submodTypeName);
					INEDTypeInfo submodType = res.lookupNedType(submodTypeName, context);
					if (submodType!=null) {
						if (line.matches(".*\\bsizeof *\\(.*"))
							addProposals(viewer, documentOffset, result, submodType.getGateDeclarations().keySet(), "gate");
						addProposals(viewer, documentOffset, result, submodType.getParamDeclarations().keySet(), "parameter");
					}
	    		}
	    	}
			else {
				if (nedTypeInfo!=null) {
					addProposals(viewer, documentOffset, result, nedTypeInfo.getParamDeclarations().keySet(), "parameter");
				}
			}
			addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedConstants, "");

			addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedOtherExpressionKeywords, "keyword");
		    addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedOperatorsTempl);
		    addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedContinuousDistributionsTempl);
		    addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedDiscreteDistributionsTempl);
		    addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedContinuousDistributionsTemplExt);
		    addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedDiscreteDistributionsTemplExt);
		    addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedFunctionsTempl);
		}

        // offer existing and standard property names after "@"
        if (line.equals("")) {
            if (info.sectionType == SECT_GLOBAL ) {
                addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedFilePropertyTempl);
            }
            if (info.sectionType == SECT_PARAMETERS && nedTypeInfo!=null) {
                addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedComponentPropertyTempl);
                addProposals(viewer, documentOffset, result, "@", nedTypeInfo.getProperties().keySet(), "", "property");
            }
            if (info.sectionType == SECT_SUBMODULE_PARAMETERS && submoduleType!=null)
                addProposals(viewer, documentOffset, result, "@", submoduleType.getProperties().keySet(), "", "property");
        }
        else if ((line.contains("=") && !line.endsWith("=")) || !line.contains("=")) {
            if (info.sectionType == SECT_PARAMETERS || info.sectionType == SECT_SUBMODULE_PARAMETERS)
                addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedParamPropertyTempl);
            if (info.sectionType == SECT_GATES || info.sectionType == SECT_SUBMODULE_GATES)
                addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedGatePropertyTempl);
        }

		// complete submodule type name
		if (info.sectionType == SECT_SUBMODULES) {
			// Debug.println("testing proposals for SUBMODULES scope");
			if (line.matches(".*:")) {
			    if (nedEnclosingTypeInfo != null)    // we are inside an inner type (use the enclosing module' inner types) 
	                addNedTypeProposals(viewer, documentOffset, result, project, nedEnclosingTypeInfo, NEDResources.MODULE_FILTER);
			    else  // top level type
			        addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.MODULE_FILTER);
			}
			else if (line.matches(".*: *<")) {  // "like" syntax
				if (nedTypeInfo!=null)
					addProposals(viewer, documentOffset, result, nedTypeInfo.getParamDeclarations().keySet(), "parameter");
			}
			else if (line.matches(".*: *<.*>")) {   // "like" syntax, cont'd
					addProposals(viewer, documentOffset, result, new String[]{" like "}, "keyword");
			}
			else if (line.matches(".*\\blike")) {
                if (nedEnclosingTypeInfo != null)    // we are inside an inner type (use the enclosing module' inner types) 
                    addNedTypeProposals(viewer, documentOffset, result, project, nedEnclosingTypeInfo, NEDResources.MODULEINTERFACE_FILTER);
                else  // top level type
                    addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.MODULEINTERFACE_FILTER);
			}
		}

		if (info.sectionType == SECT_CONNECTIONS) {
			// Debug.println("testing proposals for CONNECTIONS scope");
			if (line.matches(".*\\bconnections")) {
				// user forgot "allowunconnected" keyword
				addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedConnsKeywords, "keyword");
			}

			if (line.equals("") || line.endsWith("-->") || line.endsWith("<-->") || line.endsWith("<--")) {
	    		// right at line start or after arrow: offer submodule names and parent module's gates
	    		if (nedTypeInfo!=null) {
	    			addProposals(viewer, documentOffset, result, nedTypeInfo.getSubmodules().keySet(), "submodule");
	    			addProposals(viewer, documentOffset, result, nedTypeInfo.getGateDeclarations().keySet(), "gate");
	    			// only a single arrow can be present in the line to give channel assistance to
                    if (line.matches(".*--.*") && !line.matches(".*--.*--.*"))
                        addNedTypeProposals(viewer, documentOffset, result, project, nedTypeInfo, NEDResources.CHANNEL_FILTER);
	    		}
	    	}
	    	else if (line.endsWith(".")) {
	    		// after dot: offer gates of given submodule
	    		if (nedTypeInfo != null) {
	    		    String submodTypeName = extractSubmoduleTypeName(line, nedTypeInfo);
	    		    if (submodTypeName != null) {
	    		        // Debug.println(" offering gates of type "+submodTypeName);
	    		        INEDTypeInfo submodType = res.lookupNedType(submodTypeName, context);
	    		        if (submodType != null)
	    		            addProposals(viewer, documentOffset, result, submodType.getGateDeclarations().keySet(), "gate");
	    		    }
	    		}
	    	}

			// offer templates for connection, loop connection, connection with channel, etc
            if (line.equals(""))
                addProposals(viewer, documentOffset, result, NedCompletionHelper.proposedNedConnectionTempl);
		}

		// long millis = System.currentTimeMillis()-startMillis;
		// Debug.println("Proposal creation: "+millis+"ms");

	    return (ICompletionProposal[]) result.toArray(new ICompletionProposal[result.size()]);
	}

    private void addNedTypeProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> result,
            IProject project, INEDTypeInfo nedTypeInfoForInnerTypes, IPredicate predicate) {
        NEDResources res = NEDResourcesPlugin.getNEDResources();
        // add inner types
        if (nedTypeInfoForInnerTypes != null) {
            Set<String> innerTypeNames = new HashSet<String>();
            for (INedTypeElement innerTypeElement : nedTypeInfoForInnerTypes.getInnerTypes().values()) {
                if (predicate.matches(innerTypeElement.getNEDTypeInfo()))
                    innerTypeNames.add(innerTypeElement.getName());
            }
            addProposals(viewer, documentOffset, result, innerTypeNames, "inner type");
        }
        
        // add top level types
        // XXX offer "like" template too
        Set<String> qnames = res.getNedTypeQNames(predicate, project);
        String names[] = new String[qnames.size()];
        String descriptions[] = new String[qnames.size()];
        int i = 0;
        for (String qname : qnames) {
            INEDTypeInfo topLevelTypeInfo = res.getToplevelNedType(qname, project);
            names[i] = topLevelTypeInfo.getName();
            String packageName = StringUtils.chomp(topLevelTypeInfo.getNamePrefix(), ".");
            packageName = StringUtils.isBlank(packageName) ? "" : packageName+" - ";
            descriptions[i] =  packageName + topLevelTypeInfo.getNEDElement().getReadableTagName()+" type";
            i++;
        }
        addProposals(viewer, documentOffset, result, names, descriptions);
    }

	private boolean containsOpenBracket(String line) {
		while (line.matches(".*\\[[^\\[\\]]*\\].*"))
			line = line.replaceAll("\\[[^\\[\\]]*\\]", "###");
        return line.contains("[");
	}

	private String extractSubmoduleTypeName(String line, INEDTypeInfo parentComponent) {
		// first, get rid of everything before any arrow(s), because it causes a problem for the next regexp
		line = line.replaceFirst("^.*(-->|<--|<-->)", "");
		// identifier followed by ".", potentially a submodule index ("[something]") in between
		Matcher matcher = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*) *(\\[[^\\[\\]]*\\])? *\\.$").matcher(line);
		if (matcher.find()) { // use find() because line may start with garbage
			String submoduleName = matcher.group(1);
			INEDElement submodNode = parentComponent.getMembers().get(submoduleName);
			if (submodNode instanceof SubmoduleElement) {
				SubmoduleElement submod = (SubmoduleElement) submodNode;
				String submodTypeName = submod.getType();
				if (submodTypeName==null || submodTypeName.equals(""))
					submodTypeName = submod.getLikeType();
				return submodTypeName;
			}
		}
		return null; // bad luck
	}

    private void addProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> result, String[] proposals, String[] descriptions) {
        result.addAll(createProposals(viewer, documentOffset, new SyntaxHighlightHelper.NedWordDetector(), "", proposals, "", descriptions));
    }

	private void addProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> result, String[] proposals, String description) {
		result.addAll(createProposals(viewer, documentOffset, new SyntaxHighlightHelper.NedWordDetector(), "", proposals, "", description));
	}

	private void addProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> result, Set<String> proposals, String description) {
		result.addAll(createProposals(viewer, documentOffset, new SyntaxHighlightHelper.NedWordDetector(), "", proposals.toArray(new String[] {}), "", description));
	}

	private void addProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> result, String start, Set<String> proposals, String end, String description) {
		result.addAll(createProposals(viewer, documentOffset, new SyntaxHighlightHelper.NedWordDetector(), start, proposals.toArray(new String[] {}), end, description));
	}

	private void addProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> result, Template[] templates) {
	    result.addAll(Arrays.asList(createTemplateProposals(viewer, documentOffset, new SyntaxHighlightHelper.NedWordDetector(), templates)));
	}

	private CompletionInfo computeCompletionInfo(ITextViewer viewer, int documentOffset) {
		IDocument docu = viewer.getDocument();
        int offset = documentOffset;
        try {
    		String source = docu.get(0,offset);
    		// kill string literals
			source = source.replaceAll("\".*\"", "\"###\"");  //FIXME but ignore embedded backslash+quote \" !!!
    		// kill comments
    		source = source.replaceAll("(?m)//.*", "");

    		// completion prefix (linePrefix): stuff after last semicolon,
    		// curly brace, "parameters:", "gates:", "connections:" etc.
    		String prefix = source;
    		prefix = prefix.replaceAll("(?s)\\s+", " "); // normalize whitespace
    		prefix = prefix.replaceFirst(".*[;\\{\\}]", "");
    		prefix = prefix.replaceFirst(".*\\b(parameters|gates|types|submodules|connections|connections +[a-z]+) *:", "");
    		String prefix2 = prefix.replaceFirst("[a-zA-Z_@][a-zA-Z0-9_]*$", "").trim(); // chop off last word

    		// kill {...} regions (including bodies of inner types, etc)
    		while (source.matches("(?s).*\\{[^\\{\\}]*\\}.*"))
    		    source = source.replaceAll("(?s)\\{[^\\{\\}]*\\}", "###");

    		// detect if we are inside an inner type
    		if (source.matches("(?s).*\\btypes\\b.*\\btypes\\b.*"))
    		    return null;      // inner types within inner types are not supported
    	
    		// handle inner types
    		boolean insideInnertype = source.matches("(?s).*\\btypes\\b\\s*:[^:]*\\{.*"); 
    		String enclosingNedTypeName = null;
            if (insideInnertype) {
                String sourceBeforeTypes = source.replaceFirst("(?s)^(.*)\\btypes\\s*:.*$", "$1");
                enclosingNedTypeName = sourceBeforeTypes.replaceFirst("(?s)^.*(simple|module|network|channel|interface|channelinterface)\\s+([A-Za-z_][A-Za-z0-9_]+).*$", "$2");
                if (enclosingNedTypeName.equals(source)) 
                    enclosingNedTypeName = null;  // replace failed
                // use only the source after the type keyword (the inner type source)
                source = source.replaceFirst("(?s)^.*\\btypes\\s*:(.*)$", "$1");
            }

            // throw out the types section if it is closed (we are not in the types section)
            source = source.replaceAll("(?s)\\btypes\\b[^{]*?\\b(submodules|connections)\\b", "$1");

            // detect what section we are in
			int sectionType;
			if (source.matches("(?s).*\\bconnections\\b.*"))
				sectionType = SECT_CONNECTIONS;
			else if (source.matches("(?s).*\\bsubmodules\\b.*\\bgates\\b.*"))
				sectionType = SECT_SUBMODULE_GATES;
			else if (source.matches("(?s).*\\bsubmodules\\b.*\\{.*"))
				sectionType = SECT_SUBMODULE_PARAMETERS;
			else if (source.matches("(?s).*\\bsubmodules\\b.*"))
				sectionType = SECT_SUBMODULES;
			else if (source.matches("(?s).*\\btypes\\b.*"))
			    sectionType = SECT_TYPES;
			else if (source.matches("(?s).*\\bgates\\b.*"))
				sectionType = SECT_GATES;
			else if (source.matches("(?s).*\\{.*"))
				sectionType = SECT_PARAMETERS;
			else
				sectionType = SECT_GLOBAL;
	
			// detect module name: identifier after last "simple", "module", etc.
			String nedTypeName = source.replaceFirst("(?s)^.*\\b(simple|module|network|channel|interface|channelinterface)\\s+([A-Za-z_][A-Za-z0-9_]+).*\\{.*$", "$2");
			if (nedTypeName.equals(source)) 
			    nedTypeName = null;  // replace failed
		
			// detect submodule type: last occurrence of "identifier {"
			String submoduleTypeName = null;
			if (sectionType == SECT_SUBMODULE_GATES || sectionType == SECT_SUBMODULE_PARAMETERS) {
				String pat2 = "(?s).*[:\\s]([A-Za-z_][A-Za-z0-9_]+)\\s*\\{";
				Matcher matcher2 = Pattern.compile(pat2).matcher(source);
				if (matcher2.lookingAt())
					submoduleTypeName = matcher2.group(1);
			}

			if (sectionType == SECT_GLOBAL)
			    nedTypeName = enclosingNedTypeName = submoduleTypeName = null;
		
//			Debug.println(">>>"+source+"<<<");
//			Debug.println("ENCLOSINGNEDTYPENAME:"+enclosingNedTypeName+"  NEDTYPENAME:"+nedTypeName+"  SECTIONTYPE:"+sectionType+"  SUBMODTYPENAME:"+submoduleTypeName);
//			Debug.println("PREFIX: >>"+prefix+"<<");
//			Debug.println("PREFIX2: >>"+prefix2+"<<");
//            Debug.println("inside inner type: "+insideInnertype);

			CompletionInfo ret = new CompletionInfo();
			ret.linePrefix = prefix;
			ret.linePrefixTrimmed = prefix2;
			ret.nedTypeName = nedTypeName;
			ret.enclosingNedTypeName = enclosingNedTypeName; 
			ret.sectionType = sectionType;
			ret.submoduleTypeName = submoduleTypeName;
			return ret;

        } catch (BadLocationException e) {
        	return null;
        }
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		//XXX what the heck is this?
		//		IContextInformation[] result= new IContextInformation[5];
		//		for (int i= 0; i < result.length; i++)
		//			result[i]= new ContextInformation(
		//				MessageFormat.format(NedEditorMessages.getString("CompletionProcessor.ContextInfo.display.pattern"), new Object[] { new Integer(i), new Integer(documentOffset) }),
		//				MessageFormat.format(NedEditorMessages.getString("CompletionProcessor.ContextInfo.value.pattern"), new Object[] { new Integer(i), new Integer(documentOffset - 5), new Integer(documentOffset + 5)}));
		//		return result;
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.', '@' };
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return new char[] { '(' };
	}

	public IContextInformationValidator getContextInformationValidator() {
		return fValidator;
	}

	public String getErrorMessage() {
		return null;
	}
}
