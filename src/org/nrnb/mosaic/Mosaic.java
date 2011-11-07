/*******************************************************************************
 * Copyright 2010 Alexander Pico
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.nrnb.mosaic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import org.nrnb.mosaic.layout.CellAlgorithm;
import org.nrnb.mosaic.layout.PartitionNetworkVisualStyleFactory;
import org.nrnb.mosaic.partition.MosaicNetworkPanel;
import org.nrnb.mosaic.partition.PartitionAlgorithm;
import org.nrnb.mosaic.setting.MosaicSettingDialog;
import org.nrnb.mosaic.utils.IdMapping;
import org.nrnb.mosaic.utils.MosaicStaticValues;
import org.nrnb.mosaic.utils.MosaicUtil;

import cytoscape.Cytoscape;
import cytoscape.layout.CyLayouts;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.plugin.PluginManager;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanel;


public class Mosaic extends CytoscapePlugin{
    public static final String pluginName = "Mosaic";
    public static final double VERSION = 1.0;
    private CyLogger logger;
    public static String MosaicBaseDir;
    public static String MosaicDatabaseDir;
    public static String MosaicTemplateDir;
    public static boolean tagInternetConn;
    public static boolean tagGPMLPlugin;
    public static boolean tagNodePlugin;
    public static List<String> derbyRemotelist = new ArrayList<String>();
    public static List<String> goRemotelist = new ArrayList<String>();
    public static List<String> speciesMappinglist = new ArrayList<String>();
    private static final String HELP = pluginName + " Help";
    private PartitionAlgorithm partitionObject;
    public static MosaicNetworkPanel wsPanel;
	
    /**
     * The constructor registers our layout algorithm. The CyLayouts mechanism
     * will worry about how to get it in the right menu, etc.
     */
    public Mosaic(){
        logger = CyLogger.getLogger(Mosaic.class);
		logger.setDebug(true);
        try {
            MosaicBaseDir = PluginManager.getPluginManager().getPluginManageDirectory()
                    .getCanonicalPath() + File.separator+ Mosaic.pluginName + File.separator;
        } catch (IOException e) {
            MosaicBaseDir = File.separator+ Mosaic.pluginName + File.separator;
            e.printStackTrace();
        }
        MosaicUtil.checkFolder(MosaicBaseDir);
        MosaicDatabaseDir=MosaicBaseDir+"/DB/";
        MosaicTemplateDir=MosaicBaseDir+"/Temp/";
        MosaicUtil.checkFolder(MosaicDatabaseDir);
        MosaicUtil.checkFolder(MosaicTemplateDir);
        speciesMappinglist = MosaicUtil.readResource(this.getClass()
                .getResource(MosaicStaticValues.bridgedbSpecieslist));
        //Check internet connection
        Mosaic.tagInternetConn = MosaicUtil.checkConnection();
        if(Mosaic.tagInternetConn) {
            //Get the lastest db lists
            derbyRemotelist = MosaicUtil.readUrl(MosaicStaticValues.bridgedbDerbyDir);
            goRemotelist = MosaicUtil.readUrl(MosaicStaticValues.genmappcsDatabaseDir);
            List<String> supportedSpeList = MosaicUtil.readUrl(MosaicStaticValues.genmappcsDatabaseDir+MosaicStaticValues.supportedSpecieslist);
            if(supportedSpeList.size()>0) {
                MosaicUtil.writeFile(supportedSpeList, MosaicBaseDir+MosaicStaticValues.supportedSpecieslist);
                MosaicStaticValues.speciesList = MosaicUtil.parseSpeciesList(supportedSpeList);
            }
        } else {
            if(new File(MosaicBaseDir+MosaicStaticValues.supportedSpecieslist).exists()) {
                List<String> supportedSpeList = MosaicUtil.readFile(MosaicBaseDir+MosaicStaticValues.supportedSpecieslist);
                if(supportedSpeList.size()>0) {
                    MosaicStaticValues.speciesList = MosaicUtil.parseSpeciesList(supportedSpeList);
                }
            }            
        }
        Mosaic.tagGPMLPlugin = MosaicUtil.checkGPMLPlugin();
        partitionObject = new PartitionAlgorithm();
        CyLayouts.addLayout(partitionObject, null);
        CyLayouts.addLayout(new CellAlgorithm(), null);
        //CyLayouts.addLayout(new GpmlTest(), "GPML test");
        // Add Mosaic menu item
        JMenuItem item = new JMenuItem(pluginName);
        JMenu layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
                .getMenu("Plugins");
        item.addActionListener(new MosaicPluginActionListener(this));
        layoutMenu.add(item);
//        //for gpml test
//        item = new JMenuItem("GPML test");
//        layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
//                .getMenu("Plugins");
//        item.addActionListener(new GpmlActionListener(this));
//        layoutMenu.add(item);
        // Add help menu item
        JMenuItem getHelp = new JMenuItem(HELP);
        getHelp.setToolTipText("Open online help for " + pluginName);
        GetHelpListener getHelpListener = new GetHelpListener();
        getHelp.addActionListener(getHelpListener);
        Cytoscape.getDesktop().getCyMenus().getHelpMenu().add(getHelp);

        // create workspaces panel
        CytoPanel cytoPanel1 = Cytoscape.getDesktop().getCytoPanel(
                SwingConstants.WEST);
        //WorkspacesPanel wsPanel = new WorkspacesPanel();
        wsPanel = new MosaicNetworkPanel(logger, partitionObject);
        cytoPanel1.add(pluginName, null, wsPanel, pluginName+" Panel", 1);
//        cytoPanel1.add("GenMAPP-CS", new ImageIcon(getClass().getResource(
//                "images/genmappcs.png")), wsPanel, "Workspaces Panel", 0);
        cytoPanel1.setSelectedIndex(0);
//        // cytoPanel.remove(1);
//
//        // set properties
//        // set view thresholds to handle "overview" xGMMLs
//        CytoscapeInit.getProperties().setProperty("viewThreshold", "100000");
//        CytoscapeInit.getProperties().setProperty("secondaryViewThreshold",
//                        "120000");
//        // set default node width/height lock to avoid dependency issues
//        Cytoscape.getVisualMappingManager().getVisualStyle().getDependency()
//                        .set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED,
//                                        false);
//        // cycommands
//        //new WorkspacesCommandHandler();
    }

    public static void createVisualStyle(CyNetworkView view) {
        PartitionNetworkVisualStyleFactory.createVisualStyle(view);
    }    
}

// Handles the top-level menu selection event from Cytoscape
class MosaicPluginActionListener implements ActionListener {
    Mosaic plugin = null;

    public MosaicPluginActionListener(Mosaic plugin_) {
        plugin = plugin_;
    }

    public void actionPerformed(ActionEvent evt_) {
        try {
            if(!MosaicUtil.checkCyThesaurus()) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                        "CyThesaurus 1.31 or later verion is necessary for runing Mosaic!", Mosaic.pluginName,
                        JOptionPane.WARNING_MESSAGE);
            } else {

                if(Cytoscape.getNetworkSet().size()>0) {
                    NewDialogTask task = new NewDialogTask();

                    final JTaskConfig jTaskConfig = new JTaskConfig();
                    jTaskConfig.setOwner(Cytoscape.getDesktop());
                    jTaskConfig.displayCloseButton(false);
                    jTaskConfig.displayCancelButton(false);
                    jTaskConfig.displayStatus(true);
                    jTaskConfig.setAutoDispose(true);
                    jTaskConfig.setMillisToPopup(100); // always pop the task

                    // Execute Task in New Thread; pop open JTask Dialog Box.
                    TaskManager.executeTask(task, jTaskConfig);

                    final MosaicSettingDialog dialog = task.dialog();
                    dialog.addWindowListener(new WindowAdapter(){
                        public void windowClosed(WindowEvent e){
                            IdMapping.disConnectDerbyFileSource(Mosaic.MosaicDatabaseDir
                        +dialog.identifyLatestVersion(MosaicUtil.retrieveLocalFiles(
                        Mosaic.MosaicDatabaseDir), dialog.annotationSpeciesCode+
                        "_Derby", ".bridge")+".bridge");
                        }
                    });
                    dialog.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                            "Please load a network first!", Mosaic.pluginName,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}

class NewDialogTask implements Task {
    private TaskMonitor taskMonitor;
    private MosaicSettingDialog dialog;

    public NewDialogTask() {
    }

    /**
     * Executes Task.
     */
    //@Override
    public void run() {
        try {
            taskMonitor.setStatus("Initializing...");
            dialog = new MosaicSettingDialog(Cytoscape.getDesktop(), true);
            dialog.setLocationRelativeTo(Cytoscape.getDesktop());
            dialog.setResizable(true);
            taskMonitor.setPercentCompleted(100);
        } catch (Exception e) {
            taskMonitor.setPercentCompleted(100);
            taskMonitor.setStatus("Failed.\n");
            e.printStackTrace();
        }
    }

    public MosaicSettingDialog dialog() {
        return dialog;
    }


    /**
     * Halts the Task: Not Currently Implemented.
     */
    //@Override
    public void halt() {

    }

    /**
     * Sets the Task Monitor.
     *
     * @param taskMonitor
     *            TaskMonitor Object.
     */
    //@Override
    public void setTaskMonitor(TaskMonitor taskMonitor) throws IllegalThreadStateException {
        this.taskMonitor = taskMonitor;
    }

    /**
     * Gets the Task Title.
     *
     * @return Task Title.
     */
    //@Override
    public String getTitle() {
        return "Initializing...";
    }
}

/**
 * This class direct a browser to the help manual web page.
 */
class GetHelpListener implements ActionListener {
    private String helpURL = "http://genmapp.org/beta/mosaic/index.html";

	public void actionPerformed(ActionEvent ae) {
		cytoscape.util.OpenBrowser.openURL(helpURL);
	}
}