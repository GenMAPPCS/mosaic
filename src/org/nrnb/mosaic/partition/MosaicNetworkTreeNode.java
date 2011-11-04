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
package org.nrnb.mosaic.partition;

import cytoscape.view.NetworkTreeNode;

public class MosaicNetworkTreeNode extends NetworkTreeNode {
	private String network_uid;

	public MosaicNetworkTreeNode(Object userobj, String id) {
		super(userobj.toString(), id);
		network_uid = id;
	}

    @Override
	protected void setNetworkID(String id) {
		network_uid = id;
	}

    @Override
	protected String getNetworkID() {
		return network_uid;
	}
}