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

package org.apache.ftpserver.ftpletcontainer;

import java.util.Map;

import org.apache.ftpserver.ftplet.Ftplet;

/**
 * Interface describing an Ftplet container. Ftplet containers extend the
 * {@link Ftplet} interface and forward any events to the Ftplets hosted by the
 * container.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface FtpletContainer extends Ftplet {

    /**
     * Retrieve the {@link Ftplet} identified by the name (as provided in the
     * {@link #addFtplet(String, Ftplet)} method.
     * 
     * @param name
     *            The name of the Ftplet to retrive
     * @return The Ftplet if found, or null if the name is unknown to the
     *         container.
     */
    Ftplet getFtplet(String name);

    /**
     * Retrive all Ftplets registered with this container
     * 
     * @return A map of all Ftplets with their name as the key
     */
    Map<String, Ftplet> getFtplets();
}