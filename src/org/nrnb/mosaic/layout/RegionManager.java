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

import giny.model.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import cytoscape.Cytoscape;
import ding.view.DGraphView;
import ding.view.DingCanvas;

/**
 * This class maintains a hashmap of regions by attribute value, plus methods
 * that operate over all regions.
 * 
 */
public class RegionManager {

	private static HashMap<String, Region> regionAttMap = new HashMap<String, Region>();
	private static TreeMap<Integer, Region> sortedRegionMap = new TreeMap<Integer, Region>();

	public static void addRegion(String attValue, Region region) {
		regionAttMap.put(attValue, region);
		checkForOverlap(region);
		DGraphView dview = (DGraphView) Cytoscape.getCurrentNetworkView();
		DingCanvas backgroundLayer = dview.getCanvas(DGraphView.Canvas.BACKGROUND_CANVAS);
		backgroundLayer.add(region, 0);
	}

	public static Region getRegionByAtt(String attValue) {
		return regionAttMap.get(attValue);
	}

	public static Collection<Region> getAllRegions() {
		return regionAttMap.values();
	}

	public static void clearAll() {
		regionAttMap.clear();
		sortedRegionMap.clear();
		DGraphView dview = (DGraphView) Cytoscape.getCurrentNetworkView();
		DingCanvas bCanvas = dview
				.getCanvas(DGraphView.Canvas.BACKGROUND_CANVAS);
		bCanvas.removeAll();

		// clean up copied nodes and edges
		Iterator it = Cytoscape.getCurrentNetwork().nodesIterator();
		while (it.hasNext()) {
			Node node = (Node) it.next();
			if (node.getIdentifier().contains("__") && !node.getIdentifier().contains("__1")) {
				Cytoscape.getCurrentNetwork().removeNode(
						node.getRootGraphIndex(), true);
			}
		}

	}

	/**
	 * Checks each new region against existing regions for overlap. If
	 * overlapping, region is added to a sorted tree map.
	 * 
	 * Also hacks area values when found to be equivalent to an existing region.
	 * This helps make it sortable.
	 * 
	 * @param r
	 */
	private static void checkForOverlap(Region newRegion) {
		double newLeft = newRegion.getRegionLeft();
		double newRight = newRegion.getRegionRight();
		double newTop = newRegion.getRegionTop();
		double newBottom = newRegion.getRegionBottom();

		Region[] sra = getSortedRegionArray();
		for (int i = sra.length - 1; i >= 0; i--) { // from largest to smallest
			if (i == -1) // if first region
				break;
			Region r = sra[i];
			if (newRegion.getArea() == r.getArea()) {
				// if tie, then most elongated on top
				int eRatio = (int) ((r.getRegionWidth() / r.getRegionHeight()) / (newRegion
						.getRegionWidth() / newRegion.getRegionHeight()));
				if (eRatio <= 1) { // tie break goes to newRegion
					newRegion.setArea(newRegion.getArea() - 1);
				} else {
					r.setArea(newRegion.getArea() - 1);
				}
			}
			if ((newLeft > r.getRegionLeft() && newLeft < r.getRegionRight())
					|| (newRight > r.getRegionLeft() && newRight < r
							.getRegionRight()) || newLeft < r.getRegionLeft()
					&& newRight > r.getRegionRight()) {
				if ((newTop > r.getRegionTop() && newTop < r.getRegionBottom())
						|| (newBottom > r.getRegionTop() && newBottom < r
								.getRegionBottom())
						|| newTop < r.getRegionTop()
						&& newBottom > r.getRegionBottom()) {
					/*
					 * If we got this far, it means one region is overlapping
					 * the other. Now we want to flag the smaller one so we know
					 * when and where to apply "oil & water" exclusion.
					 */
					if (r.getArea() > newRegion.getArea()) {
						newRegion.setRegionsOverlapped(r);
						r.setOverlappingRegions(newRegion);
					} else {
						r.setRegionsOverlapped(newRegion);
						newRegion.setOverlappingRegions(r);
					}
				}
			}

		}
		sortedRegionMap.put(newRegion.getArea(), newRegion);
	}

	/**
	 * @return sra the sortedRegionArray
	 */
	public static Region[] getSortedRegionArray() {
		Region[] sra = new Region[sortedRegionMap.size()];
		int i = 0;
		for (Region r : sortedRegionMap.values()) {
			sra[i] = r;
			i++;
		}
		return sra;
	}
}
