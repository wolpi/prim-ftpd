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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.message.MessageResource;
import org.apache.ftpserver.message.MessageResourceFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parses the FtpServer "server" element into a Spring bean graph
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ServerBeanDefinitionParser extends
        AbstractSingleBeanDefinitionParser {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends FtpServer> getBeanClass(final Element element) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doParse(final Element element,
            final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        
        BeanDefinitionBuilder factoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(FtpServerFactory.class);
        
        List<Element> childs = SpringUtil.getChildElements(element);
        for (Element childElm : childs) {
            String childName = childElm.getLocalName();

            if ("listeners".equals(childName)) {
                Map<?, ?> listeners = parseListeners(childElm, parserContext, builder);

                if (listeners.size() > 0) {
                    factoryBuilder.addPropertyValue("listeners", listeners);
                }
            } else if ("ftplets".equals(childName)) {
                Map<?, ?> ftplets = parseFtplets(childElm, parserContext, builder);
                factoryBuilder.addPropertyValue("ftplets", ftplets);
            } else if ("file-user-manager".equals(childName)
                    || "db-user-manager".equals(childName)) {
                Object userManager = parserContext.getDelegate()
                        .parseCustomElement(childElm,
                                builder.getBeanDefinition());
                factoryBuilder.addPropertyValue("userManager", userManager);
            } else if ("user-manager".equals(childName)) {
                factoryBuilder.addPropertyValue("userManager", SpringUtil
                        .parseSpringChildElement(childElm, parserContext,
                                builder));
            } else if ("native-filesystem".equals(childName)) {
                Object fileSystem = parserContext.getDelegate()
                        .parseCustomElement(childElm,
                                builder.getBeanDefinition());
                factoryBuilder.addPropertyValue("fileSystem", fileSystem);
            } else if ("filesystem".equals(childName)) {
                factoryBuilder.addPropertyValue("fileSystem", SpringUtil
                        .parseSpringChildElement(childElm, parserContext,
                                builder));
            } else if ("commands".equals(childName)) {
                Object commandFactory = parserContext.getDelegate()
                        .parseCustomElement(childElm,
                                builder.getBeanDefinition());
                factoryBuilder.addPropertyValue("commandFactory", commandFactory);
            } else if ("messages".equals(childName)) {
                MessageResource mr = parseMessageResource(childElm,
                        parserContext, builder);
                factoryBuilder.addPropertyValue("messageResource", mr);

            } else {
                throw new FtpServerConfigurationException(
                        "Unknown configuration name: " + childName);
            }
        }

        // Configure login limits
        ConnectionConfigFactory connectionConfig = new ConnectionConfigFactory();
        if (StringUtils.hasText(element.getAttribute("max-logins"))) {
            connectionConfig.setMaxLogins(SpringUtil.parseInt(element,
                    "max-logins"));
        }
        if (StringUtils.hasText(element.getAttribute("max-threads"))) {
            connectionConfig.setMaxThreads(SpringUtil.parseInt(element,
                    "max-threads"));
        }
        if (StringUtils.hasText(element.getAttribute("max-anon-logins"))) {
            connectionConfig.setMaxAnonymousLogins(SpringUtil.parseInt(element,
                    "max-anon-logins"));
        }
        if (StringUtils.hasText(element.getAttribute("anon-enabled"))) {
            connectionConfig.setAnonymousLoginEnabled(SpringUtil.parseBoolean(
                    element, "anon-enabled", true));
        }
        if (StringUtils.hasText(element.getAttribute("max-login-failures"))) {
            connectionConfig.setMaxLoginFailures(SpringUtil.parseInt(element,
                    "max-login-failures"));
        }
        if (StringUtils.hasText(element.getAttribute("login-failure-delay"))) {
            connectionConfig.setLoginFailureDelay(SpringUtil.parseInt(element,
                    "login-failure-delay"));
        }

        factoryBuilder.addPropertyValue("connectionConfig", connectionConfig.createConnectionConfig());

       
        BeanDefinition factoryDefinition = factoryBuilder.getBeanDefinition();

        String factoryName = parserContext.getReaderContext().generateBeanName(factoryDefinition);
        
        BeanDefinitionHolder factoryHolder = new BeanDefinitionHolder(factoryDefinition, factoryName);
        registerBeanDefinition(factoryHolder, parserContext.getRegistry());

        // set the factory on the listener bean
        builder.getRawBeanDefinition().setFactoryBeanName(factoryName);
        builder.getRawBeanDefinition().setFactoryMethodName("createServer");

    }

    /**
     * Parse the "messages" element
     */
    private MessageResource parseMessageResource(final Element childElm,
            final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        MessageResourceFactory mr = new MessageResourceFactory();

        if (StringUtils.hasText(childElm.getAttribute("languages"))) {
            String langString = childElm.getAttribute("languages");

            String[] languages = langString.split("[\\s,]+");

            mr.setLanguages(Arrays.asList(languages));
        }

        if (StringUtils.hasText(childElm.getAttribute("directory"))) {
            mr.setCustomMessageDirectory(new File(childElm
                    .getAttribute("directory")));

        }

        return mr.createMessageResource();
    }

    /**
     * Parse the "ftplets" element
     */
    private Map<?, ?> parseFtplets(final Element childElm,
            final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        List<Element> childs = SpringUtil.getChildElements(childElm);

        if(childs.size() > 0 && childs.get(0).getLocalName().equals("map")) {
            // using a beans:map element
            return parserContext.getDelegate().parseMapElement(
                    childs.get(0),
                    builder.getBeanDefinition());
        } else {
            ManagedMap ftplets = new ManagedMap();
            for (Element ftpletElm : childs) {
                ftplets
                        .put(ftpletElm.getAttribute("name"), SpringUtil
                                .parseSpringChildElement(ftpletElm, parserContext,
                                        builder));
            }
    
            return ftplets;
        }
    }

    /**
     * Parse listeners elements
     */
    @SuppressWarnings("unchecked")
    private Map<?, ?> parseListeners(final Element listenersElm,
            final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        ManagedMap listeners = new ManagedMap();

        List<Element> childs = SpringUtil.getChildElements(listenersElm);

        for (Element listenerElm : childs) {
            Object listener = null;
            String ln = listenerElm.getLocalName();
            if ("nio-listener".equals(ln)) {
                listener = parserContext.getDelegate().parseCustomElement(
                        listenerElm, builder.getBeanDefinition());
            } else if ("listener".equals(ln)) {
                listener = SpringUtil.parseSpringChildElement(listenerElm,
                        parserContext, builder);
            } else {
                throw new FtpServerConfigurationException(
                        "Unknown listener element " + ln);
            }

            String name = listenerElm.getAttribute("name");

            listeners.put(name, listener);
        }

        return listeners;
    }
}
