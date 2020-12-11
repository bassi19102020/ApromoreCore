/*-
 * #%L
 * This file is part of "Apromore Core".
 *
 * Copyright (C) 2020 University of Tartu
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

package org.apromore.plugin.portal.csvimporter;

import com.opencsv.CSVWriter;
import org.apache.commons.lang.StringUtils;
import org.apromore.dao.model.Log;
import org.apromore.exception.UserNotFoundException;
import org.apromore.plugin.portal.PortalContext;
import org.apromore.service.EventLogService;
import org.apromore.service.UserMetadataService;
import org.apromore.service.csvimporter.model.LogErrorReport;
import org.apromore.service.csvimporter.model.LogMetaData;
import org.apromore.service.csvimporter.model.LogModel;
import org.apromore.service.csvimporter.services.*;
import org.apromore.service.csvimporter.services.legacy.LogImporter;
import org.apromore.service.csvimporter.services.legacy.LogImporterProvider;
import org.apromore.util.UserMetadataTypeEnum;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.json.JSONObject;
import org.zkoss.util.Locales;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.*;

import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;

/**
 * Controller for <code>csvimporter.zul</code>.
 */
public class CSVImporterController extends SelectorComposer<Window> implements Constants {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVImporterController.class);

    /**
     * Attribute of the ZK session containing this controller's arguments.
     */
    static final String SESSION_ATTRIBUTE_KEY = "csvimport";

    private static final int ROW_INDEX_START_FROM = 1;

    // Fields injected from Spring beans/OSGi services
    private EventLogService eventLogService = (EventLogService) SpringUtil.getBean("eventLogService");
    private UserMetadataService userMetadataService = (UserMetadataService) SpringUtil.getBean("userMetadataService");

    /* This is the better way to pass parameters, but it only works when opening the ZUL within the same browser window.
    // Fields injected from the ZK execution
    private LogImporter logImporter = (LogImporter) Executions.getCurrent().getArg().get("logImporter");
    private MetaDataService MetaDataService = (MetaDataService) Executions.getCurrent().getArg().get("MetaDataService");
    private Media media = (Media) Executions.getCurrent().getArg().get("media");
    private JSONObject mappingJSON = (JSONObject) Executions.getCurrent().getArg().get("mappingJSON");
    private JSONObject mappingJSON = (JSONObject) Executions.getCurrent().getArg().get("mappingJSON");
    */

    // Fields injected from the ZK session
    private ParquetFactoryProvider parquetFactoryProvider = (ParquetFactoryProvider) ((Map) Sessions.getCurrent().getAttribute(SESSION_ATTRIBUTE_KEY)).get("parquetFactoryProvider");
    private LogImporterProvider logImporterProvider = (LogImporterProvider) ((Map) Sessions.getCurrent().getAttribute(SESSION_ATTRIBUTE_KEY)).get("logImporterProvider");
    private Media media = (Media) ((Map) Sessions.getCurrent().getAttribute(SESSION_ATTRIBUTE_KEY)).get("media");
    private PortalContext portalContext = (PortalContext) Sessions.getCurrent().getAttribute("portalContext");
    private JSONObject mappingJSON = (JSONObject) ((Map) Sessions.getCurrent().getAttribute(SESSION_ATTRIBUTE_KEY)).get("mappingJSON");

    // Fields injected from csvimporter.zul

    private @Wire("#mainWindow")
    Window window;
    private @Wire("#toXESButton")
    Button toXESButton;
    private @Wire("#toPublicXESButton")
    Button toPublicXESButton;
    private @Wire("#matchedMapping")
    Button matchedMapping;

    //Get Data layer config
    private final String propertyFile = "datalayer.config";
    private boolean useParquet;
    private File parquetFile;


    private LogMetaData logMetaData;
    private List<List<String>> sampleLog;

    private Div popUpBox;
    private Button[] formatBtns;
    private Span[] parsedIcons;
    private List<Listbox> dropDownLists;

    ParquetImporterFactory parquetImporterFactory;
    MetaDataService metaDataService;
    ParquetImporter parquetImporter;
    MetaDataUtilities metaDataUtilities;

    LogImporter logImporter;

    @Override
    public void doFinally() throws Exception {
        super.doFinally();
        // Populate the window
        try {
            parquetImporterFactory = parquetFactoryProvider.getParquetFactory(getMediaFormat(media));
            metaDataService = parquetImporterFactory.getMetaDataService();
            parquetImporter = parquetImporterFactory.getParquetImporter();
            metaDataUtilities = parquetImporterFactory.getMetaDataUtilities();
            logImporter = logImporterProvider.getLogReader(getMediaFormat(media));

            Properties props = new Properties();
            props.load(getClass().getClassLoader().getResourceAsStream(propertyFile));

            useParquet = Boolean.parseBoolean(props.getProperty("use.parquet"));

            File parquetDir = new File(props.getProperty("parquet.dir"));
            //make directory if not exist
            parquetDir.mkdirs();

            //Add timestamp to file name
            String fileSuffix = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            parquetFile = new File(parquetDir.getPath()
                    + File.separator + media.getName().replace("." + getMediaFormat(media), fileSuffix + ".parquet"));

            Combobox setEncoding = (Combobox) window.getFellow(setEncodingId);
            setEncoding.setModel(new ListModelList<>(fileEncoding));

            setEncoding.addEventListener("onSelect", event -> {
                metaDataService.validateLog(getInputSream(media), getFileEncoding());
                this.logMetaData = metaDataService.extractMetadata(getInputSream(media), getFileEncoding());
                this.sampleLog = metaDataService.generateSampleLog(getInputSream(media), logSampleSize, getFileEncoding());
                this.logMetaData = metaDataUtilities.processMetaData(this.logMetaData, this.sampleLog);

                if (logMetaData != null && sampleLog.size() > 0) setUpUI();
            });

            Combobox setTimeZone = (Combobox) window.getFellow(setTimeZoneId);

            Calendar cal = Calendar.getInstance();
            TimeZone timeZone = cal.getTimeZone();
            int offset = timeZone.getRawOffset() / 1000;
            int hour = offset / 3600;
            int minutes = (offset % 3600) / 60;
            ListModelList model = getTimeZoneList();
            String defaultValue = String.format("(GMT%+d:%02d) %s", hour, minutes, timeZone.getID());

            model.addToSelection(defaultValue);
            setTimeZone.setModel(model);
            setTimeZone.setValue(defaultValue);

            setTimeZone.addEventListener("onSelect", event -> {
                if (getTimeZone() == null) {
                    this.logMetaData.setTimeZone(defaultValue.split(" ")[1]);
                } else {
                    this.logMetaData.setTimeZone(getTimeZone());
                }
            });

            setTimeZone.addEventListener("onClientUpdate", event -> {
                JSONObject param = (JSONObject) event.getData();
                String gmtOffset = (String) param.get("offset");
                String tz = (String) param.get("tz");
                String value = String.format("(%s) %s", gmtOffset, tz);
                setTimeZone.setValue(value);
                this.logMetaData.setTimeZone(value);
            });

            metaDataService.validateLog(getInputSream(media), getFileEncoding());
            LogMetaData tempLogMetaData = metaDataService.extractMetadata(getInputSream(media), getFileEncoding());
            this.sampleLog = metaDataService.generateSampleLog(getInputSream(media), logSampleSize, getFileEncoding());
            tempLogMetaData = metaDataUtilities.processMetaData(tempLogMetaData, this.sampleLog);

            if (mappingJSON != null) {
//        jsonMapping.put("header", logSample.getHeader());
//        jsonMapping.put("caseIdPos", logSample.getCaseIdPos());
//        jsonMapping.put("activityPos", logSample.getActivityPos());
//        jsonMapping.put("endTimestampPos", logSample.getEndTimestampPos());
//        jsonMapping.put("startTimestampPos", logSample.getStartTimestampPos());
//        jsonMapping.put("resourcePos", logSample.getResourcePos());
//        jsonMapping.put("caseAttributesPos", logSample.getCaseAttributesPos());
//        jsonMapping.put("eventAttributesPos", logSample.getEventAttributesPos());
//        jsonMapping.put("otherTimestamps", logSample.getOtherTimestamps());
//        jsonMapping.put("ignoredPos", logSample.getIgnoredPos());
//        jsonMapping.put("endTimestampFormat", logSample.getEndTimestampFormat());
//        jsonMapping.put("startTimestampFormat", logSample.getStartTimestampFormat());


                tempLogMetaData.setCaseIdPos((Integer) mappingJSON.get("caseIdPos"));
                tempLogMetaData.setActivityPos((Integer) mappingJSON.get("activityPos"));
                tempLogMetaData.setEndTimestampFormat((String) mappingJSON.get("endTimestampFormat"));
                tempLogMetaData.setEndTimestampPos((Integer) mappingJSON.get("endTimestampPos"));
                tempLogMetaData.setStartTimestampFormat((String) mappingJSON.get("startTimestampFormat"));
                tempLogMetaData.setStartTimestampPos((Integer) mappingJSON.get("startTimestampPos"));
                tempLogMetaData.setResourcePos((Integer) mappingJSON.get("resourcePos"));
//                    tempSample.getHeader().addAll((List<String>) mappingJSON.get("header"));
                tempLogMetaData.getEventAttributesPos().clear();
                tempLogMetaData.getEventAttributesPos().addAll((List<Integer>) mappingJSON.get(
                        "eventAttributesPos"));
                tempLogMetaData.getCaseAttributesPos().clear();
                tempLogMetaData.getCaseAttributesPos().addAll((List<Integer>) mappingJSON.get("caseAttributesPos"));
                tempLogMetaData.getIgnoredPos().clear();
                tempLogMetaData.getIgnoredPos().addAll((List<Integer>) mappingJSON.get("ignoredPos"));
//                    tempSample.getLines().addAll((List<List<String>>) mappingJSON.get("ignoredPos"));


                Object otherTimestamps = mappingJSON.get("otherTimestamps");
//                    Map<Integer, String> otherTimestampsObject  = (Map<Integer, String>) JSONValue.parse(mappingJSON.get("otherTimestamps").toString()) ;
//                    Object otherTimestampsObject = JSONValue.parse(mappingJSON.get(
//                            "otherTimestamps").toString());


                Map<Integer, String> otherTimestampsMap = (Map<Integer, String>) otherTimestamps;

                Map<Integer, String> otherTimestampsMap2 = new HashMap<>();
//                    otherTimestampsMap2.putAll(otherTimestampsMap);

                Iterator it = otherTimestampsMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    Object key = entry.getKey();
                    if (key != null) {
                        otherTimestampsMap2.put(Integer.parseInt(key.toString()), otherTimestampsMap.get(key));
                    }
                }

//                    ObjectMapper mapper = new ObjectMapper();
//                    otherTimestampsMap = mapper.readValue(mappingJSON.get("otherTimestamps").toString(), Map.class);

//                    for (int i = 0; i < otherTimestamps.size(); i++) {
//                        String format = otherTimestamps.get(i);
//                                jsoncargo.getJsonObject(i).getString("type");
//                        Integer postion = jsoncargo.getJsonObject(i).getInt("amount");
//                        otherTimestampsMap.put(type, amount);
//                    }

                tempLogMetaData.getOtherTimestamps().clear();
                tempLogMetaData.getOtherTimestamps().putAll(otherTimestampsMap2);

            }

            if (getTimeZone() == null) {
                tempLogMetaData.setTimeZone(defaultValue.split(" ")[1]);
            } else {
                tempLogMetaData.setTimeZone(getTimeZone());
            }
            this.logMetaData = tempLogMetaData;

            if (this.logMetaData != null) {

                //TODO:

                //Attempt 2
//                    handleMatchedMapping();

                setUpUI();
                toXESButton.setDisabled(false);
                toPublicXESButton.setDisabled(false);
                matchedMapping.setDisabled(false);
            }

        } catch (Exception e) {
            Messagebox.show(getLabels().getString("failed_to_read_log") + " " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR, event -> close());
        }
        Clients.evalJavaScript("Ap.common.pullClientTimeZone()");
    }

    //    @Listen("onClick = button#matchedMapping")
    //Create a dialog to ask for user option regarding matched schema mapping
    private void handleMatchedMapping() throws IOException {

        Window matchedMappingPopUp = (Window) portalContext.getUI().createComponent(getClass().getClassLoader(), "zul" +
                "/matchedMapping.zul", null, null);
        matchedMappingPopUp.doModal();
    }

    @Listen("onClick = #cancelButton")
    public void onClickCancelBtn(MouseEvent event) {
        close();
    }

    @Listen("onClick = #setOtherAll")
    public void ignoreToEvent(MouseEvent event) {
        Listbox lb = (Listbox) window.getFellow(String.valueOf(0));
        int eventAttributeIndex = lb.getIndexOfItem((Listitem) lb.getFellow(eventAttributeLabel));
        int ignoreAttributeIndex = lb.getIndexOfItem((Listitem) lb.getFellow(ignoreLabel));

        for (int pos = 0; pos < logMetaData.getHeader().size(); pos++) {
            lb = (Listbox) window.getFellow(String.valueOf(pos));

            if (lb.getSelectedIndex() == ignoreAttributeIndex) {
                logMetaData.getIgnoredPos().remove(Integer.valueOf(pos));
                logMetaData.getEventAttributesPos().add(pos);
                lb.setSelectedIndex(eventAttributeIndex);
            }
        }
    }

    @Listen("onClick = #setIgnoreAll")
    public void eventToIgnore(MouseEvent event) {
        Listbox lb = (Listbox) window.getFellow(String.valueOf(0));
        int eventAttributeIndex = lb.getIndexOfItem((Listitem) lb.getFellow(eventAttributeLabel));
        int ignoreAttributeIndex = lb.getIndexOfItem((Listitem) lb.getFellow(ignoreLabel));

        for (int pos = 0; pos < logMetaData.getHeader().size(); pos++) {
            lb = (Listbox) window.getFellow(String.valueOf(pos));
            if (lb.getSelectedIndex() == eventAttributeIndex) {
                logMetaData.getEventAttributesPos().remove(Integer.valueOf(pos));
                logMetaData.getIgnoredPos().add(pos);
                lb.setSelectedIndex(ignoreAttributeIndex);
            }
        }
    }

    @Listen("onClick = #toXESButton; onClick = #toPublicXESButton")
    public void convertToXes(MouseEvent event) {

        StringBuilder headNOTDefined = validateUniqueAttributes();

        if (headNOTDefined.length() != 0) {
            Messagebox.show(headNOTDefined.toString(), getLabels().getString("missing_fields"), Messagebox.OK, Messagebox.ERROR);
        } else {
            try {

                //TODO: persist mapping
//                storeMappingAsJSON(media, logMetaData);

                LogModel logModel;
                if (useParquet) {
                    logModel = parquetImporter.importParqeuetFile(
                            getInputSream(media),
                            logMetaData,
                            getFileEncoding(),
                            parquetFile,
                            false);
                } else {
                    logModel = logImporter
                            .importLog(getInputSream(media),
                                    logMetaData,
                                    getFileEncoding(),
                                    true,
                                    portalContext.getCurrentUser().getUsername(),
                                    portalContext.getCurrentFolder() == null ? 0 : portalContext.getCurrentFolder().getId(),
                                    media.getName().replaceFirst("[.][^.]+$", ""));
                }


                if (logModel != null) {
                    List<LogErrorReport> errorReport = logModel.getLogErrorReport();
                    boolean isLogPublic = "toPublicXESButton".equals(event.getTarget().getId());

                    if (errorReport.isEmpty()) {
                        saveXLog(logModel, isLogPublic);
                    } else {
                        handleInvalidData(logModel, isLogPublic);
                    }
                }

            } catch (Exception e) {
                Messagebox.show(getLabels().getString("error") + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void storeMappingAsJSON(Media media, LogMetaData logMetaData, Log log) throws UserNotFoundException {

        String username = portalContext.getCurrentUser().getUsername();

        String jsonStr = "";

        // Creating Object of ObjectMapper define in Jakson Api
        ObjectMapper Obj = new ObjectMapper();
        try {
            jsonStr = Obj.writeValueAsString(logMetaData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        userMetadataService.saveUserMetadataLinkedToOneLog("Default CSV schema mapping name", jsonStr,
                UserMetadataTypeEnum.CSV_IMPORTER, username,
                log.getId());

    }

    public ResourceBundle getLabels() {
        return ResourceBundle.getBundle("WEB-INF.zk-label", Locales.getCurrent(), getClass().getClassLoader());
    }

    // Internal methods handling page setup (doFinally)

    private String getFileEncoding() {
        Combobox setEncoding = (Combobox) window.getFellow(setEncodingId);
        return setEncoding.getValue().contains(" ")
                ? setEncoding.getValue().substring(0, setEncoding.getValue().indexOf(' '))
                : setEncoding.getValue();
    }

    private String getTimeZone() {
        Combobox setTimeZone = (Combobox) window.getFellow(setTimeZoneId);
        return setTimeZone.getValue().split(" ")[1];
    }

    private void setUpUI() {
        // Set up window size
        if (logMetaData.getHeader().size() > 8) {
            window.setMaximizable(true);
            window.setMaximized(true);
        } else {
            window.setMaximizable(false);
            int size = indexColumnWidth + logMetaData.getHeader().size() * columnWidth + 35;
            window.setWidth(size + "px");
        }
        window.setTitle("CSV Importer - " + media.getName());

        setDropDownLists();
        setCSVGrid();
        renderGridContent();
        setPopUpFormatBox();
    }

    private void setCSVGrid() {
        Grid myGrid = (Grid) window.getFellow(myGridId);

        myGrid.getChildren().clear();
        (new Columns()).setParent(myGrid);
        myGrid.getColumns().getChildren().clear();


        // dropdown lists
        Auxhead optionHead = new Auxhead();
        Auxheader index = new Auxheader();
        optionHead.appendChild(index);
        for (Listbox list : dropDownLists) {
            Auxheader listHeader = new Auxheader();
            listHeader.appendChild(list);
            optionHead.appendChild(listHeader);
        }
        myGrid.appendChild(optionHead);


        // index column
        Column indexCol = new Column();
        indexCol.setWidth(indexColumnWidth + "px");
        indexCol.setValue("");
        indexCol.setLabel("#");
        indexCol.setAlign("center");
        myGrid.getColumns().appendChild(indexCol);


        // set columns
        Button[] formatBtns = new Button[logMetaData.getHeader().size()];
        Span[] parsedIcons = new Span[logMetaData.getHeader().size()];
        for (int pos = 0; pos < logMetaData.getHeader().size(); pos++) {
            Column newColumn = new Column();
            String label = logMetaData.getHeader().get(pos);
            newColumn.setValue(label);
            newColumn.setLabel(label);
            int labelLen = (label.length() * 14) + 20;
            if (labelLen < columnWidth) {
                labelLen = columnWidth;
            }
            newColumn.setWidth(labelLen + "px");
            newColumn.setAlign("center");

            Button formatBtn = new Button();
            Span parsedIcon = new Span();

            if (pos == logMetaData.getEndTimestampPos() || pos == logMetaData.getStartTimestampPos() || logMetaData.getOtherTimestamps().containsKey(pos)) {
                showFormatBtn(formatBtn);
                showAutoParsedGreenIcon(parsedIcon);
            } else {
                hideFormatBtn(formatBtn);
            }

            formatBtn.setIconSclass("z-icon-wrench");

            final int fi = pos;
            formatBtn.addEventListener("onClick", event -> {
                openPopUpBox(fi);

//                MouseEvent me = (MouseEvent) event;
//                int x = me.getPageX();
//                int y = me.getPageY();
//                w.setLeft((x - 180) + "px");
//                w.setTop((y + 120) + "px");
            });
            formatBtns[pos] = formatBtn;
            parsedIcons[pos] = parsedIcon;

            newColumn.appendChild(parsedIcon);
            newColumn.appendChild(formatBtn);
            myGrid.getColumns().appendChild(newColumn);
            myGrid.getColumns().setSizable(true);
        }

        this.formatBtns = formatBtns;
        this.parsedIcons = parsedIcons;
    }

    private void renderGridContent() {
        Grid myGrid = (Grid) window.getFellow(myGridId);
        ListModelList<String[]> indexedResult = new ListModelList<>();

        int index = ROW_INDEX_START_FROM;
        for (List<String> myLine : sampleLog) {
            List<String> withIndex = new ArrayList<>();
            withIndex.add(String.valueOf(index));
            index++;
            withIndex.addAll(myLine);
            String[] s = withIndex.toArray(new String[0]);
            indexedResult.add(s);
        }
        if (logSampleSize <= sampleLog.size()) {
            indexedResult.add(new String[]{"..", "...", "...."});
        }
        myGrid.setModel(indexedResult);

        //set grid row renderer
        GridRendererController rowRenderer = new GridRendererController();
        rowRenderer.setAttribWidth(columnWidth);
        myGrid.setRowRenderer(rowRenderer);
    }

    private void setPopUpFormatBox() {
        Div popUPBox = (Div) window.getFellow(popUpDivId);
        if (popUPBox != null) {
            popUPBox.getChildren().clear();
        }
        Popup helpP = (Popup) window.getFellow(popUpHelpId);

        for (int pos = 0; pos < logMetaData.getHeader().size(); pos++) {
            Window item = new Window();
            item.setId(popUpFormatWindowId + pos);
            item.setWidth(columnWidth + "px");
            item.setMinheight(100);
            item.setClass("p-1");
            item.setBorder("normal");
            item.setStyle("visibility: hidden;");

            Button sp = new Button();
            sp.setStyle("margin-right:1px; float: right; line-height: 10px; min-height: 5px; padding:3px;");
            sp.setIconSclass("z-icon-times");

            A hidelink = new A();
            hidelink.appendChild(sp);
            sp.addEventListener("onClick", (Event event) -> item.setStyle(item.getStyle().replace("visible", "hidden")));

            Label popUpLabel = new Label();
            popUpLabel.setId(popUpLabelId + pos);
            if (pos == logMetaData.getEndTimestampPos() || pos == logMetaData.getStartTimestampPos() || logMetaData.getOtherTimestamps().containsKey(pos)) {
                setPopUpLabel(pos, Parsed.AUTO, popUpLabel);
            }

            Textbox textbox = new Textbox();
            textbox.setId(popUpTextBoxId + pos);
            textbox.setWidth("98%");
            textbox.setPlaceholder("dd-MM-yyyy HH:mm:ss");
            if (pos == logMetaData.getEndTimestampPos()) {
                textbox.setPlaceholder(logMetaData.getEndTimestampFormat());
                textbox.setValue(logMetaData.getEndTimestampFormat());
            } else if (pos == logMetaData.getStartTimestampPos()) {
                textbox.setPlaceholder(logMetaData.getStartTimestampFormat());
                textbox.setValue(logMetaData.getStartTimestampFormat());
            } else if (logMetaData.getOtherTimestamps().containsKey(pos)) {
                textbox.setPlaceholder(logMetaData.getOtherTimestamps().get(pos));
                textbox.setValue(logMetaData.getOtherTimestamps().get(pos));
            }

            textbox.setPopup(helpP);
            textbox.setClientAttribute("spellcheck", "false");
            textbox.setPopupAttributes(helpP, "after_start", "", "", "toggle");

            textbox.addEventListener("onChanging", (InputEvent event) -> {
                int colPos = Integer.parseInt(textbox.getId().replace(popUpTextBoxId, ""));
                Listbox box = dropDownLists.get(colPos);
                String selected = box.getSelectedItem().getValue();
                resetSelect(colPos);

                if (StringUtils.isBlank(event.getValue())) {
//                    if (logMetaData.isParsable(colPos)) {
//                        parsedAuto(colPos, selected);
//                    } else {
                    textbox.setValue("");
                    textbox.setPlaceholder(getLabels().getString("specify_timestamp_format"));
                    failedToParse(colPos);
//                    }
                } else {
                    String format = event.getValue();
                    try {
                        if (metaDataUtilities.isTimestamp(colPos, format, sampleLog)) {
                            parsedManual(colPos, selected, format);
                        } else {
                            failedToParse(colPos);
                        }
                    } catch (IllegalArgumentException e) {
                        failedToParse(colPos);
                        throw e;
                    } catch (Exception e) {
                        failedToParse(colPos);
                        throw new Exception(e.getMessage());
                    }
                }
            });

            item.appendChild(hidelink);
            item.appendChild(popUpLabel);
            item.appendChild(textbox);

            popUPBox.appendChild(item);
        }
        popUPBox.clone();

        this.popUpBox = popUPBox;
    }

    private void setDropDownLists() {

        List<Listbox> menuDropDownLists = new ArrayList<>();
        LinkedHashMap<String, String> menuItems = new LinkedHashMap<>();

        menuItems.put(caseIdLabel, getLabels().getString("case_id"));
        menuItems.put(activityLabel, getLabels().getString("activity"));
        menuItems.put(endTimestampLabel, getLabels().getString("end_timestamp"));
        menuItems.put(startTimestampLabel, getLabels().getString("start_timestamp"));
        menuItems.put(otherTimestampLabel, getLabels().getString("other_timestamp"));
        menuItems.put(resourceLabel, getLabels().getString("resource"));
        menuItems.put(caseAttributeLabel, getLabels().getString("case_attribute"));
        menuItems.put(eventAttributeLabel, getLabels().getString("event_attribute"));
        menuItems.put(ignoreLabel, getLabels().getString("ignore_attribute"));


        for (int pos = 0; pos < logMetaData.getHeader().size(); pos++) {
            Listbox box = new Listbox();
            box.setMold("select"); // set listBox to select mode
            box.setId(String.valueOf(pos)); // set id of list as column position.
            box.setWidth(columnWidth - 20 + "px");

            for (Map.Entry<String, String> myItem : menuItems.entrySet()) {
                Listitem item = new Listitem();
                item.setValue(myItem.getKey());
                item.setLabel(myItem.getValue());
                item.setId(myItem.getKey());

                if ((box.getSelectedItem() == null) && (
                        (myItem.getKey().equals(caseIdLabel) && logMetaData.getCaseIdPos() == pos) ||
                                (myItem.getKey().equals(activityLabel) && logMetaData.getActivityPos() == pos) ||
                                (myItem.getKey().equals(endTimestampLabel) && logMetaData.getEndTimestampPos() == pos)) ||
                        (myItem.getKey().equals(startTimestampLabel) && logMetaData.getStartTimestampPos() == pos) ||
                        (myItem.getKey().equals(otherTimestampLabel) && ((Map<Integer, String>) logMetaData.getOtherTimestamps()).containsKey(pos)) ||
                        (myItem.getKey().equals(resourceLabel) && logMetaData.getResourcePos() == pos) ||
                        (myItem.getKey().equals(caseAttributeLabel) && logMetaData.getCaseAttributesPos().contains(pos)) ||
                        (myItem.getKey().equals(eventAttributeLabel) && logMetaData.getEventAttributesPos().contains(pos)) ||
                        (myItem.getKey().equals(ignoreLabel) && logMetaData.getIgnoredPos().contains(pos))
                ) {
                    item.setSelected(true);
                }
                box.appendChild(item);
            }


            box.addEventListener("onSelect", (Event event) -> {
                String selected = box.getSelectedItem().getValue();
                int colPos = Integer.parseInt(box.getId());

                resetSelect(colPos);
                closePopUpBox(colPos);
                hideFormatBtn(formatBtns[colPos]);
                hideParsedIcon(parsedIcons[colPos]);

                switch (selected) {
                    case caseIdLabel:
                        resetUniqueAttribute(logMetaData.getCaseIdPos());
                        logMetaData.setCaseIdPos(colPos);
                        break;
                    case activityLabel:
                        resetUniqueAttribute(logMetaData.getActivityPos());
                        logMetaData.setActivityPos(colPos);
                        break;
                    case endTimestampLabel:
                        resetUniqueAttribute(logMetaData.getEndTimestampPos());
                        timestampSelected(colPos, selected);
                        break;
                    case startTimestampLabel:
                        resetUniqueAttribute(logMetaData.getStartTimestampPos());
                        timestampSelected(colPos, selected);
                        break;
                    case resourceLabel:
                        resetUniqueAttribute(logMetaData.getResourcePos());
                        logMetaData.setResourcePos(colPos);
                        break;
                    case otherTimestampLabel:
                        timestampSelected(colPos, selected);
                        break;
                    case caseAttributeLabel:
                        logMetaData.getCaseAttributesPos().add(colPos);
                        break;
                    case eventAttributeLabel:
                        logMetaData.getEventAttributesPos().add(colPos);
                        break;
                    case ignoreLabel:
                        logMetaData.getIgnoredPos().add(colPos);
                        break;
                    default:
                }
            });

            menuDropDownLists.add(box);
        }

        this.dropDownLists = menuDropDownLists;
    }

    private void resetUniqueAttribute(int oldColPos) {
        if (oldColPos != -1) {
            // reset value of the unique attribute
            resetSelect(oldColPos);
            closePopUpBox(oldColPos);
            hideFormatBtn(formatBtns[oldColPos]);
            hideParsedIcon(parsedIcons[oldColPos]);

            // set it as event attribute
            Listbox lb = (Listbox) window.getFellow(String.valueOf(0));
            int eventAttributeIndex = lb.getIndexOfItem((Listitem) lb.getFellow(eventAttributeLabel));
            Listbox oldBox = dropDownLists.get(oldColPos);
            oldBox.setSelectedIndex(eventAttributeIndex);
            logMetaData.getEventAttributesPos().add(oldColPos);
        }
    }

    private void resetSelect(int pos) {
        // reset value for old select
        if (logMetaData.getCaseIdPos() == pos) {
            logMetaData.setCaseIdPos(-1);
        } else if (logMetaData.getActivityPos() == pos) {
            logMetaData.setActivityPos(-1);
        } else if (logMetaData.getEndTimestampPos() == pos) {
            logMetaData.setEndTimestampPos(-1);
        } else if (logMetaData.getStartTimestampPos() == pos) {
            logMetaData.setStartTimestampPos(-1);
        } else if (logMetaData.getResourcePos() == pos) {
            logMetaData.setResourcePos(-1);
        } else if (logMetaData.getOtherTimestamps().containsKey(pos)) {
            logMetaData.getOtherTimestamps().remove(pos);
        } else if (logMetaData.getIgnoredPos().contains(pos)) {
            logMetaData.getIgnoredPos().remove(Integer.valueOf(pos));
        } else if (logMetaData.getCaseAttributesPos().contains(pos)) {
            logMetaData.getCaseAttributesPos().remove(Integer.valueOf(pos));
        } else if (logMetaData.getEventAttributesPos().contains(pos)) {
            logMetaData.getEventAttributesPos().remove(Integer.valueOf(pos));
        }
    }

    private void timestampSelected(int colPos, String selected) {
        showFormatBtn(formatBtns[colPos]);
        String possibleFormat = getPopUpFormatText(colPos);
        if (possibleFormat != null && !possibleFormat.isEmpty() && metaDataUtilities.isTimestamp(colPos, possibleFormat, sampleLog)) {
            parsedManual(colPos, selected, possibleFormat);
        } else if (metaDataUtilities.isTimestamp(colPos, sampleLog)) {
            parsedAuto(colPos, selected);
        } else {
            failedToParse(colPos);
        }
    }

    private void parsedManual(int colPos, String selected, String format) {
        updateTimestampPos(colPos, selected, format);
        setPopUpLabel(colPos, Parsed.MANUAL, null);
    }

    private void parsedAuto(int colPos, String selected) {
        updateTimestampPos(colPos, selected, null);
        setPopUpLabel(colPos, Parsed.AUTO, null);
        setPopUpFormatText(colPos, "");
    }

    private void failedToParse(int colPos) {
        logMetaData.getEventAttributesPos().add(colPos);
        setPopUpLabel(colPos, Parsed.FAILED, null);
        openPopUpBox(colPos);
    }

    private void updateTimestampPos(int pos, String timestampLabel, String format) {
        if (timestampLabel.equals(endTimestampLabel)) {
            logMetaData.setEndTimestampPos(pos);
            logMetaData.setEndTimestampFormat(format);
        } else if (timestampLabel.equals(startTimestampLabel)) {
            logMetaData.setStartTimestampPos(pos);
            logMetaData.setStartTimestampFormat(format);
        } else if (timestampLabel.equals(otherTimestampLabel)) {
            logMetaData.getOtherTimestamps().put(pos, format);
        }
    }

    private void openPopUpBox(int pos) {
        Window myPopUp = (Window) popUpBox.getFellow(popUpFormatWindowId + pos);
        myPopUp.setStyle(myPopUp.getStyle().replace("hidden", "visible"));
        Clients.evalJavaScript("adjustPos(" + pos + ")");
    }

    private void closePopUpBox(int pos) {
        Window myPopUp = (Window) popUpBox.getFellow(popUpFormatWindowId + pos);
        myPopUp.setStyle(myPopUp.getStyle().replace("visible", "hidden"));
    }

    private void setPopUpLabel(int pos, Enum type, Label check_lbl) {
        if (check_lbl == null) {
            Window myPopUp = (Window) popUpBox.getFellow(popUpFormatWindowId + pos);
            check_lbl = (Label) myPopUp.getFellow(popUpLabelId + pos);
        }

        if (type == Parsed.AUTO) {
            check_lbl.setZclass(greenLabelCSS);
            check_lbl.setValue(parsedAutoMessage);
            check_lbl.setMultiline(true);
            showAutoParsedGreenIcon(parsedIcons[pos]);
        } else if (type == Parsed.MANUAL) {
            check_lbl.setZclass(greenLabelCSS);
            check_lbl.setValue(parsedMessage);
            showManualParsedGreenIcon(parsedIcons[pos]);
        } else if (type == Parsed.FAILED) {
            check_lbl.setZclass(redLabelCSS);
            check_lbl.setValue(couldNotParseMessage);
            showParsedRedIcon(parsedIcons[pos]);
        }
    }

    private void showFormatBtn(Button myButton) {
        myButton.setSclass("ap-csv-importer-format-icon");
        myButton.setTooltiptext(specifyTimestampformat);
    }

    private void hideFormatBtn(Button myButton) {
        myButton.setSclass("ap-csv-importer-format-icon ap-hidden");
    }

    private void showAutoParsedGreenIcon(Span parsedIcon) {
        parsedIcon.setSclass("ap-csv-importer-parsed-icon z-icon-check-circle");
        parsedIcon.setTooltip(autoParsed);
    }

    private void showManualParsedGreenIcon(Span parsedIcon) {
        parsedIcon.setSclass("ap-csv-importer-parsed-icon z-icon-check-circle");
        parsedIcon.setTooltip(manualParsed);
    }

    private void showParsedRedIcon(Span parsedIcon) {
        parsedIcon.setSclass("ap-csv-importer-failedParse-icon z-icon-times-circle");
        parsedIcon.setTooltip(errorParsing);
    }

    private void hideParsedIcon(Span parsedIcon) {
        parsedIcon.setSclass("ap-hidden");
    }

    private String getPopUpFormatText(int pos) {
        Window myPopUp = (Window) popUpBox.getFellow(popUpFormatWindowId + pos);
        Textbox txt = (Textbox) myPopUp.getFellow(popUpTextBoxId + pos);
        return txt.getValue();
    }

    private void setPopUpFormatText(int pos, String text) {
        Window myPopUp = (Window) popUpBox.getFellow(popUpFormatWindowId + pos);
        Textbox txt = (Textbox) myPopUp.getFellow(popUpTextBoxId + pos);
        txt.setValue(text);
    }


    // Internal methods supporting event handlers (@Listen)

    private void close() {
        window.detach();
        window.invalidate();
    }

    private StringBuilder validateUniqueAttributes() {
        StringBuilder importMessage = new StringBuilder();
        String mess = getLabels().getString("no_attribute_has_been_selected_as");

        if (logMetaData.getCaseIdPos() == -1) {
            importMessage.append(mess).append(getLabels().getString("case_id"));
        }
        if (logMetaData.getActivityPos() == -1) {
            if (importMessage.length() == 0) {
                importMessage.append(mess).append(getLabels().getString("activity"));
            } else {
                importMessage.append(System.lineSeparator()).append(System.lineSeparator()).append(mess).append(getLabels().getString("activity"));
            }
        }
        if (logMetaData.getEndTimestampPos() == -1) {
            if (importMessage.length() == 0) {
                importMessage.append(mess).append(getLabels().getString("end_timestamp"));
            } else {
                importMessage.append(System.lineSeparator()).append(System.lineSeparator()).append(mess).append(getLabels().getString("end_timestamp"));
            }
        }

        return importMessage;
    }

    private void handleInvalidData(LogModel logModel, boolean isPublic) throws IOException {

        Window errorPopUp = (Window) portalContext.getUI().createComponent(getClass().getClassLoader(), "zul/invalidData.zul", null, null);
        errorPopUp.doModal();

        List<LogErrorReport> errorReport = logModel.getLogErrorReport();

//      Since the log is imported as a stream, errorCount can be predicted at this stage
        Label errorCount = (Label) errorPopUp.getFellow(errorCountLblId);
        errorCount.setValue(String.valueOf(errorReport.size()));

        Label columnList = (Label) errorPopUp.getFellow(invalidColumnsListLblId);

        Set<String> invColList = new HashSet<String>();
        Set<String> igColList = new HashSet<String>();
        Set<Integer> invTimestampPos = new HashSet<>();

        for (LogErrorReport error : errorReport) {
            if (error.getHeader() != null && !error.getHeader().isEmpty()) {
                invColList.add(error.getHeader());
                if (logMetaData.getOtherTimestamps().containsKey(error.getColumnIndex())) {
                    igColList.add(error.getHeader());
                    invTimestampPos.add(error.getColumnIndex());
                }
            }
        }
        if (!invColList.isEmpty()) {
            columnList.setValue(getLabels().getString("the_following_columns_include_errors") + columnList(invColList));
        }

        if (!igColList.isEmpty()) {
            Label ignoredList = (Label) errorPopUp.getFellow(ignoredColumnsListLblId);
            Label ignoreLbl = (Label) errorPopUp.getFellow(ignoreColLblId);
            ignoreLbl.setVisible(true);
            ignoredList.setValue(columnList(igColList));

            Button skipColumns = (Button) errorPopUp.getFellow(skipColumnsBtnId);
            skipColumns.setVisible(true);
            skipColumns.addEventListener("onClick", event -> {
                        errorPopUp.invalidate();
                        errorPopUp.detach();

                        for (int pos : invTimestampPos) {
                            logMetaData.getOtherTimestamps().remove(pos);
                            logMetaData.getIgnoredPos().add(pos);
                        }

                        LogModel logModelSkippedCol;
                        if (useParquet) {
                            logModelSkippedCol = parquetImporter.importParqeuetFile(
                                    getInputSream(media),
                                    logMetaData,
                                    getFileEncoding(),
                                    parquetFile,
                                    false);
                        } else {
                            logModelSkippedCol = logImporter.importLog(getInputSream(media),
                                    logMetaData,
                                    getFileEncoding(),
                                    true,
                                    portalContext.getCurrentUser().getUsername(),
                                    portalContext.getCurrentFolder() == null ? 0 : portalContext.getCurrentFolder().getId(),
                                    media.getName().replaceFirst("[.][^.]+$", ""));
                        }

                        if (logModelSkippedCol != null) {
                            if (logModelSkippedCol.getLogErrorReport().isEmpty()) {
                                saveXLog(logModelSkippedCol, isPublic);
                            } else {
                                handleInvalidData(logModelSkippedCol, isPublic);
                            }
                        }
                    }
            );
        }

        Button downloadBtn = (Button) errorPopUp.getFellow(downloadReportBtnId);
        downloadBtn.addEventListener("onClick", event -> {
                    downloadErrorLog(errorReport);
                }
        );

        Button skipRows = (Button) errorPopUp.getFellow(skipRowsBtnId);
        skipRows.addEventListener("onClick", event -> {
                    errorPopUp.invalidate();
                    errorPopUp.detach();

                    LogModel logModelSkippedRow;

                    if (useParquet) {
                        logModelSkippedRow = parquetImporter.importParqeuetFile(
                                getInputSream(media),
                                logMetaData,
                                getFileEncoding(),
                                parquetFile,
                                true);

                    } else {
                        logModelSkippedRow = logImporter.importLog(
                                getInputSream(media),
                                logMetaData,
                                getFileEncoding(),
                                true,
                                portalContext.getCurrentUser().getUsername(),
                                portalContext.getCurrentFolder() == null ? 0 : portalContext.getCurrentFolder().getId(),
                                media.getName().replaceFirst("[.][^.]+$", ""));
                    }

                    if (logModelSkippedRow != null)
                        saveXLog(logModelSkippedRow, isPublic);
                }
        );

        Button cancelButton = (Button) errorPopUp.getFellow(handleCancelBtnId);
        cancelButton.addEventListener("onClick", event -> {
            errorPopUp.invalidate();
            errorPopUp.detach();
        });
    }

    private String columnList(Set<String> list) {
        StringBuilder colList = new StringBuilder();
        for (String col : list) {
            colList.append(col).append(" | ");
        }
        if (colList.length() > 0) {
            return colList.toString().substring(0, colList.toString().length() - 2);
        }
        return null;
    }

    private void downloadErrorLog(List<LogErrorReport> errorReport) throws Exception {

        File tempFile = File.createTempFile("ErrorReport", ".csv");
        try (FileWriter writer = new FileWriter(tempFile);

             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.NO_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END);) {

            String[] headerRecord = {"#", "Row Index", "Column", "Error message"};
            csvWriter.writeNext(headerRecord);

            int counter = 1;
            for (LogErrorReport error : errorReport) {
                csvWriter.writeNext(new String[]{String.valueOf(counter++), String.valueOf(error.getRowIndex()), error.getHeader(), error.getError()});
            }

            InputStream csvLogStream = new FileInputStream(tempFile);
            Filedownload.save(csvLogStream, "text/csv; charset-UTF-8", "ErrorReport.csv");
        } catch (Exception e) {
            Messagebox.show(getLabels().getString("failed_to_download_error_log") + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
        } finally {
            tempFile.delete();

        }
    }

    private void saveXLog(LogModel logModel, boolean isPublic) {

        try {
            if (useParquet) {

                String successMessage;
                if (logModel.isRowLimitExceeded()) {
                    successMessage = MessageFormat.format(getLabels().getString("limit_reached"), logModel.getRowsCount());
                } else {
                    successMessage = MessageFormat.format(getLabels().getString("successful_upload"), logModel.getRowsCount());
                }
                Messagebox.show(successMessage, new Messagebox.Button[]{Messagebox.Button.OK}, event -> close());
                portalContext.refreshContent();

            } else {
                storeMappingAsJSON(media, logMetaData, logModel.getImportLog());
                String successMessage;
                if (logModel.isRowLimitExceeded()) {
                    successMessage = MessageFormat.format(getLabels().getString("limit_reached"), logModel.getRowsCount());
                } else {
                    successMessage = MessageFormat.format(getLabels().getString("successful_upload"), logModel.getRowsCount());
                }

                Messagebox.show(successMessage, new Messagebox.Button[]{Messagebox.Button.OK}, event -> close());
                portalContext.refreshContent();

            }

            // TODO: remove test code
//            userMetadataService.saveUserMetadataLinkedToOneLog("testCSV content", UserMetadataTypeEnum.CSV_IMPORTER,
//                    "admin", 140);


//            List<Integer> logIdlist = new ArrayList<>();
//            logIdlist.add(138);
//            logIdlist.add(139);
//
//            userMetadataService.saveUserMetadata("test metadata content", UserMetadataTypeEnum.CSV_IMPORTER, "admin",
//                    logIdlist);
//            Set<Usermetadata> usermetadatatest = userMetadataService.getUserMetadata("admin", logIdlist,
//                    UserMetadataTypeEnum.CSV_IMPORTER);
//            for(Usermetadata usermetadata : usermetadatatest) {
//                LOGGER.info("RESULT:::::::::" + usermetadata.getId());
//            }


//            userMetadataService.saveUserMetadataLinkedToOneLog("test metadata content",
//                    UserMetadataTypeEnum.DASHBOARD, "admin", 138);
//            userMetadataService.updateUserMetadata(16, username, "new content");
//            userMetadataService.deleteUserMetadata(17, username);
//            for (Usermetadata usermetadata : userMetadataService.getUserMetadata(username, 166,
//                    UserMetadataTypeEnum.DASHBOARD)) {
//                LOGGER.info("RESULT :" + usermetadata.getId() + usermetadata.getContent());
//            }
//            LOGGER.info("Result: " + userMetadataService.canUserEditMetadata(username, 18));
//            LOGGER.info("Result: " + userMetadataService.canUserEditMetadata(username, 10));

//            userMetadataService.saveDashTemplate("dash template", "admin");

//            List logIdList = new LinkedList();
//            logIdList.add(221);
//            logIdList.add(222);
//            logIdList.add(223);
//            Set<Usermetadata> usermetadataList = new HashSet<>();
//            usermetadataList = userMetadataService.getUserMetadata("admin", logIdList,
//                    UserMetadataTypeEnum.CSV_IMPORTER);
//            for (Usermetadata usermetadata : usermetadataList) {
//                LOGGER.info("RESULT :" + usermetadata.getId() + usermetadata.getContent());
//            }

        } catch (Exception e) {
            Messagebox.show(getLabels().getString("failed_to_write_log") + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    private InputStream getInputSream(Media media) {

        return media.isBinary() ? media.getStreamData() : new ByteArrayInputStream(media.getByteData());
    }

    private static String getMediaFormat(Media media) throws Exception {
        if (media.getName().lastIndexOf('.') < 0)
            throw new Exception("Can't read file format");
        return media.getName().substring(media.getName().lastIndexOf('.') + 1);
    }

    private ListModelList<String> getTimeZoneList() {

        ListModelList<String> listModelList = new ListModelList<>();
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            TimeZone zone = TimeZone.getTimeZone(id);
            int offset = zone.getRawOffset() / 1000;
            int hour = offset / 3600;
            int minutes = (offset % 3600) / 60;
            listModelList.add(String.format("(GMT%+d:%02d) %s", hour, minutes, id));
        }
        return listModelList;
    }
}