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

import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parses the FtpServer "native-filesystem" element into a Spring bean graph
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class FileSystemBeanDefinitionParser extends
        AbstractSingleBeanDefinitionParser {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends FileSystemFactory> getBeanClass(
            final Element element) {
        return NativeFileSystemFactory.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doParse(final Element element,
            final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {
        if (StringUtils.hasText(element.getAttribute("case-insensitive"))) {
            builder.addPropertyValue("caseInsensitive", Boolean
                    .valueOf(element.getAttribute("case-insensitive")));
        }
        if (StringUtils.hasText(element.getAttribute("create-home"))) {
            builder.addPropertyValue("createHome", Boolean
                    .valueOf(element.getAttribute("create-home")));
        }
    }
}
