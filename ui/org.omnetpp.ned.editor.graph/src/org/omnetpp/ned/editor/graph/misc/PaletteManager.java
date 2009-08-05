package org.omnetpp.ned.editor.graph.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteStack;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.tools.MarqueeSelectionTool;
import org.eclipse.jface.resource.ImageDescriptor;
import org.omnetpp.common.displaymodel.IDisplayString;
import org.omnetpp.common.displaymodel.IHasDisplayString;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.ned.editor.graph.GraphicalNedEditor;
import org.omnetpp.ned.model.NEDElement;
import org.omnetpp.ned.model.ex.ChannelInterfaceNodeEx;
import org.omnetpp.ned.model.ex.ChannelNodeEx;
import org.omnetpp.ned.model.ex.CompoundModuleNodeEx;
import org.omnetpp.ned.model.ex.ConnectionNodeEx;
import org.omnetpp.ned.model.ex.ModuleInterfaceNodeEx;
import org.omnetpp.ned.model.ex.NEDElementUtilEx;
import org.omnetpp.ned.model.ex.SimpleModuleNodeEx;
import org.omnetpp.ned.model.ex.SubmoduleNodeEx;
import org.omnetpp.ned.model.interfaces.IHasName;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;
import org.omnetpp.ned.model.notification.INEDChangeListener;
import org.omnetpp.ned.model.notification.NEDModelEvent;
import org.omnetpp.ned.model.pojo.PropertyNode;
import org.omnetpp.ned.resources.NEDResourcesPlugin;

/**
 * @author rhornig
 * Responsible for managing palette entries and keeping them in sync with 
 * the components in NEDResources plugin
 */
// TODO currently the whole palette is rebuilt on each model change
// this is not necessay. Detect the changes in the palette and add/remove/modify only
// entries which really need refresh
public class PaletteManager implements INEDChangeListener {
    GraphicalNedEditor hostingEditor;
    PaletteRoot nedPalette = new PaletteRoot();
    PaletteContainer toolsDrawer, componentsDrwawer;

    public PaletteManager(GraphicalNedEditor hostingEditor) {
        super();
        this.hostingEditor = hostingEditor;
        toolsDrawer = createToolsDrawer(nedPalette);
        componentsDrwawer = createComponentsDrawer();
    }

    /* (non-Javadoc)
     * @see org.omnetpp.ned.model.notification.INEDChangeListener#modelChanged(org.omnetpp.ned.model.notification.NEDModelEvent)
     * Called when a change occured in the module which forces palette redraw
     */
    public void modelChanged(NEDModelEvent event) {
        nedPalette.getChildren().clear();
        nedPalette.add(toolsDrawer);
        nedPalette.add(componentsDrwawer);
        nedPalette.addAll(createSubmodulesDrawer());
//        System.out.println("Palette change occured.");
    }

    public PaletteRoot getRootPalette() {
        modelChanged(null);
        return nedPalette;
    }

    /**
     * creates several submodule drawers using currently parsed types
     * and using the GROUP property as the drawer name
     * @return
     */
    static private List<PaletteContainer> createSubmodulesDrawer() {
        String defaultGroupName = "Submodules";
        TreeMap<String,PaletteDrawer> containerMap = new TreeMap<String, PaletteDrawer>(); 
        
        // create the default 
        PaletteDrawer defDrawer 
            = new PaletteDrawer(defaultGroupName, ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_FOLDER));
        containerMap.put(defaultGroupName, defDrawer);

        // get all the possible type names in alphabetical order
        List<String> typeNames 
                = new ArrayList<String>(NEDResourcesPlugin.getNEDResources().getModuleNames());
        Collections.sort(typeNames);
        
        for(String name : typeNames) {
            INEDTypeInfo comp = NEDResourcesPlugin.getNEDResources().getComponent(name);
            NEDElement nedElement = comp.getNEDElement();

            // skip this type if it is a top level network
            if (nedElement instanceof CompoundModuleNodeEx &&
                    ((CompoundModuleNodeEx)nedElement).getIsNetwork()) {
                continue;
            }
            
            // set the default images for the palette entry
            ImageDescriptor imageDescNorm = ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SUBMODULE);
            ImageDescriptor imageDescLarge = ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SUBMODULE);
            if (nedElement instanceof IHasDisplayString) {
                IDisplayString dps = ((IHasDisplayString)nedElement).getEffectiveDisplayString();
                String iid = dps.getAsStringDef(IDisplayString.Prop.IMAGE);
                if (iid != null && !"".equals(iid)) {
                    imageDescNorm = ImageFactory.getDescriptor(iid,"vs",null,0);
                    imageDescLarge = ImageFactory.getDescriptor(iid,"vs",null,0);
                }
            }
            
            // determine which palette group it belongs to or put it to the default
            PropertyNode property = (PropertyNode)comp.getProperties().get("group"); 
            String group = (property == null) ? defaultGroupName
                            : NEDElementUtilEx.getPropertyValue(property);
            
            if (group == null || "".equals(group))
                group = defaultGroupName;
            
            // get the correct drawer and create a new one if does not exists
            PaletteDrawer currentDrawer = containerMap.get(group);
            if (currentDrawer == null) {
                currentDrawer = new PaletteDrawer(group, ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_FOLDER));
                containerMap.put(group, currentDrawer);
            }
            
            // create the tool entry
            CombinedTemplateCreationEntry combined = new CombinedTemplateCreationEntry(
                    name, "A submodule with type "+name,
                    new ModelFactory(SubmoduleNodeEx.getStaticTagName(),name.toLowerCase(), name), 
                    imageDescNorm, imageDescLarge );
            // add to the selected drawer
            currentDrawer.add(combined);
        }
        
        return new ArrayList<PaletteContainer>(containerMap.values());
    }

    static private PaletteStack createChannelsStack() {
        PaletteStack connectionStack = new PaletteStack("Connections","Connect modules using this tool",ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION));

        ConnectionCreationToolEntry defConnTool = new ConnectionCreationToolEntry(
                "Connection",
                "Create connections between submodules, or submodule and parent module",
                new ModelFactory(ConnectionNodeEx.getStaticTagName()), 
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION)//$NON-NLS-1$
        );
        // sets the required connection tool
        defConnTool.setToolClass(NedConnectionCreationTool.class);
        connectionStack.add(defConnTool);

        // get all the possible type names in alphabetical order
        List<String> channelNames 
                = new ArrayList<String>(NEDResourcesPlugin.getNEDResources().getChannelNames());
        Collections.sort(channelNames);
        
        for(String name : channelNames) {

            ConnectionCreationToolEntry tool 
               = new ConnectionCreationToolEntry(
                    name,
                    "Create a connection using the "+name+" type as channel",
                    new ModelFactory(ConnectionNodeEx.getStaticTagName(),name.toLowerCase(), name), 
                    ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION),
                    ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION)//$NON-NLS-1$
            );
            // sets the required connection tool
            tool.setToolClass(NedConnectionCreationTool.class);
            connectionStack.add(tool);
        }
        return connectionStack;
    }
        

    /**
     * Builds a drawer containing basic tools like selection connection etc.
     * @param root
     * @return
     */
    static private PaletteContainer createToolsDrawer(PaletteRoot root) {
        PaletteGroup controlGroup = new PaletteGroup("Tools");

        ToolEntry tool = new PanningSelectionToolEntry("Selector","Select module(s) and connections");
        tool.setToolClass(NedSelectionTool.class);
        controlGroup.add(tool);
        root.setDefaultEntry(tool);

        MarqueeToolEntry marquee = new MarqueeToolEntry("Connection selector","Drag out an area to select connections in it");
        marquee.setToolProperty(MarqueeSelectionTool.PROPERTY_MARQUEE_BEHAVIOR, 
                new Integer(MarqueeSelectionTool.BEHAVIOR_CONNECTIONS_TOUCHED));
        controlGroup.add(marquee);
        
//        tool = new ConnectionCreationToolEntry(
//                "Connection",
//                "The connection tool can be used to connect various modules and submodules",
//                new ModelFactory(ConnectionNodeEx.getStaticTagName()), 
//                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION),
//                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION)//$NON-NLS-1$
//        );
//        // sets the required connection tool
//        tool.setToolClass(NedConnectionCreationTool.class);
        controlGroup.add(createChannelsStack());
        
        return controlGroup;
    }

    /**
     * Builds a drawer conaining base NED components without specifying types like 
     * simple and compound modules
     * @return
     */
    static private PaletteContainer createComponentsDrawer() {

        PaletteGroup drawer = new PaletteGroup("Components");//$NON-NLS-1$

        CombinedTemplateCreationEntry combined;

        combined = new CombinedTemplateCreationEntry(
        		"Submodule",
        		"Create a submodule in a compound module",
        		new ModelFactory(SubmoduleNodeEx.getStaticTagName(), IHasName.INITIAL_NAME, IHasName.INITIAL_NAME), 
        		ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SUBMODULE),
        		ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SUBMODULE)
        );
        drawer.add(combined);

        combined = new CombinedTemplateCreationEntry(
                "Simple module",
                "Create a simple module type",
                new ModelFactory(SimpleModuleNodeEx.getStaticTagName(), IHasName.INITIAL_NAME), 
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SIMPLEMODULE),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SIMPLEMODULE)
        );
        drawer.add(combined);

        combined = new CombinedTemplateCreationEntry(
                "Compound Module",
                "Create a compound module type that may contain submodules",
                new ModelFactory(CompoundModuleNodeEx.getStaticTagName(), IHasName.INITIAL_NAME), 
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_COMPOUNDMODULE),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_COMPOUNDMODULE)
        );
        drawer.add(combined);

        combined = new CombinedTemplateCreationEntry(
                "Channel",
                "Create a channel type",
                new ModelFactory(ChannelNodeEx.getStaticTagName(), IHasName.INITIAL_NAME, IHasName.INITIAL_NAME.toLowerCase()), 
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNEL),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNEL)
        );
        drawer.add(combined);

        combined = new CombinedTemplateCreationEntry(
        		"Interface",
        		"Create a module interface type",
        		new ModelFactory(ModuleInterfaceNodeEx.getStaticTagName(), IHasName.INITIAL_NAME), 
        		ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_INTERFACE),
        		ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_INTERFACE)
        );
        drawer.add(combined);

        combined = new CombinedTemplateCreationEntry(
                "Channel interface",
                "Create a channel interface type",
                new ModelFactory(ChannelInterfaceNodeEx.getStaticTagName(), IHasName.INITIAL_NAME), 
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNELINTERFACE),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNELINTERFACE)
        );
        drawer.add(combined);

        // TODO add other top level components (channel etc.)
        // as a multi tool of the current connection tool
        return drawer;
    }


}
