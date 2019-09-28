/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sshd.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;

public class SecurityUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);

    public static boolean isBouncyCastleRegistered() {
        return true;
    }

    private static String safeClassname(Object obj) {
        return obj != null ? obj.getClass().getName() : "null";
    }

    public static synchronized KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyFactory result = KeyFactory.getInstance(algorithm);
        LOG.trace("getKeyFactory({}) -> {}", algorithm, safeClassname(result));
        return (KeyFactory)result;
    }

    public static synchronized Cipher getCipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        Cipher result = Cipher.getInstance(transformation);
        LOG.trace("getCipher({}) -> {}", transformation, safeClassname(result));
        return result;
    }

    public static synchronized MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest result = MessageDigest.getInstance(algorithm);
        LOG.trace("getMessageDigest({}) -> {}", algorithm, safeClassname(result));
        return result;
    }

    public static synchronized KeyPairGenerator getKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator result = KeyPairGenerator.getInstance(algorithm);
        LOG.trace("KeyPairGenerator({}) -> {}", algorithm, safeClassname(result));
        return result;
    }

    public static synchronized KeyAgreement getKeyAgreement(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyAgreement result = KeyAgreement.getInstance(algorithm);
        LOG.trace("getKeyAgreement({}) -> {}", algorithm, safeClassname(result));
        return result;
    }

    public static synchronized Mac getMac(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        Mac result = Mac.getInstance(algorithm);
        LOG.trace("getMac({}) -> {}", algorithm, safeClassname(result));
        return result;
    }

    public static synchronized Signature getSignature(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        Signature result = Signature.getInstance(algorithm);
        LOG.trace("getSignature({}) -> {}", algorithm, safeClassname(result));
        return result;
    }

}
