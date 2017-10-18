package org.primftpd.prefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum StorageType {

    PLAIN("1"),
    ROOT("2"),
    SAF("3"),
    RO_SAF("4");

    private final String xmlValue;
    private StorageType(String xmlValue) {
        this.xmlValue = xmlValue;
    }
    public String xmlValue() {
        return xmlValue;
    }

    private static final Map<String, StorageType> XML_TO_ENUM;
    static {
        Map<String, StorageType> tmp = new HashMap<>();
        for (StorageType storageType : values()) {
            tmp.put(storageType.xmlValue, storageType);
        }
        XML_TO_ENUM = Collections.unmodifiableMap(tmp);
    }

    public static StorageType byXmlVal(String xmlVal) {
        return XML_TO_ENUM.get(xmlVal);
    }
}
