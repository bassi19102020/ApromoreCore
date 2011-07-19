
package org.apromore.manager.model_portal;

import javax.activation.DataHandler;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ExportFormatOutputMsgType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ExportFormatOutputMsgType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Result" type="{http://www.apromore.org/manager/model_portal}ResultType"/>
 *         &lt;element name="Native" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExportFormatOutputMsgType", propOrder = {
    "result",
    "_native"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2011-07-19T05:06:01+10:00", comments = "JAXB RI vhudson-jaxb-ri-2.2-27")
public class ExportFormatOutputMsgType {

    @XmlElement(name = "Result", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2011-07-19T05:06:01+10:00", comments = "JAXB RI vhudson-jaxb-ri-2.2-27")
    protected ResultType result;
    @XmlElement(name = "Native", required = true)
    @XmlMimeType("application/octet-stream")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2011-07-19T05:06:01+10:00", comments = "JAXB RI vhudson-jaxb-ri-2.2-27")
    protected DataHandler _native;

    /**
     * Gets the value of the result property.
     * 
     * @return
     *     possible object is
     *     {@link ResultType }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2011-07-19T05:06:01+10:00", comments = "JAXB RI vhudson-jaxb-ri-2.2-27")
    public ResultType getResult() {
        return result;
    }

    /**
     * Sets the value of the result property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResultType }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2011-07-19T05:06:01+10:00", comments = "JAXB RI vhudson-jaxb-ri-2.2-27")
    public void setResult(ResultType value) {
        this.result = value;
    }

    /**
     * Gets the value of the native property.
     * 
     * @return
     *     possible object is
     *     {@link DataHandler }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2011-07-19T05:06:01+10:00", comments = "JAXB RI vhudson-jaxb-ri-2.2-27")
    public DataHandler getNative() {
        return _native;
    }

    /**
     * Sets the value of the native property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataHandler }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2011-07-19T05:06:01+10:00", comments = "JAXB RI vhudson-jaxb-ri-2.2-27")
    public void setNative(DataHandler value) {
        this._native = value;
    }

}
