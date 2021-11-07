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

package org.apache.ftpserver.ssl.impl;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import junit.framework.TestCase;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*
*/
public class ExtendedAliasKeymanagerTest extends TestCase {

    private KeyManager km;

    @Override
    protected void setUp() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");

        FileInputStream fis = new FileInputStream(
                "src/test/resources/keymanager-test.jks");
        ks.load(fis, "".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "".toCharArray());

        km = kmf.getKeyManagers()[0];
    }

    public void testServerAliasWithAliasDSAKey() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km, "dsakey");

        assertEquals("dsakey", akm.chooseServerAlias("DSA", null, null));
        assertEquals(null, akm.chooseServerAlias("RSA", null, null));
    }

    public void testServerAliasWithAliasRSAKey() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km, "rsakey");

        assertEquals(null, akm.chooseServerAlias("DSA", null, null));
        assertEquals("rsakey", akm.chooseServerAlias("RSA", null, null));
    }

    public void testServerAliasWithoutAlias() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km, null);

        assertEquals("dsakey", akm.chooseServerAlias("DSA", null, null));
        assertEquals("rsakey", akm.chooseServerAlias("RSA", null, null));
    }

    public void testServerAliasNonExistingKey() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km,
                "nonexisting");

        assertEquals(null, akm.chooseServerAlias("DSA", null, null));
        assertEquals(null, akm.chooseServerAlias("RSA", null, null));
    }

    public void testEngineServerAliasWithAliasDSAKey() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km, "dsakey");

        assertEquals("dsakey", akm.chooseEngineServerAlias("DSA", null, null));
        assertEquals(null, akm.chooseEngineServerAlias("RSA", null, null));
    }

    public void testEngineServerAliasWithAliasRSAKey() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km, "rsakey");

        assertEquals(null, akm.chooseEngineServerAlias("DSA", null, null));
        assertEquals("rsakey", akm.chooseEngineServerAlias("RSA", null, null));
    }

    public void testEngineServerAliasWithoutAlias() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km, null);

        assertEquals("dsakey", akm.chooseEngineServerAlias("DSA", null, null));
        assertEquals("rsakey", akm.chooseEngineServerAlias("RSA", null, null));
    }

    public void testEngineServerAliasNonExistingKey() throws Exception {
        ExtendedAliasKeyManager akm = new ExtendedAliasKeyManager(km,
                "nonexisting");

        assertEquals(null, akm.chooseEngineServerAlias("DSA", null, null));
        assertEquals(null, akm.chooseEngineServerAlias("RSA", null, null));
    }
}