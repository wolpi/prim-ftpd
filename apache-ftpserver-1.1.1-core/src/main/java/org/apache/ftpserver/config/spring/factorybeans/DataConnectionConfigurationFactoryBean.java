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

package org.apache.ftpserver.config.spring.factorybeans;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring {@link FactoryBean} which extends {@link DataConnectionConfigurationFactory}
 * making it easier to use Spring's standard &lt;bean&gt; tag instead of 
 * FtpServer's custom XML tags to configure things.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @see DataConnectionConfigurationFactory
 */
public class DataConnectionConfigurationFactoryBean extends DataConnectionConfigurationFactory implements FactoryBean {

    public Object getObject() throws Exception {
        return createDataConnectionConfiguration();
    }

    public Class<?> getObjectType() {
        return DataConnectionConfiguration.class;
    }

    public boolean isSingleton() {
        return false;
    }
    
}
