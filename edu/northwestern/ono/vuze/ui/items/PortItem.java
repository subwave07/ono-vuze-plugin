/**
 * Ono Project
 *
 * File:         CustomerItem.java
 * RCS:          $Id: PortItem.java,v 1.3 2010/03/29 16:48:04 drc915 Exp $
 * Description:  CustomerItem class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 24, 2007 at 3:34:16 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.ui.items
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2007, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package edu.northwestern.ono.vuze.ui.items;

import java.util.List;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.position.GenericPeer;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.vuze.tables.utils.CoreTableColumn;
import edu.northwestern.ono.vuze.ui.OnoView;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CustomerItem class ...
 */
public class PortItem extends CoreTableColumn implements
		TableCellRefreshListener {

	
	/** Default Constructor */
	  public PortItem() {
	    super("port", POSITION_LAST, 100, OnoView.NEARBY_PEERS);
	    setObfustication(true);
	    this.setRefreshInterval(INTERVAL_LIVE);
	   }
	  
	  public void refresh(TableCell cell) {
		  GenericPeer p = (GenericPeer)cell.getDataSource();

		  if (p.isOnoPeer()) {
			  p = OnoPeerManager.getInstance().getOnoPeer(p.getIp());
		  } else {
			  assert(p.isBRPPeer()); //remove this if a third peer type is added
			  p = BRPPeerManager.getInstance().getBRPPeer(p.getIp());

		  }
		  
		  String sText = (p == null || p.getPort()==0) ? "Unknown" 
				  : p.getPort()+"";

		  if (cell.setText(sText) || !cell.isValid()) {

			  cell.setSortValue(p!=null?p.getPort():Long.MAX_VALUE);

		  }

	  }
	  
		public void addCellRefreshListener(TableCellRefreshListener listener) {
			//if (listener == this) return;
			super.addCellRefreshListener(listener);
		}



		public List getCellRefreshListeners() {
			List myList = super.getCellRefreshListeners();
			if (!myList.contains(this)) myList.add(this);
			return myList;
		}
		    
		  }
