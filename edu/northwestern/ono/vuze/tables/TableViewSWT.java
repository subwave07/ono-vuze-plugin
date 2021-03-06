/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
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
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package edu.northwestern.ono.vuze.tables;

import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener;
import org.gudy.azureus2.ui.swt.views.IView;

import edu.northwestern.ono.vuze.tables.common.TableRowCore;
import edu.northwestern.ono.vuze.tables.common.TableView;


/**
 * @author TuxPaper
 * @created Feb 2, 2007
 *
 */
public interface TableViewSWT<DATASOURCETYPE>
	extends TableView<DATASOURCETYPE>
{
	/** Helpful output when trying to debug add/removal of rows */
	public final static boolean DEBUGADDREMOVE = System.getProperty("debug.swt.table.addremove", "0").equals("1");

	void addKeyListener(KeyListener listener);

	public void addMenuFillListener(TableViewSWTMenuFillListener l);

	DragSource createDragSource(int style);

	DropTarget createDropTarget(int style);

	public Composite getComposite();

	/**
	 * @return
	 */
	IView[] getCoreTabViews();

	TableRowCore getRow(DropTargetEvent event);

	/**
	 * @param dataSource
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	TableRowSWT getRowSWT(DATASOURCETYPE dataSource);

	Composite getTableComposite();

	void initialize(Composite composite);

	/**
	 * @param image
	 * @param shellOffset
	 * @return
	 */
	Image obfusticatedImage(Image image, Point shellOffset);

	/**
	 * @param listener
	 */
	void removeKeyListener(KeyListener listener);

	/**
	 * @param mainPanelCreator
	 */
	void setMainPanelCreator(TableViewSWTPanelCreator mainPanelCreator);


	/**
	 * @param size
	 */
	void setRowDefaultIconSize(Point size);
	

	/**
	 * @param coreTabViews
	 */
	void setCoreTabViews(IView[] coreTabViews);

	/**
	 * @param x
	 * @param y
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	TableCellSWT getTableCell(int x, int y);

	/**
	 * @return Offset potision of the cursor relative to the cell the cursor is in
	 *
	 * @since 3.0.4.3
	 */
	Point getTableCellMouseOffset(TableCellSWT tableCell);

	/**
	 * @param listener
	 *
	 * @since 3.1.1.1
	 */
	void removeRefreshListener(TableRowRefreshListener listener);

	/**
	 * @param listener
	 *
	 * @since 3.1.1.1
	 */
	void addRefreshListener(TableRowRefreshListener listener);

	/**
	 * @return
	 *
	 * @since 4.1.0.9
	 */
	String getFilterText();

	/**
	 * @param filterCheck
	 *
	 * @since 4.1.0.9
	 */
	void enableFilterCheck(Text txtFilter, TableViewFilterCheck<DATASOURCETYPE> filterCheck);

	/**
	 * @param s
	 *
	 * @since 4.1.0.8
	 */
	void setFilterText(String s);

	/**
	 * @param composite
	 * @param min
	 * @param max
	 * 
	 * @since 4.1.0.9
	 */
	boolean enableSizeSlider(Composite composite, int min, int max);

	void disableSizeSlider();
}
