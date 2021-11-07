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

import java.io.IOException;

/**
 * Defines methods that all ftplets must implement.
 * 
 * A ftplet is a small Java program that runs within an FTP server. Ftplets
 * receive and respond to requests from FTP clients.
 * 
 * This interface defines methods to initialize a ftplet, to service requests,
 * and to remove a ftplet from the server. These are known as life-cycle methods
 * and are called in the following sequence:
 * 
 * <ol>
 * <li>The ftplet is constructed.</li>
 * <li>Then initialized with the init method.</li>
 * <li>All the callback methods will be invoked.</li>
 * <li>The ftplet is taken out of service, then destroyed with the destroy
 * method.</li>
 * <li>Then garbage collected and finalized.</li>
 * </ol>
 * 
 * All the callback methods return FtpletEnum. If it returns null
 * FtpletEnum.DEFAULT will be assumed. If any ftplet callback method throws
 * exception, that particular connection will be disconnected.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface Ftplet {

    /**
     * Called by the ftplet container to indicate to a ftplet that the ftplet is
     * being placed into service. The ftplet container calls the init method
     * exactly once after instantiating the ftplet. The init method must
     * complete successfully before the ftplet can receive any requests.
     * @param ftpletContext The current {@link FtpletContext}
     * @throws FtpException 
     */
    void init(FtpletContext ftpletContext) throws FtpException;

    /**
     * Called by the Ftplet container to indicate to a ftplet that the ftplet is
     * being taken out of service. This method is only called once all threads
     * within the ftplet's service method have exited. After the ftplet
     * container calls this method, callback methods will not be executed. If
     * the ftplet initialization method fails, this method will not be called.
     */
    void destroy();

    /**
     * Called by the ftplet container before a command is executed by the
     * server. The implementation should return based on the desired action to
     * be taken by the server:
     * <ul>
     * <li>{@link FtpletResult#DEFAULT}: The server continues as normal and
     * executes the command</li>
     * <li>{@link FtpletResult#NO_FTPLET}: The server does not call any more
     * Ftplets before this command but continues execution of the command as
     * usual</li>
     * <li>{@link FtpletResult#SKIP}: The server skips over execution of the
     * command. Note that the Ftplet is responsible for returning the
     * appropriate reply to the client, or the client might deadlock.</li>
     * <li>{@link FtpletResult#DISCONNECT}: The server will immediately
     * disconnect the client.</li>
     * <li>Ftplet throws exception: Same as {@link FtpletResult#DISCONNECT}</li>
     * </ul>
     * 
     * @param session
     *            The current session
     * @param request
     *            The current request
     * @return The desired action to be performed by the server
     * @throws FtpException
     * @throws IOException
     */
    FtpletResult beforeCommand(FtpSession session, FtpRequest request)
            throws FtpException, IOException;

    /**
	 * Called by the ftplet container after a command has been executed by the
	 * server. The implementation should return based on the desired action to
	 * be taken by the server:
	 * <ul>
	 * <li>{@link FtpletResult#DEFAULT}: The server continues as normal</li>
	 * <li>{@link FtpletResult#NO_FTPLET}: The server does not call any more
	 * Ftplets before this command but continues as normal</li>
	 * <li>{@link FtpletResult#SKIP}: Same as {@link FtpletResult#DEFAULT}</li>
	 * <li>{@link FtpletResult#DISCONNECT}: The server will immediately
	 * disconnect the client.</li>
	 * <li>Ftplet throws exception: Same as {@link FtpletResult#DISCONNECT}</li>
	 * </ul>
	 * 
	 * @param session
	 *            The current session
	 * @param request
	 *            The current request
	 * @param reply
	 *            the reply that was sent for this command. Implementations can
	 *            use this to check the reply code and thus determine if the
	 *            command was successfully processed or not.
	 * @return The desired action to be performed by the server
	 * @throws FtpException
	 * @throws IOException
	 */
    FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply)
            throws FtpException, IOException;

    /**
     * Client connect notification method.
     * @param session The current {@link FtpSession}
     * @return The desired action to be performed by the server
     * @throws FtpException 
     * @throws IOException 
     */
    FtpletResult onConnect(FtpSession session) throws FtpException, IOException;

    /**
     * Client disconnect notification method. This is the last callback method.
     * @param session The current {@link FtpSession}
     * @return The desired action to be performed by the server
     * @throws FtpException 
     * @throws IOException 
     */
    FtpletResult onDisconnect(FtpSession session) throws FtpException,
            IOException;
}