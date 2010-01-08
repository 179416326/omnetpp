/**
 *
 */
package org.omnetpp.scave.actions;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jface.action.Action;
import org.omnetpp.scave.editors.datatable.DataTree;
import org.omnetpp.scave.editors.datatable.ResultFileManagerTreeContentProvider;

@SuppressWarnings("unchecked")
public class PredefinedLevelsAction extends Action {
    private final DataTree dataTree;

    private Class[] levels;

    public PredefinedLevelsAction(String text, DataTree dataTree, Class[] levels) {
        super(text, Action.AS_RADIO_BUTTON);
        this.dataTree = dataTree;
        this.levels = levels;
    }

    @Override
    public boolean isChecked() {
        setChecked(isLevelsEqualsIgnoreModuleNameLevel(dataTree.getContentProvider().getLevels(), levels));
        return super.isChecked();
    }

    @Override
    public void run() {
        if (!isChecked()) {
            ResultFileManagerTreeContentProvider contentProvider = dataTree.getContentProvider();
            Class[] currentLevels = contentProvider.getLevels();
            int index = ArrayUtils.indexOf(currentLevels, ResultFileManagerTreeContentProvider.ModuleNameNode.class);
            if (index == -1)
                contentProvider.setLevels(levels);
            else {
                Class[] clonedLevels = levels.clone();
                index = ArrayUtils.indexOf(clonedLevels, ResultFileManagerTreeContentProvider.ModulePathNode.class);
                if (index != -1)
                    clonedLevels[index] = ResultFileManagerTreeContentProvider.ModuleNameNode.class;
                contentProvider.setLevels(clonedLevels);
            }
            dataTree.refresh();
        }
    }

    public static boolean isLevelsEqualsIgnoreModuleNameLevel(Class[] levels1, Class[] levels2) {
        if (levels1.length != levels2.length)
            return false;
        for (int i = 0; i < levels1.length; i++)
            if (!toModulePathNodeClass(levels1[i]).equals(toModulePathNodeClass(levels2[i])))
                return false;
        return true;
    }

    private static Class toModulePathNodeClass(Class clazz) {
        if (clazz.equals(ResultFileManagerTreeContentProvider.ModuleNameNode.class))
            return ResultFileManagerTreeContentProvider.ModulePathNode.class;
        else
            return clazz;
    }
}