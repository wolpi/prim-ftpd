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

package org.apache.ftpserver.command.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.mina.filter.ssl.SslFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Internal class, do not use directly.</strong>
 * 
 * This server supports explicit SSL support.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AUTH extends AbstractCommand {

    private static final String SSL_SESSION_FILTER_NAME = "sslSessionFilter";

    private final Logger LOG = LoggerFactory.getLogger(AUTH.class);

    private static final List<String> VALID_AUTH_TYPES = Arrays.asList("SSL", "TLS", "TLS-C", "TLS-P");

    /**
     * Execute command
     */
    public void execute(final FtpIoSession session,
            final FtpServerContext context, final FtpRequest request)
            throws IOException, FtpException {

        // reset state variables
        session.resetState();

        // argument check
        if (!request.hasArgument()) {
            session.write(LocalizedFtpReply.translate(session, request, context,
                    FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                    "AUTH", null));
            return;
        }

        // check SSL configuration
        if (session.getListener().getSslConfiguration() == null) {
            session.write(LocalizedFtpReply.translate(session, request, context,
                    431, "AUTH", null));
            return;
        }

        // check that we don't already have a SSL filter in place due to running
        // in implicit mode
        // or because the AUTH command has already been issued. This is what the
        // RFC says:

        // "Some servers will allow the AUTH command to be reissued in order
        // to establish new authentication. The AUTH command, if accepted,
        // removes any state associated with prior FTP Security commands.
        // The server must also require that the user reauthorize (that is,
        // reissue some or all of the USER, PASS, and ACCT commands) in this
        // case (see section 4 for an explanation of "authorize" in this
        // context)."

        // Here we choose not to support reissued AUTH
        if (session.getFilterChain().contains(SslFilter.class)) {
            session.write(LocalizedFtpReply.translate(session, request, context,
                    534, "AUTH", null));
            return;
        }

        // check parameter
        String authType = request.getArgument().toUpperCase();
        if (VALID_AUTH_TYPES.contains(authType)) {
            if(authType.equals("TLS-C")) {
                authType = "TLS";
            } else if(authType.equals("TLS-P")) {
                authType = "SSL";
            }

            try {
                secureSession(session, authType);
                session.write(LocalizedFtpReply.translate(session, request, context,
                        234, "AUTH." + authType, null));
            } catch (FtpException ex) {
                throw ex;
            } catch (Exception ex) {
                LOG.warn("AUTH.execute()", ex);
                throw new FtpException("AUTH.execute()", ex);
            }
        } else {
            session.write(LocalizedFtpReply.translate(session, request, context,
                    FtpReply.REPLY_502_COMMAND_NOT_IMPLEMENTED, "AUTH", null));
        }
    }

    private void secureSession(final FtpIoSession session, final String type)
            throws GeneralSecurityException, FtpException {
        SslConfiguration ssl = session.getListener().getSslConfiguration();

        if (ssl != null) {
            session.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE);

            SslFilter sslFilter = new SslFilter(ssl.getSSLContext());
            if (ssl.getClientAuth() == ClientAuth.NEED) {
                sslFilter.setNeedClientAuth(true);
            } else if (ssl.getClientAuth() == ClientAuth.WANT) {
                sslFilter.setWantClientAuth(true);
            }

            // note that we do not care about the protocol, we allow both types
            // and leave it to the SSL handshake to determine the protocol to
            // use. Thus the type argument is ignored.

            if (ssl.getEnabledCipherSuites() != null) {
                sslFilter.setEnabledCipherSuites(ssl.getEnabledCipherSuites());
            }

            session.getFilterChain().addFirst(SSL_SESSION_FILTER_NAME,
                    sslFilter);

            if("SSL".equals(type)) {
                session.getDataConnection().setSecure(true);
            }
        } else {
            throw new FtpException("Socket factory SSL not configured");
        }
    }
}
