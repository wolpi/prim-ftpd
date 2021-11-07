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

package org.apache.ftpserver.message;

import java.util.List;
import java.util.Map;

/**
 * This is message resource interface.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface MessageResource {

    /**
     * Get all the available languages.
     * @return A list of available languages
     */
    List<String> getAvailableLanguages();

    /**
     * Get the message for the corresponding code and sub id. If not found it
     * will return null.
     * @param code The reply code
     * @param subId The sub ID
     * @param language The language
     * @return The message matching the provided inputs, or null if not found
     */
    String getMessage(int code, String subId, String language);

    /**
     * Get all the messages.
     * @param language The language
     * @return All messages for the provided language
     */
    Map<String, String> getMessages(String language);
}
