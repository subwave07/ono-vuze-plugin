/**
 * Ono Project
 *
 * File:         IDDBDeleteAction.java
 * RCS:          $Id: IDDBDeleteAction.java,v 1.2 2010/03/29 16:48:03 drc915 Exp $
 * Description:  IDDBDeleteAction class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 25, 2007 at 5:03:44 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht
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
package edu.northwestern.ono.dht;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IDDBDeleteAction class ...
 */
public interface IDDBDeleteAction {


    /**
     * @param event
     */
    public void handleTimeout(IDistributedDatabaseEvent  event);

    /**
     *
     * @param event
     */
    public void handleComplete(IDistributedDatabaseEvent  event);
}
