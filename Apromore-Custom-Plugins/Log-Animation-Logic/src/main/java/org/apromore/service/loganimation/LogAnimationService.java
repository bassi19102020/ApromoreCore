/*
 * Copyright © 2009-2018 The Apromore Initiative.
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

package org.apromore.service.loganimation;

import java.util.List;

import org.apromore.xes.model.XLog;

public interface LogAnimationService {

   class Log {
       public String fileName;
       public XLog   xlog;
       public String color;
   }

   /**
    * @param bpmn  a BPMN 2.0 model, never <code>null</code>
    * @param logs  the logs of the model executions to be animated, never <code>null</code>
    * @return a JSON representation of the animation
    * @throws Exception  if the representation couldn't be generated
    */
   public String createAnimation(String bpmn, List<Log> logs) throws Exception;
}
