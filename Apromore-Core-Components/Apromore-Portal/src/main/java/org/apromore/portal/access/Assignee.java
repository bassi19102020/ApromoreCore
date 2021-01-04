/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

package org.apromore.portal.access;

import org.apromore.dao.model.Group.Type;

public class Assignee {
    private String name;
    private String rowGuid;
    private Type type; // Type.USER or Type.GROUP

    public Assignee(String name, String rowGuid, Type type) {
        this.name = name;
        this.rowGuid = rowGuid;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getRowGuid() {
        return rowGuid;
    }

    public Type getType() {
        return type;
    }
}
