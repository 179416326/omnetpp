package org.omnetpp.common.contentassist;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.omnetpp.common.util.StringUtils;

/**
 * A basic content proposal provider. Provides a basic IContentProposal implementation,
 * and sorts and filters by prefix the set of proposal candidates provided by the
 * abstract getProposalCandidates() method.
 * 
 * Note: although IContentProposalProvider is for field editors, we use this class
 * in the text editor content assist too; we just re-wrap IContentProposals to 
 * ICompletionProposal. 
 * 
 * @author Andras
 */
public abstract class ContentProposalProvider implements IContentProposalProvider {
	private boolean useWholePrefix = false;
	
	/**
	 * Constructor.
	 * @param useWholePrefix whether the whole substring before the cursor needs to be 
	 *                       be matched by completions (false: only the last "word").
	 */
	public ContentProposalProvider(boolean useWholePrefix) {
		this.useWholePrefix = useWholePrefix;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.fieldassist.IContentProposalProvider#getProposals(java.lang.String, int)
	 */
	public IContentProposal[] getProposals(String contents, int position) {
		ArrayList<IContentProposal> result = new ArrayList<IContentProposal>();

		String prefix = contents.substring(0, position);
		
		// calculate the last word that the user started to type. This is the basis of
		// proposal filtering: they have to start with this prefix.
		String prefixToMatch = useWholePrefix ? prefix : getSuffixToComplete(prefix);

		IContentProposal[] candidates = getProposalCandidates(prefix);

		if (candidates!=null) {
			Arrays.sort(candidates);

			// check if any of the proposals has description. If they do, we set "(no description)" 
			// on the others as well. Reason: if left at null, previous tooltip will be shown, 
			// which is very confusing.
			boolean descriptionSeen = false;
			for (IContentProposal p : candidates)
				if (!StringUtils.isEmpty(p.getDescription()))
					descriptionSeen = true;

			// collect those candidates that match the last incomplete word in the editor
			for (IContentProposal candidate : candidates) {
				String content = candidate.getContent();
				if (content.startsWith(prefixToMatch) && content.length()!= prefixToMatch.length()) {
					// from the content, drop the prefix that has already been typed by the user
					String modifiedContent = content.substring(prefixToMatch.length(), content.length());
					int modifiedCursorPosition = candidate.getCursorPosition() + modifiedContent.length() - content.length();
					String description = (StringUtils.isEmpty(candidate.getDescription()) && descriptionSeen) ? "(no description)" : candidate.getDescription();
					result.add(new ContentProposal(modifiedContent, candidate.getLabel(), description, modifiedCursorPosition));
				}
			}
		}

		if (result.isEmpty()) {
			// returning an empty array or null apparently causes NPE in the framework, so return a blank proposal instead
			//XXX may cause multiple "(no proposal)" strings to appear in the text editor completion
			result.add(new ContentProposal("", "(no proposal)", null, 0));
		}
		return result.toArray(new IContentProposal[] {});
	}

	/**
	 * Return the suffix of the text (the last, incomplete "word" the user is typing) 
	 * for which completions will be provided.
	 * 
	 *  Default version detects words (A-Z, a-z, 0-9, underscore); this can be overridden
	 *  in subclasses.
	 */
	protected String getSuffixToComplete(String text) {
		// calculate the last word that the user started to type. This is the basis of
		// proposal filtering: they have to start with this prefix.
		return text.replaceFirst("^.*?([A-Za-z0-9_]*)$", "$1");
	}

	/**
	 * Generate a list of proposal candidates. They will be sorted and filtered by prefix
	 * before presenting them to the user.
	 */
	abstract protected IContentProposal[] getProposalCandidates(String prefix);

	/**
	 * Turn strings into proposals.
	 */
	protected static IContentProposal[] toProposals(String[] strings) {
		IContentProposal[] p = new IContentProposal[strings.length];
		for (int i=0; i<p.length; i++)
			p[i] = new ContentProposal(strings[i], strings[i], null);
		return p;
	}
	
}
