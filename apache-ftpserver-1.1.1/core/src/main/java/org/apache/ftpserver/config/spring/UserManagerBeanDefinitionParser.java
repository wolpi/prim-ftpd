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

import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.DbUserManagerFactory;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parses the FtpServer "file-user-manager" or "db-user-manager" elements into a
 * Spring bean graph
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class UserManagerBeanDefinitionParser extends
        AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(final Element element) {
        return null;
    }

    @Override
    protected void doParse(final Element element,
            final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {


        Class<?> factoryClass;
        if (element.getLocalName().equals("file-user-manager")) {
            factoryClass = PropertiesUserManagerFactory.class;
        } else {
            factoryClass = DbUserManagerFactory.class;
        }
        BeanDefinitionBuilder factoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(factoryClass);

        
        // common for both user managers
        if (StringUtils.hasText(element.getAttribute("encrypt-passwords"))) {
            String encryptionStrategy = element.getAttribute("encrypt-passwords"); 
            
            if(encryptionStrategy.equals("true") || encryptionStrategy.equals("md5")) {
                factoryBuilder.addPropertyValue("passwordEncryptor", new Md5PasswordEncryptor());
            } else if(encryptionStrategy.equals("salted")) {
                factoryBuilder.addPropertyValue("passwordEncryptor", new SaltedPasswordEncryptor());
            } else {
                factoryBuilder.addPropertyValue("passwordEncryptor", new ClearTextPasswordEncryptor());
            }
        }
        
        if (factoryClass == PropertiesUserManagerFactory.class) {
            if (StringUtils.hasText(element.getAttribute("file"))) {
                factoryBuilder.addPropertyValue("file", element.getAttribute("file"));
            }
            if (StringUtils.hasText(element.getAttribute("url"))) {
                factoryBuilder.addPropertyValue("url", element.getAttribute("url"));
            }
        } else {
            Element dsElm = SpringUtil.getChildElement(element,
                    FtpServerNamespaceHandler.FTPSERVER_NS, "data-source");

            // schema ensure we get the right type of element
            Element springElm = SpringUtil.getChildElement(dsElm, null, null);
            Object o;
            if ("bean".equals(springElm.getLocalName())) {
                o = parserContext.getDelegate().parseBeanDefinitionElement(
                        springElm, builder.getBeanDefinition());
            } else {
                // ref
                o = parserContext.getDelegate().parsePropertySubElement(
                        springElm, builder.getBeanDefinition());

            }
            factoryBuilder.addPropertyValue("dataSource", o);

            factoryBuilder.addPropertyValue("sqlUserInsert", getSql(element,
                    "insert-user"));
            factoryBuilder.addPropertyValue("sqlUserUpdate", getSql(element,
                    "update-user"));
            factoryBuilder.addPropertyValue("sqlUserDelete", getSql(element,
                    "delete-user"));
            factoryBuilder.addPropertyValue("sqlUserSelect", getSql(element,
                    "select-user"));
            factoryBuilder.addPropertyValue("sqlUserSelectAll", getSql(element,
                    "select-all-users"));
            factoryBuilder.addPropertyValue("sqlUserAdmin",
                    getSql(element, "is-admin"));
            factoryBuilder.addPropertyValue("sqlUserAuthenticate", getSql(element,
                    "authenticate"));
        }

        BeanDefinition factoryDefinition = factoryBuilder.getBeanDefinition();
        String factoryId = parserContext.getReaderContext().generateBeanName(factoryDefinition);
        
        BeanDefinitionHolder factoryHolder = new BeanDefinitionHolder(factoryDefinition, factoryId);
        registerBeanDefinition(factoryHolder, parserContext.getRegistry());

        // set the factory on the listener bean
        builder.getRawBeanDefinition().setFactoryBeanName(factoryId);
        builder.getRawBeanDefinition().setFactoryMethodName("createUserManager");

    }

    private String getSql(final Element element, final String elmName) {
        return SpringUtil.getChildElementText(element,
                FtpServerNamespaceHandler.FTPSERVER_NS, elmName);
    }
}
