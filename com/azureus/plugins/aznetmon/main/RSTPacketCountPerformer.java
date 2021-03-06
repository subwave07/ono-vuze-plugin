package com.azureus.plugins.aznetmon.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

/**
 * Created on Dec 17, 2007
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

public class RSTPacketCountPerformer implements UTTimerEventPerformer {

    PluginInterface pi=null;

    public RSTPacketCountPerformer(PluginInterface pluginInterface){

        pi = pluginInterface;

    }

    public void perform(UTTimerEvent utTimerEvent)
    {
        try{

            //ToDo: - This is for windows only.
            String[] cmd = new String[3];
            cmd[0] = "cmd.exe";
            cmd[1] = "/C";
            cmd[2] = "netstat -s";

            RuntimeExec re = new RuntimeExec(cmd);

            re.exec();

            try{Thread.sleep(1000);}catch(Exception e){}

            String output = re.getStdOut();

            if( output != null ){

                parseOutput(output);

            }//if

        }catch(Exception e){

            RSTPacketStats stats = RSTPacketStats.getInstance();
            stats.setStatus( e.toString() );

        }

    }//perform

    /**
     *
     * @param output - expects results of netstat -s for a windows machine.
     */
    void parseOutput(String output) {
        String[] lines = output.split("\n");
        if( lines.length > 0 ){

            RSTPacketStats.RSTPacketData pktData = parse(lines);
            if( isResultZero( pktData ) ){
                //Likely the default language of computer isn't English, try backup plan.
                pktData = parseByOrder(lines);
            }

            //Get the deltas.
            RSTPacketStats stats = RSTPacketStats.getInstance();
            RSTPacketStats.RSTPacketData recent = stats.getMostRecent();

            if( recent != null ){

                pktData.calculate( recent );

            }

            stats.add( pktData );

//            if(pi!=null){
//                addToVersionServerStore( stats );
//            }

        }//if
    }

    RSTPacketStats.RSTPacketData parse(String[] lines) {

        RSTPacketStats.RSTPacketData pktData = new RSTPacketStats.RSTPacketData();
        boolean foundTCP = false;
        int tcpCount = 0;
        for(int i=0;i<lines.length;i++){
        	
            if( lines[i].indexOf('=')> 0 && foundTCP){
            	
                String[] value = lines[i].split("=");
                //only collect stats for IPv4.
                if( value.length==2 ){
                    value[0] = value[0].trim();
                    value[1] = value[1].trim();

                    //look for some key values.
                    if( tcpCount==0) {// "active opens".equalsIgnoreCase(value[0]) ){
                        pktData.nActiveOpens += Integer.parseInt( value[1] );
                    }else if( tcpCount==1) {//( "passive opens".equalsIgnoreCase(value[0]) ){
                        pktData.nPassiveOpens += Integer.parseInt( value[1] );
                    }else if( tcpCount==2) {//( "failed connection attempts".equalsIgnoreCase(value[0]) ){
                        pktData.nFailedConnAttempt += Integer.parseInt( value[1] );
                    }else if( tcpCount==3) {//( "reset connections".equalsIgnoreCase(value[0]) ){
                        pktData.nConnReset += Integer.parseInt( value[1] );
                    }else if( tcpCount==4) {//( "current connections".equalsIgnoreCase(value[0]) ){
                        pktData.nCurrentOpen += Integer.parseInt( value[1] );
                    }
                    
                    tcpCount++;

                }//if

            }//if
            else if (lines[i].contains("TCP") && lines[i].contains("IPv4")){ // vista has ipv6 too
            	foundTCP = true;            	
            } else if (lines[i].trim().length()>0) foundTCP = false;

        }//for
        //if the results are zero include some debug output. This will be remove before release.
        //use the first 5 and last 5 lines in output.
        if( pktData.nConnReset==0 && pktData.nActiveOpens==0
                && pktData.nPassiveOpens==0 && pktData.nFailedConnAttempt==0)
        {
            pktData.isDebugMode=true;
            if( lines.length<10 ){
                pktData.debugLines = lines;
            }else{
                int n = lines.length;
                pktData.debugLines = new String[10];
                pktData.debugLines[0]=lines[0];
                pktData.debugLines[1]=lines[1];
                pktData.debugLines[2]=lines[2];
                pktData.debugLines[3]=lines[3];
                pktData.debugLines[4]=lines[4];
                pktData.debugLines[5]=lines[n-5];
                pktData.debugLines[6]=lines[n-4];
                pktData.debugLines[7]=lines[n-3];
                pktData.debugLines[8]=lines[n-2];
                pktData.debugLines[9]=lines[n-1];
            }
        }//if - send debug info

        return pktData;
    }

    /**
     * Some computers don't have English as the default language. This is a
     * back-up parsing method to get results from them. Use the order once the
     * string IPv4 is seen.
     *
     * @param lines -
     * @return  -
     */
    RSTPacketStats.RSTPacketData parseByOrder(String[] lines)
    {
        RSTPacketStats.RSTPacketData retVal = new RSTPacketStats.RSTPacketData();

        if(lines==null){
            return retVal;
        }

        int nLines = lines.length;
        for(int i=0; i<nLines; i++){
            String curr = lines[i];
            if( curr.indexOf("IPv4")>0 ){
                RSTPacketStats.RSTPacketData candidate = parseIPvDataSections(lines,i);
                //A result in debugMode is a signal this section failes.
                if( !candidate.isDebugMode ){
                    return candidate;
                }
            }
        }

        retVal.isDebugMode=true;
        //retVal.msg = "Failed to find a valid IPv4 section.";

        return retVal;
    }

    /**
     * Helper method of parseByOrder.  To be a valid section the data must look like the
     * following.
     *
     *   Active Opens                        = 6533
     *   Passive Opens                       = 271
     *   Failed Connection Attempts          = 3058
     *   Reset Connections                   = 159
     *   Current Connections                 = 47
     *   Segments Received                   = 758345
     *   Segments Sent                       = 988831
     *   Segments Retransmitted              = 42455
     *
     *   If must have 8 '=' signs in a row, and the results must obey
     *   certain conditions.
     *
     * @param lines -
     * @param index - start index
     * @return - non-zero
     */
    private RSTPacketStats.RSTPacketData parseIPvDataSections(String[] lines, int index)
    {
        RSTPacketStats.RSTPacketData candidate = new RSTPacketStats.RSTPacketData();
        candidate.isDebugMode=true; //assume this parse fails and unset on success.
        
        if(lines==null){
            candidate.msg = "null value";
            return candidate;
        }

        List results = new ArrayList();
        int nEqualsCount=0;
        StringBuffer sb = new StringBuffer();
        try{

            for(int i=index+1 ; i<index+12; i++ ){

                if( lines[i].indexOf("=")>0 ){
                    nEqualsCount++;
                    String[] parts = lines[i].split("=");
                    try{
                        results.add( new Integer( parts[1].trim() ) );
                    }catch(Exception e){
                        candidate.isDebugMode=true;
                        candidate.msg = e+" for "+lines[i];
                        return candidate;
                    }
                }//if
            }//for

            if( results.size()==8 ){

                int[] check = toIntArray(results);
                //items 0-4 must be less then item 5;
                if( check[0]>check[5] || check[1]>check[5] || check[2]>check[5]
                        || check[3]>check[5] || check[4]>check[5])
                {
                    candidate.msg ="failed sanity check 1";
                    return candidate;
                }

                //items 3 must be less then item 1 + item 2
                if( check[1]+check[2]<check[3] )
                {
                    candidate.msg="failed sanity check 2";
                    return candidate;
                }

                return makeGoodCandidate( results );

            }else{
                candidate.msg = "Expected 8 but found "+nEqualsCount+" in section.";
            }

        }catch(Throwable t){
            candidate.isDebugMode=true;
            candidate.msg = t+"\n"+sb.toString();
        }

        return candidate;
    }


    /**
     * toIntArray.
     * @param l - Really a List<Integer> in 1.5 symantics.
     * @return - int[]
     */
    int[] toIntArray(List l){

        if(l==null){
            return new int[0];
        }

        int size = l.size();
        int[] retVal = new int[size];

        for(int i=0;i<size;i++){
            retVal[i] = ((Integer) l.get(i)).intValue();
        }

        return retVal;        
    }//toIntArray

    RSTPacketStats.RSTPacketData makeGoodCandidate( List l ){
        RSTPacketStats.RSTPacketData pktData = new RSTPacketStats.RSTPacketData();

        pktData.nActiveOpens       = ((Integer) l.get(0)).intValue();
        pktData.nPassiveOpens      = ((Integer) l.get(1)).intValue();
        pktData.nFailedConnAttempt = ((Integer) l.get(2)).intValue();
        pktData.nConnReset         = ((Integer) l.get(3)).intValue();
        pktData.nCurrentOpen       = ((Integer) l.get(4)).intValue();

        pktData.msg="candidate";

        return pktData;
    }


    /**
     * Might want to try an alternate way of collection the data.
     * @param pktData -
     * @return - true if all columns are zero.
     */
    boolean isResultZero(RSTPacketStats.RSTPacketData pktData ){

        if( pktData.nActiveOpens==0 &&
            pktData.nConnReset==0 &&
            pktData.nCurrentOpen==0 &&
            pktData.nFailedConnAttempt==0 &&
            pktData.nPassiveOpens==0 )
        {
            return true;
        }

        return false;
    }

    /**
     *  Is the "Share ISP Monitor Results" checked?
     *
     *  Check the boolean parameter -- "aznetmon.share" --
     *
     * @return - true if the check box is selected.
     */
    boolean verifyShareCheckButton()
    {
        return COConfigurationManager.getBooleanParameter("aznetmon.share");            
    }//verifyShareCheckButton


    public boolean addToVersionServerStore( RSTPacketStats stats )
    {

        if(pi==null){
            return false;
        }

        if( !verifyShareCheckButton() ){
            //Don't have permision to share results.
            return false;
        }

        try{
            PluginConfig pc = pi.getPluginconfig();

            //ToDo: add synchronization.
            //NOTE: Will need some type of synchronization block starting here.

            //if the result is going to cross the hour boundary, then accumulate the result.
            if( !stats.isTimeToStoreResults() ){
                return false;
            }

            RSTPacketStats.RSTPacketData data = stats.gatherUnstoredResults();

            Map vsData = pc.getPluginMapParameter(RSTPacketStats.PLUGIN_MAP_NAME,new HashMap());
            List accumulatedResults = (List) vsData.get(RSTPacketStats.RESET_RECORDS);
            if ( accumulatedResults == null ){
            	accumulatedResults = new ArrayList();
            	vsData.put(RSTPacketStats.RESET_RECORDS,accumulatedResults);
            }
            Map appendData = new RSTPacketStore( data );

            if( data.isDebugMode ){
                String debug = (String) vsData.get(RSTPacketStats.RESET_DEBUG);
                debug = data.getDebugOutput();
                vsData.put(RSTPacketStats.RESET_DEBUG,debug);
            }

            //Don't allow total records to go above 100.
            if( accumulatedResults.size()>100 ){
                accumulatedResults.remove(0);
            }
            
            accumulatedResults.add( appendData );
            pc.setPluginMapParameter(RSTPacketStats.PLUGIN_MAP_NAME,vsData);

            //NOTE: Will need some type of synchronization block ending here.

            return true;
        }catch(Exception e){

        	e.printStackTrace();
        	
            return false;
        }


    }//addToVersionServer


    /**
     * Map just to have a specical constructor.
     */
    static class RSTPacketStore extends HashMap{

        public RSTPacketStore(RSTPacketStats.RSTPacketData data){

            //sticking with Java 1.4 syntax here.
            put( RSTPacketStats.TIME, new Long( data.timestamp ) );
            put( RSTPacketStats.N_RST, new Integer( data.deltaConnReset ) );
            put( RSTPacketStats.N_ACTIVE, new Integer( data.deltaActiveOpens ) );
            put( RSTPacketStats.N_PASSIVE, new Integer( data.deltaPassiveOpens ) );
            put( RSTPacketStats.N_FAILED, new Integer( data.deltaFailedConnAttempt ) );
            put( RSTPacketStats.C_OPEN, new Integer( data.nCurrentOpen ) );

        }//

    }

    ///*** testing ***/

    /**
     * For testing.
     * @param args
     */
    public static void main(String[] args)
    {

        RSTPacketCountPerformer rpce = new RSTPacketCountPerformer(null);

        UTTimerEvent empty = new UTTimerEvent(){

            public void cancel() {

            }
        };

        rpce.perform(empty);

        try{Thread.sleep(60 * 1000);}catch(Exception e){}

        rpce.perform(empty);

    }//main


}//class

