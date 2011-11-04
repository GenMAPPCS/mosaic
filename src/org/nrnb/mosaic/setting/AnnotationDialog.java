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

import org.nrnb.mosaic.utils.IdMapping;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

public class AnnotationDialog implements Task {
    private  String localDerbyDBPath;
    private  String localGOslimDBPath;
    private  String selectedMappingID;
    private  String selectedMappingType;
    private  String ensemblIDType;
    private TaskMonitor taskMonitor;
    private boolean success = false;
        
    public AnnotationDialog(String localDerbyDB, String localGOslimDB,
        String mappingID, String mappingType, String ensemblType) {
        localDerbyDBPath = localDerbyDB;
        localGOslimDBPath = localGOslimDB;
        selectedMappingID = mappingID;
        selectedMappingType = mappingType;
        ensemblIDType = ensemblType;
//        if(ensemblType.toString().toLowerCase().indexOf("ensembl")!=-1) {
//            ensemblIDType = "Ensembl";
//        } else
//            ensemblIDType = ensemblType;
    }

    public void run() {
        try {
            taskMonitor.setStatus("Annotating current network ......");
            taskMonitor.setPercentCompleted(-1);
            long start=System.currentTimeMillis();
            System.out.println("--------------"+this.selectedMappingType+"-----------------");
            //System.out.println(localGOslimDBPath);
            IdMapping idMapper = new IdMapping();
            idMapper.mapID(this.localDerbyDBPath, this.localGOslimDBPath,
                    this.selectedMappingID, this.selectedMappingType, this.ensemblIDType);
//            if(this.selectedMappingType.indexOf("ensembl")==-1) {
//                System.out.println("--------------Mapping Other ID-----------------");
//                IdMapping.mapID(this.localDerbyDBPath, this.selectedMappingID, this.ensemblIDType);
//                IdMapping.mapAnnotation(this.localGOslimDBPath, this.ensemblIDType);
//            } else {
//                System.out.println("--------------Mapping Ensembl-----------------");
//                IdMapping.mapAnnotation(this.localGOslimDBPath, this.selectedMappingID);
//            }
            long pause=System.currentTimeMillis();
            System.out.println("Running time:"+(pause-start)/1000/60+"min "+(pause-start)/1000%60+"sec");
            taskMonitor.setStatus("Done");
            taskMonitor.setPercentCompleted(100);
            success = true;
        } catch (Exception e) {
            taskMonitor.setPercentCompleted(100);
            taskMonitor.setStatus("failed.\n");
            e.printStackTrace();
        }
    }

    public boolean success() {
        return success;
    }

    public void halt() {
    }

    public void setTaskMonitor(TaskMonitor taskMonitor){
        this.taskMonitor = taskMonitor;
    }

    public String getTitle() {
        return new String("Annotating current network");
    }
}