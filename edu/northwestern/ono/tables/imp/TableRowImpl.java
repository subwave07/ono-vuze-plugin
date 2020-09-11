/*
 * File    : TableRowImpl.java
 * Originally TorrentRow.java, and changed to be more generic by TuxPaper
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package edu.northwestern.ono.tables.imp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRowMouseListener;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.TrackerTorrentImpl;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

import edu.northwestern.ono.tables.TableCellCore;
import edu.northwestern.ono.tables.TableColumnCore;
import edu.northwestern.ono.tables.TableRowCore;




/** Represents an entire row in a table.  Stores each cell belonging to the
 * row and handles refreshing them.
 *
 * @see TableCellImpl
 * 
 * @author TuxPaper
 *            2005/Oct/07: Moved TableItem.SetData("TableRow", ..) to 
 *                         BufferedTableRow
 *            2005/Oct/07: Removed all calls to BufferedTableRoe.getItem()
 */
public class TableRowImpl 
       extends BufferedTableRow 
       implements TableRowCore
{
  /** List of cells in this column.  They are not stored in display order */
  private Map mTableCells;
  private String sTableID;
  
  private Object coreDataSource;
  private Object pluginDataSource;
  private boolean bDisposed;
  private boolean bSetNotUpToDateLastRefresh = false;

  private static AEMonitor sortedDisposal_mon = new AEMonitor( "TableRowImpl" );
  
  // XXX add rowVisuallyupdated bool like in ListRow

  /**
   * Default constructor
   * 
   * @param table
   * @param sTableID
   * @param columnsSorted
   * @param dataSource
   * @param bSkipFirstColumn
   */
  public TableRowImpl(Table table, String sTableID,
			TableColumnCore[] columnsSorted, Object dataSource,
			boolean bSkipFirstColumn) {
		super((TableOrTreeSWT) table);
    this.sTableID = sTableID;
    coreDataSource = dataSource;
    mTableCells = new HashMap();
    bDisposed = false;

    // create all the cells for the column
    for (int i = 0; i < columnsSorted.length; i++) {
    	if (columnsSorted[i] == null)
    		continue;
      //System.out.println(dataSource + ": " + tableColumns[i].getName() + ": " + tableColumns[i].getPosition());
    	TableCellImpl cell = new TableCellImpl(TableRowImpl.this, columnsSorted[i], 
          bSkipFirstColumn ? i+1 : i);
      mTableCells.put(columnsSorted[i].getName(), cell);
      //if (i == 10) cell.bDebug = true;
    }
  }

  public boolean isValid() {
  	if (bDisposed)
  		return true;

    boolean valid = true;
    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
      if (cell != null)
        valid &= cell.isValid();
    }
    return valid;
  }

  /** TableRow Implementation which returns the 
   * associated plugin object for the row.  Core Column Object who wish to get 
   * core data source must re-class TableRow as TableRowCore and use
   * getDataSource(boolean)
   *
   * @see TableRowCore.getDataSource()
   */
  public Object getDataSource() {
    return getDataSource(false);
  }

  public String getTableID() {
    return sTableID;
  }
  
  public TableCell getTableCell(String field) {
  	if (bDisposed)
  		return null;
    return (TableCell)mTableCells.get(field);
  }
  
  /* Start Core-Only functions */
  ///////////////////////////////

  public void delete(boolean bDeleteSWTObject) {
		sortedDisposal_mon.enter();

		try {
			if (bDisposed)
				return;

//			if (TableView.DEBUGADDREMOVE)
//				System.out.println((table.isDisposed() ? "" : table.getData("Name"))
//						+ " row delete; index=" + getIndex());

			try {
				// Manually dispose of TableCellImpl objects, since row does
				// not contain a list of them.
				Iterator iter = mTableCells.values().iterator();
				while (iter.hasNext()) {
					TableCellCore item = (TableCellCore) iter.next();
					item.dispose();
				}
			} finally {
				// Dispose of row
				if (bDeleteSWTObject) {
					dispose();
				}
			}

			bDisposed = true;
		} finally {
			sortedDisposal_mon.exit();
		}
	}
  
  public boolean refresh(boolean bDoGraphics) {
    if (bDisposed)
      return false;
    
    boolean bVisible = isVisible();

    return refresh(bDoGraphics, bVisible);
  }

  public boolean refresh(boolean bDoGraphics, boolean bVisible) {
    // If this were called from a plugin, we'd have to refresh the sorted column
    // even if we weren't visible
    
    if (!bVisible) {
    	if (!bSetNotUpToDateLastRefresh) {
    		setUpToDate(false);
    		bSetNotUpToDateLastRefresh = true;
    	}
  		return false;
  	}
    
		bSetNotUpToDateLastRefresh = false;
		
		//System.out.println(SystemTime.getCurrentTime() + "refresh " + getIndex());

		boolean changed = false;
    Iterator iter = mTableCells.values().iterator();
    while(iter.hasNext()) {
      TableCellCore item = (TableCellCore)iter.next();
      changed |= item.refresh(bDoGraphics, bVisible);
    }
    return changed;
  }

  public void locationChanged(int iStartColumn) {
    if (bDisposed || !isVisible())
      return;

  	Iterator iter = mTableCells.values().iterator();
  	while(iter.hasNext()) {
  		TableCellCore item = (TableCellCore)iter.next();
  		if (item.getTableColumn().getPosition() > iStartColumn)
  		  item.locationChanged();
  	}
  }

  public void doPaint(GC gc) {
  	doPaint(gc, isVisible());
  }

  public void doPaint(GC gc, boolean bVisible) {
    if (bDisposed || !bVisible)
      return;

    Iterator iter = mTableCells.values().iterator();
    while(iter.hasNext()) {
      TableCellCore item = (TableCellCore) iter.next();
  		if (item.needsPainting()) {
  			item.doPaint(gc);
  		}
    }
  }

  public TableCellCore getTableCellCore(String field) {
  	if (bDisposed)
  		return null;

    return (TableCellCore)mTableCells.get(field);
  }

  public Object getDataSource(boolean bCoreObject) {
  	if (bDisposed)
  		return null;

    if (bCoreObject)
      return coreDataSource;
      
    if (pluginDataSource != null)
      return pluginDataSource;

    if (sTableID.equals(TableManager.TABLE_MYTORRENTS_COMPLETE) ||
        sTableID.equals(TableManager.TABLE_MYTORRENTS_INCOMPLETE)) {
      DownloadManager dm = (DownloadManager)coreDataSource;
      if (dm != null) {
        try {
          pluginDataSource = DownloadManagerImpl.getDownloadStatic(dm);
        } catch (DownloadException e) { /* Ignore */ }
      }
    }
    if (sTableID.equals(TableManager.TABLE_TORRENT_PEERS)) {
      PEPeer peer = (PEPeer)coreDataSource;
      if (peer != null)
        pluginDataSource = PeerManagerImpl.getPeerForPEPeer( peer );
    }

    if (sTableID.equals(TableManager.TABLE_TORRENT_PIECES)) {
      // XXX There is no Piece object for plugins yet
      PEPiece piece = (PEPiece)coreDataSource;
      if (piece != null)
        pluginDataSource = null;
    }

    if (sTableID.equals(TableManager.TABLE_TORRENT_FILES)) {
      DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)coreDataSource;
      if (fileInfo != null){
    	  try {
    		  pluginDataSource = 
    			  new DiskManagerFileInfoImpl(
    					  DownloadManagerImpl.getDownloadStatic(fileInfo.getDownloadManager()),
    					  fileInfo);
          } catch (DownloadException e) { /* Ignore */ }
      }
    }

    if (sTableID.equals(TableManager.TABLE_MYSHARES)) {
      pluginDataSource = coreDataSource;
    }

    if (sTableID.equals(TableManager.TABLE_MYTRACKER)) {
      TRHostTorrent item = (TRHostTorrent)coreDataSource;
      if (item != null)
        pluginDataSource = new TrackerTorrentImpl(item);
    }
    
    return pluginDataSource;
  }
  
	public boolean isRowDisposed() {
		return bDisposed;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#getIndex()
	 */
	public int getIndex() {
		if (bDisposed)
			return -1;

		return super.getIndex();
	}

	public boolean setTableItem(int newIndex) {
		if (bDisposed) {
			System.out.println("XXX setTI: bDisposed from " + Debug.getCompressedStackTrace());
			return false;
		}

		return setTableItem(newIndex, false);
	}
	
	public void setForeground(Color c) {
  	// Don't need to set when not visible
  	if (!isVisible())
  		return;
  	
  	super.setForeground(c);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#invalidate()
	 */
	public void invalidate() {
		super.invalidate();
		
  	if (bDisposed)
  		return;

    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
      if (cell != null)
        cell.invalidate(true);
    }
	}
	
	public void repaint() {
		super.invalidate();
	}

	public void setUpToDate(boolean upToDate) {
  	if (bDisposed)
  		return;

    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
      if (cell != null)
        cell.setUpToDate(upToDate);
    }
	}

	public String toString() {
		String result = "TableRowImpl@" + Integer.toHexString(hashCode());
		
		// In the rare case that we are calling this outside of a SWT thread, this
		// may break, so we will just ignore those exceptions.
		String index = null;
		try {index = String.valueOf(getIndex());}
		catch (SWTException se) {/* do nothing */}
		
		return result + ((index == null) ? "" : ("/#" + index)); 		
	}

	
	public void addMouseListener(TableRowMouseListener listener) {
		// TODO Auto-generated method stub
		
	}

	
	public void removeMouseListener(TableRowMouseListener listener) {
		// TODO Auto-generated method stub
		
	}

	
	public void setForeground(int[] rgb) {
		// TODO Auto-generated method stub
		
	}

	
	public void setForegroundToErrorColor() {
		// TODO Auto-generated method stub
		
	}

	public Object getData(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setData(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	public void setAlternatingBGColor(boolean bEvenIfNotVisible) {
		// TODO Auto-generated method stub
		
	}

}
