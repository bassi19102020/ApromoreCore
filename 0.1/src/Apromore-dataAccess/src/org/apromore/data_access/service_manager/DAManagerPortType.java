package org.apromore.data_access.service_manager;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 2.2.7
 * Tue Jul 13 17:52:40 EST 2010
 * Generated source version: 2.2.7
 * 
 */
 
@WebService(targetNamespace = "http://www.apromore.org/data_access/service_manager", name = "DAManagerPortType")
@XmlSeeAlso({org.apromore.data_access.model_manager.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface DAManagerPortType {

    @WebResult(name = "DeleteProcessVersionsOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "DeleteProcessVersions")
    public org.apromore.data_access.model_manager.DeleteProcessVersionsOutputMsgType deleteProcessVersions(
        @WebParam(partName = "payload", name = "DeleteProcessVersionsInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.DeleteProcessVersionsInputMsgType payload
    );

    @WebResult(name = "WriteUserOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "WriteUser")
    public org.apromore.data_access.model_manager.WriteUserOutputMsgType writeUser(
        @WebParam(partName = "payload", name = "WriteUserInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.WriteUserInputMsgType payload
    );

    @WebResult(name = "ReadNativeOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "ReadNative")
    public org.apromore.data_access.model_manager.ReadNativeOutputMsgType readNative(
        @WebParam(partName = "payload", name = "ReadNativeInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.ReadNativeInputMsgType payload
    );

    @WebResult(name = "DeleteEditSessionOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "DeleteEditSession")
    public org.apromore.data_access.model_manager.DeleteEditSessionOutputMsgType deleteEditSession(
        @WebParam(partName = "payload", name = "DeleteEditSessionInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.DeleteEditSessionInputMsgType payload
    );

    @WebResult(name = "ReadEditSessionOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "ReadEditSession")
    public org.apromore.data_access.model_manager.ReadEditSessionOutputMsgType readEditSession(
        @WebParam(partName = "payload", name = "ReadEditSessionInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.ReadEditSessionInputMsgType payload
    );

    @WebResult(name = "ReadDomainsOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "ReadDomains")
    public org.apromore.data_access.model_manager.ReadDomainsOutputMsgType readDomains(
        @WebParam(partName = "payload", name = "ReadDomainsInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.ReadDomainsInputMsgType payload
    );

    @WebResult(name = "WriteEditSessionOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "WriteEditSession")
    public org.apromore.data_access.model_manager.WriteEditSessionOutputMsgType writeEditSession(
        @WebParam(partName = "payload", name = "WriteEditSessionInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.WriteEditSessionInputMsgType payload
    );

    @WebResult(name = "ReadFormatsOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "ReadFormats")
    public org.apromore.data_access.model_manager.ReadFormatsOutputMsgType readFormats(
        @WebParam(partName = "payload", name = "ReadFormatsInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.ReadFormatsInputMsgType payload
    );

    @WebResult(name = "ReadCanonicalAnfOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "ReadCanonicalAnf")
    public org.apromore.data_access.model_manager.ReadCanonicalAnfOutputMsgType readCanonicalAnf(
        @WebParam(partName = "payload", name = "ReadCanonicalAnfInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.ReadCanonicalAnfInputMsgType payload
    );

    @WebResult(name = "ReadUserOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "ReadUser")
    public org.apromore.data_access.model_manager.ReadUserOutputMsgType readUser(
        @WebParam(partName = "payload", name = "ReadUserInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.ReadUserInputMsgType payload
    );

    @WebResult(name = "EditDataProcessOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "EditDataProcess")
    public org.apromore.data_access.model_manager.EditDataProcessOutputMsgType editDataProcess(
        @WebParam(partName = "payload", name = "EditDataProcessInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.EditDataProcessInputMsgType payload
    );

    @WebResult(name = "ReadProcessSummariesOutputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager", partName = "payload")
    @WebMethod(operationName = "ReadProcessSummaries")
    public org.apromore.data_access.model_manager.ReadProcessSummariesOutputMsgType readProcessSummaries(
        @WebParam(partName = "payload", name = "ReadProcessSummariesInputMsg", targetNamespace = "http://www.apromore.org/data_access/model_manager")
        org.apromore.data_access.model_manager.ReadProcessSummariesInputMsgType payload
    );
}
