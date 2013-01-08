package org.omnetpp.cdt.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.cpp.ICPPUsingDirective;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.index.IIndexMacro;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent.FileVersion;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent.InclusionKind;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.omnetpp.cdt.Activator;
import org.omnetpp.cdt.cache.Index.IndexFile;
import org.omnetpp.cdt.cache.Index.IndexInclude;
import org.omnetpp.common.Debug;
import org.omnetpp.common.util.Pair;

/**
 * This class provides the preprocessor with the content of source files.
 * The content can be either a character array or an index structure.
 * <p>
 * If an include file is requested more than once for a translation unit,
 * the file content is returned only for the first query, subsequent queries
 * will receive a skipped content. Here we assume that all header files contains
 * the necessary top level #ifdefs to prevent their content included twice.
 * This improves scanning time of large projects tremendously.
 * <p>
 * When the content is generated from index files, then it is guaranteed that
 * all macros referred by #if/#elif/#ifdef/#ifndef directives has the same value
 * as when the index file was generated. This is required because the index file
 * contains only the content of the 'active' branches.
 *
 * @author tomi
 */
@SuppressWarnings("restriction")
class FileContentProvider extends InternalFileContentProvider {

    private final Index index = Activator.getIndex();
    private final FileCache fileCache = Activator.getFileCache();

    // the translation unit for whose content is generated by this instance
    private IFile translationUnit;

    // files already included into the current translation unit
    private Set<Path> includedFiles = new HashSet<Path>();

    // values of macros accessible from the scanner
    private IMacroValueProvider currentMacroValues;

    static interface IMacroValueProvider {
        char[] getMacroValue(String name);
    }

    public FileContentProvider(IFile translationUnit) {
        this.translationUnit = translationUnit;
    }

    public void setMacroValueProvider(IMacroValueProvider provider) {
        this.currentMacroValues = provider;
    }

    public IFile getTranslationUnit() {
        return translationUnit;
    }

    @Override
    public boolean getInclusionExists(String path) {
        return fileCache.isFileExists(new Path(path));
    }

    public InternalFileContent getContentForTranslationUnit() {
        Path path = (Path)translationUnit.getLocation();
        InternalFileContent content = getFileContent(path);
        if (content != null)
            includedFiles.add(path); // to prevent a .h to include itself
        return content;
    }

    @Override
    public InternalFileContent getContentForInclusion(String absolutePath, IMacroDictionary macroDictionary/*XXX*/) {
        Path path = new Path(absolutePath);

        if (includedFiles.contains(path))
            return new InternalFileContent(path.toOSString(), InclusionKind.SKIP_FILE);

        InternalFileContent content = getFileContent(path);

        if (content != null)
            includedFiles.add(path);

        return content;
    }

    @Override
    public InternalFileContent getContentForInclusion(IIndexFileLocation ifl, String astPath) {
        throw new UnsupportedOperationException();
    }

    private InternalFileContent getFileContent(Path path) {
        // check index
        IndexFile indexFile = index.resolve(path);
        if (indexFile != null) {
            try {
                Map<String,char[]> macroValues = new HashMap<String,char[]>();
                List<IIndexFile> files= new ArrayList<IIndexFile>();
                List<IIndexMacro> macros= new ArrayList<IIndexMacro>();
                Set<Path> processedFiles= new HashSet<Path>();
                collectFileContent(indexFile, processedFiles, files, macros, macroValues, 0);
                // add included files only, if no exception was thrown
                for (Path file : processedFiles) {
                    includedFiles.add(file);
                }
                return new InternalFileContent(path.toOSString(), macros, Collections.<ICPPUsingDirective>emptyList(), files, Collections.<FileVersion>emptyList()/*XXX*/);
            } catch (MacroValueChangedException e) {
                if (e.depth == 0) {
                    // TODO message dialog?
                    //Activator.logError(e);
                    Debug.println(e.getMessage());
                }
            }
        }

        // check file cache
        return fileCache.getFileContent(path);
    }

    private class MacroValueChangedException extends Exception {
        private static final long serialVersionUID = 1L;
        int depth;
        public MacroValueChangedException(int depth, String message) {
            super(message);
            this.depth = depth;
        }
    }

    /**
     * Collects macros and includes contained in {@code file} following the includes recursively.
     * During the collection it also collects the values of defined macros, and checks that
     * referenced macros has the same value as when the index file was generated.
     *
     * @param file the file whose content is collected
     * @param processedFiles already processed files, to avoid double inclusion
     * @param files included files collected
     * @param macros defined macros collected
     * @param macroValues values of macros defined during the traversal
     * @throws MacroValueChangedException
     */
    private void collectFileContent(IndexFile file, Set<Path> processedFiles, List<IIndexFile> files,
                                    List<IIndexMacro> macros, Map<String,char[]> macroValues, int depth)
        throws MacroValueChangedException
    {
        if (!processedFiles.add(file.getPath()) || includedFiles.contains(file.getPath())) {
            return;
        }

        files.add(file);

        for (Object d : file.getPreprocessingDirectives()) {
            if (d instanceof Pair) {
                // check value of referenced macro
                if (currentMacroValues != null) {
                    @SuppressWarnings("unchecked")
                    Pair<String,char[]> ref = (Pair<String,char[]>)d;
                    String name = ref.first;
                    char[] expectedValue = ref.second;
                    char[] foundValue = macroValues.containsKey(name) ? macroValues.get(name) :currentMacroValues.getMacroValue(name);
                    if (!Arrays.equals(expectedValue, foundValue))
                        throw new MacroValueChangedException(depth, String.format("Index mismatch: in file %s the macro %s was %s, now it is %s",
                                file.getPath().toOSString(), name,
                                expectedValue != null ? "'"+new String(expectedValue)+"'" : "<undefined>",
                                foundValue != null ? "'" + new String(foundValue) + "'" : "<undefined>"));
                }
            }
            else if (d instanceof IIndexMacro) {
                // add macro
                IIndexMacro macro = (IIndexMacro)d;
                char[] expansion = macro.getExpansion();
                macros.add(macro);
                macroValues.put(macro.getName(), expansion);
            } else if (d instanceof IndexInclude) {
                // recurse on included file
                Path includedFilePath = ((IndexInclude) d).getPath();
                if (includedFilePath != null) {
                    IndexFile includedFile = index.resolve(includedFilePath);
                    if (includedFile != null) {
                        collectFileContent(includedFile, processedFiles, files, macros, macroValues, depth+1);
                    }
                }
            }
        }
    }
}
