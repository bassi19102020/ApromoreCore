/*-
 * #%L
 * This file is part of "Apromore Core".
 * 
 * Copyright (C) 2012 - 2017 Queensland University of Technology.
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

package org.apromore.portal.dialogController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apromore.plugin.editor.EditorPlugin;
import org.apromore.portal.common.Constants;
import org.apromore.portal.common.UserSessionManager;
import org.apromore.portal.context.EditorPluginResolver;
import org.apromore.portal.dialogController.dto.ApromoreSession;
import org.apromore.portal.model.EditSessionType;
import org.apromore.portal.model.ExportFormatResultType;
import org.apromore.portal.model.PluginMessages;
import org.apromore.portal.model.ProcessSummaryType;
import org.apromore.portal.model.VersionSummaryType;
import org.apromore.portal.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;

/**
 * ApromoreSession and ApromoreSession.EditSessionType represent data objects of the model being opened in the editor 
 * However, they don't contain the XML native model data which can be retrieved from the editor
 * Remember to update these data objects after every action on the model to keep it in a consistent state.
 * For example, after save as a new model, these data objects must be updated to the new model info.
 * @todo there is a duplication between ApromoreSession and EditSessionType, they need to be clean later.
 *  
 * @author Bruce Nguyen
 *
 */
public class BPMNEditorController extends BaseController {
    public static final String EVENT_MESSAGE_SAVE = "SaveEvent";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BPMNEditorController.class.getCanonicalName());
    private EventQueue<Event> qeBPMNEditor = EventQueues.lookup(Constants.EVENT_QUEUE_BPMN_EDITOR, EventQueues.SESSION, true);

    private MainController mainC;
    private ApromoreSession session;
    private EditSessionType editSession;
    private ProcessSummaryType process;
    private VersionSummaryType vst;
    boolean isNewProcess = false;

    @Inject private UserSessionManager userSessionManager;

    public BPMNEditorController() {
        super();
        if (userSessionManager.getCurrentUser() == null) {
        	throw new AssertionError("Cannot open the editor without any login user!");
        }

        String id = Executions.getCurrent().getParameter("id");
        isNewProcess = Boolean.valueOf(Executions.getCurrent().getParameter("newProcess"));
        if (id == null) {
            throw new AssertionError("No id parameter in URL");
        }

        session = userSessionManager.getEditSession(id);
        if (session == null) {
            // throw new AssertionError("No edit session associated with id " + id);
            throw new AssertionError("Your session has expired. Please close this browser tab and refresh the Portal tab");
        }

        editSession = session.getEditSession();
        mainC = session.getMainC();
        process = session.getProcess();
        vst = session.getVersion();
        
        Map<String, Object> param = new HashMap<>();
        try {
            PluginMessages pluginMessages = null;
            String bpmnXML = (String) session.get("bpmnXML");
            
            if(bpmnXML == null) {
            	if (isNewProcess) {
            		bpmnXML = "<?xml version='1.0' encoding='UTF-8'?>" +
            				  "<bpmn:definitions xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            				                    "xmlns:bpmn='http://www.omg.org/spec/BPMN/20100524/MODEL' "  +
            				                    "xmlns:bpmndi='http://www.omg.org/spec/BPMN/20100524/DI' " +
            				                    "xmlns:dc='http://www.omg.org/spec/DD/20100524/DC' " +
            				                    "targetNamespace='http://bpmn.io/schema/bpmn' " +
            				                    "id='Definitions_1'>" +
            				    "<bpmn:process id='Process_1' isExecutable='false'>" +
            				      "<bpmn:startEvent id='StartEvent_1'/>" +
            				    "</bpmn:process>" +
            				    "<bpmndi:BPMNDiagram id='BPMNDiagram_1'>" +
            				      "<bpmndi:BPMNPlane id='BPMNPlane_1' bpmnElement='Process_1'>" +
            				        "<bpmndi:BPMNShape id='_BPMNShape_StartEvent_2' bpmnElement='StartEvent_1'>" +
            				          "<dc:Bounds height='36.0' width='36.0' x='173.0' y='102.0'/>" +
            				        "</bpmndi:BPMNShape>" +
            				      "</bpmndi:BPMNPlane>" +
            				    "</bpmndi:BPMNDiagram>" +
            				  "</bpmn:definitions>";
            	}
            	else {
            		// Note: process models created by merging are not BPMN, cannot use processService.getBPMNRepresentation 
            		//bpmnXML = processService.getBPMNRepresentation(procName, procID, branch, version);
            		String annotation = (editSession.getAnnotation() == null) ? editSession.getNativeType() : editSession.getAnnotation();
            		
                    ExportFormatResultType exportResult =
                            getService().exportFormat(editSession.getProcessId(),
                            		editSession.getProcessName(),
                            		editSession.getOriginalBranchName(),
                            		editSession.getCurrentVersionNumber(),
                            		editSession.getNativeType(),
                            		editSession.getUsername());
                    bpmnXML = StreamUtil.convertStreamToString(exportResult.getNative().getInputStream());
                    param.put("doAutoLayout", "false");
            	}
            	
                param.put("bpmnXML",       escapeXML(bpmnXML));
                param.put("url",           getURL(editSession.getNativeType()));
                param.put("importPath",    getImportPath(editSession.getNativeType()));
                param.put("exportPath",    getExportPath(editSession.getNativeType()));
                param.put("editor",        "bpmneditor");
            } else {
                param.put("bpmnXML",       bpmnXML);
                param.put("url",           getURL("BPMN 2.0"));
                param.put("importPath",    getImportPath("BPMN 2.0"));
                param.put("exportPath",    getExportPath("BPMN 2.0"));
                param.put("editor",        "bpmneditor");
                param.put("doAutoLayout", "false");
            }
            
            this.setTitle(editSession.getProcessName() + " (" + "v" + editSession.getCurrentVersionNumber() + ")");
            
            if (mainC != null) {
                mainC.showPluginMessages(pluginMessages);
            }

            List<EditorPlugin> editorPlugins = EditorPluginResolver.resolve("bpmnEditorPlugins");
            param.put("plugins", editorPlugins);

            Executions.getCurrent().pushArg(param);

        } catch (Exception e) {
            LOGGER.error("",e);
            e.printStackTrace();
        }
        
        BPMNEditorController editorController = this;
        
        this.addEventListener("onSave", new EventListener<Event>() {
            @Override
            public void onEvent(final Event event) throws InterruptedException {
            	if (isNewProcess) {
            		new SaveAsDialogController(process, vst, session, false, eventToString(event));
            	}
            	else {
            		new SaveAsDialogController(process, vst, session, true, eventToString(event));
            	}
            }
        });
        
        this.addEventListener("onSaveAs", new EventListener<Event>() {
            @Override
            public void onEvent(final Event event) throws InterruptedException {
                new SaveAsDialogController(process, vst, session, false, eventToString(event));
            }
        });
        
        qeBPMNEditor.subscribe(
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        if (EVENT_MESSAGE_SAVE.equals(event.getName())) {
                            String[] data = (String[])event.getData();
                            setTitle(data[0], data[1]);
                            editorController.isNewProcess = false;
                        }
                    }
                });
    }
    
    private void setTitle(String processName, String versionNumber) {
        this.setTitle(processName + " (" + "v" + versionNumber + ")");
    }

    /**
     * @param json
     * @return the <var>json</var> escaped so that it can be quoted in Javascript.
     *     Specifically, it replaces apostrophes with \\u0027 and removes embedded newlines and leading and trailing whitespace.
     */
    private String escapeXML(String xml) {
    	return xml.replaceAll("(\\r|\\n|\\r\\n)+", " ").replace("'", "\\'");
    }
    
    /**
     * YAWL models package their event data as an array of {@link String}s, EPML packages it as a {@link String}; this function
     * hides the difference.
     *
     * @param event ZK event
     * @throws RuntimeException if the data associated with <var>event</var> is neither a {@link String} nor an array of {@link String}s
     */
    private static String eventToString(final Event event) {
        if (event.getData() instanceof String[]) {
            return ((String[]) event.getData())[0];
        }
        if (event.getData() instanceof String) {
            return (String) event.getData();
        }

        throw new RuntimeException("Unsupported class of event data: " + event.getData());
    }
    

}
