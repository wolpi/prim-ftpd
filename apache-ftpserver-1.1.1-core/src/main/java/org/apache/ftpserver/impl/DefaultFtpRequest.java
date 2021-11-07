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

package org.apache.ftpserver.impl;

import org.apache.ftpserver.ftplet.FtpRequest;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * FTP request object.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultFtpRequest implements FtpRequest {

    private final String line;

    private final String command;

    private final String argument;
    
    /**
     * timestamp when this request was received
     */
    private final long receivedTime;

    /**
     * Default constructor.
     */
    public DefaultFtpRequest(final String requestLine) {
    	//Assuming we create the request as soon as we receive the command from 
    	//the client, set the received time to current time. If we do a whole 
    	//bunch of things after we receive the command from the client and 
    	//before constructing this FtpRequest object, then this method is not 
    	//going to be accurate and need to look for an alternative solution. 
    	this.receivedTime = System.currentTimeMillis();
        line = requestLine.trim();
        int spInd = line.indexOf(' ');
        command = parseCmd(line, spInd);
        argument = parseArg(line, spInd);
    }

    /**
     * Parse the ftp command line.
     */
    private String parseCmd(final String lineToParse, int spInd) {
        String cmd = null;
        if (spInd != -1) {
            cmd = line.substring(0, spInd).toUpperCase();
        } else {
            cmd = line.toUpperCase();
        }
        if ((cmd.length() > 0) && (cmd.charAt(0) == 'X')) {
            cmd = cmd.substring(1);
        }
        return cmd;
    }

    private String parseArg(final String lineToParse, int spInd) {
        String arg = null;
        if (spInd != -1) {
            arg = line.substring(spInd + 1);
            if (arg.equals("")) {
                arg = null;
            }
        }
        return arg;
    }

    /**
     * Get the ftp command.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Get ftp input argument.
     */
    public String getArgument() {
        return argument;
    }

    /**
     * Get the ftp request line.
     */
    public String getRequestLine() {
        return line;
    }

    /**
     * Has argument.
     */
    public boolean hasArgument() {
        return getArgument() != null;
    }
    
    public long getReceivedTime() {
    	return receivedTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getRequestLine();
    }
}
