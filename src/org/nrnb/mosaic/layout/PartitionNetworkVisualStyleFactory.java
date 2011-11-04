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
package org.nrnb.mosaic.layout;

import org.nrnb.mosaic.layout.CellAlgorithm;
import org.nrnb.mosaic.utils.MosaicStaticValues;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.CyAttributesUtils;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.view.CyNetworkView;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.EdgeAppearanceCalculator;
import cytoscape.visual.GlobalAppearanceCalculator;
import cytoscape.visual.LabelPosition;
import cytoscape.visual.LineStyle;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualPropertyDependency;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.LinearNumberToNumberInterpolator;
import cytoscape.visual.mappings.ObjectMapping;
import cytoscape.visual.mappings.PassThroughMapping;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import org.nrnb.mosaic.Mosaic;
import org.nrnb.mosaic.color.MFEdgeAppearanceCalculator;
import org.nrnb.mosaic.color.MFNodeAppearanceCalculator;
import org.nrnb.mosaic.partition.PartitionAlgorithm;
import org.nrnb.mosaic.utils.MosaicUtil;


public class PartitionNetworkVisualStyleFactory {

	/**
	 * 
	 */
	public static String attributeName = MosaicStaticValues.MF_ATTNAME;
    private static LabelPosition lp = new LabelPosition();
    private static Calculator colorCalculator;
	public static DiscreteMapping disMappingLabelPosition = new DiscreteMapping(
			lp, "region_name", ObjectMapping.NODE_MAPPING);

	public static VisualStyle createVisualStyle(CyNetworkView view) {

		VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
		CalculatorCatalog catalog = vmm.getCalculatorCatalog();
		VisualStyle mfStyle = catalog.getVisualStyle(attributeName);
		if (mfStyle == null) { // Create the MF visual style
			try {
				mfStyle = (VisualStyle) vmm.getVisualStyle().clone();
			} catch (CloneNotSupportedException e) {
				mfStyle = new VisualStyle(attributeName);
			}
			mfStyle.setName(attributeName);
			NodeAppearanceCalculator nac = new MFNodeAppearanceCalculator();
			EdgeAppearanceCalculator eac = new MFEdgeAppearanceCalculator();
			mfStyle.getDependency().set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED, false);
			nac.getDefaultAppearance().set(VisualPropertyType.NODE_HEIGHT,
					MFNodeAppearanceCalculator.FEATURE_NODE_HEIGHT);
			nac.getDefaultAppearance().set(VisualPropertyType.NODE_WIDTH,
					MFNodeAppearanceCalculator.FEATURE_NODE_WIDTH);
			nac.getDefaultAppearance().set(VisualPropertyType.NODE_SHAPE,
					NodeShape.RECT);
			nac.getDefaultAppearance().set(
					VisualPropertyType.NODE_BORDER_COLOR, new Color(0, 0, 0));
			nac.getDefaultAppearance().set(VisualPropertyType.NODE_FILL_COLOR,
					new Color(255, 255, 255));

            CyAttributes attribs = Cytoscape.getNodeAttributes();
            Map attrMap = CyAttributesUtils
                    .getAttribute(attributeName, attribs);
            Collection values = attrMap.values();
            /*
             * key will be a List attribute value, so we need to pull out
             * individual list items
             */
            List uniqueValueList = MosaicUtil.setupNodeAttributeValues(attributeName);
            
            // NODE MAPPINGS
            PassThroughMapping passMappingLabel = new PassThroughMapping("",
                    "canonicalName");
            Calculator labelCalculator = new BasicCalculator(
                    attributeName, passMappingLabel,
                    VisualPropertyType.NODE_LABEL);
            nac.setCalculator(labelCalculator);

            DiscreteMapping disMappingBorderColor = new DiscreteMapping(
                    Color.black, CellAlgorithm.NODE_COPIED,
                    ObjectMapping.NODE_MAPPING);
            disMappingBorderColor.putMapValue(Boolean.TRUE, Color.red);
            Calculator borderColorCalculator = new BasicCalculator(
                    attributeName, disMappingBorderColor,
                    VisualPropertyType.NODE_BORDER_COLOR);
            nac.setCalculator(borderColorCalculator);


            ContinuousMapping contMappingNodeWidth = new ContinuousMapping(PartitionAlgorithm.NETWORK_LIMIT_MIN, ObjectMapping.NODE_MAPPING);
            contMappingNodeWidth.setControllingAttributeName(
                    PartitionAlgorithm.SUBNETWORK_SIZE, view.getNetwork(),
                    false);
            contMappingNodeWidth.setInterpolator(new LinearNumberToNumberInterpolator());
            contMappingNodeWidth.addPoint(PartitionAlgorithm.NETWORK_LIMIT_MIN, new BoundaryRangeValues(10, 20, 20));
            contMappingNodeWidth.addPoint(PartitionAlgorithm.NETWORK_LIMIT_MAX, new BoundaryRangeValues(80, 80, 100));
            Calculator nodeWidthCalculator = new BasicCalculator(attributeName, contMappingNodeWidth, VisualPropertyType.NODE_WIDTH);
            nac.setCalculator(nodeWidthCalculator);

            ContinuousMapping contMappingNodeHeight = new ContinuousMapping(PartitionAlgorithm.NETWORK_LIMIT_MIN, ObjectMapping.NODE_MAPPING);
            contMappingNodeHeight.setControllingAttributeName(
                    PartitionAlgorithm.SUBNETWORK_SIZE, view.getNetwork(),
                    false);
            contMappingNodeHeight.setInterpolator(new LinearNumberToNumberInterpolator());
            contMappingNodeHeight.addPoint(PartitionAlgorithm.NETWORK_LIMIT_MIN, new BoundaryRangeValues(10, 20, 20));
            contMappingNodeHeight.addPoint(PartitionAlgorithm.NETWORK_LIMIT_MAX, new BoundaryRangeValues(80, 80, 100));
            Calculator nodeHeightCalculator = new BasicCalculator(attributeName, contMappingNodeHeight, VisualPropertyType.NODE_HEIGHT);
            nac.setCalculator(nodeHeightCalculator);

            DiscreteMapping disMappingNodeFill = new DiscreteMapping(
                    Color.white, ObjectMapping.NODE_MAPPING);
            disMappingNodeFill.setControllingAttributeName(attributeName, view
                    .getNetwork(), false);

            /*
             * Create random colors
             */
            final float increment = 1f / ((Number) uniqueValueList.size())
                    .floatValue();
            float hue = 0;
            float sat = 0;
            float br = 0;
            int i = 0;
            for (Object key : uniqueValueList) {
                hue = hue + increment;
                sat = (Math.abs(((Number) Math.cos((8 * i) / (2 * Math.PI)))
                        .floatValue()) * 0.7f) + 0.3f;
                br = (Math.abs(((Number) Math.sin(((i) / (2 * Math.PI))
                        + (Math.PI / 2))).floatValue()) * 0.7f) + 0.3f;
                disMappingNodeFill.putMapValue(key, new Color(Color.HSBtoRGB(
                        hue, sat, br)));
                i++;
            }
            colorCalculator = new BasicCalculator(
                    attributeName, disMappingNodeFill,
                    VisualPropertyType.NODE_FILL_COLOR);

            nac.setCalculator(colorCalculator);

            mfStyle.setNodeAppearanceCalculator(nac);

			// EDGE MAPPINGS
			DiscreteMapping disMappingEdgeColor = new DiscreteMapping(
					LineStyle.SOLID, CellAlgorithm.UNASSIGNED_EDGE_ATT,
					ObjectMapping.EDGE_MAPPING);
			disMappingEdgeColor.putMapValue(Boolean.TRUE, LineStyle.LONG_DASH);
			disMappingEdgeColor.putMapValue(Boolean.FALSE, LineStyle.SOLID);
			Calculator edgeColorCalculator = new BasicCalculator(
					attributeName, disMappingEdgeColor,
					VisualPropertyType.EDGE_LINE_STYLE);
			eac.setCalculator(edgeColorCalculator);

			DiscreteMapping disMappingEdgeLineStyle = new DiscreteMapping(
					Color.blue, CellAlgorithm.UNASSIGNED_EDGE_ATT,
					ObjectMapping.EDGE_MAPPING);
			disMappingEdgeLineStyle.putMapValue(Boolean.TRUE, Color.darkGray);
			disMappingEdgeLineStyle.putMapValue(Boolean.FALSE, Color.blue);
			Calculator edgeLineStyleCalculator = new BasicCalculator(
					attributeName, disMappingEdgeLineStyle,
					VisualPropertyType.EDGE_COLOR);
			eac.setCalculator(edgeLineStyleCalculator);
			
			ContinuousMapping contMappingLineWidth = new ContinuousMapping(1, ObjectMapping.EDGE_MAPPING);
			contMappingLineWidth.setControllingAttributeName(
					PartitionAlgorithm.SUBNETWORK_CONNECTIONS, view.getNetwork(),
					false);
			contMappingLineWidth.setInterpolator(new LinearNumberToNumberInterpolator());
			contMappingLineWidth.addPoint(PartitionAlgorithm.NETWORK_LIMIT_MIN, new BoundaryRangeValues(1, 2, 2));
            contMappingLineWidth.addPoint(PartitionAlgorithm.NETWORK_LIMIT_MAX, new BoundaryRangeValues(40, 40, 60));
			//contMappingLineWidth.addPoint(PartitionAlgorithm.NETWORK_LIMIT_MAX, new BoundaryRangeValues(70, 70, 90));
			Calculator lineWidthCalculator = new BasicCalculator(attributeName, contMappingLineWidth, VisualPropertyType.EDGE_LINE_WIDTH);
			eac.setCalculator(lineWidthCalculator);

			mfStyle.setEdgeAppearanceCalculator(eac);

			// GLOBAL MAPPINGS
			GlobalAppearanceCalculator gac = mfStyle
					.getGlobalAppearanceCalculator();
			gac.setDefaultBackgroundColor(Color.white);
			// set edge opacity
			VisualPropertyType type = VisualPropertyType.EDGE_OPACITY;
			type.setDefault(mfStyle, new Integer(150));
			// set node shape
			type = VisualPropertyType.NODE_SHAPE;
			type.setDefault(mfStyle, NodeShape.ELLIPSE);

			mfStyle.setGlobalAppearanceCalculator(gac);

			catalog.addVisualStyle(mfStyle);

            //Set the uniqueValueList to the pull down menu in NetworkPanel
            Mosaic.wsPanel.setFuntionValues(uniqueValueList, (DiscreteMapping)PartitionNetworkVisualStyleFactory.colorCalculator.getMappings().get(0));
		}
		CyNetworkView myView = Cytoscape.getCurrentNetworkView();
		vmm.setNetworkView(myView);
		vmm.setVisualStyle(mfStyle);
		myView.setVisualStyle(attributeName);

		Cytoscape.getVisualMappingManager().setNetworkView(myView);
		Cytoscape.getVisualMappingManager().applyAppearances();

		return mfStyle;
	}

    public static void highlightNodes(String attribute) {
        CyNetworkView view = Cytoscape.getCurrentNetworkView();
        VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
		CalculatorCatalog catalog = vmm.getCalculatorCatalog();
		VisualStyle mfStyle = catalog.getVisualStyle(attributeName);
        NodeAppearanceCalculator nodeAppCalculator = mfStyle.getNodeAppearanceCalculator();

        if(attribute.equals("Show all")) {
            nodeAppCalculator.setCalculator(PartitionNetworkVisualStyleFactory.colorCalculator);
        } else {
            DiscreteMapping oldDisMappingNodeFill = (DiscreteMapping)PartitionNetworkVisualStyleFactory.colorCalculator.getMappings().get(0);
            DiscreteMapping disMappingNodeFill = new DiscreteMapping(
                        Color.white, ObjectMapping.NODE_MAPPING);
            disMappingNodeFill.setControllingAttributeName(attributeName, view
                        .getNetwork(), false);
            disMappingNodeFill.putMapValue(attribute, oldDisMappingNodeFill.getMapValue(attribute));
            disMappingNodeFill.putMapValue("none", Color.WHITE);
            Calculator colorCalculator = new BasicCalculator(
                    attributeName, disMappingNodeFill,
                    VisualPropertyType.NODE_FILL_COLOR);
            nodeAppCalculator.setCalculator(colorCalculator);
        }

        mfStyle.setNodeAppearanceCalculator(nodeAppCalculator);
        vmm.setNetworkView(view);
		vmm.setVisualStyle(mfStyle);
		view.setVisualStyle(attributeName);

		Cytoscape.getVisualMappingManager().setNetworkView(view);
		Cytoscape.getVisualMappingManager().applyAppearances();
        CyLayoutAlgorithm layout = CyLayouts.getLayout("force-directed");
        layout.doLayout(Cytoscape.getCurrentNetworkView());
    }
}
