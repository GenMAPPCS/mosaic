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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class LegendPanel extends JPanel {
    private JPanel drawingPane;
    private boolean drawTag = false;
    private Map<String, Color> legendColorMap;
        
    public LegendPanel() {
        super(new BorderLayout());
        drawTag = false;
        //Set up the drawing area.
        drawingPane = new DrawingPane();
        //Put the drawing area in a scroll pane.
        JScrollPane scroller = new JScrollPane(drawingPane);
        scroller.setPreferredSize(new Dimension(200,200));
        //Lay out this demo.
        add(scroller, BorderLayout.CENTER);
    }

    public void initialize(Map<String, Color> legendMap) {
        System.out.println("BBB");
        legendColorMap = legendMap;
        drawTag = true;
        drawingPane.repaint();
        drawingPane.revalidate();
    }

    /** The component inside the scroll pane. */
    public class DrawingPane extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int startX = 5;
            int startY = 5;
            int longest = 0;
            if(drawTag) {
                List keyList = new ArrayList(legendColorMap.keySet());
                Collections.sort(keyList);
                for(int i=0;i<keyList.size();i++) {
                    Object o = keyList.get(i);
                    if(o.toString().length()>longest)
                        longest = o.toString().length();
                    Color rectColor = legendColorMap.get(o);
                    g.setColor(rectColor);
                    g.fillRect(startX, startY+i*12, 20, 8);
                    g.setColor(Color.BLACK);
                    g.drawString(o.toString(), startX+30, startY+7+i*12);
                    //g.fillRect(startX, startY+i*12, 20, 8);
                }
                if(keyList.size()>0)
                    setPreferredSize(new Dimension(startX+30+longest,startY+7+keyList.size()*12));
                else
                    setPreferredSize(new Dimension(100,100));
            }
        }
    }
}
