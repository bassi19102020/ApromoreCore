/*
 * Copyright © 2009-2015 The Apromore Initiative.
 *
 * This file is part of "Apromore".
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package com.signavio.warehouse.directory.handler;

import javax.servlet.ServletContext;

import com.signavio.platform.annotations.HandlerConfiguration;

@HandlerConfiguration(context=DirectoryHandler.class, uri="/parents", rel="parents")
public class ParentDirectoriesHandler extends AbstractParentDirectoriesHandler {

	public ParentDirectoriesHandler(ServletContext servletContext) {
		super(servletContext);
	}

}