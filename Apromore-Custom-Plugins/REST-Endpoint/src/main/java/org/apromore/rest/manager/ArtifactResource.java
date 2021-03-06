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
package org.apromore.rest.manager;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeFactory;
import org.apromore.dao.LogRepository;
import org.apromore.dao.model.Log;
import org.apromore.portal.model.LogSummaryType;
import org.apromore.portal.model.UserType;
import org.apromore.rest.AbstractResource;
import org.apromore.rest.ResourceException;
import org.apromore.service.EventLogService;
import org.apromore.service.WorkspaceService;
import org.apromore.service.csvimporter.model.LogMetaData;
import org.apromore.service.csvimporter.services.ParquetFactoryProvider;
import org.apromore.service.csvimporter.services.legacy.LogImporterProvider;
import org.apromore.service.helper.UserInterfaceHelper;
import org.apromore.service.model.FolderTreeNode;
import org.deckfour.xes.model.XLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for artifact management.
 *
 * Artifacts are organized in a folder tree whose root URL path is <code>/rest/Home/</code>.
 */
@Path("/Home")
public final class ArtifactResource extends AbstractResource {

    /** Logger.  Named after the class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactResource.class);

    @Context
    private HttpHeaders httpHeaders;

    /**
     * Download a log.
     *
     * <pre>curl http://localhost:9000/rest/Home/foo -u admin:password &gt; foo.xes.gz</pre>
     *
     * @param path  the path identifying the folder in which the event log resides
     * @param name  the name of the event log within the folder; shall not contain "/"
     * @return a GZIPped XES XML document
     * @throws ResourceException if no event log with the given path and name exists
     */
    @GET
    @Path("{path:(.*/)*}{name}")
    public Response getLog(final @PathParam("path") String path,
                           final @PathParam("name") String name) throws Exception {

        // Only authorize admin accounts
        UserType user = authenticatedUser();
        authorize(user, "ROLE_ADMIN");

        // Try to access the folder using the given credentials
        WorkspaceService workspaceService = osgiService(WorkspaceService.class);
        int folderId = findFolderIdByPath(path, user.getId(), workspaceService);

        // Look for the event log in the folder
        Log log = osgiService(LogRepository.class).findByNameAndFolderId(name, folderId == 0 ? null : folderId);
        if (log == null) {
            throw new ResourceException(Response.Status.NOT_FOUND,
                "No log named \"" + name + "\" in folder " + path);
        }

        // Obtain the serialization of the event log
        EventLogService eventLogService = osgiService(EventLogService.class);
        XLog xLog = eventLogService.getXLog(log.getId());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        eventLogService.exportToStream(baos, xLog);

        return Response.status(Response.Status.OK)
                       .header("Content-Encoding", "gzip")  // In JAX-RS 2, could use .encoding("gzip")
                       .type(MediaType.APPLICATION_XML)
                       .entity(baos.toByteArray())
                       .build();
    }

    /**
     * Logs may only be created, not modified.
     *
     * A content type is mandatory.  The supported MIME types are <code>application/xml</code> for XES
     * documents, <code>test/csv</code> for comma-separated values, and <code>application/x-parquet</code>
     * for Apache Parquet files.
     *
     * XES documents are the most straightforward to POST, requiring only the HTTP Basic authentication
     * and the content type to be specified.
     *
     * <pre>curl http://localhost:9000/rest/Home/foo \
     *     -u admin:password \
     *     --header "Content-Type: application/xml" \
     *     --data @foo.xes</pre>
     *
     * For CSV and parquet, additional <code>Apromore-Log-*</code> HTTP headers must be present for every
     * attribute in the POSTed document, specifying its type.
     * Timestamp attributes Apromore-Log-Start-Timestamp, Apromore-Log-End-Timestamp, and
     * Apromore-Log-Other-Timestamp must include a format parameter.
     *
     * <pre>curl http://localhost:9000/rest/Home/bar \
     *     -u admin:password \
     *     --header "Apromore-Log-Case-ID: Service ID" \
     *     --header "Apromore-Log-Activity: Operation" \
     *     --header "Apromore-Log-End-Timestamp: End Date; format=\"dd MM yy HH mm\"" \
     *     --header "Apromore-Log-Start-Timestamp: Start Date; format=\"dd MM yy HH mm\"" \
     *     --header "Apromore-Log-Resource: Agent" \
     *     --header "Apromore-Log-Event-Attribute: Agent Position" \
     *     --header "Apromore-Log-Case-Attribute: Customer ID" \
     *     --header "Apromore-Log-Case-Attribute: Product" \
     *     --header "Apromore-Log-Case-Attribute: Service Type" \
     *     --header "Apromore-Log-Time-Zone: GMT+10:00" \
     *     --header "Content-Type: text/csv" \
     *     --data-binary @bar.csv</pre>
     *
     * @param path  the folder in which to create the log
     * @param name  the name of the created log
     * @param body  the content of the log in the expected content type
     * @return the actual event log created, including the generated id
     * @throws ResourceException if <var>logSummary</var> isn't suitable
     */
    @POST
    @Path("{path:(.*/)*}{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public LogSummaryType postLog(final @PathParam("path") String path,
                                  final @PathParam("name") String name,
                                  final InputStream body) throws Exception {

        // Only authorize admin accounts
        UserType user = authenticatedUser();
        authorize(user, "ROLE_ADMIN");

        // Try to access the folder using the given credentials
        int folderId = findFolderIdByPath(path, user.getId(), osgiService(WorkspaceService.class));

        // Import the event log
        Log log;
        if (httpHeaders.getMediaType().isCompatible(MediaType.valueOf(MediaType.APPLICATION_XML))) {
            log = osgiService(EventLogService.class).importLog(
                user.getUsername(),
                folderId,
                name,
                body,
                "xes",  // extension; specifies uncompressed (no GZIP) XES format
                "",  // domain
                DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toString(),
                true); // publicModel

        } else if (httpHeaders.getMediaType().isCompatible(new MediaType("text", "csv"))) {
            final int bufferSize = 1024;  // maximum size of the header line of any potential CSV log
            log = logWithMetadata("csv", splitInputStreamUsingMemory(body, bufferSize), user, name, folderId);

        } else if (httpHeaders.getMediaType().isCompatible(new MediaType("application", "x-parquet"))) {
            // Because parquet's metadata is at the end of the stream, we have to copy into a temp file
            log = logWithMetadata("parquet", splitInputStreamUsingTempFile(body), user, name, folderId);

        } else {
            throw new ResourceException(Response.Status.UNSUPPORTED_MEDIA_TYPE,
                "Request had content type " + httpHeaders.getMediaType() + " but only " +
                MediaType.APPLICATION_XML + ", text/csv, application/x-parquet are supported");
        }

        // Return a description of the created event log
        return (LogSummaryType) osgiService(UserInterfaceHelper.class).buildLogSummary(log);
    }

    /**
     * Split an {@link InputStream} into two by buffering the beginning of it into memory and rewinding.
     *
     * @param inputStream  an open stream which hasn't yet been read
     * @param headerBufferLength  how many bytes to read into the initial buffer
     * @return a pair of copies of the <var>inputStream</var>, the first of which contains only the
     *     first <var>headerBufferLength</var> bytes, the second of which contains everything
     */
    private static InputStream[] splitInputStreamUsingMemory(final InputStream inputStream,
        final int headerBufferLength) throws Exception {

        InputStream bodyInputStream = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);
        assert bodyInputStream.markSupported();

        bodyInputStream.mark(headerBufferLength);  // reserve enough of a buffer to fit the header
        byte[] line = new byte[headerBufferLength];
        bodyInputStream.read(line);
        bodyInputStream.reset();  // put the header back into the stream
        InputStream headerInputStream = new ByteArrayInputStream(line);

        return new InputStream[] {headerInputStream, bodyInputStream};
    }

    /**
     * Split an {@link InputStream} into two by copying its content into a temporary file.
     *
     * @param inputStream  an open stream which hasn't yet been read
     * @return a pair of file-backed copies of the <var>inputStream</var>
     */
    private static InputStream[] splitInputStreamUsingTempFile(final InputStream inputStream) throws Exception {
        File tmpFile = File.createTempFile("apromore-rest-log-upload-", null);
        Files.copy(inputStream, tmpFile.toPath(), REPLACE_EXISTING);
        inputStream.close();

        return new InputStream[] {new FileInputStream(tmpFile), new FileInputStream(tmpFile)};
    }

    /**
     * @param mediaFormat  the extension for the required format, e.g. "csv", "xlsx", "parquet", etc.
     * @param inputStreams  an array of two streams, the first of which will be used for the header and the
     *     second will be used for the body
     * @param user  the owner of the created log
     @ @param name  the name of the created log
     * @param folderId  the folder containing the created log
     */
    private Log logWithMetadata(final String mediaFormat, final InputStream[] inputStreams,
                                final UserType user, final String name, final int folderId) throws Exception {

        // Read the header
        LogMetaData logMetaData = osgiService(ParquetFactoryProvider.class)
            .getParquetFactory(mediaFormat)
            .getMetaDataService()
            .extractMetadata(inputStreams[0], "UTF-8");

        // Populate log metadata
        List<String> h = logMetaData.getHeader();
        logMetaData.setCaseIdPos(h.indexOf(findAttribute("Apromore-Log-Case-ID")));
        logMetaData.setActivityPos(h.indexOf(findAttribute("Apromore-Log-Activity")));

        final Pattern pattern = Pattern.compile("(?<name>[^;]*);\\s*format\\s*=\\s*\"(?<format>[^\"]*)\"\\s*");

        String endTimestampHeader = findAttribute("Apromore-Log-End-Timestamp");
        if (endTimestampHeader != null) {
            Matcher matcher = pattern.matcher(endTimestampHeader);
            if (!matcher.matches()) {
                throw new ResourceException(Response.Status.BAD_REQUEST,
                    "Malformed Apromore-Log-End-Timestamp header");
            }
            logMetaData.setEndTimestampPos(h.indexOf(matcher.group("name")));
            logMetaData.setEndTimestampFormat(matcher.group("format"));
        }

        String startTimestampHeader = findAttribute("Apromore-Log-Start-Timestamp");
        if (startTimestampHeader != null) {
            Matcher matcher = pattern.matcher(startTimestampHeader);
            if (!matcher.matches()) {
                throw new ResourceException(Response.Status.BAD_REQUEST,
                    "Malformed Apromore-Log-Start-Timestamp header");
            }
            logMetaData.setStartTimestampPos(h.indexOf(matcher.group("name")));
            logMetaData.setStartTimestampFormat(matcher.group("format"));
        }

        HashMap<Integer, String> map = new HashMap<>();
        for (String otherTimestampHeader: findAttributes("Apromore-Log-Other-Timestamp")) {
            Matcher matcher = pattern.matcher(otherTimestampHeader);
            if (!matcher.matches()) {
                throw new ResourceException(Response.Status.BAD_REQUEST,
                    "Malformed Apromore-Log-Other-Timestamp header");
            }
            map.put(h.indexOf(matcher.group("name")), matcher.group("format"));
        }
        logMetaData.setOtherTimestamps(map);

        logMetaData.setResourcePos(h.indexOf(findAttribute("Apromore-Log-Resource")));
        logMetaData.setEventAttributesPos(findAttributes("Apromore-Log-Event-Attribute")
            .stream().map(s -> h.indexOf(s)).collect(Collectors.toList()));
        logMetaData.setCaseAttributesPos(findAttributes("Apromore-Log-Case-Attribute")
            .stream().map(s -> h.indexOf(s)).collect(Collectors.toList()));
        logMetaData.setIgnoredPos(findAttributes("Apromore-Log-Ignored")
            .stream().map(s -> h.indexOf(s)).collect(Collectors.toList()));
        logMetaData.setTimeZone(findAttribute("Apromore-Log-Time-Zone"));

        return osgiService(LogImporterProvider.class)
            .getLogReader(mediaFormat)
            .importLog(inputStreams[1], logMetaData, "UTF-8", true, user.getUsername(), folderId, name)
            .getImportLog();
    }

    private String findAttribute(final String name) {
        List<String> values = httpHeaders.getRequestHeader(name);
        
        return values == null ? null : values.stream().findFirst().orElse(null);
    }

    /**
     * @param name  an HTTP header name
     * @return all values of the <var>name</var>d header occurring within {@link #httpHeaders}, honoring both
     *     duplicate headers and headers with comma-delimited lists of values
     */
    private List<String> findAttributes(final String name) {
        List<String> requestHeaders = httpHeaders.getRequestHeader(name);

        if (requestHeaders == null) {
            return Collections.emptyList();
        }

        return requestHeaders == null ? Collections.emptyList()
            : requestHeaders.stream()
                            .flatMap(s -> (Arrays.asList(s.split(","))).stream())
                            .map(s -> s.trim())
                            .collect(Collectors.toList());
    }

    /**
     * Lookup a folder by its path.
     *
     * This method ought to be made part of the {@link WorkspaceService}.
     *
     * @param path  a slash-delimited folder path, e.g. "foo/bar/".  Empty string denotes the root folder.
     *     Beware that "/" would be treated as a subfolder inside the root folder with the name "".
     * @param userId  the row GUID of the user under whose authority the folder is accessed
     * @param workspaceService  used to access folders
     * @return the primary key of the folder at the given <var>path</var>
     * @throws ResourceException if the <var>path</var> is not an existing folder
     */
    private static int findFolderIdByPath(final String path,
                                          final String userId,
                                          final WorkspaceService workspaceService) throws ResourceException {

        int folderId = 0;  // the root folder, "Home"

        // Descend into the requested directory
        List<FolderTreeNode> nodes = workspaceService.getWorkspaceFolderTree(userId);
        if (!path.isEmpty()) {
            subfolder: for (String pathElement: path.split("/")) {
                for (FolderTreeNode node: nodes) {
                    if (node.getName().equals(pathElement)) {
                        folderId = node.getId();
                        nodes = node.getSubFolders();
                        continue subfolder;
                    }
                }
                throw new ResourceException(Response.Status.NOT_FOUND,
                    "Subfolder \"" + pathElement + "\" of \" " + path + "\" does not exist");
            }
        }

        return folderId;
    }
}
