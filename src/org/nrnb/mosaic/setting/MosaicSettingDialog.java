/*******************************************************************************
 * Copyright 2011 Chao Zhang
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
package org.nrnb.mosaic.setting;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.nrnb.mosaic.Mosaic;
import org.nrnb.mosaic.layout.CellAlgorithm;
import org.nrnb.mosaic.layout.PartitionNetworkVisualStyleFactory;
import org.nrnb.mosaic.partition.PartitionAlgorithm;
import org.nrnb.mosaic.utils.MosaicStaticValues;
import org.nrnb.mosaic.utils.MosaicUtil;
import org.nrnb.mosaic.utils.IdMapping;

public class MosaicSettingDialog extends JDialog
        implements ActionListener {
    public String annotationSpeciesCode = "";
    private String annotationButtonLabel = "Annotate";
    private List<String> speciesValues = new ArrayList<String>();
    private List<String> downloadDBList = new ArrayList<String>();
    private List<String> currentAttributeList = new ArrayList<String>();
    private List<String> rAnnIdeValues = new ArrayList<String>();
    private List<String> idMappingTypeValues = new ArrayList<String>();

    /** Creates new form MosaicSettingPanel */
    public MosaicSettingDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.setTitle(Mosaic.pluginName+" Settings"+ " "+Mosaic.VERSION);
        loadCurrentValues();
        initComponents();
        initValues();
//        this.repaint();
        this.pack();
    }

    private void loadCurrentValues() {
    }

    private void initValues() {
        System.out.println("**************initialize values*************");
        //rAnnSpeComboBox.removeAll();
        //rAnnTypComboBox.removeAll();
        speciesValues = Arrays.asList(MosaicStaticValues.speciesList);
        currentAttributeList = Arrays.asList(cytoscape.Cytoscape
                .getNodeAttributes().getAttributeNames());
        Collections.sort(currentAttributeList);
        rAnnIdeValues.add("ID");
        rAnnIdeValues.addAll(currentAttributeList);
        rAnnIdeComboBox.setModel(new DefaultComboBoxModel(rAnnIdeValues.toArray()));
        System.out.println("Current species: "+annotationSpeciesCode);
        //Guess species of current network for annotation
        String[] defaultSpecies = getSpeciesCommonName(CytoscapeInit
                .getProperties().getProperty("defaultSpeciesName"));
        System.out.println("Guess species: "+defaultSpecies[0]);
        if(!defaultSpecies[0].equals("")) {
            annotationSpeciesCode = defaultSpecies[1];
            rAnnSpeComboBox.setModel(new DefaultComboBoxModel(speciesValues.toArray()));
            rAnnSpeComboBox.setSelectedIndex(speciesValues.indexOf(defaultSpecies[0]));
            downloadDBList = checkMappingResources(annotationSpeciesCode);
            System.out.println(downloadDBList);
            checkDownloadStatus();

            if(downloadDBList.isEmpty()) {
                idMappingTypeValues = IdMapping.getSourceTypes();
                rAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
                setDefaultAttType("ID");
            }
        }
//        rAnnIdeComboBox.setEnabled(false);
//        rAnnTypComboBox.setEnabled(false);

        //updates ui based on current network attributes
        checkAnnotationStatus();
        //checkDownloadStatus();
            
        aAttParComboBox.setModel(new DefaultComboBoxModel(
                checkAttributes(MosaicStaticValues.BP_ATTNAME).toArray()));
        aAttParComboBox.setSelectedItem(MosaicStaticValues.BP_ATTNAME);
        aAttParComboBox.addActionListener(this);
        aAttLayComboBox.setModel(new DefaultComboBoxModel(new Object[]{"none", MosaicStaticValues.CC_ATTNAME}));
        aAttLayComboBox.setSelectedItem(MosaicStaticValues.CC_ATTNAME);
        aAttNodComboBox.setModel(new DefaultComboBoxModel(
                checkAttributes(MosaicStaticValues.MF_ATTNAME).toArray()));
        aAttNodComboBox.setSelectedItem(MosaicStaticValues.MF_ATTNAME);
        
        if (!Mosaic.tagGPMLPlugin) {
            lTepPreRadioButton.setEnabled(false);
            lTepPreComboBox.setEnabled(false);
            lTepCusRadioButton.setEnabled(false);
            lTepCusTextField.setEnabled(false);
            lTepCusButton.setEnabled(false);
        }
        //2011-09-14
        //Temporarily comment those unimplemented functions
        lTepPanel.setVisible(false);
        //sParSpaLabel.setVisible(false);
        //sParSpaTextField.setVisible(false);
        sParCroLabel.setVisible(false);
        sParCroCheckBox.setVisible(false);
        sParPatLabel.setVisible(false);
        sParPatComboBox.setVisible(false);
    }

    private String[] getSpeciesCommonName(String speName) {
        String[] result = {"", ""};
        for (String line : Mosaic.speciesMappinglist) {
            String tempMappingString = line.replace("\t", " ").toUpperCase();
            if(tempMappingString.indexOf(speName.toUpperCase())!=-1) {
                String[] s = line.split("\t");
                result[0] = s[2].trim();
                result[1] = s[3].trim();
                return result;
            }
        }
        return null;
    }

    private List<String> checkMappingResources(String species){
        List<String> downloadList = new ArrayList<String>();
        List<String> localFileList = new ArrayList<String>();

        String latestDerbyDB = identifyLatestVersion(Mosaic.derbyRemotelist,
                species+"_Derby", ".zip");
        String latestGOAnnotationDB = identifyLatestVersion(Mosaic.goRemotelist,
                species+"_GO", ".zip");

        localFileList = MosaicUtil.retrieveLocalFiles(Mosaic.MosaicDatabaseDir);
        if(localFileList==null || localFileList.isEmpty()) {
            downloadList.add(MosaicStaticValues.bridgedbDerbyDir+latestDerbyDB+".zip");
            downloadList.add(MosaicStaticValues.genmappcsDatabaseDir+latestGOAnnotationDB+".zip");
            System.out.println("No any local db, need download all");
        }  else {
            String localDerbyDB = identifyLatestVersion(localFileList,
                    species+"_Derby", ".bridge");
            if(latestDerbyDB.equals("")&&!localDerbyDB.equals(""))
                latestDerbyDB = localDerbyDB;
            if(localDerbyDB.equals("")||!localDerbyDB.equals(latestDerbyDB))
                downloadList.add(MosaicStaticValues.bridgedbDerbyDir+latestDerbyDB+".zip");
            String localGOslimDB = identifyLatestVersion(localFileList,
                    species+"_GO", ".zip");
            if(latestGOAnnotationDB.equals("")&&!localGOslimDB.equals(""))
                latestGOAnnotationDB = localGOslimDB;
            if(localGOslimDB.equals("")||!localGOslimDB.equals(latestGOAnnotationDB))
                downloadList.add(MosaicStaticValues.genmappcsDatabaseDir+latestGOAnnotationDB+".zip");
        }
        return downloadList;
    }

    public String identifyLatestVersion(List<String> dbList, String prefix, String surfix) {
        String result = "";
        int latestdate = 0;
        for (String filename : dbList) {
            Pattern p = Pattern.compile(prefix+"_\\d{8}\\"+surfix);
            Matcher m = p.matcher(filename);
            if(m.find()) {
                filename = m.group();
                String datestr = filename.substring(filename.lastIndexOf("_")+1,
                        filename.indexOf("."));
                if (datestr.matches("^\\d{8}$")) {
                    int date = new Integer(datestr);
                    if (date > latestdate) {
                        latestdate = date;
                        result = filename.substring(0,filename.lastIndexOf("."));
                    }
                }
            }
        }
        return result;
    }
    
    private void checkDownloadStatus() {
        if(downloadDBList.isEmpty()) {
            rAnnIdeComboBox.setEnabled(true);
            rAnnTypComboBox.setEnabled(true);
            rAnnMesButton.setText(this.annotationButtonLabel);
            if(this.annotationButtonLabel == "Re-annotate") {
                rAnnMesButton.setForeground(Color.BLACK);
                rAnnMesLabel.setText("You can optionally re-annotate this network and old annotation will be replaced.");
                rAnnMesLabel.setForeground(Color.BLACK);
                submitButton.setEnabled(true);
            } else {
                rAnnMesButton.setForeground(Color.RED);
                rAnnMesLabel.setText("You need to first annotate this network with the GO terms selected above.");
                rAnnMesLabel.setForeground(Color.RED);
                submitButton.setEnabled(false);
            }
        } else {
            rAnnIdeComboBox.setEnabled(false);
            rAnnTypComboBox.setEnabled(false);
            if(!Mosaic.tagInternetConn) {
                rAnnMesButton.setText("Help!");
                rAnnMesButton.setForeground(Color.RED);
                rAnnMesLabel.setText("Please check internet connection.");
                rAnnMesLabel.setForeground(Color.RED);
            } else {
                rAnnMesButton.setText("Download");
                rAnnMesButton.setForeground(Color.RED);
                rAnnMesLabel.setText("You need to first download annotation databases for selected species.");
                rAnnMesLabel.setForeground(Color.RED);
            }
            submitButton.setEnabled(false);
        }
    }
    
    private void checkAnnotationStatus() {
        String partitionAttr = this.aAttParComboBox.getSelectedItem().toString();
        String layoutAttr = this.aAttLayComboBox.getSelectedItem().toString();
        String colorAttr = this.aAttNodComboBox.getSelectedItem().toString();
        List CurrentNetworkAtts = Arrays.asList(Cytoscape.getNodeAttributes()
                .getAttributeNames());
        int numberOfNodes = Cytoscape.getCurrentNetwork().nodesList().size();
        //If user didn't choose GO attribute for partition, disable 'The deepest level of GO term for partition'
        if(!isGOAttr(partitionAttr)) {
            sParLevComboBox.setEnabled(false);
            sParPatComboBox.setEnabled(false);
        } else {
            sParLevComboBox.setEnabled(true);
            sParPatComboBox.setEnabled(true);
        }
        if((isGOAttr(partitionAttr)&&(!CurrentNetworkAtts.contains(partitionAttr)||
                checkAnnotationRate(partitionAttr)==0))||
                (isGOAttr(layoutAttr)&&(!CurrentNetworkAtts.contains(layoutAttr)||
                checkAnnotationRate(layoutAttr)==0))||
                (isGOAttr(colorAttr)&&(!CurrentNetworkAtts.contains(colorAttr)||
                checkAnnotationRate(colorAttr)==0))) {
            //Any of three global settings is GO attribute and annotation rate equals 0.
            //Force user to fetch the annotations, and user can not turn off the annotation panel.
            submitButton.setEnabled(false);
            rAnnSpeComboBox.setEnabled(true);
            rAnnGOtComboBox.setEnabled(true);
            rAnnIdeComboBox.setEnabled(true);
            rAnnTypComboBox.setEnabled(true);
            rAnnMesButton.setEnabled(true);
            this.annotationButtonLabel = "Annotate";
            rAnnMesButton.setText(this.annotationButtonLabel);
            rAnnMesButton.setForeground(Color.RED);
            rAnnMesLabel.setEnabled(true);
            rAnnMesLabel.setText("You need to first annotate this network with the GO terms selected above.");
            rAnnMesLabel.setForeground(Color.RED);
        } else if(!(isGOAttr(partitionAttr)||isGOAttr(layoutAttr)||isGOAttr(colorAttr))) {
            //None of three global settings is GO attribute, user can not turn on the annotation panel.
            submitButton.setEnabled(true);
            rAnnSpeComboBox.setEnabled(false);
            rAnnGOtComboBox.setEnabled(false);
            rAnnIdeComboBox.setEnabled(false);
            rAnnTypComboBox.setEnabled(false);
            rAnnMesButton.setEnabled(false); 
            rAnnMesLabel.setEnabled(false);
        } else {
            submitButton.setEnabled(true);
            rAnnSpeComboBox.setEnabled(true);
            rAnnGOtComboBox.setEnabled(true);
            rAnnIdeComboBox.setEnabled(true);
            rAnnTypComboBox.setEnabled(true);
            rAnnMesButton.setEnabled(true);
            this.annotationButtonLabel = "Re-annotate";
            rAnnMesButton.setText(this.annotationButtonLabel);
            rAnnMesButton.setForeground(Color.BLACK);
            rAnnMesLabel.setEnabled(true);
            rAnnMesLabel.setText("You can optionally re-annotate this network and old annotations wiil be replaced.");
            rAnnMesLabel.setForeground(Color.BLACK);
        }

        aAttParRateLabel.setText(checkAnnotationRate(partitionAttr)+"/"+numberOfNodes+" nodes have attribute values");
        aAttLayRateLabel.setText(checkAnnotationRate(layoutAttr)+"/"+numberOfNodes+" nodes have attribute values");
        aAttNodRateLabel.setText(checkAnnotationRate(colorAttr)+"/"+numberOfNodes+" nodes have attribute values");
        checkDownloadStatus();
    }
    
    private boolean isGOAttr(String selectedAttribute) {
        if(selectedAttribute.equals(MosaicStaticValues.BP_ATTNAME)||
                selectedAttribute.equals(MosaicStaticValues.CC_ATTNAME)||
                selectedAttribute.equals(MosaicStaticValues.MF_ATTNAME)) {
            return true;
        } else {
            return false;
        }
    }
    
    private int checkAnnotationRate(String goAttribute) {
        int count = 0;
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        CyAttributes currentAttrs = Cytoscape.getNodeAttributes();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            if (currentAttrs.hasAttribute(cn.getIdentifier(), goAttribute)) {
                byte type = currentAttrs.getType(goAttribute);
                if (type == CyAttributes.TYPE_SIMPLE_LIST) {
                    List list = currentAttrs.getListAttribute(
                        cn.getIdentifier(), goAttribute);
                    if (list.size() > 1){
                        count++;
                    } else if (list.size() == 1){
                        if (list.get(0) != null)
                            if (!list.get(0).equals(""))
                                count++;
                    }
                } else if (type == CyAttributes.TYPE_STRING) {
                    if (!currentAttrs.getStringAttribute(cn.getIdentifier(),
                            goAttribute).equals("null"))
                        count++;
                } else {
                    //we don't have to be as careful with other attribute types
                    if (!currentAttrs.getAttribute(cn.getIdentifier(),
                            goAttribute).equals(null))
                        count++;
                }
            }
        }
        return count;
    }

    private List<String> checkAttributes(String goAttribute){
        List<String> result = new ArrayList();
        result.add("none");
        if(!currentAttributeList.contains(goAttribute))
            result.add(goAttribute);
        result.addAll(currentAttributeList);
        return result;
    }
    
    private void setDefaultAttType(String idName) {        
        String sampleID = Cytoscape.getCurrentNetwork().nodesList().get(0)
                .toString();
        if(!idName.equals("ID")) {
            CyAttributes attribs = Cytoscape.getNodeAttributes();
            if (attribs.getType(idName) == CyAttributes.TYPE_SIMPLE_LIST) {
                List<Object> attList = attribs.getListAttribute(sampleID, idName);
                for(int i=0;i<attList.size();i++) {
                    if(attList.get(i) != null) {
                        sampleID = attList.get(i).toString();
                        break;
                    }
                }                
            } else {
                sampleID = Cytoscape.getNodeAttributes().getAttribute(sampleID, idName).toString();
            }
        }
        Set<String> guessResult = IdMapping.guessIdType(sampleID);
        if(guessResult.isEmpty()) {
            rAnnTypComboBox.setSelectedIndex(findMatchType("Ensembl"));
        } else {
            rAnnTypComboBox.setSelectedIndex(findMatchType(guessResult.toArray()[0]
                    .toString()));
        }
    }
    
    private int findMatchType(String matchSeq) {
        if(matchSeq.equals("Ensembl") && annotationSpeciesCode.equals("At"))
            matchSeq = "Gramene Arabidopsis";
        int i = idMappingTypeValues.indexOf(matchSeq);
        if(i==-1) {
            int n=0;
            for(String type:idMappingTypeValues) {
                if(type.trim().toLowerCase().indexOf("ensembl")!=-1)
                    return n;
                n++;
            }
            return 0;
        } else {
            return i;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        javax.swing.JPanel aSelPanel = new javax.swing.JPanel();
        aAttParLabel = new javax.swing.JLabel();
        aAttLayLabel = new javax.swing.JLabel();
        aAttNodLabel = new javax.swing.JLabel();
        aAttParComboBox = new javax.swing.JComboBox();
        aAttLayComboBox = new javax.swing.JComboBox();
        aAttNodComboBox = new javax.swing.JComboBox();
        aAttParRateLabel = new javax.swing.JLabel();
        aAttLayRateLabel = new javax.swing.JLabel();
        aAttNodRateLabel = new javax.swing.JLabel();
        rAnnPanel = new javax.swing.JPanel();
        rAnnMesLabel = new javax.swing.JLabel();
        rAnnMesButton = new javax.swing.JButton();
        rAnnSpeLabel = new javax.swing.JLabel();
        rAnnSpeComboBox = new javax.swing.JComboBox();
        rAnnIdeComboBox = new javax.swing.JComboBox();
        rAnnTypComboBox = new javax.swing.JComboBox();
        rAnnIdeLabel = new javax.swing.JLabel();
        rAnnTypLabel = new javax.swing.JLabel();
        rAnnGOtLabel = new javax.swing.JLabel();
        rAnnGOtComboBox = new javax.swing.JComboBox();
        lTepPanel = new javax.swing.JPanel();
        lTepPreRadioButton = new javax.swing.JRadioButton();
        lTepCusRadioButton = new javax.swing.JRadioButton();
        lTepPreComboBox = new javax.swing.JComboBox();
        lTepCusTextField = new javax.swing.JTextField();
        lTepCusButton = new javax.swing.JButton();
        ButtonPanel = new javax.swing.JPanel();
        submitButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        sParPanel = new javax.swing.JPanel();
        sParFewLabel = new javax.swing.JLabel();
        sParMorLabel = new javax.swing.JLabel();
        sParLevLabel = new javax.swing.JLabel();
        sParPatLabel = new javax.swing.JLabel();
        sParFewTextField = new javax.swing.JTextField();
        sParMorTextField = new javax.swing.JTextField();
        sParLevComboBox = new javax.swing.JComboBox();
        sParPatComboBox = new javax.swing.JComboBox();
        sParSpaLabel = new javax.swing.JLabel();
        sParSpaTextField = new javax.swing.JTextField();
        sParCroCheckBox = new javax.swing.JCheckBox();
        sParCroLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        aSelPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Attributes"));

        aAttParLabel.setText("Attribute to use for partitioning");
        aAttParLabel.setMaximumSize(new java.awt.Dimension(228, 14));
        aAttParLabel.setMinimumSize(new java.awt.Dimension(228, 14));
        aAttParLabel.setPreferredSize(new java.awt.Dimension(228, 14));

        aAttLayLabel.setText("Attribute to use for layout");
        aAttLayLabel.setMaximumSize(new java.awt.Dimension(228, 14));
        aAttLayLabel.setMinimumSize(new java.awt.Dimension(228, 14));
        aAttLayLabel.setPreferredSize(new java.awt.Dimension(228, 14));

        aAttNodLabel.setText("Attribute to use for node color");
        aAttNodLabel.setMaximumSize(new java.awt.Dimension(228, 14));
        aAttNodLabel.setMinimumSize(new java.awt.Dimension(228, 14));
        aAttNodLabel.setPreferredSize(new java.awt.Dimension(228, 14));

        aAttParComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "annotation.GO BIOLOGICAL_PROCESS" }));
        aAttParComboBox.setMinimumSize(new java.awt.Dimension(212, 18));
        aAttParComboBox.setPreferredSize(new java.awt.Dimension(212, 18));
        aAttParComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aAttParComboBoxActionPerformed(evt);
            }
        });

        aAttLayComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "annotation.GO CELLULAR_COMPONENT" }));
        aAttLayComboBox.setPreferredSize(new java.awt.Dimension(212, 18));
        aAttLayComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aAttLayComboBoxActionPerformed(evt);
            }
        });

        aAttNodComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "annotation.GO MOLECULAR_FUNCTION" }));
        aAttNodComboBox.setPreferredSize(new java.awt.Dimension(212, 18));
        aAttNodComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aAttNodComboBoxActionPerformed(evt);
            }
        });

        aAttParRateLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        aAttParRateLabel.setText("0/0 nodes have attribute values");

        aAttLayRateLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        aAttLayRateLabel.setText("0/0 nodes have attribute values");

        aAttNodRateLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        aAttNodRateLabel.setText("0/0 nodes have attribute values");

        javax.swing.GroupLayout aSelPanelLayout = new javax.swing.GroupLayout(aSelPanel);
        aSelPanel.setLayout(aSelPanelLayout);
        aSelPanelLayout.setHorizontalGroup(
            aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, aSelPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(aAttNodLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                    .addComponent(aAttLayLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                    .addComponent(aAttParLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(aAttLayComboBox, 0, 180, Short.MAX_VALUE)
                    .addComponent(aAttParComboBox, 0, 180, Short.MAX_VALUE)
                    .addComponent(aAttNodComboBox, 0, 180, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(aAttParRateLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                    .addComponent(aAttLayRateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                    .addComponent(aAttNodRateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE))
                .addContainerGap())
        );
        aSelPanelLayout.setVerticalGroup(
            aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(aSelPanelLayout.createSequentialGroup()
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aAttParLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttParRateLabel)
                    .addComponent(aAttParComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aAttLayLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttLayRateLabel)
                    .addComponent(aAttLayComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aAttNodLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttNodRateLabel)
                    .addComponent(aAttNodComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        rAnnPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Retrieve GO Annotations"));
        rAnnPanel.setPreferredSize(new java.awt.Dimension(660, 140));

        rAnnMesLabel.setForeground(java.awt.Color.red);
        rAnnMesLabel.setText("You need to first annotate this network with the GO terms.");

        rAnnMesButton.setForeground(java.awt.Color.red);
        rAnnMesButton.setText("Annotate");
        rAnnMesButton.setMaximumSize(new java.awt.Dimension(32767, 32767));
        rAnnMesButton.setMinimumSize(new java.awt.Dimension(90, 18));
        rAnnMesButton.setPreferredSize(new java.awt.Dimension(108, 23));
        rAnnMesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rAnnMesButtonActionPerformed(evt);
            }
        });

        rAnnSpeLabel.setText("Species");
        rAnnSpeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        rAnnSpeLabel.setMaximumSize(new java.awt.Dimension(228, 14));
        rAnnSpeLabel.setMinimumSize(new java.awt.Dimension(228, 14));
        rAnnSpeLabel.setPreferredSize(new java.awt.Dimension(228, 14));

        rAnnSpeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yeast" }));
        rAnnSpeComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        rAnnSpeComboBox.setPreferredSize(new java.awt.Dimension(108, 18));
        rAnnSpeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rAnnSpeComboBoxActionPerformed(evt);
            }
        });

        rAnnIdeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ID" }));
        rAnnIdeComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        rAnnIdeComboBox.setPreferredSize(new java.awt.Dimension(108, 18));
        rAnnIdeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rAnnIdeComboBoxActionPerformed(evt);
            }
        });

        rAnnTypComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Ensembl Yeast" }));
        rAnnTypComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        rAnnTypComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        rAnnIdeLabel.setText("Identifier attribute");
        rAnnIdeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        rAnnIdeLabel.setMaximumSize(new java.awt.Dimension(228, 14));
        rAnnIdeLabel.setMinimumSize(new java.awt.Dimension(228, 14));
        rAnnIdeLabel.setPreferredSize(new java.awt.Dimension(228, 14));

        rAnnTypLabel.setText("Type of identifier");
        rAnnTypLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        rAnnTypLabel.setMaximumSize(new java.awt.Dimension(148, 14));
        rAnnTypLabel.setMinimumSize(new java.awt.Dimension(148, 14));
        rAnnTypLabel.setPreferredSize(new java.awt.Dimension(148, 14));

        rAnnGOtLabel.setText("Type of GO");
        rAnnGOtLabel.setMaximumSize(new java.awt.Dimension(130, 14));
        rAnnGOtLabel.setMinimumSize(new java.awt.Dimension(130, 14));
        rAnnGOtLabel.setPreferredSize(new java.awt.Dimension(130, 14));

        rAnnGOtComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SlimMosaic", "SlimMosaic2", "SlimPIR", "SlimGeneric", "Full" }));
        rAnnGOtComboBox.setToolTipText("We recommend SlimMosaic. GO Full will generate 100's of partitions and node colors.");
        rAnnGOtComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        rAnnGOtComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        javax.swing.GroupLayout rAnnPanelLayout = new javax.swing.GroupLayout(rAnnPanel);
        rAnnPanel.setLayout(rAnnPanelLayout);
        rAnnPanelLayout.setHorizontalGroup(
            rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rAnnPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(rAnnPanelLayout.createSequentialGroup()
                        .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(rAnnIdeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                            .addComponent(rAnnSpeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rAnnIdeComboBox, 0, 97, Short.MAX_VALUE)
                            .addComponent(rAnnSpeComboBox, 0, 97, Short.MAX_VALUE))
                        .addGap(42, 42, 42)
                        .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(rAnnGOtLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                            .addComponent(rAnnTypLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(rAnnMesLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rAnnMesButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                    .addComponent(rAnnGOtComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 105, Short.MAX_VALUE)
                    .addComponent(rAnnTypComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 105, Short.MAX_VALUE))
                .addContainerGap())
        );
        rAnnPanelLayout.setVerticalGroup(
            rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rAnnPanelLayout.createSequentialGroup()
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rAnnMesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnMesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rAnnSpeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnSpeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnGOtLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnGOtComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rAnnIdeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnTypLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnTypComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnIdeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lTepPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Layout Template"));

        buttonGroup1.add(lTepPreRadioButton);
        lTepPreRadioButton.setSelected(true);
        lTepPreRadioButton.setText("Choose prebuilt template");
        lTepPreRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lTepPreRadioButtonActionPerformed(evt);
            }
        });

        buttonGroup1.add(lTepCusRadioButton);
        lTepCusRadioButton.setText("Upload custom template");
        lTepCusRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lTepCusRadioButtonActionPerformed(evt);
            }
        });

        lTepPreComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lTepCusTextField.setText("C:\\test.gpml");
        lTepCusTextField.setEnabled(false);

        lTepCusButton.setText("upload");
        lTepCusButton.setEnabled(false);

        javax.swing.GroupLayout lTepPanelLayout = new javax.swing.GroupLayout(lTepPanel);
        lTepPanel.setLayout(lTepPanelLayout);
        lTepPanelLayout.setHorizontalGroup(
            lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lTepPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lTepCusRadioButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lTepPreRadioButton))
                .addGap(10, 10, 10)
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lTepPanelLayout.createSequentialGroup()
                        .addComponent(lTepCusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lTepCusButton, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lTepPreComboBox, 0, 483, Short.MAX_VALUE))
                .addContainerGap())
        );
        lTepPanelLayout.setVerticalGroup(
            lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lTepPanelLayout.createSequentialGroup()
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lTepPreRadioButton)
                    .addComponent(lTepPreComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lTepCusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lTepCusRadioButton)
                    .addComponent(lTepCusButton))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        submitButton.setText("Run");
        submitButton.setMaximumSize(new java.awt.Dimension(70, 23));
        submitButton.setMinimumSize(new java.awt.Dimension(70, 23));
        submitButton.setPreferredSize(new java.awt.Dimension(70, 23));
        submitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.setPreferredSize(new java.awt.Dimension(70, 23));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ButtonPanelLayout = new javax.swing.GroupLayout(ButtonPanel);
        ButtonPanel.setLayout(ButtonPanelLayout);
        ButtonPanelLayout.setHorizontalGroup(
            ButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ButtonPanelLayout.createSequentialGroup()
                .addGap(490, 490, 490)
                .addComponent(submitButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18))
        );
        ButtonPanelLayout.setVerticalGroup(
            ButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ButtonPanelLayout.createSequentialGroup()
                .addContainerGap(27, Short.MAX_VALUE)
                .addGroup(ButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(submitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        sParPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Set Parameters"));

        sParFewLabel.setText("Minimum nodes to view");

        sParMorLabel.setText("Maximum nodes to view");
        sParMorLabel.setMaximumSize(new java.awt.Dimension(228, 14));
        sParMorLabel.setMinimumSize(new java.awt.Dimension(228, 14));
        sParMorLabel.setPreferredSize(new java.awt.Dimension(228, 14));

        sParLevLabel.setText("GO level cutoff for partitioning");
        sParLevLabel.setMaximumSize(new java.awt.Dimension(228, 14));
        sParLevLabel.setMinimumSize(new java.awt.Dimension(228, 14));
        sParLevLabel.setPreferredSize(new java.awt.Dimension(228, 14));

        sParPatLabel.setText("GO hierarchy type");
        sParPatLabel.setMaximumSize(new java.awt.Dimension(148, 14));
        sParPatLabel.setMinimumSize(new java.awt.Dimension(148, 14));
        sParPatLabel.setOpaque(true);
        sParPatLabel.setPreferredSize(new java.awt.Dimension(148, 14));

        sParFewTextField.setColumns(5);
        sParFewTextField.setText("5");
        sParFewTextField.setMinimumSize(new java.awt.Dimension(90, 18));
        sParFewTextField.setPreferredSize(new java.awt.Dimension(108, 18));
        sParFewTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sParFewTextFieldActionPerformed(evt);
            }
        });

        sParMorTextField.setColumns(5);
        sParMorTextField.setText("200");
        sParMorTextField.setMinimumSize(new java.awt.Dimension(90, 18));
        sParMorTextField.setPreferredSize(new java.awt.Dimension(108, 18));

        sParLevComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All the way", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14" }));
        sParLevComboBox.setSelectedIndex(2);
        sParLevComboBox.setToolTipText("We recommend level 2, followed by interactive partitioning of specific terms of interest.");
        sParLevComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sParLevComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        sParPatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Relative", "Absolute" }));
        sParPatComboBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sParPatComboBox.setPreferredSize(new java.awt.Dimension(108, 18));

        sParSpaLabel.setText("Spacing between nodes");
        sParSpaLabel.setMaximumSize(new java.awt.Dimension(148, 14));
        sParSpaLabel.setMinimumSize(new java.awt.Dimension(148, 14));
        sParSpaLabel.setOpaque(true);
        sParSpaLabel.setPreferredSize(new java.awt.Dimension(148, 14));

        sParSpaTextField.setColumns(5);
        sParSpaTextField.setText("30.0");
        sParSpaTextField.setMinimumSize(new java.awt.Dimension(90, 18));
        sParSpaTextField.setPreferredSize(new java.awt.Dimension(108, 18));

        sParCroCheckBox.setMaximumSize(new java.awt.Dimension(32767, 32767));
        sParCroCheckBox.setMinimumSize(new java.awt.Dimension(90, 18));
        sParCroCheckBox.setPreferredSize(new java.awt.Dimension(108, 18));

        sParCroLabel.setText("Prune cross- region edges?");
        sParCroLabel.setMaximumSize(new java.awt.Dimension(148, 14));
        sParCroLabel.setMinimumSize(new java.awt.Dimension(148, 14));
        sParCroLabel.setOpaque(true);
        sParCroLabel.setPreferredSize(new java.awt.Dimension(148, 14));

        javax.swing.GroupLayout sParPanelLayout = new javax.swing.GroupLayout(sParPanel);
        sParPanel.setLayout(sParPanelLayout);
        sParPanelLayout.setHorizontalGroup(
            sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sParPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sParFewLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParMorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParLevLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParLevComboBox, 0, 98, Short.MAX_VALUE)
                    .addComponent(sParMorTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                    .addComponent(sParFewTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE))
                .addGap(42, 42, 42)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(sParSpaLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                    .addComponent(sParPatLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                    .addComponent(sParCroLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParPatComboBox, 0, 104, Short.MAX_VALUE)
                    .addComponent(sParCroCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                    .addComponent(sParSpaTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
                .addContainerGap())
        );
        sParPanelLayout.setVerticalGroup(
            sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sParPanelLayout.createSequentialGroup()
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sParFewLabel)
                        .addComponent(sParFewTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sParSpaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sParSpaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sParCroCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sParMorTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sParMorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sParCroLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sParPanelLayout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(sParLevLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sParLevComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sParPatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(sParPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sParPatLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(rAnnPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 666, Short.MAX_VALUE)
                    .addComponent(aSelPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lTepPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ButtonPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(aSelPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rAnnPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sParPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lTepPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ButtonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sParFewTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sParFewTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sParFewTextFieldActionPerformed

    private void aAttParComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aAttParComboBoxActionPerformed
        // TODO add your handling code here:
        checkAnnotationStatus();
    }//GEN-LAST:event_aAttParComboBoxActionPerformed

    private void aAttLayComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aAttLayComboBoxActionPerformed
        // TODO add your handling code here:
        checkAnnotationStatus();
    }//GEN-LAST:event_aAttLayComboBoxActionPerformed

    private void aAttNodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aAttNodComboBoxActionPerformed
        // TODO add your handling code here:
        checkAnnotationStatus();
    }//GEN-LAST:event_aAttNodComboBoxActionPerformed

    private void rAnnSpeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rAnnSpeComboBoxActionPerformed
        // TODO add your handling code here:
        //Regenerate list of ID types when user select another species.
        System.out.println("change species");
        IdMapping.disConnectDerbyFileSource(Mosaic.MosaicDatabaseDir
                    +identifyLatestVersion(MosaicUtil.retrieveLocalFiles(
                    Mosaic.MosaicDatabaseDir), annotationSpeciesCode+
                    "_Derby", ".bridge")+".bridge");
        String[] speciesCode = getSpeciesCommonName(rAnnSpeComboBox.getSelectedItem().toString());
        annotationSpeciesCode = speciesCode[1];        
        downloadDBList = checkMappingResources(annotationSpeciesCode);
        checkDownloadStatus();
        if(downloadDBList.isEmpty()) {
            IdMapping.connectDerbyFileSource(Mosaic.MosaicDatabaseDir
                    +identifyLatestVersion(MosaicUtil.retrieveLocalFiles(
                    Mosaic.MosaicDatabaseDir), annotationSpeciesCode+
                    "_Derby", ".bridge")+".bridge");
            idMappingTypeValues = IdMapping.getSourceTypes();
            //System.out.println("No. of types "+ idMappingTypeValues.size());
            rAnnTypComboBox.removeAllItems();
            rAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
        }
        rAnnIdeComboBox.setSelectedItem("ID");
        setDefaultAttType("ID");
        CytoscapeInit.getProperties().setProperty("defaultSpeciesName", speciesCode[0]);
    }//GEN-LAST:event_rAnnSpeComboBoxActionPerformed

    private void rAnnIdeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rAnnIdeComboBoxActionPerformed
        // TODO add your handling code here:
        setDefaultAttType(rAnnIdeComboBox.getSelectedItem().toString());
    }//GEN-LAST:event_rAnnIdeComboBoxActionPerformed

    private void rAnnMesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rAnnMesButtonActionPerformed
        // TODO add your handling code here:
        if(((JButton)evt.getSource()).getText().equals("Download")) {
            System.out.println("download button on click");
//            FileDownloadDialog annDownloadDialog
//                = new FileDownloadDialog(Cytoscape.getDesktop(), downloadDBList);
//            annDownloadDialog.setLocationRelativeTo(Cytoscape.getDesktop());
//            annDownloadDialog.setSize(450, 100);
//            annDownloadDialog.setVisible(true);
            final JTaskConfig jTaskConfig = new JTaskConfig();
            jTaskConfig.setOwner(cytoscape.Cytoscape.getDesktop());
            jTaskConfig.displayCloseButton(true);
            jTaskConfig.displayCancelButton(false);
            jTaskConfig.displayStatus(true);
            jTaskConfig.setAutoDispose(true);
            jTaskConfig.setMillisToPopup(100);
            FileDownloadDialog task
                = new FileDownloadDialog(downloadDBList);
            TaskManager.executeTask(task, jTaskConfig);
            downloadDBList = checkMappingResources(annotationSpeciesCode);
            checkDownloadStatus();
            if(downloadDBList.isEmpty()) {
                IdMapping.connectDerbyFileSource(Mosaic.MosaicDatabaseDir
                    +identifyLatestVersion(MosaicUtil.retrieveLocalFiles(
                    Mosaic.MosaicDatabaseDir), annotationSpeciesCode+
                    "_Derby", ".bridge")+".bridge");
                idMappingTypeValues = IdMapping.getSourceTypes();
                //System.out.println(idMappingTypeValues.toString());
                rAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
            }
            rAnnIdeComboBox.setSelectedItem("ID");
            setDefaultAttType("ID");            
        } else if (((JButton)evt.getSource()).getText().equals(this.annotationButtonLabel)) {
            String[] selectSpecies = getSpeciesCommonName(rAnnSpeComboBox.getSelectedItem().toString());
            //annotationSpeciesCode = speciesCode[1];
            if(!selectSpecies[0].equals("")) {
                List<String> localFileList = MosaicUtil.retrieveLocalFiles(
                    Mosaic.MosaicDatabaseDir);
                String localDerbyDB = Mosaic.MosaicDatabaseDir +
                        identifyLatestVersion(localFileList,selectSpecies[1]+
                        "_Derby", ".bridge") + ".bridge";
                String localGOslimDB = Mosaic.MosaicDatabaseDir+
                        identifyLatestVersion(localFileList,selectSpecies[1]+
                        "_GO"+rAnnGOtComboBox.getSelectedItem().toString().toLowerCase(), ".txt") + ".txt";
                final JTaskConfig jTaskConfig = new JTaskConfig();
                jTaskConfig.setOwner(cytoscape.Cytoscape.getDesktop());
                jTaskConfig.displayCloseButton(true);
                jTaskConfig.displayCancelButton(false);
                jTaskConfig.displayStatus(true);
                jTaskConfig.setAutoDispose(true);
                jTaskConfig.setMillisToPopup(100);
                AnnotationDialog task = new AnnotationDialog(localDerbyDB,
                        localGOslimDB, rAnnIdeComboBox.getSelectedItem().toString(),
                        rAnnTypComboBox.getSelectedItem().toString(),
                        idMappingTypeValues.get(findMatchType("Ensembl")));
                TaskManager.executeTask(task, jTaskConfig);
                this.annotationButtonLabel = "Re-annotate";
                rAnnMesButton.setText(this.annotationButtonLabel);
                rAnnMesButton.setForeground(Color.BLACK);
                rAnnMesLabel.setText("You can optionally re-annotate this network and old annotation will be replaced.");
                rAnnMesLabel.setForeground(Color.BLACK);
                checkAnnotationStatus();
            } else {
                System.out.println("Retrive species error!");
            }
        } else if (((JButton)evt.getSource()).getText().equals("Help!")) {
            JOptionPane.showConfirmDialog(Cytoscape.getDesktop(),
                    "You need internet connection for downloading databases.",
                    "Warning", JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_rAnnMesButtonActionPerformed

    private void submitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitButtonActionPerformed
        // TODO add your handling code here:
        beforeSubmit();
        // Comment CellAlgorithm for testing partitioning, 07/10/2011
        //CellAlgorithm.attributeName = null;
        //PartitionNetworkVisualStyleFactory.attributeName = null;

        if (null != CellAlgorithm.attributeName) {
            PartitionAlgorithm.layoutName = CellAlgorithm.LAYOUT_NAME;
        }
        System.out.println(PartitionNetworkVisualStyleFactory.attributeName);
//        IdMapping.disConnectDerbyFileSource(Mosaic.MosaicDatabaseDir
//                    +identifyLatestVersion(MosaicUtil.retrieveLocalFiles(
//                    Mosaic.MosaicDatabaseDir), annotationSpeciesCode+
//                    "_Derby", ".bridge")+".bridge");
        this.dispose();
//        CyLayoutAlgorithm layout = CyLayouts.getLayout("partition");
//        layout.doLayout(Cytoscape.getCurrentNetworkView(), taskMonitor);
        final JTaskConfig jTaskConfig = new JTaskConfig();
        jTaskConfig.setOwner(Cytoscape.getDesktop());
        jTaskConfig.displayCloseButton(false);
        jTaskConfig.displayCancelButton(false);
        jTaskConfig.displayStatus(true);
        jTaskConfig.setAutoDispose(true);
        jTaskConfig.setMillisToPopup(100); // always pop the task

        // Execute Task in New Thread; pop open JTask Dialog Box.
        TaskManager.executeTask(new RunLayout(), jTaskConfig);
    }//GEN-LAST:event_submitButtonActionPerformed

    private void lTepPreRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lTepPreRadioButtonActionPerformed
        // TODO add your handling code here:
        if(lTepPreRadioButton.isSelected()) {
            lTepPreComboBox.setEnabled(true);
            lTepCusTextField.setEnabled(false);
            lTepCusButton.setEnabled(false);
        } else {
            lTepPreComboBox.setEnabled(false);
            lTepCusTextField.setEnabled(true);
            lTepCusButton.setEnabled(true);
        }
    }//GEN-LAST:event_lTepPreRadioButtonActionPerformed

    private void lTepCusRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lTepCusRadioButtonActionPerformed
        // TODO add your handling code here:
        if(lTepCusRadioButton.isSelected()) {
            lTepPreComboBox.setEnabled(false);
            lTepCusTextField.setEnabled(true);
            lTepCusButton.setEnabled(true);
        } else {
            lTepPreComboBox.setEnabled(true);
            lTepCusTextField.setEnabled(false);
            lTepCusButton.setEnabled(false);
        }
    }//GEN-LAST:event_lTepCusRadioButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        // TODO add your handling code here:
//        IdMapping.disConnectDerbyFileSource(Mosaic.MosaicDatabaseDir
//                    +identifyLatestVersion(MosaicUtil.retrieveLocalFiles(
//                    Mosaic.MosaicDatabaseDir), annotationSpeciesCode+
//                    "_Derby", ".bridge")+".bridge");
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ButtonPanel;
    private javax.swing.JComboBox aAttLayComboBox;
    private javax.swing.JLabel aAttLayLabel;
    private javax.swing.JLabel aAttLayRateLabel;
    private javax.swing.JComboBox aAttNodComboBox;
    private javax.swing.JLabel aAttNodLabel;
    private javax.swing.JLabel aAttNodRateLabel;
    private javax.swing.JComboBox aAttParComboBox;
    private javax.swing.JLabel aAttParLabel;
    private javax.swing.JLabel aAttParRateLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton lTepCusButton;
    private javax.swing.JRadioButton lTepCusRadioButton;
    private javax.swing.JTextField lTepCusTextField;
    private javax.swing.JPanel lTepPanel;
    private javax.swing.JComboBox lTepPreComboBox;
    private javax.swing.JRadioButton lTepPreRadioButton;
    private javax.swing.JComboBox rAnnGOtComboBox;
    private javax.swing.JLabel rAnnGOtLabel;
    private javax.swing.JComboBox rAnnIdeComboBox;
    private javax.swing.JLabel rAnnIdeLabel;
    private javax.swing.JButton rAnnMesButton;
    private javax.swing.JLabel rAnnMesLabel;
    private javax.swing.JPanel rAnnPanel;
    private javax.swing.JComboBox rAnnSpeComboBox;
    private javax.swing.JLabel rAnnSpeLabel;
    private javax.swing.JComboBox rAnnTypComboBox;
    private javax.swing.JLabel rAnnTypLabel;
    private javax.swing.JCheckBox sParCroCheckBox;
    private javax.swing.JLabel sParCroLabel;
    private javax.swing.JLabel sParFewLabel;
    private javax.swing.JTextField sParFewTextField;
    private javax.swing.JComboBox sParLevComboBox;
    private javax.swing.JLabel sParLevLabel;
    private javax.swing.JLabel sParMorLabel;
    private javax.swing.JTextField sParMorTextField;
    private javax.swing.JPanel sParPanel;
    private javax.swing.JComboBox sParPatComboBox;
    private javax.swing.JLabel sParPatLabel;
    private javax.swing.JLabel sParSpaLabel;
    private javax.swing.JTextField sParSpaTextField;
    private javax.swing.JButton submitButton;
    // End of variables declaration//GEN-END:variables

    public void actionPerformed(ActionEvent e) {
//        System.out.println(e.getClass());
    }

    private void beforeSubmit() {
        if (aAttParComboBox.getSelectedItem().equals("none")) {
            PartitionAlgorithm.attributeName = null;
        } else {
            PartitionAlgorithm.attributeName = aAttParComboBox.getSelectedItem().toString();
        }
        if (aAttLayComboBox.getSelectedItem().equals("none")) {
            CellAlgorithm.attributeName = null;
        } else {
            CellAlgorithm.attributeName = aAttLayComboBox.getSelectedItem().toString();
        }
        if (aAttNodComboBox.getSelectedItem().equals("none")) {
            PartitionNetworkVisualStyleFactory.attributeName = null;
        } else {
            PartitionNetworkVisualStyleFactory.attributeName = aAttNodComboBox.getSelectedItem().toString();
        }
        PartitionAlgorithm.NETWORK_LIMIT_MIN = new Integer(sParFewTextField.getText()).intValue();
        PartitionAlgorithm.NETWORK_LIMIT_MAX = new Integer(sParMorTextField.getText()).intValue();
        CellAlgorithm.distanceBetweenNodes = new Double(sParSpaTextField.getText()).doubleValue();
        CellAlgorithm.pruneEdges = sParCroCheckBox.isSelected();
        if(sParLevComboBox.getSelectedItem().equals("All the way")) {
            PartitionAlgorithm.GO_LEVEL = 100;
        } else {
            PartitionAlgorithm.GO_LEVEL = new Integer(sParLevComboBox.getSelectedItem().toString()).intValue();
        }
    }

    private class RunLayout implements Task {
        private TaskMonitor taskMonitor;

        public RunLayout() {
        }

        /**
         * Executes Task.
         */
        //@Override
        public void run() {
                try {
                    taskMonitor.setStatus("Running partition...");
                    CyLayoutAlgorithm layout = CyLayouts.getLayout("partition");
                    layout.doLayout(Cytoscape.getCurrentNetworkView(), taskMonitor);
                    //layout.doLayout(Cytoscape.getCurrentNetworkView());
                    taskMonitor.setPercentCompleted(100);
                } catch (Exception e) {
                    taskMonitor.setPercentCompleted(100);
                    taskMonitor.setStatus("Failed.\n");
                    e.printStackTrace();
                }
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
                return "Running partition...";
        }
    }
}
