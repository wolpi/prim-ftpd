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

package org.apache.ftpserver.ipfilter;

/**
 * Defines various types of IP Filters.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public enum IpFilterType {

	/**
	 * filter type that allows a set of predefined IP addresses, also known as a
	 * white list.
	 */
	ALLOW,

	/**
	 * filter type that blocks a set of predefined IP addresses, also known as a
	 * black list.
	 */
	DENY;

	/**
	 * Parses the given string into its equivalent enum.
	 * 
	 * @param value
	 *            the string value to parse.
	 * @return the equivalent enum
	 */
	public static IpFilterType parse(String value) {
		for (IpFilterType type : values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid IpFilterType: " + value);
	}

}
