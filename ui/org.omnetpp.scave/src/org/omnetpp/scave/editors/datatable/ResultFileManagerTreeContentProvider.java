package org.omnetpp.scave.editors.datatable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.WordUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;
import org.omnetpp.common.Debug;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.engine.DoubleVector;
import org.omnetpp.scave.engine.FileRun;
import org.omnetpp.scave.engine.HistogramResult;
import org.omnetpp.scave.engine.IDList;
import org.omnetpp.scave.engine.ResultFile;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.ResultItem;
import org.omnetpp.scave.engine.Run;
import org.omnetpp.scave.engine.RunAttribute;
import org.omnetpp.scave.engine.ScalarResult;
import org.omnetpp.scave.engine.StringMap;
import org.omnetpp.scave.engine.StringVector;
import org.omnetpp.scave.engine.VectorResult;
import org.omnetpp.scave.engineext.ResultFileManagerEx;

/**
 * This class provides a customizable tree of various data from the result file manager.
 * The tree is built up from levels that may be freely reordered and switched on/off.
 * Levels include experiment, measurement, replication, config, run number, file name, run id, module path, module name, result item, result item attributes, etc.
 *
 * @author levy
 */
@SuppressWarnings("unchecked")
public class ResultFileManagerTreeContentProvider {
    public final static Class[] LEVELS1 = new Class[] { ExperimentNode.class, MeasurementNode.class, ReplicationNode.class, ModulePathNode.class, ResultItemNode.class, ResultItemAttributeNode.class};
    public final static Class[] LEVELS2 = new Class[] { ExperimentMeasurementReplicationNode.class, ModulePathNode.class, ResultItemNode.class, ResultItemAttributeNode.class};
    public final static Class[] LEVELS3 = new Class[] { ConfigNode.class, RunNumberNode.class, ModulePathNode.class, ResultItemNode.class, ResultItemAttributeNode.class};
    public final static Class[] LEVELS4 = new Class[] { ConfigRunNumberNode.class, ModulePathNode.class, ResultItemNode.class, ResultItemAttributeNode.class};
    public final static Class[] LEVELS5 = new Class[] { FileNameNode.class, ModulePathNode.class, ResultItemNode.class, ResultItemAttributeNode.class};
    public final static Class[] LEVELS6 = new Class[] { RunIdNode.class, ModulePathNode.class, ResultItemNode.class, ResultItemAttributeNode.class};

    private static boolean debug = false;

    protected ResultFileManagerEx manager;

    protected IDList idList;

    protected Class<? extends Node>[] levels;

    protected Node[] rootNodes;

    public ResultFileManagerTreeContentProvider() {
        setDefaultLevels();
    }

    public void setResultFileManager(ResultFileManagerEx manager) {
        this.manager = manager;
        rootNodes = null;
    }

    public void setIDList(IDList idList) {
        this.idList = idList;
        rootNodes = null;
    }

    public Class<? extends Node>[] getLevels() {
        return levels;
    }

    public void setLevels(Class<? extends Node>[] levels) {
        if (debug)
            System.out.println("setLevels(): " + levels);
        this.levels = levels;
        rootNodes = null;
    }

    public String getLevelsName() {
        StringBuffer name = new StringBuffer();
        for (int i = 0; i < levels.length; i++) {
            Class level = levels[i];
            try {
                if (i != 0)
                    name.append(" / ");
                name.append(level.getMethod("getLevelName").invoke(null));
            }
            catch (Exception e) {
                // void
            }
        }
        return name.toString();
    }

    public void setDefaultLevels() {
        setLevels(LEVELS2);
    }

    public Node[] getChildNodes(final List<Node> path) {
        long startMillis = System.currentTimeMillis();
        if (manager == null || idList == null)
            return new Node[0];
        final Node firstNode = path.size() == 0 ? null : path.get(0);
        // cache
        if (firstNode == null) {
            if (rootNodes != null)
                return rootNodes;
        }
        else {
            if (firstNode.children != null)
                return firstNode.children;
        }
        Node[] nodes = ResultFileManager.callWithReadLock(manager, new Callable<Node[]>() {
            public Node[] call() throws Exception {
                MultiValueMap nodeIdsMap = new MultiValueMap();
                int currentLevelIndex;
                if (firstNode == null)
                    currentLevelIndex = -1;
                else {
                    currentLevelIndex = ArrayUtils.indexOf(levels, firstNode.getClass());
                    if (currentLevelIndex == -1)
                        return new Node[0];
                }
                int nextLevelIndex;
                if (firstNode instanceof ModuleNameNode) {
                    ModuleNameNode moduleNameNode = (ModuleNameNode)firstNode;
                    nextLevelIndex = currentLevelIndex + (moduleNameNode.leaf ? 1 : 0);
                }
                else
                    nextLevelIndex = currentLevelIndex + 1;
                boolean collector = false;
                for (int j = nextLevelIndex + 1; j < levels.length; j++)
                    if (!levels[j].equals(ResultItemAttributeNode.class))
                        collector = true;
                Class<? extends Node> nextLevelClass = nextLevelIndex < levels.length ? levels[nextLevelIndex] : null;
                if (nextLevelClass != null) {
                    int idCount = firstNode == null ? idList.size() : firstNode.ids.length;
                    for (int i = 0; i < idCount; i++) {
                        long id = firstNode == null ? idList.get(i) : firstNode.ids[i];
                        MatchContext matchContext = new MatchContext(manager, id);
                        if (matchesPath(path, id, matchContext)) {
                            if (nextLevelClass.equals(ExperimentNode.class))
                                nodeIdsMap.put(new ExperimentNode(matchContext.getRunAttribute(RunAttribute.EXPERIMENT)), id);
                            else if (nextLevelClass.equals(MeasurementNode.class))
                                nodeIdsMap.put(new MeasurementNode(matchContext.getRunAttribute(RunAttribute.MEASUREMENT)), id);
                            else if (nextLevelClass.equals(ReplicationNode.class))
                                nodeIdsMap.put(new ReplicationNode(matchContext.getRunAttribute(RunAttribute.REPLICATION)), id);
                            else if (nextLevelClass.equals(ExperimentMeasurementReplicationNode.class))
                                nodeIdsMap.put(new ExperimentMeasurementReplicationNode(matchContext.getRunAttribute(RunAttribute.EXPERIMENT), matchContext.getRunAttribute(RunAttribute.MEASUREMENT), matchContext.getRunAttribute(RunAttribute.REPLICATION)), id);
                            else if (nextLevelClass.equals(ConfigNode.class))
                                nodeIdsMap.put(new ConfigNode(matchContext.getRunAttribute(RunAttribute.CONFIGNAME)), id);
                            else if (nextLevelClass.equals(RunNumberNode.class))
                                nodeIdsMap.put(new RunNumberNode(matchContext.getRun().getRunNumber()), id);
                            else if (nextLevelClass.equals(ConfigRunNumberNode.class))
                                nodeIdsMap.put(new ConfigRunNumberNode(matchContext.getRunAttribute(RunAttribute.CONFIGNAME), matchContext.getRun().getRunNumber()), id);
                            else if (nextLevelClass.equals(FileNameNode.class))
                                nodeIdsMap.put(new FileNameNode(matchContext.getResultFile().getFileName()), id);
                            else if (nextLevelClass.equals(RunIdNode.class))
                                nodeIdsMap.put(new RunIdNode(matchContext.getRun().getRunName()), id);
                            else if (nextLevelClass.equals(FileNameRunIdNode.class))
                                nodeIdsMap.put(new FileNameRunIdNode(matchContext.getResultFile().getFileName(), matchContext.getRun().getRunName()), id);
                            else if (nextLevelClass.equals(ModuleNameNode.class)) {
                                String moduleName = matchContext.getResultItem().getModuleName();
                                String modulePrefix = getModulePrefix(path, null);
                                if (moduleName.startsWith(modulePrefix)) {
                                    String remainingName = StringUtils.removeStart(StringUtils.removeStart(moduleName, modulePrefix), ".");
                                    String name = StringUtils.substringBefore(remainingName, ".");
                                    nodeIdsMap.put(new ModuleNameNode(StringUtils.isEmpty(name) ? "." : name, !remainingName.contains(".")), id);
                                }
                            }
                            else if (nextLevelClass.equals(ModulePathNode.class))
                                nodeIdsMap.put(new ModulePathNode(matchContext.getResultItem().getModuleName()), id);
                            else if (nextLevelClass.equals(ResultItemNode.class)) {
                                if (collector)
                                    nodeIdsMap.put(new ResultItemNode(manager, -1, matchContext.getResultItem().getName()), id);
                                else
                                    nodeIdsMap.put(new ResultItemNode(manager, id, null), id);
                            }
                            else if (nextLevelClass.equals(ResultItemAttributeNode.class)) {
                                ResultItem resultItem = matchContext.getResultItem();
                                ResultItem.Type type = resultItem.getType();
                                boolean isIntegerType = type == ResultItem.Type.TYPE_INT;
                                nodeIdsMap.put(new ResultItemAttributeNode("Module name", String.valueOf(resultItem.getModuleName())), id);
                                nodeIdsMap.put(new ResultItemAttributeNode("Type", type.toString().replaceAll("TYPE_", "").toLowerCase()), id);
                                StringMap attributes = resultItem.getAttributes();
                                StringVector keys = attributes.keys();
                                for (int j = 0; j < keys.size(); j++) {
                                    String key = keys.get(j);
                                    nodeIdsMap.put(new ResultItemAttributeNode(StringUtils.capitalize(key), attributes.get(key)), id);
                                }

                                if (resultItem instanceof ScalarResult) {
                                    ScalarResult scalar = (ScalarResult)resultItem;
                                    nodeIdsMap.put(new ResultItemAttributeNode("Value", toIntegerAwareString(scalar.getValue(), isIntegerType)), id);
                                }
                                else if (resultItem instanceof VectorResult) {
                                    VectorResult vector = (VectorResult)resultItem;
                                    nodeIdsMap.put(new ResultItemAttributeNode("Count", String.valueOf(vector.getCount())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Min", toIntegerAwareString(vector.getMin(), isIntegerType)), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Max", toIntegerAwareString(vector.getMax(), isIntegerType)), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Mean", String.valueOf(vector.getMean())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("StdDev", String.valueOf(vector.getStddev())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Variance", String.valueOf(vector.getVariance())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Start event number", String.valueOf(vector.getStartEventNum())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("End event number", String.valueOf(vector.getEndEventNum())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Start time", String.valueOf(vector.getStartTime())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("End time", String.valueOf(vector.getEndTime())), id);
                                }
                                else if (resultItem instanceof HistogramResult) {
                                    HistogramResult histogram = (HistogramResult)resultItem;
                                    nodeIdsMap.put(new ResultItemAttributeNode("Count", String.valueOf(histogram.getCount())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Min", toIntegerAwareString(histogram.getMin(), isIntegerType)), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Max", toIntegerAwareString(histogram.getMax(), isIntegerType)), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Mean", String.valueOf(histogram.getMean())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("StdDev", String.valueOf(histogram.getStddev())), id);
                                    nodeIdsMap.put(new ResultItemAttributeNode("Variance", String.valueOf(histogram.getVariance())), id);
                                    DoubleVector bins = histogram.getBins();
                                    DoubleVector values = histogram.getValues();
                                    if (bins != null && values != null && bins.size() > 0 && values.size() > 0) {
                                        ResultItemAttributeNode binsNode = new ResultItemAttributeNode("Bins", String.valueOf(histogram.getBins().size()));
                                        List<Node> list = new ArrayList<Node>();
                                        for (int j = 0; j < bins.size(); j++) {
                                            double lowerBound = bins.get(j);
                                            double upperBound = j < bins.size() - 1 ? bins.get(j + 1) : Double.POSITIVE_INFINITY;
                                            double value = values.get(j);
                                            String name = "[" + toIntegerAwareString(lowerBound, isIntegerType) + ", ";
                                            if (isIntegerType)
                                                name += toIntegerAwareString(upperBound - 1, isIntegerType) + "]";
                                            else
                                                name += String.valueOf(upperBound) + ")";
                                            list.add(new NameValueNode(name, toIntegerAwareString(value, true)));
                                        }
                                        binsNode.children = list.toArray(new Node[0]);
                                        nodeIdsMap.put(binsNode, id);
                                    }
                                }
                                else
                                    throw new IllegalArgumentException();
                            }
                            else
                                throw new IllegalArgumentException();
                        }
                    }
                }
                Node[] nodes = (Node[])nodeIdsMap.keySet().toArray(new Node[0]);
                Arrays.sort(nodes, new Comparator<Node>() {
                    public int compare(Node o1, Node o2) {
                        return StringUtils.dictionaryCompare((o1).getColumnText(0), (o2).getColumnText(0));
                    }
                });
                for (Node node : nodes) {
                    Collection ids = nodeIdsMap.getCollection(node);
                    node.ids = new long[ids.size()];
                    Iterator it = ids.iterator();
                    for (int i = 0; i < ids.size(); i++)
                        node.ids[i] = (Long)it.next();
                    // add quick value if applicable
                    if (node.ids.length == 1 && !collector && StringUtils.isEmpty(node.value) &&
                        (!(node instanceof ModuleNameNode) || ((ModuleNameNode)node).leaf))
                    {
                        ResultItem resultItem = manager.getItem(node.ids[0]);
                        node.value = getResultItemShortDescription(resultItem);
                    }
                }
                // update cache
                if (firstNode == null)
                    rootNodes = nodes;
                else
                    firstNode.children = nodes;
                return nodes;
            }

            protected String toIntegerAwareString(double value, boolean isIntegerType) {
                if (!isIntegerType || Double.isInfinite(value) || Double.isNaN(value) || Math.floor(value) != value)
                    return String.valueOf(value);
                else
                    return String.valueOf((long)value);
            }
        });

        long totalMillis = System.currentTimeMillis() - startMillis;
        if (debug)
            Debug.println("getChildNodes() for path = " + path + ": " + totalMillis + "ms");

        return nodes;
    }

    protected static String getModulePrefix(final List<Node> path, Node nodeLimit) {
        StringBuffer modulePrefix = new StringBuffer();
        for (int i =  path.size() - 1; i >= 0; i--) {
            Node node = path.get(i);
            if (node == nodeLimit)
                break;
            String name = null;
            if (node instanceof ModuleNameNode)
                name = ((ModuleNameNode)node).name;
            else if (node instanceof ModulePathNode)
                name = ((ModulePathNode)node).path;
            if (name != null) {
                if (modulePrefix.length() == 0)
                    modulePrefix.append(name);
                else {
                    modulePrefix.append('.');
                    modulePrefix.append(name);
                }
            }
        }
        return modulePrefix.toString();
    }

    protected static String getResultItemShortDescription(ResultItem resultItem) {
        if (resultItem instanceof ScalarResult) {
            ScalarResult scalar = (ScalarResult)resultItem;
            return String.valueOf(scalar.getValue());
        }
        else if (resultItem instanceof VectorResult) {
            VectorResult vector = (VectorResult)resultItem;
            return String.valueOf(vector.getMean()) + " (" + String.valueOf(vector.getCount()) + ")";
        }
        else if (resultItem instanceof HistogramResult) {
            HistogramResult histogram = (HistogramResult)resultItem;
            return String.valueOf(histogram.getMean()) + " (" + String.valueOf(histogram.getCount()) + ")";
        }
        else
            throw new IllegalArgumentException();
    }

    protected static String getResultItemReadableClassName(ResultItem resultItem) {
        return resultItem.getClass().getSimpleName().replaceAll("Result", "").toLowerCase();
    }

    protected static class MatchContext {
        private ResultFileManager manager;
        private long id;
        private ResultItem resultItem;
        private FileRun fileRun;
        private ResultFile resultFile;
        private Run run;

        public MatchContext(ResultFileManager manager, long id) {
            this.manager = manager;
            this.id = id;
        }

        public String getRunAttribute(String key) {
            return manager.getRunAttribute(id, key);
        }

        public ResultItem getResultItem() {
            if (resultItem == null)
                resultItem = manager.getItem(id);
            return resultItem;
        }
        public FileRun getFileRun() {
            if (fileRun == null)
                fileRun = getResultItem().getFileRun();
            return fileRun;
        }

        public ResultFile getResultFile() {
            if (resultFile == null)
                resultFile = getFileRun().getFile();
            return resultFile;
        }
        public Run getRun() {
            if (run == null)
                run = getFileRun().getRun();
            return run;
        }
    }

    protected boolean matchesPath(List<Node> path, long id, MatchContext matchContext) {
        for (Node node : path)
            if (!node.matches(path, id, matchContext))
                return false;
        return true;
    }

    /* Various tree node types */

    public static Class[] getAvailableLevelClasses() {
        return new Class[] {
            ExperimentNode.class,
            MeasurementNode.class,
            ReplicationNode.class,
            ExperimentMeasurementReplicationNode.class,
            ConfigNode.class,
            RunNumberNode.class,
            ConfigRunNumberNode.class,
            FileNameNode.class,
            RunIdNode.class,
            FileNameRunIdNode.class,
            ModulePathNode.class,
            ModuleNameNode.class,
            ResultItemNode.class,
            ResultItemAttributeNode.class};
    }

    protected static abstract class Node {
        public long[] ids;

        public Node[] children;

        public String value = "";

        public boolean isExpandedByDefault() {
            return false;
        }

        public Image getImage() {
            return ImageFactory.getIconImage(ImageFactory.MODEL_IMAGE_FOLDER);
        }

        public abstract String getColumnText(int index);

        public abstract boolean matches(List<Node> path, long id, MatchContext matchContext);
    }

    public static class NameValueNode extends Node {
        public String name;

        public NameValueNode(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public static String getLevelName() {
            return null;
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? name : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return true;
        }

        @Override
        public Image getImage() {
            return ImageFactory.getIconImage(ImageFactory.TOOLBAR_IMAGE_PROPERTIES);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            NameValueNode other = (NameValueNode) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            }
            else if (!value.equals(other.value))
                return false;
            return true;
        }
    }

    public static class ExperimentNode extends Node {
        public String name;

        public ExperimentNode(String name) {
            this.name = name;
        }

        public static String getLevelName() {
            return "Experiment";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? name + " (experiment)" : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return name.equals(matchContext.getRunAttribute(RunAttribute.EXPERIMENT));
        }

        @Override
        public boolean isExpandedByDefault() {
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExperimentNode other = (ExperimentNode) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    public static class MeasurementNode extends Node {
        public String name;

        public MeasurementNode(String name) {
            this.name = name;
        }

        public static String getLevelName() {
            return "Measurement";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? StringUtils.defaultIfEmpty(name, "default")  + " (measurement)" : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return name.equals(matchContext.getRunAttribute(RunAttribute.MEASUREMENT));
        }

        @Override
        public boolean isExpandedByDefault() {
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MeasurementNode other = (MeasurementNode) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    public static class ReplicationNode extends Node {
        public String name;

        public ReplicationNode(String name) {
            this.name = name;
        }

        public static String getLevelName() {
            return "Replication";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? name + " (replication)" : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return name.equals(matchContext.getRunAttribute(RunAttribute.REPLICATION));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ReplicationNode other = (ReplicationNode) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    public static class ExperimentMeasurementReplicationNode extends Node {
        public String experiment;

        public String measurement;

        public String replication;

        public ExperimentMeasurementReplicationNode(String experiment, String measurement, String replication) {
            this.experiment = experiment;
            this.measurement = measurement;
            this.replication = replication;
        }

        public static String getLevelName() {
            return "Experiment + Measurement + Replication";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? experiment + (StringUtils.isEmpty(measurement) ? "" : " : " + measurement) + " : " + replication : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return experiment.equals(matchContext.getRunAttribute(RunAttribute.EXPERIMENT)) && measurement.equals(matchContext.getRunAttribute(RunAttribute.MEASUREMENT)) && replication.equals(matchContext.getRunAttribute(RunAttribute.REPLICATION));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((experiment == null) ? 0 : experiment.hashCode());
            result = prime * result + ((measurement == null) ? 0 : measurement.hashCode());
            result = prime * result + ((replication == null) ? 0 : replication.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExperimentMeasurementReplicationNode other = (ExperimentMeasurementReplicationNode) obj;
            if (experiment == null) {
                if (other.experiment != null)
                    return false;
            }
            else if (!experiment.equals(other.experiment))
                return false;
            if (measurement == null) {
                if (other.measurement != null)
                    return false;
            }
            else if (!measurement.equals(other.measurement))
                return false;
            if (replication == null) {
                if (other.replication != null)
                    return false;
            }
            else if (!replication.equals(other.replication))
                return false;
            return true;
        }
    }

    public static class ConfigNode extends Node {
        public String name;

        public ConfigNode(String name) {
            this.name = name;
        }

        public static String getLevelName() {
            return "Config";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? name + " (config)" : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return matchContext.getRunAttribute(RunAttribute.CONFIGNAME).equals(name);
        }

        @Override
        public boolean isExpandedByDefault() {
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConfigNode other = (ConfigNode) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    public static class RunNumberNode extends Node {
        public long runNumber;

        public RunNumberNode(long runNumber) {
            this.runNumber = runNumber;
        }

        public static String getLevelName() {
            return "Run Number";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? String.valueOf(runNumber) + " (run number)" : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return matchContext.getRun().getRunNumber() == runNumber;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (runNumber ^ (runNumber >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RunNumberNode other = (RunNumberNode) obj;
            if (runNumber != other.runNumber)
                return false;
            return true;
        }
    }

    public static class ConfigRunNumberNode extends Node {
        public String config;

        public long runNumber;

        public ConfigRunNumberNode(String config, long runNumber) {
            this.config = config;
            this.runNumber = runNumber;
        }

        public static String getLevelName() {
            return "Config + Run Number";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? config + " : " + runNumber : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return matchContext.getRunAttribute(RunAttribute.CONFIGNAME).equals(config) && matchContext.getRun().getRunNumber() == runNumber;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((config == null) ? 0 : config.hashCode());
            result = prime * result + (int) (runNumber ^ (runNumber >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConfigRunNumberNode other = (ConfigRunNumberNode) obj;
            if (config == null) {
                if (other.config != null)
                    return false;
            }
            else if (!config.equals(other.config))
                return false;
            if (runNumber != other.runNumber)
                return false;
            return true;
        }
    }

    public static class FileNameNode extends Node {
        public String fileName;

        public FileNameNode(String name) {
            this.fileName = name;
        }

        public static String getLevelName() {
            return "File Name";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? fileName + " (file name)" : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return matchContext.getResultFile().getFileName().equals(fileName);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FileNameNode other = (FileNameNode) obj;
            if (fileName == null) {
                if (other.fileName != null)
                    return false;
            }
            else if (!fileName.equals(other.fileName))
                return false;
            return true;
        }
    }

    public static class RunIdNode extends Node {
        public String runId;

        public RunIdNode(String runId) {
            this.runId = runId;
        }

        public static String getLevelName() {
            return "Run Id";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? runId + " (run id)" : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return matchContext.getRun().getRunName().equals(runId);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((runId == null) ? 0 : runId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RunIdNode other = (RunIdNode) obj;
            if (runId == null) {
                if (other.runId != null)
                    return false;
            }
            else if (!runId.equals(other.runId))
                return false;
            return true;
        }
    }

    public static class FileNameRunIdNode extends Node {
        public String fileName;

        public String runId;

        public FileNameRunIdNode(String fileName, String runId) {
            this.fileName = fileName;
            this.runId = runId;
        }

        public static String getLevelName() {
            return "File Name + Run Id";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? fileName + " : " + runId : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return matchContext.getResultFile().getFileName().equals(fileName) && matchContext.getRun().getRunName().equals(runId);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
            result = prime * result + ((runId == null) ? 0 : runId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FileNameRunIdNode other = (FileNameRunIdNode) obj;
            if (fileName == null) {
                if (other.fileName != null)
                    return false;
            }
            else if (!fileName.equals(other.fileName))
                return false;
            if (runId == null) {
                if (other.runId != null)
                    return false;
            }
            else if (!runId.equals(other.runId))
                return false;
            return true;
        }
    }

    public static class ModulePathNode extends Node {
        public String path;

        public ModulePathNode(String path) {
            this.path = path;
        }

        public static String getLevelName() {
            return "Module Path";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? path : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            return matchContext.getResultItem().getModuleName().equals(this.path);
        }

        @Override
        public Image getImage() {
            return ImageFactory.getIconImage(ImageFactory.MODEL_IMAGE_SIMPLEMODULE);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ModulePathNode other = (ModulePathNode) obj;
            if (path == null) {
                if (other.path != null)
                    return false;
            }
            else if (!path.equals(other.path))
                return false;
            return true;
        }
    }

    public static class ModuleNameNode extends Node {
        public String name;

        public boolean leaf;

        public ModuleNameNode(String name, boolean leaf) {
            this.name = name;
            this.leaf = leaf;
        }

        public static String getLevelName() {
            return "Module Name";
        }

        @Override
        public String getColumnText(int index) {
            return index == 0 ? name : value;
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            String modulePrefix = getModulePrefix(path, this);
            modulePrefix = StringUtils.isEmpty(modulePrefix) ? name : modulePrefix + "." + name;
            return matchContext.getResultItem().getModuleName().startsWith(modulePrefix);
        }

        @Override
        public Image getImage() {
            return ImageFactory.getIconImage(ImageFactory.MODEL_IMAGE_SIMPLEMODULE);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ModuleNameNode other = (ModuleNameNode) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    public static class ResultItemNode extends Node {
        public ResultFileManager manager;

        public long id;

        public String name;

        public ResultItemNode(ResultFileManager manager, long id, String name) {
            Assert.isTrue(id != -1 || name != null);
            this.manager = manager;
            this.id = id;
            this.name = name;
        }

        public static String getLevelName() {
            return "Result Item";
        }

        @Override
        public String getColumnText(int index) {
            if (name != null) {
                return index == 0 ? name : value;
            }
            else {
                ResultItem resultItem = manager.getItem(id);
                return index == 0 ? resultItem.getName() + " (" + getResultItemReadableClassName(resultItem) + ")" : getResultItemShortDescription(resultItem);
            }
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            if (name != null)
                return matchContext.getResultItem().getName().equals(name);
            else
                return this.id == id;
        }

        @Override
        public Image getImage() {
            if (name != null) {
                int allType = -1;
                for (long id : ids) {
                    int type = ResultFileManager.getTypeOf(id);
                    if (allType == -1)
                        allType = type;
                    else if (allType != type)
                        return ImageFactory.getIconImage(ImageFactory.MODEL_IMAGE_FOLDER);
                }
                if (allType == ResultFileManager.SCALAR)
                    return ImageFactory.getIconImage(ImageFactory.TOOLBAR_IMAGE_SHOWSCALARS);
                else if (allType == ResultFileManager.VECTOR)
                    return ImageFactory.getIconImage(ImageFactory.TOOLBAR_IMAGE_SHOWVECTORS);
                else if (allType == ResultFileManager.HISTOGRAM)
                    return ImageFactory.getIconImage(ImageFactory.TOOLBAR_IMAGE_SHOWHISTOGRAMS);
                else
                    return ImageFactory.getIconImage(ImageFactory.DEFAULT);
            }
            else {
                ResultItem resultItem = manager.getItem(id);
                if (resultItem instanceof ScalarResult)
                    return ImageFactory.getIconImage(ImageFactory.TOOLBAR_IMAGE_SHOWSCALARS);
                else if (resultItem instanceof VectorResult)
                    return ImageFactory.getIconImage(ImageFactory.TOOLBAR_IMAGE_SHOWVECTORS);
                else if (resultItem instanceof HistogramResult)
                    return ImageFactory.getIconImage(ImageFactory.TOOLBAR_IMAGE_SHOWHISTOGRAMS);
                else
                    return ImageFactory.getIconImage(ImageFactory.DEFAULT);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (id ^ (id >>> 32));
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ResultItemNode other = (ResultItemNode) obj;
            if (id != other.id)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    public static class ResultItemAttributeNode extends NameValueNode {
        private String methodName;

        public ResultItemAttributeNode(String name, String value) {
            super(name, value);
            methodName = "get" + WordUtils.capitalize(name.toLowerCase()).replaceAll(" ", "");
        }

        public static String getLevelName() {
            return "Result Item Attribute";
        }

        @Override
        public boolean matches(List<Node> path, long id, MatchContext matchContext) {
            try {
                ResultItem resultItem = matchContext.getResultItem();
                Method method = resultItem.getClass().getMethod(methodName);
                return value.equals(String.valueOf(method.invoke(resultItem)));
            }
            catch (Exception e) {
                return false;
            }
        }
    }
}
