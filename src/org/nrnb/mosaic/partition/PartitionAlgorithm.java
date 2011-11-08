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
package org.nrnb.mosaic.partition;

import org.nrnb.mosaic.layout.PartitionNetworkVisualStyleFactory;
import org.nrnb.mosaic.utils.MosaicStaticValues;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;
import cytoscape.ding.CyGraphAllLOD;
import cytoscape.ding.DingNetworkView;
import cytoscape.groups.CyGroup;
import cytoscape.layout.AbstractLayout;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.layout.LayoutProperties;
import cytoscape.view.CyDesktopManager;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.SwingConstants;
import org.nrnb.mosaic.Mosaic;
import org.nrnb.mosaic.utils.MosaicUtil;

/**
 * PartitionAlgorithm
 */
public class PartitionAlgorithm extends AbstractLayout implements
        PropertyChangeListener {
	double distanceBetweenNodes = 80.0d;
	LayoutProperties layoutProperties = null;

	public static String layoutName = "force-directed";
	public ArrayList<Object> nodeAttributeValues = new ArrayList();
	// private Object[] layoutNames = null;
	public static String attributeName = MosaicStaticValues.BP_ATTNAME;
	private HashMap<Object, List<CyNode>> attributeValueNodeMap;
	private List<CyNetworkView> views = new ArrayList<CyNetworkView>();
	private List<CyGroup> groups = new ArrayList<CyGroup>();
	private List<CyNode> unconnectedNodes = new ArrayList<CyNode>();
	public static int NETWORK_LIMIT_MIN = 5; // min to show network
	public static int NETWORK_LIMIT_MAX = 200;
    public static int GO_LEVEL = 100;
	private static final int SUBNETWORK_COUNT_WARNING = 100; // will warn if >
	public static final String SUBNETWORK_CONNECTIONS = "_subnetworkConnections";
	public static final String SUBNETWORK_SIZE = "_subnetworkSize";
    public Map<String, String> goDescMappingFile = new HashMap<String, String>();
    private Object[][] networkTreeArray;
    private CyNetwork rootNetwork;

	/**
	 * Creates a new PartitionAlgorithm object.
	 */
	public PartitionAlgorithm() {
		super();

		// support fit-to-screen when maximizing tiled subnetworks
		Cytoscape.getDesktop().getSwingPropertyChangeSupport()
				.addPropertyChangeListener(
						CytoscapeDesktop.NETWORK_VIEW_FOCUSED, this);
	}

	/**
	 * External interface to update our settings
	 */
	public void updateSettings() {
		updateSettings(true);
	}

	/**
	 * Signals that we want to update our internal settings
	 * 
	 * @param force
	 *            force the settings to be updated, if true
	 */
	public void updateSettings(boolean force) {
		// nothing here... see Mosaic
	}

	/**
	 * Returns the short-hand name of this algorithm NOTE: is related to the
	 * menu item order
	 * 
	 * @return short-hand name
	 */
	public String getName() {
		return "partition";
	}

	/**
	 * Returns the user-visible name of this layout
	 * 
	 * @return user visible name
	 */
	public String toString() {
		return "Partition Only";
	}

	/**
	 * Return true if we support performing our layout on a limited set of nodes
	 * 
	 * @return true if we support selected-only layout
	 */
	public boolean supportsSelectedOnly() {
		return false;
	}

	/**
	 * Returns the types of node attributes supported by this algorithm.
	 * 
	 * @return the list of supported attribute types, or null if node attributes
	 *         are not supported
	 */
	public byte[] supportsNodeAttributes() {
		return null;
	}

	/**
	 * Sets the attribute to use for the weights
	 * 
	 * @param value
	 *            the name of the attribute
	 */
	public void setLayoutAttribute(String value) {
		attributeName = value;
	}

	/**
	 * Returns the types of edge attributes supported by this algorithm.
	 * 
	 * @return the list of supported attribute types, or null if edge attributes
	 *         are not supported
	 */
	public byte[] supportsEdgeAttributes() {
		return null;
	}

	public void buildSubnetworkOverview(CyNetwork net) {

		CyNetwork overview_network = Cytoscape.createNetwork(new int[0],
				new int[0], "Overview", net);
		CyNetworkView overview_view = Cytoscape.getNetworkView(overview_network
				.getIdentifier());

		CyAttributes nAttributes = Cytoscape.getNodeAttributes();
		CyAttributes eAttributes = Cytoscape.getEdgeAttributes();
        List<String> edgeAttName = Arrays.asList(eAttributes.getAttributeNames());
		int[] edges = net.getEdgeIndicesArray();
        
        if(edgeAttName.indexOf(SUBNETWORK_CONNECTIONS)!=-1) {
            for (int edgeInt : edges) {
                int nodeInt1 = Cytoscape.getRootGraph().getEdgeSourceIndex(edgeInt);
                int nodeInt2 = Cytoscape.getRootGraph().getEdgeTargetIndex(edgeInt);
                String node1 = net.getNode(nodeInt1).getIdentifier();
                //System.out.println(node1);
                String node2 = net.getNode(nodeInt2).getIdentifier();
                if (nAttributes.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
                    List<Object> nodePartitionList1 = nAttributes.getListAttribute(node1, attributeName);
                    List<Object> nodePartitionList2 = nAttributes.getListAttribute(node2, attributeName);

                    for (Object np1 : nodePartitionList1) {
                        if(!existNetwork(np1))
                            continue;
                        for (Object np2 : nodePartitionList2) {
                            if(!existNetwork(np2))
                                continue;
                            // skip if same partition
                            if (np1.toString().equalsIgnoreCase(np2.toString()))
                                continue;
                            // create nodes and edges
                            CyNode cn1 = Cytoscape.getCyNode(np1.toString(), true);
                            CyNode cn2 = Cytoscape.getCyNode(np2.toString(), true);
                            CyEdge ce = Cytoscape.getCyEdge(cn1, cn2,
                                    Semantics.INTERACTION, "subnetworkInteraction",
                                    true);
                            
                            eAttributes.setAttribute(ce.getIdentifier(), SUBNETWORK_CONNECTIONS, 0.0);
                        }
                    }
                }
            }
        }        
		for (int edgeInt : edges) {
			int nodeInt1 = Cytoscape.getRootGraph().getEdgeSourceIndex(edgeInt);
			int nodeInt2 = Cytoscape.getRootGraph().getEdgeTargetIndex(edgeInt);
			String node1 = net.getNode(nodeInt1).getIdentifier();
            //System.out.println(node1);
			String node2 = net.getNode(nodeInt2).getIdentifier();
            if (nAttributes.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
				List<Object> nodePartitionList1 = nAttributes.getListAttribute(
						node1, attributeName);
				List<Object> nodePartitionList2 = nAttributes.getListAttribute(
						node2, attributeName);

				for (Object np1 : nodePartitionList1) {
                    if(!existNetwork(np1))
                        continue;
					for (Object np2 : nodePartitionList2) {
                        if(!existNetwork(np2))
                            continue;
						// skip if same partition
						if (np1.toString().equalsIgnoreCase(np2.toString()))
							continue;
						// create nodes and edges
						CyNode cn1 = Cytoscape.getCyNode(np1.toString(), true);
						CyNode cn2 = Cytoscape.getCyNode(np2.toString(), true);
						CyEdge ce = Cytoscape.getCyEdge(cn1, cn2,
								Semantics.INTERACTION, "subnetworkInteraction",
								true);
						overview_network.addNode(cn1);
						overview_network.addNode(cn2);
						overview_network.addEdge(ce);

                        if(MosaicUtil.isValidGOTerm(nodeAttributeValues)) {
                            if(goDescMappingFile.containsKey(np1)) {
                                nAttributes.setAttribute(cn1.getIdentifier(), "canonicalName", goDescMappingFile.get(np1).toString());
                            } else {
                                nAttributes.setAttribute(cn1.getIdentifier(), "canonicalName", np1.toString());
                            }
                            if(goDescMappingFile.containsKey(np2)) {
                                nAttributes.setAttribute(cn2.getIdentifier(), "canonicalName", goDescMappingFile.get(np2).toString());
                            } else {
                                nAttributes.setAttribute(cn2.getIdentifier(), "canonicalName", np2.toString());
                            }
                        } else {
                            nAttributes.setAttribute(cn1.getIdentifier(), "canonicalName", np1.toString());
                            nAttributes.setAttribute(cn2.getIdentifier(), "canonicalName", np2.toString());
						}
                        
						if (null != eAttributes.getDoubleAttribute(ce
								.getIdentifier(), SUBNETWORK_CONNECTIONS)) {
							eAttributes.setAttribute(ce.getIdentifier(), SUBNETWORK_CONNECTIONS,
                                    eAttributes.getDoubleAttribute(ce.getIdentifier(),
                                    SUBNETWORK_CONNECTIONS) + 1.0);

						} else { // first pass; thus create attribute
							//use double so you can passThrough to Edge Line Width
							eAttributes.setAttribute(ce.getIdentifier(), SUBNETWORK_CONNECTIONS, 1.0);
						}
					}
				}
			} else { // TODO: fix assumption of String!
				String nodeRegion1 = nAttributes.getStringAttribute(node1,attributeName);
				String nodeRegion2 = nAttributes.getStringAttribute(node2,attributeName);
			}
		}

		Iterator<CyNode> nodeIt = overview_network.nodesIterator();
		while (nodeIt.hasNext()) {
			String nodeId = nodeIt.next().getIdentifier();
            nAttributes.setAttribute(nodeId, SUBNETWORK_SIZE,
					attributeValueNodeMap.get(nodeId).size());
		}

		Cytoscape.getVisualMappingManager().setVisualStyle(
				PartitionNetworkVisualStyleFactory.attributeName);
		CyLayoutAlgorithm layout = CyLayouts.getLayout("degree-circle");
		layout.doLayout(overview_view);
		views.add(0, overview_view);
	}

	public void populateNodes(String attributeName) {

		CyAttributes attribs = Cytoscape.getNodeAttributes();
		Iterator<CyNode> it = Cytoscape.getCurrentNetwork().nodesIterator();
		List<CyNode> selectedNodes = null;
		List<CyNode> unassignedNodes = Cytoscape.getCurrentNetwork()
				.nodesList();

		boolean valueFound = false;

		while (it.hasNext()) {

			valueFound = false;
			CyNode node = it.next();

			// assign unconnected nodes to a special category and move on
			//AP: buggy for networks without edges
			//AP: why is this needed, anyways?
//			int[] edges = Cytoscape.getCurrentNetwork()
//					.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(),
//							true, true, true);
//			if (edges.length <= 0) {
//				unconnectedNodes.add(node);
//				continue;
//			}

			String val = null;
			String terms[] = new String[1];
			// add support for parsing List type attributes
			if (attribs.getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
				List valList = attribs.getListAttribute(node.getIdentifier(),
						attributeName);
				// System.out.println ("Got values for node: " + node + " = " +
				// valList);
				// iterate through all elements in the list
				if (valList != null && valList.size() > 0) {
					terms = new String[valList.size()];
					for (int i = 0; i < valList.size(); i++) {
						Object o = valList.get(i);
						terms[i] = o.toString();
					}
				}
				val = join(terms);
			} else {
				String valCheck = attribs.getStringAttribute(node
						.getIdentifier(), attributeName);
				if (valCheck != null && !valCheck.equals("")) {
					val = valCheck;
				}
			}

			if ((!(val == null) && (!val.equals("null")) && (val.length() > 0))) {

				for (Object o : nodeAttributeValues) {
                    if (val.indexOf(o.toString()) >= 0) {
						selectedNodes = attributeValueNodeMap.get(o);
						if (selectedNodes == null) {
							selectedNodes = new ArrayList<CyNode>();
						}
						if (!selectedNodes.contains(node)) {
							selectedNodes.add(node);
							attributeValueNodeMap.put(o.toString(),
									selectedNodes);
							valueFound = true;
						}
					}
				}
			}
			if (!valueFound)
			// put this node in 'unassigned' category
			// but do we need to treat separately the case where there is a
			// value not in the template
			// from the case where there is no value?
			{
				selectedNodes = attributeValueNodeMap.get("unassigned");
				if (selectedNodes == null) {
					selectedNodes = new ArrayList<CyNode>();
				}
				if (!selectedNodes.contains(node)) {
					selectedNodes.add(node);
					attributeValueNodeMap.put("unassigned", selectedNodes);
					valueFound = true;
				}
			}
		}
	}

	private String join(String values[]) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			buf.append(values[i]);
			if (i < values.length - 1) {
				buf.append(", ");
			}
		}
		return buf.toString();
	}

    private CyNetwork getParentNetwork(String networkID) {
        for(CyNetwork c:Cytoscape.getNetworkSet()) {
            if(c.getTitle().equals(networkID))
                return c;
        }
        return Cytoscape.getNetwork(networkID);
    }

	/**
	 * build a subnetwork for selected nodes leverages from
	 * cytoscape.actions.NewWindowSelectedNodesOnlyAction
	 * 
	 * @param current_network
	 */
    public void buildSubNetwork(CyNetwork current_network, int index) {
        CyNetworkView current_network_view = null;
        CyNetwork parentNet = null;
		if (Cytoscape.viewExists(current_network.getIdentifier())) {
			current_network_view = Cytoscape.getNetworkView(current_network
					.getIdentifier());
		} // end of if ()

		List nodes = attributeValueNodeMap.get(networkTreeArray[index][0]);
		// System.out.println("Got nodes for attributeValue: " + attributeValue
		// + " = " + nodes.size());
		if (nodes == null) {
			return;
		}
        
		//System.out.println("**************SIZE: "+ nodes.size()+"**************");
        //System.out.println(goInfo[1].toString().trim()+":"+Cytoscape.viewExists(goInfo[1].toString().trim()));
        if(networkTreeArray[index][1]!="root") {
            parentNet = getParentNetwork(goDescMappingFile.get(networkTreeArray[index][1].toString().trim()));
            //System.out.println("aaa:"+parentNet.getIdentifier());
        } else {
            parentNet = current_network;
        }
        //System.out.println(current_network.getConnectingEdges(new ArrayList(nodes)).size());
		CyNetwork new_network = Cytoscape.createNetwork(nodes, current_network
				.getConnectingEdges(new ArrayList(nodes)),
		// CyNetworkNaming.getSuggestedSubnetworkTitle(current_network),
				networkTreeArray[index][4].toString(), // for network title
				parentNet, (nodes.size() >= NETWORK_LIMIT_MIN)
						&& nodes.size() <= NETWORK_LIMIT_MAX);
        //System.out.println(new_network.getTitle()+"\t"+nodes.size());
        networkTreeArray[index][5] = new_network.getIdentifier();
		// optional create network view
        //new_network.setIdentifier(goInfo[0].toString().trim());        
		CyNetworkView new_view = Cytoscape.getNetworkView(new_network
				.getIdentifier());
//        for(CyNetwork c:Cytoscape.getNetworkSet())
//            System.out.println(c.getIdentifier());
        if (new_view == Cytoscape.getNullNetworkView()) {
			return;
		}
        
		views.add(new_view);
        
		// listen for window maximize or restore.
		Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(
				new_view).addPropertyChangeListener(
				JInternalFrame.IS_MAXIMUM_PROPERTY, this);

		// apply layout
		if (current_network_view != Cytoscape.getNullNetworkView()) {

			// CyLayoutAlgorithm layout =
			// CyLayouts.getLayout("force-directed");
			// System.out.println("Layout: " + new_view.getTitle() +" :: "+
			// layoutName);

			CyLayoutAlgorithm layout = CyLayouts.getLayout(layoutName);
			layout.doLayout(new_view);

		}

		// set graphics level of detail
		((DingNetworkView) new_view).setGraphLOD(new CyGraphAllLOD());

		if (PartitionNetworkVisualStyleFactory.attributeName != null) {
			Cytoscape.getVisualMappingManager().setVisualStyle(
					PartitionNetworkVisualStyleFactory.attributeName);
		}        
	}
    
	public void buildSubNetwork(CyNetwork current_network, String attributeValue) {

		CyNetworkView current_network_view = null;

		if (Cytoscape.viewExists(current_network.getIdentifier())) {
			current_network_view = Cytoscape.getNetworkView(current_network
					.getIdentifier());
		} // end of if ()

		List nodes = attributeValueNodeMap.get(attributeValue);
		// System.out.println("Got nodes for attributeValue: " + attributeValue
		// + " = " + nodes.size());
		if (nodes == null) {
			return;
		}

		//System.out.println("SIZE: " +attributeValue +": "+ nodes.size());

		CyNetwork new_network = Cytoscape.createNetwork(nodes, current_network
				.getConnectingEdges(new ArrayList(nodes)),
		// CyNetworkNaming.getSuggestedSubnetworkTitle(current_network),
				attributeValue, // for network title
				current_network, (nodes.size() >= NETWORK_LIMIT_MIN)
						&& nodes.size() <= NETWORK_LIMIT_MAX);
		// optional create network view

		CyNetworkView new_view = Cytoscape.getNetworkView(new_network
				.getIdentifier());

		if (new_view == Cytoscape.getNullNetworkView()) {
			return;
		}

		views.add(new_view);

		// listen for window maximize or restore.
		Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(
				new_view).addPropertyChangeListener(
				JInternalFrame.IS_MAXIMUM_PROPERTY, this);

		// apply layout
		if (current_network_view != Cytoscape.getNullNetworkView()) {

			// CyLayoutAlgorithm layout =
			// CyLayouts.getLayout("force-directed");
			// System.out.println("Layout: " + new_view.getTitle() +" :: "+
			// layoutName);

			CyLayoutAlgorithm layout = CyLayouts.getLayout(layoutName);
			layout.doLayout(new_view);

		}

		// set graphics level of detail
		((DingNetworkView) new_view).setGraphLOD(new CyGraphAllLOD());

		if (PartitionNetworkVisualStyleFactory.attributeName != null) {
			Cytoscape.getVisualMappingManager().setVisualStyle(
					PartitionNetworkVisualStyleFactory.attributeName);
		}
	}

	// }

	/**
	 * layout the subnetwork views in a grid
	 */
	public void tileNetworkViews() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				CyDesktopManager.arrangeFrames(CyDesktopManager.Arrange.GRID);
				// finally loop through the network views and fitContent
				for (CyNetworkView view : views) {
					// Cytoscape.setCurrentNetworkView(view.getIdentifier());
					view.fitContent();
				}

			}
		});
        Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST).setSelectedIndex(1);
	}

	/**
	 * The layout protocol...
	 */
	public void construct() {
        System.out.println("*******************PartitionAlgorithm******************");
        System.out.println("Partition Attribute:" +attributeName);
		// if "(none)" was selected in setting, then skip partitioning
		if (null == attributeName) {

			// create visual style if set
			if (PartitionNetworkVisualStyleFactory.attributeName != null) {
				// if a non-null attribute has been selection for node coloring
				// and if not running PartitionOnly
				Mosaic.createVisualStyle(Cytoscape.getCurrentNetworkView());
			}

			// pass on to CellAlgorithm
			CyLayoutAlgorithm layout = CyLayouts.getLayout(layoutName);
			layout.doLayout(Cytoscape.getCurrentNetworkView());
		} else {

			// Our node map needs to be reset in case we have a new network
			attributeValueNodeMap = new HashMap<Object, List<CyNode>>();

			taskMonitor.setStatus("Partitioning the network by "
					+ attributeName);
			taskMonitor.setPercentCompleted(1);

			// Reset partition groups
			// TODO: CyGroup bug: can't get rid of groups created in previous
			// session!
			// for (CyGroup cg : CyGroupManager.getGroupList()) {
			// CyGroupManager.removeGroup(cg);
			// }

			nodeAttributeValues = MosaicUtil.setupNodeAttributeValues(attributeName);
            System.out.println("Partition Attribute number: " +nodeAttributeValues.size());
            Set<Object> attributeValues = new HashSet<Object>(nodeAttributeValues);
			// warn before building more than 100 subnetworks;
            int numberOfSub = 0;
            if(MosaicUtil.isValidGOTerm(nodeAttributeValues)) {
                buildNetworkTreeMap(attributeValues);
                for (int i=0;i<networkTreeArray.length;i++) {
                    if(this.GO_LEVEL>=new Integer(networkTreeArray[i][3].toString()).intValue()) {
                        numberOfSub++;
                    }
                }
            } else {
                numberOfSub = nodeAttributeValues.size();
            }
			int response = JOptionPane.YES_OPTION;
			if (numberOfSub > SUBNETWORK_COUNT_WARNING) {
				// TODO: add dialog to continue
				response = JOptionPane.showConfirmDialog((java.awt.Window) taskMonitor,
                        "Building " + nodeAttributeValues.size()
                        + " subnetworks may take a while. Are you sure you want to proceed?",
                        "Warning", JOptionPane.YES_NO_OPTION);
			}
			if (JOptionPane.YES_OPTION == response) {

				populateNodes(attributeName);

				if (PartitionNetworkVisualStyleFactory.attributeName != null) {
					// if a non-null attribute has been selection for node
					// coloring
					// and if not running Floorplan Only
					//AP: buggy
					Mosaic.createVisualStyle(Cytoscape.getCurrentNetworkView());
				}

				
				CyNetwork net = Cytoscape.getCurrentNetwork();
                rootNetwork = net;
				//CyNetworkView view = Cytoscape.getNetworkView(net.getIdentifier());

				int nbrProcesses = attributeValues.size();
				int count = 0;

                if(MosaicUtil.isValidGOTerm(nodeAttributeValues)) {
                    //buildNetworkTreeMap(attributeValues);
                    for (int i=0;i<networkTreeArray.length;i++) {
                        if(this.GO_LEVEL>=new Integer(networkTreeArray[i][3].toString()).intValue()) {
                            count++;
                            taskMonitor.setPercentCompleted((100 * count)
                                    / nbrProcesses);
                            taskMonitor.setStatus("building subnetwork for " + networkTreeArray[i][0]);
                            buildSubNetwork(net, i);
                        }
                    }
                } else {
                    for (Object val : attributeValues) {
                        count++;
                        taskMonitor.setPercentCompleted((100 * count)
                                / nbrProcesses);
                        taskMonitor.setStatus("building subnetwork for " + val);
                        buildSubNetwork(net, val.toString());
                    }
                }
                //2011-09-14
                //Temporarily comment those unimplemented functions
				System.out.println("*******************Build sub network overview***********************");
                buildSubnetworkOverview(net);
                Mosaic.wsPanel.focusNetworkNode(net.getIdentifier());
				tileNetworkViews(); // tile and fit content in each view
                System.out.println("*******************End***********************");
			}
		}
	}

    public void buildNetworkTreeMap(Set<Object> attributeValues) {
        Object[] goTermList = attributeValues.toArray();
        Map<String, String> readMappingFile = MosaicUtil.readGOMappingFile(this.getClass().getResource(MosaicStaticValues.BP_GO_PathFile), attributeValues);
        goDescMappingFile = MosaicUtil.readMappingFile(this.getClass().getResource(MosaicStaticValues.GO_DescFile), attributeValues, 0);
        Map<String, String> pathTermMap = new HashMap<String, String>();
        ArrayList<String> pathList = new ArrayList(readMappingFile.keySet());
        for(String gopath:pathList) {
            int goPathLevel = gopath.trim().length()-gopath.replace(".", "").trim().length();
            if(pathTermMap.containsKey(readMappingFile.get(gopath))){
                String currentPath = pathTermMap.get(readMappingFile.get(gopath));
                int currentGoPathLevel = currentPath.trim().length()-currentPath.replace(".", "").trim().length();
                if(goPathLevel<currentGoPathLevel)
                    pathTermMap.put(readMappingFile.get(gopath), gopath);
            } else {
                pathTermMap.put(readMappingFile.get(gopath), gopath);
            }
        }
        Map<String, String> termTreeMap = new HashMap<String, String>();
        for(int i=0;i<goTermList.length;i++) {
            if(pathTermMap.containsKey(goTermList[i].toString())) {
                String queryPath = pathTermMap.get(goTermList[i].toString());
                int queryPathLevel = queryPath.trim().length()-queryPath.replace(".", "").trim().length();
                for(int j=0;j<goTermList.length;j++) {
                    if(j!=i) {
                        if(pathTermMap.containsKey(goTermList[j].toString())) {
                            String targetPath = pathTermMap.get(goTermList[j].toString());
                            int targetPathLevel = targetPath.trim().length()-targetPath.replace(".", "").trim().length();
                            if(queryPathLevel>targetPathLevel){
                                if(queryPath.indexOf(targetPath)==0) {
                                //if(targetPath.equals(queryPath.substring(0, targetPath.length()))) {
                                    if(termTreeMap.containsKey(goTermList[i].toString())) {
                                        String currentPath = pathTermMap.get(termTreeMap.get(goTermList[i].toString()));
                                        int currentGoPathLevel = currentPath.trim().length()-currentPath.replace(".", "").trim().length();
                                        if(targetPathLevel>currentGoPathLevel)
                                            termTreeMap.put(goTermList[i].toString(), goTermList[j].toString());
                                    } else {
                                        termTreeMap.put(goTermList[i].toString(), goTermList[j].toString());
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println(goTermList[i].toString()+" doesn't has GO path!");
            }
            if(!termTreeMap.containsKey(goTermList[i].toString())) {
                termTreeMap.put(goTermList[i].toString(), "root");
            }
        }
        networkTreeArray = new String[attributeValues.size()][6];
        for(int i=0;i<goTermList.length;i++) {
            networkTreeArray[i][0] = goTermList[i];
            networkTreeArray[i][1] = termTreeMap.get(goTermList[i].toString());
            if(pathTermMap.containsKey(goTermList[i].toString())) {
                networkTreeArray[i][2] = pathTermMap.get(goTermList[i].toString());
                networkTreeArray[i][3] = (networkTreeArray[i][2].toString().trim().length()- networkTreeArray[i][2].toString().replace(".", "").trim().length()+1)+"";
            } else {
                networkTreeArray[i][2] = "";
                networkTreeArray[i][3] = "100";
            }
            if(goDescMappingFile.containsKey(goTermList[i].toString())) {
                networkTreeArray[i][4] = goDescMappingFile.get(goTermList[i].toString());
            } else {
                networkTreeArray[i][4] = goTermList[i].toString();
            }
            networkTreeArray[i][5] = "";
        }
        networkTreeArray = MosaicUtil.dataSort(networkTreeArray, 3);
    }

    public void partitionSubnetwork(String selectedNetwork, ArrayList<String> childNetworkList) {
        for(String networkName : childNetworkList){
            int index = getIndex(networkName);
            if(index!=-1)
                buildSubNetwork(rootNetwork, index);
        }        
    }

    private int getIndex(String desc) {
        for(int i=0;i<networkTreeArray.length;i++) {
            if(networkTreeArray[i][4].equals(desc)) {
                return i;
            }
        }
        return -1;
        
    }
    public void destroyAllSubNet(String selectedNetwork, String childNetwork) {
        //System.out.println("Deleting "+selectedNetwork);
        ArrayList<String> childList = getChildList(childNetwork);
        if(childList.size()>0){
            for(String networkID : childList) {
                destroyAllSubNet(selectedNetwork, networkID);
            }
        }
        if(!selectedNetwork.equals(childNetwork)) {
            CyNetwork currentNetwork = getNetworkByTitle(getDesc(childNetwork));
            if(currentNetwork != null) {
                Cytoscape.destroyNetwork(currentNetwork);
                resetNetworkID(childNetwork);
            }
        }
    }

    public void updateOverview(){
        Cytoscape.destroyNetwork(getNetworkByTitle("Overview"));
        CyNetwork net = Cytoscape.getCurrentNetwork();
        buildSubnetworkOverview(rootNetwork);
        tileNetworkViews();
    }

    public void resetNetworkID(Object GOterm) {
        for(int i=0;i<networkTreeArray.length;i++) {
            if(networkTreeArray[i][0].equals(GOterm))
                networkTreeArray[i][5]="";
        }
    }

    private CyNetwork getNetworkByTitle(String networkTitle) {
        for(CyNetwork c:Cytoscape.getNetworkSet()) {
            if(c.getTitle().equals(networkTitle))
                return c;
        }
        return null;
    }

    private ArrayList<String> getChildList(String parentNetwork) {
        ArrayList<String> result = new ArrayList();
        for(int i=0;i<networkTreeArray.length;i++) {
            if(networkTreeArray[i][1].equals(parentNetwork)) {
                result.add(networkTreeArray[i][0].toString());
            }
        }
        return result;
    }

    public ArrayList<String> getUnbuildChildList(String parentNetwork) {
        ArrayList<String> result = new ArrayList();
        for(int i=0;i<networkTreeArray.length;i++) {
            if(networkTreeArray[i][1].equals(parentNetwork)) {
                if(networkTreeArray[i][5].equals(""))
                    result.add(networkTreeArray[i][4].toString());
            }
        }
        return result;
    }

    public ArrayList<String> getExistChildList(String parentNetwork) {
        ArrayList<String> result = new ArrayList();
        for(int i=0;i<networkTreeArray.length;i++) {
            if(networkTreeArray[i][1].equals(parentNetwork)) {
                if(!networkTreeArray[i][5].equals(""))
                    result.add(networkTreeArray[i][4].toString());
            }
        }
        return result;
    }

    private String getNetworkID(String GOterm) {
        for(int i=0;i<networkTreeArray.length;i++) {
            if(networkTreeArray[i][0].equals(GOterm)) {
                if(!networkTreeArray[i][5].equals(""))
                    return networkTreeArray[i][5].toString();
                else
                    return "";
            }
        }
        return "";
    }

    public String getDesc(String GOterm) {
        for(int i=0;i<networkTreeArray.length;i++) {
            if(networkTreeArray[i][0].equals(GOterm)) {
                return networkTreeArray[i][4].toString();
            }
        }
        return "";
    }

    public String getGOTerm(String desc) {
        if(MosaicUtil.isValidGOTerm(nodeAttributeValues)) {
            for(int i=0;i<networkTreeArray.length;i++) {
                if(networkTreeArray[i][4].equals(desc)) {
                    return networkTreeArray[i][0].toString();
                }
            }
            return "";
        } else {
            return "";
        }
    }

    private boolean existNetwork(Object networkID) {
        if(MosaicUtil.isValidGOTerm(nodeAttributeValues)) {
            for(int i=0;i<networkTreeArray.length;i++) {
                if(networkTreeArray[i][0].equals(networkID)) {
                    if(!networkTreeArray[i][5].equals("")) {
                        if (Cytoscape.viewExists(networkTreeArray[i][5].toString()))
                            return true;
                        else
                            return false;
                    }  else
                        return false;
                }
            }
            return false;
        } else {
            return true;
        }
    }

	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Possible solution for large label drawing bug on tile view
		if (evt.getPropertyName().equals(JInternalFrame.IS_MAXIMUM_PROPERTY)) {
			CyNetworkView cnv = Cytoscape.getCurrentNetworkView();
			cnv.fitContent();
			cnv.setZoom(cnv.getZoom() * 0.9);
		}
	}

}
