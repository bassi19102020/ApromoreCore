/*-
 * #%L
 * This file is part of "Apromore Core".
 *
 * Copyright (C) 2016 - 2017 Queensland University of Technology.
 * %%
 * Copyright (C) 2018 - 2020 Apromore Pty Ltd.
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

/**
 *
 */
package org.apromore.dao.jpa;

import org.apromore.apmlog.APMLog;
import org.apromore.apmlog.APMLogService;
import org.apromore.cache.ehcache.CacheRepository;
import org.apromore.common.ConfigBean;
import org.apromore.dao.LogRepositoryCustom;
import org.apromore.dao.model.Log;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.*;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XTimeBoundsImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * implementation of the org.apromore.dao.LogRepositoryCustom interface.
 * @author <a href="mailto:raffaele.conforti@unimelb.edu.au">Raffaele Conforti</a>
 */
public class LogRepositoryCustomImpl implements LogRepositoryCustom {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogRepositoryCustomImpl.class);

    private static final String APMLOG_CACHE_KEY_SUFFIX = "APMLog";
    private static final String GET_ALL_LOGS_JPA = "SELECT l FROM Log l ";
    private static final String GET_ALL_LOGS_FOLDER_JPA = "SELECT l FROM GroupLog gl JOIN gl.log l JOIN gl.group g JOIN l.folder f, User u JOIN u.groups g2 WHERE (u.rowGuid = :userRowGuid) AND (g = g2) AND (gl.hasRead = TRUE) AND f.id = ";
    private static final String GET_ALL_SORT_JPA = " ORDER by l.id";
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    @PersistenceContext
    private EntityManager em;

   
    /* ************************** JPA Methods here ******************************* */

    /**
     * @see org.apromore.dao.LogRepositoryCustom#findAllLogs(String)
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Log> findAllLogs(final String conditions) {
        StringBuilder strQry = new StringBuilder(0);
        strQry.append(GET_ALL_LOGS_JPA);
        if (conditions != null && !conditions.isEmpty()) {
            strQry.append(" WHERE ").append(conditions);
        }
        strQry.append(GET_ALL_SORT_JPA);

        Query query = em.createQuery(strQry.toString());
        return query.getResultList();
    }

    /**
     * @see org.apromore.dao.LogRepositoryCustom#findAllLogsByFolder(Integer, String, String)
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Log> findAllLogsByFolder(final Integer folderId, final String userRowGuid, final String conditions) {
        StringBuilder strQry = new StringBuilder(0);
        if (folderId == 0) {
            strQry.append("SELECT l FROM GroupLog gl JOIN gl.log l JOIN gl.group g, User u JOIN u.groups g2 WHERE (u.rowGuid = :userRowGuid) AND (g = g2) AND (gl.hasRead = TRUE) AND l.folder IS NULL");
        } else {
            strQry.append(GET_ALL_LOGS_FOLDER_JPA).append(folderId);
        }
        if (conditions != null && !conditions.isEmpty()) {
            strQry.append(" AND ").append(conditions);
        }
        strQry.append(GET_ALL_SORT_JPA);

        Query query = em.createQuery(strQry.toString());
        query.setParameter("userRowGuid", userRowGuid);

        return query.getResultList();
    }

}
