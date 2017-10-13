package org.primftpd.filesystem;

import org.primftpd.pojo.LsOutputBean;
import org.primftpd.pojo.LsOutputParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public abstract class RootFileSystemView<T extends RootFile<X>, X> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract T createFile(LsOutputBean bean, String absPath);

    protected abstract String absolute(String file);

    public T getFile(String file) {
        logger.trace("getFile({})", file);

        file = absolute(file);

        LsOutputParser parser = new LsOutputParser();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("su", "-c", "ls", "-lAd", file);
        try {
            Process proc = processBuilder.start();
            List<LsOutputBean> beans = parser.parse(proc.getInputStream());
            if (!beans.isEmpty()) {
                return createFile(beans.get(0), file);
            } else {
                // probably new
                String name;
                if (file.contains("/")) {
                    name = file.substring(file.lastIndexOf('/') + 1, file.length());
                } else {
                    name = file;
                }
                LsOutputBean bean = new LsOutputBean(name);
                return createFile(bean, file);
            }
        } catch (IOException e) {
            logger.error("could not run su", e);
        }
        logger.error("bad path: '{}'", file);
        return null;
    }
}
