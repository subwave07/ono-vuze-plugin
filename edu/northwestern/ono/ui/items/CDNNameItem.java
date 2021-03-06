/**
 * Ono Project
 *
 * File:         CustomerItem.java
 * RCS:          $Id: CDNNameItem.java,v 1.8 2010/03/29 16:48:03 drc915 Exp $
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
package edu.northwestern.ono.ui.items;

import java.util.List;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.tables.utils.CoreTableColumn;
import edu.northwestern.ono.ui.OnoView;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CustomerItem class ...
 */
public class CDNNameItem extends CoreTableColumn implements
TableCellRefreshListener {


	/** Default Constructor */
	public CDNNameItem() {
		super("cdnname", POSITION_LAST, 100, OnoView.NEARBY_PEERS);   
		this.setRefreshInterval(INTERVAL_LIVE);
	}

	public void refresh(TableCell cell) {
		OnoPeer op = (OnoPeer)cell.getDataSource();
		String sText = op.printCustomers(true);	
		if (sText.length()<=2 && op.isCIDRMatch()){
			sText = "Manual Selection";
		}

		if ((cell.setText(sText) || !cell.isValid())) {

			if (sText.length()<=2) cell.setSortValue("ZZZZ");
			else cell.setSortValue(op.getMaxCosSim()==null?sText:op.getMaxCosSim().getKey());
			
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


