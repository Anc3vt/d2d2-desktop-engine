package com.ancevt.d2d2.engine.desktop;

import com.ancevt.d2d2.log.Logger;

import java.io.PrintStream;

public class DesktopLogger implements Logger {
    private int level = INFO;
    private boolean colorized = true;

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setColorized(boolean colorized) {
        this.colorized = colorized;
    }

    @Override
    public boolean isColorized() {
        return colorized;
    }

    private void logMessage(Object tag, Object msg, PrintStream stream, Throwable throwable) {
        String tagStr = (tag instanceof Class<?> clazz) ? clazz.getSimpleName() : String.valueOf(tag);
        String formattedMsg = UnixTextColorFilter.filterText(String.valueOf(msg), colorized);
        stream.printf("%s: %s%n", tagStr, formattedMsg);
        if (throwable != null) {
            throwable.printStackTrace(stream);
        }
    }

    @Override
    public void error(Object tag, Object msg) {
        if (level < ERROR) return;
        logMessage(tag, msg, System.err, null);
    }

    @Override
    public void error(Object tag, Object msg, Throwable throwable) {
        if (level < ERROR) return;
        logMessage(tag, msg, System.err, throwable);
    }

    @Override
    public void info(Object tag, Object msg) {
        if (level < INFO) return;
        logMessage(tag, msg, System.out, null);
    }

    @Override
    public void debug(Object tag, Object msg) {
        if (level < DEBUG) return;
        logMessage(tag, msg, System.out, null);
    }

}
