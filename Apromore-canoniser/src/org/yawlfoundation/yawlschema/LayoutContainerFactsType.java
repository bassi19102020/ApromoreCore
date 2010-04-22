//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.03.23 at 09:25:45 AM EST 
//


package org.yawlfoundation.yawlschema;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for LayoutContainerFactsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LayoutContainerFactsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="vertex" type="{http://www.yawlfoundation.org/yawlschema}LayoutVertexFactsType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="label" type="{http://www.yawlfoundation.org/yawlschema}LayoutLabelFactsType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="decorator" type="{http://www.yawlfoundation.org/yawlschema}LayoutDecoratorFactsType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LayoutContainerFactsType", propOrder = {
    "vertexOrLabelOrDecorator"
})
public class LayoutContainerFactsType {

    @XmlElements({
        @XmlElement(name = "vertex", type = LayoutVertexFactsType.class),
        @XmlElement(name = "decorator", type = LayoutDecoratorFactsType.class),
        @XmlElement(name = "label", type = LayoutLabelFactsType.class)
    })
    protected List<Object> vertexOrLabelOrDecorator;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String id;

    /**
     * Gets the value of the vertexOrLabelOrDecorator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the vertexOrLabelOrDecorator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVertexOrLabelOrDecorator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LayoutVertexFactsType }
     * {@link LayoutDecoratorFactsType }
     * {@link LayoutLabelFactsType }
     * 
     * 
     */
    public List<Object> getVertexOrLabelOrDecorator() {
        if (vertexOrLabelOrDecorator == null) {
            vertexOrLabelOrDecorator = new ArrayList<Object>();
        }
        return this.vertexOrLabelOrDecorator;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
