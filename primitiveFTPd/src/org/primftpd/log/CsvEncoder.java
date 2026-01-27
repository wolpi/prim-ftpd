package org.primftpd.log;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.encoder.EncoderBase;

public class CsvEncoder
        extends EncoderBase<ILoggingEvent> {
    private static final int MAX_MSG_LEN = 250;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.S", Locale.GERMAN)
                             .withZone(ZoneId.systemDefault());

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();

        sb.append("\"")
          .append(DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())))
          .append("\";");
        sb.append("\"")
          .append(event.getLevel())
          .append("\";");
        sb.append("\"")
          .append(event.getLoggerName())
          .append("\";");

        String msg = event.getFormattedMessage();
        sb.append("\"");
        if (msg != null && msg.length() > MAX_MSG_LEN) {
            sb.append(msg, 0, MAX_MSG_LEN - 3).append("...");
        } else {
            sb.append(msg != null ? msg : "");
        }
        sb.append("\";");

        sb.append("\"");
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            appendThrowable(sb, tp);
        }
        sb.append("\";");
        sb.append("\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendThrowable(StringBuilder sb,
                                 IThrowableProxy tp) {
        sb.append(tp.getClassName()).append(": ").append(tp.getMessage());
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        if (stepArray != null) {
            for (StackTraceElementProxy step : stepArray) {
                sb.append("\n\t").append(step.getSTEAsString());
            }
        }
        if (tp.getCause() != null) {
            sb.append("\nCaused by: ");
            appendThrowable(sb, tp.getCause());
        }
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }
}