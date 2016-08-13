package org.primftpd.pojo;

/**
 * JDK versions before 8 did not contain a base64 decoder. But android does.
 * For test cases apache commons-codec is used.
 */
public interface Base64Decoder {
    byte[] decode(String str);
}
