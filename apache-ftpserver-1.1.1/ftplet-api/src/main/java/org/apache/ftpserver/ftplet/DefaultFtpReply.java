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

package org.apache.ftpserver.ftplet;


/**
 * FTP reply object.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultFtpReply implements FtpReply {

    private int code;

    private String message;
    
    /**
     * time when this reply was sent.  
     */
    private long sentTime = 0L;

    private static final String CRLF = "\r\n";

    /**
     * Constructor for single-line messages
     * @param code The reply code
     * @param message The reply message
     */
    public DefaultFtpReply(final int code, final String message) {
        this.code = code;
        this.message = message;
        this.sentTime = System.currentTimeMillis();
    }

    /**
     * Constructor for multi-line replies
     * @param code The reply code
     * @param message The reply message, one line per String
     */
    public DefaultFtpReply(final int code, final String[] message) {
        this.code = code;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < message.length; i++) {
            sb.append(message[i]);
            sb.append('\n');
        }
        this.message = sb.toString();
        this.sentTime = System.currentTimeMillis();
    }

    /**
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    public long getSentTime() {
    	return sentTime;
    }
    
    public boolean isPositive() {
    	return code < 400;
    }
    
    private boolean isDigit(char c) {
    	return c >= 48 && c <= 57;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        int code = getCode();
        String notNullMessage = getMessage();
        if (notNullMessage == null) {
            notNullMessage = "";
        }

        StringBuilder sb = new StringBuilder();

        // remove any carriage returns
        notNullMessage = notNullMessage.replace("\r", "");
        
        // remove trailing line feeds
        if(notNullMessage.endsWith("\n")) {
            notNullMessage = notNullMessage.substring(0, notNullMessage.length() - 1);
        }
        
        String[] lines = notNullMessage.split("\n");

        // no newline
        if (lines.length == 1) {
            sb.append(code);
            sb.append(" ");
            sb.append(notNullMessage);
            sb.append(CRLF);
        } else {

            sb.append(code);
            sb.append("-");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (i + 1 == lines.length) {
                    sb.append(code);
                    sb.append(" ");
                }

                // "If an intermediary line begins with a 3-digit number, the Server
                // must pad the front  to avoid confusion.
                if(i > 0 
                		&& i + 1 < lines.length 
                		&& line.length() > 2 
                		&& isDigit(line.charAt(0))
                		&& isDigit(line.charAt(1))
                		&& isDigit(line.charAt(2))
                	) {
                	sb.append("  ");
                }
                sb.append(line);
                sb.append(CRLF);
            }

        }

        return sb.toString();
    }

}
