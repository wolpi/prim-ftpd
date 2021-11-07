/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ftpserver.config.spring;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.FtpServerConfigurationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Various util methods for the Spring config parsing and configuration
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SpringUtil {

    /**
     * Get all child elements for the element
     * 
     * @param elm
     *            The element for which to locate children
     * @return All children
     */
    public static List<Element> getChildElements(final Element elm) {
        List<Element> elements = new ArrayList<Element>();
        NodeList childs = elm.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);

            if (child instanceof Element) {
                elements.add((Element) child);
            }
        }

        return elements;
    }

    /**
     * Get the first child element matching the local name and namespace
     * 
     * @param parent
     *            The element for which to locate the child
     * @param ns
     *            The namespace to match, or null for any namespace
     * @param localName
     *            The local name to match, or null for any local name
     * @return The first child matching the criteria
     */
    public static Element getChildElement(final Element parent,
            final String ns, final String localName) {
        List<Element> elements = getChildElements(parent);

        for (Element element : elements) {
            if ((ns == null || ns.equals(element.getNamespaceURI())
                    && (localName == null || localName.equals(element
                            .getLocalName())))) {
                return element;
            }
        }

        return null;
    }

    /**
     * Get the text context of first child element matching the local name and
     * namespace
     * 
     * @param parent
     *            The element for which to locate the child
     * @param ns
     *            The namespace to match, or null for any namespace
     * @param localName
     *            The local name to match, or null for any local name
     * @return The text content of the first child matching the criteria or null
     *         if element not found
     */
    public static String getChildElementText(final Element parent,
            final String ns, final String localName) {
        List<Element> elements = getChildElements(parent);

        for (Element element : elements) {
            if ((ns == null || ns.equals(element.getNamespaceURI())
                    && (localName == null || localName.equals(element
                            .getLocalName())))) {
                return DomUtils.getTextValue(element);
            }
        }

        return null;
    }

    /**
     * Parse specific Spring elements, bean and ref
     * 
     * @param parent
     *            The element in which we will look for Spring elements
     * @param parserContext
     *            The Spring parser context
     * @param builder
     *            The Spring bean definition builder
     * @return The Spring bean definition
     */
    public static Object parseSpringChildElement(final Element parent,
            final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        Element springElm = getChildElement(parent, null, null);

        String ln = springElm.getLocalName();
        if ("bean".equals(ln)) {
            return parserContext.getDelegate().parseBeanDefinitionElement(
                    springElm, builder.getBeanDefinition());
        } else if ("ref".equals(ln)) {
            return parserContext.getDelegate().parsePropertySubElement(
                    springElm, builder.getBeanDefinition());
        } else {
            throw new FtpServerConfigurationException("Unknown spring element "
                    + ln);
        }
    }

    /**
     * Parses a attribute value into a boolean. If the attribute is missing or
     * has no content, a default value is returned
     * 
     * @param parent
     *            The element
     * @param attrName
     *            The attribute name
     * @param defaultValue
     *            The default value
     * @return The value, or the default value
     */
    public static boolean parseBoolean(final Element parent,
            final String attrName, final boolean defaultValue) {
        if (StringUtils.hasText(parent.getAttribute(attrName))) {
            return Boolean.parseBoolean(parent.getAttribute(attrName));
        }
        return defaultValue;
    }

    /**
     * Parses a attribute value into an integer.
     * 
     * @param parent
     *            The element
     * @param attrName
     *            The attribute name
     * @return The value
     * @throws NumberFormatException
     *             If the attribute does not contain a number
     */
    public static int parseInt(final Element parent, final String attrName) {
        return Integer.parseInt(parent.getAttribute(attrName));
    }

    /**
     * Parses a attribute value into an integer. If the attribute is missing or
     * has no content, a default value is returned
     * 
     * @param parent
     *            The element
     * @param attrName
     *            The attribute name
     * @param defaultValue
     *            The default value
     * @return The value, or the default value
     */
    public static int parseInt(final Element parent, final String attrName,
            final int defaultValue) {
        if (StringUtils.hasText(parent.getAttribute(attrName))) {
            return Integer.parseInt(parent.getAttribute(attrName));
        }
        return defaultValue;
    }

    /**
     * Return the string value of an attribute, or null if the attribute is
     * missing
     * 
     * @param parent
     *            The element
     * @param attrName
     *            The attribute name
     * @return The attribute string value
     */
    public static String parseString(final Element parent, final String attrName) {
        if (parent.hasAttribute(attrName)) {
            return parent.getAttribute(attrName);
        } else {
            return null;
        }
    }

    /**
     * Return an attribute value as a {@link File}
     * 
     * @param parent
     *            The element
     * @param attrName
     *            The attribute name
     * @return The file representing the path used in the attribute
     */
    public static File parseFile(final Element parent, final String attrName) {
        if (StringUtils.hasText(parent.getAttribute(attrName))) {
            return new File(parent.getAttribute(attrName));
        }
        return null;
    }

    /**
     * Return an attribute value as an {@link InetAddress}
     * 
     * @param parent
     *            The element
     * @param attrName
     *            The attribute name
     * @return The attribute value parsed into a {@link InetAddress}
     */
    public static InetAddress parseInetAddress(final Element parent,
            final String attrName) {
        if (StringUtils.hasText(parent.getAttribute(attrName))) {
            try {
                return InetAddress.getByName(parent.getAttribute(attrName));
            } catch (UnknownHostException e) {
                throw new FtpServerConfigurationException("Unknown host", e);
            }
        }
        return null;
    }
    /**
     * Return an attribute value after checking it is a valid {@link InetAddress}
     * 
     * @param parent
     *            The element
     * @param attrName
     *            The attribute name
     * @return The attribute string value.
     */
    public static String parseStringFromInetAddress(final Element parent,
    		final String attrName){
    	if ( parseInetAddress(parent, attrName)!=null){
    		return parent.getAttribute(attrName);
    	}
    	return null;
    }
}
