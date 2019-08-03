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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpStatistics;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.impl.DefaultFtpRequest;
import org.apache.ftpserver.impl.DefaultFtpSession;

/*
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class FtpLetContainerTestTemplate extends TestCase {

    private final List<String> calls = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        MockFtplet.callback = new MockFtpletCallback();
        MockFtpletCallback.returnValue = FtpletResult.DEFAULT;
    }

    protected abstract FtpletContainer createFtpletContainer(Map<String, Ftplet> ftplets);

    private static class MockFtpletContext implements FtpletContext {
        public FileSystemFactory getFileSystemManager() {
            return null;
        }

        public FtpStatistics getFtpStatistics() {
            return null;
        }

        public Ftplet getFtplet(String name) {
            return null;
        }

        public UserManager getUserManager() {
            return null;
        }
    }
    
    public void testAddAndGetFtplet() {
        MockFtplet ftplet1 = new MockFtplet();
        MockFtplet ftplet2 = new MockFtplet();

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);
        
        FtpletContainer container = createFtpletContainer(ftplets);
        
        assertSame(ftplet1, container.getFtplet("ftplet1"));
        assertSame(ftplet2, container.getFtplet("ftplet2"));
    }
    
    public void testFtpletLifecyclePreContainerInit() throws FtpException {
        MockFtplet ftplet = new MockFtplet();

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet);

        FtpletContainer container = createFtpletContainer(ftplets);

        // ftplet should be initialized before the container is
        assertNull(ftplet.context);
        container.init(new MockFtpletContext());
        assertNotNull(ftplet.context);
        
        // make sure ftplets get's destroyed
        assertFalse(ftplet.destroyed);
        
        container.destroy();

        assertTrue(ftplet.destroyed);
        
    }
    
    public void testOnConnect() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onConnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onConnect(session);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onConnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onConnect(session);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.onConnect(new DefaultFtpSession(null));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnDisconnect() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onDisconnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onDisconnect(session);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onDisconnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onDisconnect(session);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);
        
        container.onDisconnect(new DefaultFtpSession(null));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnLogin() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onLogin(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onLogin(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onLogin(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onLogin(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "PASS"), null);

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnDeleteStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onDeleteStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onDeleteStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onDeleteStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onDeleteStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "DELE"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnDeleteEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onDeleteEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onDeleteEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "DELE"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnUploadStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onUploadStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onUploadStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onUploadStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onUploadStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "STOR"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnUploadEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onUploadEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onUploadEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onUploadEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onUploadEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "STOR"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnDownloadStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onDownloadStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onDownloadStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onDownloadStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onDownloadStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "RETR"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnDownloadEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onDownloadEnd(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onDownloadEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onDownloadEnd(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onDownloadEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "RETR"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnRmdirStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onRmdirStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onRmdirStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onRmdirStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onRmdirStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "RMD"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnRmdirEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onRmdirEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onRmdirEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "RMD"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnMkdirStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onMkdirStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onMkdirStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onMkdirStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onMkdirStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "MKD"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnMkdirEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onMkdirEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onMkdirEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "MKD"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnAppendStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onAppendStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onAppendStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onAppendStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onAppendStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "APPE"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnAppendEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onAppendEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onAppendEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onAppendEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onAppendEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "APPE"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnUploadUniqueStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onUploadUniqueStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onUploadUniqueStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onUploadUniqueStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onUploadUniqueStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "STOU"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnUploadUniqueEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onUploadUniqueEnd(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onUploadUniqueEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onUploadUniqueEnd(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onUploadUniqueEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "STOU"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnRenameStart() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onRenameStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onRenameStart(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onRenameStart(FtpSession session,
                    FtpRequest request) throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onRenameStart(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "RNTO"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnRenameEnd() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onRenameEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onRenameEnd(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onRenameEnd(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onRenameEnd(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.afterCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "RNTO"), new DefaultFtpReply(200, "foo"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    public void testOnSite() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onSite(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onSite(session, request);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onSite(FtpSession session, FtpRequest request)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onSite(session, request);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.beforeCommand(new DefaultFtpSession(null), new DefaultFtpRequest(
                "SITE"));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    /**
     * First test checking the call order of Ftplets
     */
    public void testFtpletCallOrder1() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onConnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onConnect(session);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onConnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onConnect(session);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet1", ftplet1);
        ftplets.put("ftplet2", ftplet2);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.onConnect(new DefaultFtpSession(null));

        assertEquals(2, calls.size());
        assertEquals("ftplet1", calls.get(0));
        assertEquals("ftplet2", calls.get(1));
    }

    /**
     * First test checking the call order of Ftplets
     */
    public void testFtpletCallOrder2() throws FtpException, IOException {
        MockFtplet ftplet1 = new MockFtplet() {
            @Override
            public FtpletResult onConnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet1");
                return super.onConnect(session);
            }
        };
        MockFtplet ftplet2 = new MockFtplet() {
            @Override
            public FtpletResult onConnect(FtpSession session)
                    throws FtpException, IOException {
                calls.add("ftplet2");
                return super.onConnect(session);
            }
        };

        Map<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        ftplets.put("ftplet2", ftplet2);
        ftplets.put("ftplet1", ftplet1);

        FtpletContainer container = createFtpletContainer(ftplets);

        container.onConnect(new DefaultFtpSession(null));

        assertEquals(2, calls.size());
        assertEquals("ftplet2", calls.get(0));
        assertEquals("ftplet1", calls.get(1));
    }

}
