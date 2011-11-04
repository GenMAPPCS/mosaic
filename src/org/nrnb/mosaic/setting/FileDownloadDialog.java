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

import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import java.util.List;
import org.nrnb.mosaic.utils.download.Downloader;

/**
 *
 * @author Chao
 */
public class FileDownloadDialog implements Task {
    public  List<String> downloadFileList;
    private TaskMonitor taskMonitor;
    private boolean success = false;
    
    public FileDownloadDialog(List<String> downloadDBList) {
        downloadFileList = downloadDBList;
    }

    public void run() {
        try {
            taskMonitor.setStatus("Downloading databases ...");
            taskMonitor.setPercentCompleted(-1);
            for(String fileName:this.downloadFileList) {
                Downloader d = new Downloader();
                d.download(fileName);
                int progress = d.getProgress();
                while (progress < 99) {
                    taskMonitor.setStatus(fileName + ": " + progress + "% ...");
                    //System.out.println(fileName + ": " + progress + "%");
                    Thread.sleep(250);
                    progress = d.getProgress();
                }
                // must wait for completion of uncompression before giving up thread
                d.waitFor();
            }
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
        return new String("Downloading databases");
    }
}