package com.azureus.plugins.aznetmon.main;

import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
//import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.azureus.plugins.aznetmon.util.CdnMonitorTimeUtils;

/**
 * Created on Jan 17, 2008
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class RSTUnloadableView implements UISWTViewEventListener
{
    boolean isCreated = false;

    private Display rDisplay;
    private Composite pluginComposite;

    DataListener dataListener;

    Label statusString;
    Label statusLabel;

    BooleanParameter shareData;

    Label blank; //placeholder for button.
    Table table;

    Group azWikiGroup;

    PluginInterface pluginInterface;


    
    public RSTUnloadableView(PluginInterface _pi){
        pluginInterface=_pi;

    }



    public boolean eventOccurred(UISWTViewEvent event) {

        switch( event.getType() ){

            case UISWTViewEvent.TYPE_CREATE:
                if(isCreated){
                    return false;
                }
                isCreated = true;
                break;

            case UISWTViewEvent.TYPE_INITIALIZE:
                initialize( (Composite) event.getData() );
                break;

            case UISWTViewEvent.TYPE_CLOSE:
            case UISWTViewEvent.TYPE_DESTROY:
                delete();
                break;
        }
        return true;
    }

    private void delete(){

        RSTPacketStats stats = RSTPacketStats.getInstance();
        stats.removeListener(dataListener);

        blank=null;
        statusString=null;
        statusLabel=null;
        shareData=null;
        table=null;
        isCreated = false;
    }

    private void initialize(Composite parent){

        //Have a Group to visit the wiki page at the top.
        /////////////////////////////////////////
        //Add group to link to Azureus Wiki page.
        /////////////////////////////////////////
        createAzWikiGroup(parent);

        rDisplay = parent.getDisplay();
        
        pluginComposite = new Composite(parent, SWT.NULL );
        GridData gd = new GridData(GridData.FILL_BOTH);
        pluginComposite.setLayoutData(gd);

        GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		this.pluginComposite.setLayout(layout);

        //This is where the enable upload button goes.
        createShareDataCheckBox();

        //Need a seperator bar. //ToDo: add bar.

        //First element
        createStatusLabel();

        //ToDo: Delete this item - no longer needed.
        //3rd element : PUSH style is the default style for normal buttons
        createRefreshButton();

        //new row (as it's 3 elements per row)
		//4th Element : the table
        createTable();

        RSTPacketStats stats = RSTPacketStats.getInstance();
        dataListener = new DataListener(){
            public void update()
            {
                rDisplay.asyncExec( new Runnable(){
                    public void run(){
                        refreshView();
                    }//run
                });
            }//update
        };

        stats.addListener( dataListener );

        //Do the first update.
        refreshView();
    }

    private void createRefreshButton() {

        blank = new Label(this.pluginComposite, SWT.NULL );
        blank.setText(" ");

    }

    private void createStatusLabel() {
        statusLabel = new Label(pluginComposite, SWT.NULL);
        statusLabel.setText(" Status :");

        //2nd element (class member in order to be changed later)
        statusString = new Label(pluginComposite,SWT.NULL);
        statusString.setText("Help monitor which ISPs are sending RST packets.");

        //We'll use a GridData in order to make this Label take the maximum
        //space horizontaly
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        statusString.setLayoutData(gridData);
    }

    private void createShareDataCheckBox() {
        GridData gData;
        shareData = new BooleanParameter(pluginComposite,"aznetmon.share","Share ISP Monitor Results");
        shareData.setSelected(true);
        Button dontAskWhy = (Button)shareData.getControl();
        dontAskWhy.setText("Share ISP Monitor Results");

        gData = new GridData();
        gData.horizontalSpan = 4;
        gData.horizontalIndent = 10;
        shareData.setLayoutData(gData);
    }

    private void createTable() {
        GridData gridData;
        table = new Table(pluginComposite, SWT.SINGLE | SWT.BORDER);
        //Headers visible
        table.setHeaderVisible(true);

        //This table columns :
        String[] columnNames = {"Time", "% RST" ,"# RST","# RST total", "%inbound conn", "current open",
                                "# Active Open", "# Passive Open", "# Failed"};
        //and their width
        int[] columnWidths = {125,75,75,75,100,100,100,100,75};

        //Initialise this table
        for(int i = 0 ; i < columnNames.length ; i++) {
            TableColumn column = new TableColumn(table,SWT.NULL);
            column.setText(columnNames[i]);
            column.setWidth(columnWidths[i]);
        }

        //Add a GridData in order to make it grab all the view space
        //And use the 3 columns
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        table.setLayoutData(gridData);
    }

    private void createAzWikiGroup(Composite parent) {
        azWikiGroup = new Group(parent, SWT.WRAP);
        GridData wikiGridData;
        wikiGridData = new GridData();
        wikiGridData.widthHint = 350;
        azWikiGroup.setLayoutData(wikiGridData);
        GridLayout wikiLayout = new GridLayout();
        wikiLayout.numColumns = 1;
        wikiLayout.marginHeight = 1;
        azWikiGroup.setLayout(wikiLayout);

        azWikiGroup.setText("Please visit");

        final Label linkLabel = new Label(azWikiGroup, SWT.NULL);
        linkLabel.setText( "ISP Network Monitor" );
        linkLabel.setData("http://azureus.aelitis.com/wiki/index.php/ISP_Network_Monitor");
//        linkLabel.setCursor(Cursors.handCursor);
        linkLabel.setForeground(Colors.blue);
        wikiGridData = new GridData();
        wikiGridData.horizontalIndent = 10;
        linkLabel.setLayoutData( wikiGridData );
        linkLabel.addMouseListener(new MouseAdapter() {
          public void mouseDoubleClick(MouseEvent arg0) {
              Utils.launch((String) (arg0.widget).getData());
          }
          public void mouseUp(MouseEvent arg0) {
              Utils.launch((String) (arg0.widget).getData());
          }
        });
    }


    private void refreshView() {

        //Not needed here by a good habit.
        Thread t = new Thread() {
            public void run(){

                try{
                    RSTPacketStats stats = RSTPacketStats.getInstance();
                    final String status = stats.getStatus();
                    if(statusString!=null){

                        rDisplay.asyncExec( new Runnable(){
                            public void run(){
                                statusString.setText(status);
                            }
                        } );

                    }//if
                    //Refresh the table.
                    RSTPacketStats.RSTPacketData[] rData = stats.getMostRecent(100);

                    int len = rData.length;
                    for(int i=0;i<len;i++){
                        addTableElement( rData[i] );
                    }//for

                }catch(Throwable thr){
                    RSTPacketStats stats = RSTPacketStats.getInstance();
                    stats.setStatus( thr.toString() );
                }finally{

                }

            }//run
        };

        table.removeAll();
        t.start();
    }//refreshView

    private void addTableElement( final RSTPacketStats.RSTPacketData rData ){

        //make sure display isn't null or disposed.
        if(rDisplay == null || rDisplay.isDisposed())
        return;

        //send it to table.
        rDisplay.asyncExec( new Runnable() {
            public void run() {
                //check again that table hasn't disposed.
                if(table == null || table.isDisposed()){
                    return;
                }

                TableItem item = new TableItem(table,SWT.NULL);

                //We use a Calendar to format the Date
                String dataTime = CdnMonitorTimeUtils.getReadableTime(rData.timestamp);
                item.setText(0, dataTime);

                DecimalFormat formatter = new DecimalFormat("#0.0 %");

                item.setText(1,formatter.format( rData.percentRSTConn ));
                item.setText(2,rData.deltaConnReset+"");
                item.setText(3,rData.nConnReset +"");

                item.setText(4,formatter.format(rData.percentIncomingConn));
                item.setText(5,rData.nCurrentOpen+"");

                item.setText(6,rData.deltaActiveOpens+"");//#active open
                item.setText(7,rData.deltaPassiveOpens+"");//#passive open
                item.setText(8,rData.deltaFailedConnAttempt+"");//#failed open

            }
        });

    }//addTableElement

}
