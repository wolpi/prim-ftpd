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

package org.apache.ftpserver.example.springwar;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ftpserver.FtpServer;

/*
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class FtpServerServlet extends HttpServlet {

    private static final long serialVersionUID = 5539642787624981705L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        FtpServer server = (FtpServer) getServletContext().getAttribute(FtpServerListener.FTPSERVER_CONTEXT_NAME);
        
        PrintWriter wr = resp.getWriter();
        
        wr.print("<html>");
        wr.print("<head>");
        wr.print("<title>FtpServer status servlet</title>");
        wr.print("</head>");
        wr.print("<body>");
        wr.print("<form method='post'>");


        if(server.isStopped()) {
            wr.print("<p>FtpServer is stopped.</p>");
        } else {
            if(server.isSuspended()) {
                wr.print("<p>FtpServer is suspended.</p>");
                wr.print("<p><input type='submit' name='resume' value='Resume'></p>");
                wr.print("<p><input type='submit' name='stop' value='Stop'></p>");
            } else {
                wr.print("<p>FtpServer is running.</p>");
                wr.print("<p><input type='submit' name='suspend' value='Suspend'></p>");
                wr.print("<p><input type='submit' name='stop' value='Stop'></p>");
            }
        }
        
        wr.print("</form>");
        wr.print("</body>");
        wr.print("</html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        FtpServer server = (FtpServer) getServletContext().getAttribute(FtpServerListener.FTPSERVER_CONTEXT_NAME);
        
        if(req.getParameter("stop") != null) {
            server.stop();
        } else if(req.getParameter("resume") != null) {
            server.resume();
        } else if(req.getParameter("suspend") != null) {
            server.suspend();
        }
        
        resp.sendRedirect("/");
    }

}
