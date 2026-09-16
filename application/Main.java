package application;

import java.io.File;

public final class Main {
    static {
        try {
            File dylib = new File("/Users/md.azizulhakimkhanrafid/Downloads/javafx-sdk-21.0.11/lib/libjfxwebkit.dylib");
            if (dylib.exists()) {
                System.load(dylib.getAbsolutePath());
            }
        } catch (Throwable ignored) {}
    }

    private Main() {
    }

    public static void main(String[] args) {
        LearningApplication.main(args);
    }
}

