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
package org.apromore.plugin.portal.accesscontrol.controllers;

import com.google.common.base.Strings;
import org.apromore.dao.model.Group;
import org.apromore.dao.model.Group.Type;
import org.apromore.dao.model.Usermetadata;
import org.apromore.dao.model.UsermetadataType;
import org.apromore.exception.UserNotFoundException;
import org.apromore.manager.client.ManagerService;
import org.apromore.plugin.portal.PortalContext;
import org.apromore.plugin.portal.accesscontrol.model.Artifact;
import org.apromore.plugin.portal.accesscontrol.model.Assignee;
import org.apromore.plugin.portal.accesscontrol.model.Assignment;
import org.apromore.portal.common.UserSessionManager;
import org.apromore.portal.common.notification.Notification;
import org.apromore.portal.model.*;
import org.apromore.portal.types.EventQueueTypes;
import org.apromore.service.AuthorizationService;
import org.apromore.service.SecurityService;
import org.apromore.service.UserMetadataService;
import org.apromore.util.AccessType;
import org.apromore.util.UserMetadataTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.json.JSONObject;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import java.util.*;

/**
 * Controller for handling share interface
 * Corresponds to components/access/access.zul
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class AccessController extends SelectorComposer<Div> {

    private static Logger LOGGER = LoggerFactory.getLogger(AccessController.class);

    @WireVariable("managerService")
    private ManagerService managerService;

    @WireVariable("securityService")
    private SecurityService securityService;

    @WireVariable("authorizationService")
    private AuthorizationService authorizationService;

    @WireVariable("userMetadataService")
    private UserMetadataService userMetadataService;

    Map<String, Object> argMap = (Map<String, Object>) Executions.getCurrent().getArg();
    private Object selectedItem = argMap.get("selectedItem");
    private UserType currentUser = (UserType) argMap.get("currentUser");
    Boolean autoInherit = (Boolean) argMap.get("autoInherit");
    Boolean showRelatedArtifacts = (Boolean) argMap.get("showRelatedArtifacts");
    private String userName;

    private Integer selectedItemId;
    private String selectedItemName;
    private Assignment selectedAssignment;

    private ListModelList<Assignee> candidateAssigneeModel;
    private ListModelList<Assignment> assignmentModel;
    private ListModelList<Artifact> artifactModel;
    Map<Group, AccessType> groupAccessTypeMap;
    Map<String, Assignment> assignmentMap;
    Map<String, Assignment> ownerMap;
    Map<Integer, Artifact> artifactMap;
    Map<String, ListModelList<Artifact>> groupArtifactsMap;

    @Wire("#selectedIconLog")
    Span selectedIconLog;

    @Wire("#selectedIconModel")
    Span selectedIconModel;

    @Wire("#selectedIconFolder")
    Span selectedIconFolder;

    @Wire("#selectedIconMetadata")
    Span selectedIconMetadata;

    @Wire("#selectedName")
    Label selectedName;

    @Wire("#relatedDepsWrapper")
    Div relatedDepsWrapper;

    @Wire("#relatedDependencies")
    Div relatedDependencies;

    @Wire("#candidateAssigneeCombobox")
    Combobox candidateAssigneeCombobox;

    @Wire("#candidateAssigneeAdd")
    Button candidateAssigneeAdd;

    @Wire("#btnApply")
    Button btnApply;

    @Wire("#assignmentListbox")
    Listbox assignmentListbox;

    @Wire("#artifactListbox")
    Listbox artifactListbox;

    @Wire("#editBtn")
    Button editBtn;

    @Wire("#umWarning")
    Div umWarning;

    private Div container;

    public AccessController() throws Exception {

        pullArgs();
        if (currentUser == null) {
            currentUser = UserSessionManager.getCurrentUser();
        }
        userName = currentUser.getUsername();
        selectedAssignment = null;
        groupArtifactsMap = new HashMap<String, ListModelList<Artifact>>();
    }

    private void pullArgs() throws Exception {
        Map<String, Object> argMap = (Map<String, Object>) Executions.getCurrent().getArg();
        selectedItem = argMap.get("selectedItem");
        currentUser = (UserType) argMap.get("currentUser");
        autoInherit = (Boolean) argMap.get("autoInherit");
        showRelatedArtifacts = (Boolean) argMap.get("showRelatedArtifacts");
    }

    private void checkShowRelatedArtifacts() {
        if (showRelatedArtifacts && isLogSelected()) {
            artifactListbox.setVisible(true);
        } else {
            artifactListbox.setVisible(false);
        }
    }

    @Override
    public void doAfterCompose(Div div) throws Exception {
        super.doAfterCompose(div);
        container = div;

        loadCandidateAssignee();
        setSelectedItem(selectedItem);

        assignmentListbox.addEventListener("onChange", new EventListener<Event>() {
            @Override
            public void onEvent(Event ev) {
                try {
                    ForwardEvent event = (ForwardEvent) ev;
                    InputEvent inputEvent = (InputEvent) event.getOrigin();
                    Combobox combobox = (Combobox) inputEvent.getTarget();
                    String access = (String) combobox.getValue();
                    String rowGuid = (String) combobox.getClientDataAttribute("id");
                    updateAssignment(rowGuid, access);
                } catch(Exception e) {
                    LOGGER.error("Something is wrong in updating access");
                    Messagebox.show("Unable to update permission", "Apromore", Messagebox.OK, Messagebox.ERROR);
                }

            }
        });

        editBtn.addEventListener("onRemove", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                JSONObject param = (JSONObject) event.getData();
                String rowGuid = (String) param.get("rowGuid");
                String name = (String) param.get("name");
                Assignment assignment = assignmentMap.get(rowGuid);
                if (ownerMap.containsKey(rowGuid) && ownerMap.size() == 1) {
                    ensureOneOwnerWarning();
                    return;
                }
                if (assignment != null) {
                    assignmentModel.remove(assignment);
                    assignmentMap.remove(rowGuid);
                    if (Objects.equals(assignment.getAccess(), AccessType.OWNER.getLabel())) {
                        ownerMap.remove(rowGuid);
                    }
                }
            }
        });

        editBtn.addEventListener("onIncludeMetadata", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                JSONObject param = (JSONObject) event.getData();
                String rowGuid = (String) param.get("rowGuid");
                String name = (String) param.get("name");
                Assignment assignment = assignmentMap.get(rowGuid);
                if (assignment != null) {
                    assignment.setShareUserMetadata(!assignment.isShareUserMetadata());
                    int index = assignmentModel.indexOf(assignment);
                    assignmentModel.set(index, assignment); // trigger change
                }
            }
        });

        EventQueues.lookup("accessControl", EventQueues.DESKTOP, true).subscribe(
            new EventListener() {
                public void onEvent(Event evt) {
                    if ("onSelect".equals(evt.getName())) {
                        Object selItem = evt.getData();
                        setSelectedItem(selItem);
                    }
                }
            });
    }

    public void destroy() {
        getSelf().detach();
        EventQueues.lookup("accessControl", EventQueues.DESKTOP, true).publish(new Event("onClose", null, null));
    }

    private boolean updateAssignment(String rowGuid, String access) {
        if (Strings.isNullOrEmpty(access) || Strings.isNullOrEmpty(rowGuid)) {
            LOGGER.error("New empty access assignment is returned");
            return false;
        }
        Assignment assignment = assignmentMap.get(rowGuid);
        if (assignment == null) {
            return false;
        }
        String oldAccess = assignment.getAccess();
        if (Strings.isNullOrEmpty(oldAccess)) {
            LOGGER.error("Previous access assignment is empty");
            return false;
        }
        if (oldAccess.equals(access)) { // No change please ignore
            return true;
        }
        String ownerLabel = AccessType.OWNER.getLabel();
        // If an attempt tries change the last owner access type
        if (ownerLabel.equals(oldAccess) && !ownerLabel.equals(access) && ownerMap.size() == 1 && ownerMap.containsKey(rowGuid)) {
            Clients.evalJavaScript("Ap.share.revertCombobox('" + rowGuid + "', '" + oldAccess + "')");
            ensureOneOwnerWarning();
            return false;
        }
        assignment.setAccess(access);
        if (ownerLabel.equals(access)) {
            ownerMap.put(rowGuid, assignment);
        } else if (ownerMap.containsKey(rowGuid)){
            ownerMap.remove(rowGuid);
        }
        return true;
    }

    private void ensureOneOwnerWarning() {
        Messagebox.show("You cannot remove the only owner for this file", "Apromore", Messagebox.OK, Messagebox.ERROR);
    }

    @SuppressWarnings("unchecked")
    private void loadCandidateAssignee() {
        List<Group> groups = securityService.findAllGroups();
        List<Assignee> candidates = new ArrayList<Assignee>();

        for (Group group : groups) {
            String groupName = group.getName();
            candidates.add(new Assignee(groupName, group.getRowGuid(), group.getType()));
        }
        candidateAssigneeModel = new ListModelList<>(candidates, false);
        candidateAssigneeModel.setMultiple(false);
        candidateAssigneeCombobox.setModel(ListModels.toListSubModel(candidateAssigneeModel, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String input = (String) o1;
                Assignee assignee = (Assignee) o2;
                return assignee.getName().toLowerCase().contains(input.toLowerCase()) ? 0 : 1;
            }
        }, 20));
    }

    private void loadAssignments(Map<Group, AccessType> groupAccessTypeMap) {
        List<Assignment> assignments = new ArrayList<Assignment>();
        assignmentMap = new HashMap<String, Assignment>();
        ownerMap = new HashMap<String, Assignment>();

        for (Map.Entry<Group, AccessType> entry : groupAccessTypeMap.entrySet()) {
            Group group = entry.getKey();
            AccessType accessType = entry.getValue();
            String rowGuid = group.getRowGuid();
            Assignment assignment = new Assignment(group.getName(), rowGuid, Type.USER, accessType.getLabel());
            assignments.add(assignment);
            assignmentMap.put(rowGuid, assignment);
            if (accessType == AccessType.OWNER) {
                ownerMap.put(rowGuid, assignment);
            }
        }
        assignmentModel = new ListModelList<>(assignments, false);
        assignmentListbox.setMultiple(false);
        assignmentListbox.setModel(assignmentModel);
    }

    private void clearAssignments() {
        assignmentMap = new HashMap<String, Assignment>();
        ownerMap = new HashMap<String, Assignment>();
        assignmentModel = new ListModelList<>();
        assignmentListbox.setMultiple(false);
        assignmentListbox.setModel(assignmentModel);
    }

    public boolean isLogSelected() {
        if (selectedItem == null) {
            return false;
        }
        return selectedItem.getClass().equals(LogSummaryType.class);
    }

    public void clearArtifacts() {
        artifactModel = new ListModelList<Artifact>();
        artifactMap = new HashMap<Integer, Artifact>();
        artifactListbox.setModel(artifactModel);
    }

    public void updateArtifacts(String rowGuid) {
        if (!isLogSelected()) {
            return;
        }
        artifactModel = new ListModelList<Artifact>();
        groupArtifactsMap.put(rowGuid, artifactModel);
        artifactMap = new HashMap<Integer, Artifact>();
        Set<Usermetadata> userMetadataSet = userMetadataService.getUserMetadataByLog(selectedItemId, UserMetadataTypeEnum.FILTER);
        userMetadataSet.addAll(userMetadataService.getUserMetadataByLog(selectedItemId, UserMetadataTypeEnum.DASHBOARD));
        for (Usermetadata userMetadata: userMetadataSet) {
            Integer id = userMetadata.getId();
            String name = userMetadata.getName();
            String updatedTime = Objects.equals(userMetadata.getUpdatedTime(), null) ?
                    userMetadata.getCreatedTime() :
                    userMetadata.getUpdatedTime();
            UsermetadataType usermetadataType = userMetadata.getUsermetadataType();
            Artifact artifact = new Artifact(id, name, updatedTime, usermetadataType);
            artifactModel.add(artifact);
            artifactMap.put(id, artifact);
        }
        // Set<Artifact> selectionSet = new HashSet<Artifact>();
        String selectedUserName = securityService.findGroupByRowGuid(rowGuid).getName();
        try {
            Set<Usermetadata> selectedUserMetadataSet = userMetadataService.getUserMetadataByUserAndLog(selectedUserName, selectedItemId, UserMetadataTypeEnum.FILTER);
            selectedUserMetadataSet.addAll(userMetadataService.getUserMetadataByUserAndLog(selectedUserName, selectedItemId, UserMetadataTypeEnum.DASHBOARD));
            for (Usermetadata userMetadata: selectedUserMetadataSet) {
                Integer id = userMetadata.getId();
                String name = userMetadata.getName();
                String updatedTime = userMetadata.getUpdatedTime();
                UsermetadataType usermetadataType = userMetadata.getUsermetadataType();
                Artifact artifact = new Artifact(id, name, updatedTime, usermetadataType);
                // selectionSet.add(artifact);
            }
        } catch (Exception e) {
            LOGGER.info("Cannot find usermeta data selection for the current user");
        }
        artifactModel.setMultiple(true);
        artifactListbox.setModel(artifactModel);
        // artifactModel.setSelection(selectionSet);
    }

    /**
     * Apply the changes in the access control by comparing assignment listbox with previous access control list
     */
    private void applyChanges() {
        Map<Group, AccessType> groupAccessTypeChanges = new HashMap<Group, AccessType>(groupAccessTypeMap);

        for (Assignment assignment : assignmentModel) {
            String name = assignment.getName();
            String rowGuid = assignment.getRowGuid();
            boolean shareUserMetadata = assignment.isShareUserMetadata();
            shareUserMetadata = false;
            AccessType accessType = AccessType.getAccessType(assignment.getAccess());
            Group group = securityService.findGroupByRowGuid(rowGuid);
            if (groupAccessTypeChanges.containsKey(group)) {
                AccessType orgAccessType = groupAccessTypeChanges.get(group);
                groupAccessTypeChanges.remove(group);
                // TODO: Future improvement, only update necessary excluding log and its artifacts
                // if (accessType == orgAccessType) {
                //    continue; // No update necessary as no change is detected
                // }
            }
            if (selectedItem instanceof FolderType) {
                authorizationService.saveFolderAccessType(selectedItemId, rowGuid, accessType);
            } else if (selectedItem instanceof ProcessSummaryType) {
                authorizationService.saveProcessAccessType(selectedItemId, rowGuid, accessType);
            } else if (selectedItem instanceof LogSummaryType) {
                authorizationService.saveLogAccessType(selectedItemId, rowGuid, accessType, shareUserMetadata);
                if (groupArtifactsMap.containsKey(rowGuid)) {
                    Set<Artifact> selectedArtifacts = groupArtifactsMap.get(rowGuid).getSelection();
                    for (Artifact artifact: selectedArtifacts) {
                        userMetadataService.saveUserMetadataAccessType(artifact.getId(), rowGuid, accessType);
                    }
                }
            } else if (selectedItem instanceof UserMetadataSummaryType) {
                authorizationService.saveUserMetadataAccessType(selectedItemId, rowGuid, accessType);
            } else {
                LOGGER.error("Unknown item type.");
            }
        }

        // Delete the remaining
        for (Map.Entry<Group, AccessType> entry : groupAccessTypeChanges.entrySet()) {
            Group group = entry.getKey();
            AccessType accessType = entry.getValue();
            String rowGuid = group.getRowGuid();
            String name = group.getName();
            if (selectedItem instanceof FolderType) {
                authorizationService.deleteFolderAccess(selectedItemId, rowGuid);
            } else if (selectedItem instanceof ProcessSummaryType) {
                authorizationService.deleteProcessAccess(selectedItemId, rowGuid);
            } else if (selectedItem instanceof LogSummaryType) {
                try {
                    authorizationService.deleteLogAccess(selectedItemId, rowGuid, name);
                } catch (UserNotFoundException e) {
                    LOGGER.error("User not found", e.getMessage(), e);
                    Messagebox.show("The user cannot be found.", "Delete access error", Messagebox.OK,
                            Messagebox.ERROR);
                }
            } else if (selectedItem instanceof UserMetadataSummaryType) {
                authorizationService.deleteUserMetadataAccess(selectedItemId, rowGuid);
            }
        }
    }

    private void loadRelatedDependencies() {
        relatedDepsWrapper.setVisible(false);
        relatedDependencies.getChildren().clear();
        relatedDependencies.appendChild(new Label("Log: Dependent log"));
        relatedDependencies.appendChild(new Label("Process Model: Dependent model"));
    }

    public void setSelectedItem(Object selectedItem) {
        Span selectedIcon;
        selectedIconFolder.setVisible(false);
        selectedIconLog.setVisible(false);
        selectedIconModel.setVisible(false);
        selectedIconMetadata.setVisible(false);
        this.selectedItem = selectedItem;

        checkShowRelatedArtifacts();
        clearArtifacts();
        if (selectedItem == null) {
            clearAssignments();
            btnApply.setDisabled(true);
            candidateAssigneeAdd.setDisabled(true);
            return;
        }
        btnApply.setDisabled(false);
        candidateAssigneeAdd.setDisabled(false);

        if (selectedItem instanceof FolderType) {
            FolderType folder = (FolderType) selectedItem;
            selectedItemId = folder.getId();
            selectedItemName = folder.getFolderName();
            groupAccessTypeMap = authorizationService.getFolderAccessType(selectedItemId);
            selectedIcon = selectedIconFolder;
        } else if (selectedItem instanceof ProcessSummaryType) {
            ProcessSummaryType process = (ProcessSummaryType) selectedItem;
            selectedItemId = process.getId();
            selectedItemName = process.getName();
            groupAccessTypeMap = authorizationService.getProcessAccessType(selectedItemId);
            selectedIcon = selectedIconModel;
        } else if (selectedItem instanceof LogSummaryType) {
            LogSummaryType log = (LogSummaryType) selectedItem;
            selectedItemId = log.getId();
            selectedItemName = log.getName();
            groupAccessTypeMap = authorizationService.getLogAccessType(selectedItemId);
            selectedIcon = selectedIconLog;
            // shareUM.setVisible(true);
        } else if (selectedItem instanceof UserMetadataSummaryType) {
            UserMetadataSummaryType usermetadata = (UserMetadataSummaryType) selectedItem;
            selectedItemId = usermetadata.getId();
            selectedItemName = usermetadata.getName();
            groupAccessTypeMap = authorizationService.getUserMetadataAccessType(selectedItemId);
            selectedIcon = selectedIconMetadata;
            umWarning.setVisible(true);
        } else {
            return;
        }
        loadRelatedDependencies();
        loadAssignments(groupAccessTypeMap);
        selectedIcon.setVisible(true);
        selectedName.setValue(selectedItemName);
    }

    @Listen("onClick = #candidateAssigneeAdd")
    public void onClickCandidateUserAdd() {
        Set<Assignee> assignees = candidateAssigneeModel.getSelection();
        if (assignees != null && assignees.size() == 1 && selectedItem != null && selectedItemId != null) {
            Assignee assignee = assignees.iterator().next();
            String rowGuid = assignee.getRowGuid();

            boolean showWarning = false;
            if (selectedItem instanceof UserMetadataSummaryType) {
                showWarning =
                        !userMetadataService.canAccessAssociatedLog(((UserMetadataSummaryType) selectedItem).getId(),
                                rowGuid);
            }

            Assignment assignment = new Assignment(assignee.getName(), rowGuid, assignee.getType(), AccessType.VIEWER.getLabel());
            assignment.setShowWarning(showWarning);
            if (!assignmentModel.contains(assignment)) {
                assignmentModel.add(assignment);
                assignmentMap.put(rowGuid, assignment);
            }
        }
    }

    @Listen("onSelect = #assignmentListbox")
    public void onSelectAssignmentListbox(SelectEvent event) throws Exception {
        if (!isLogSelected()) {
            return;
        }
        Set<Assignment> assignments = event.getSelectedObjects();
        if (assignments.size() == 1) {
            selectedAssignment = assignments.iterator().next();
            updateArtifacts(selectedAssignment.getRowGuid());
        } else {
            selectedAssignment = null;
            updateArtifacts(null);
        }
    }

    @Listen("onClick = #btnApply")
    public void onClickBtnApply() {
        applyChanges();
        destroy();
        Notification.info("Sharing is susccessfully applied");

        PortalContext portalContext = (PortalContext) Sessions.getCurrent().getAttribute("portalContext");
        String username = portalContext.getCurrentUser().getUsername();

        EventQueue eqAccessRight = EventQueues.lookup(EventQueueTypes.UPDATE_USERMETADATA,
                EventQueues.APPLICATION, true);
        eqAccessRight.publish(new Event("update_usermetadata", null, username));
    }

    @Listen("onClick = #btnCancel")
    public void onClickBtnCancel() {
        destroy();
    }

}
