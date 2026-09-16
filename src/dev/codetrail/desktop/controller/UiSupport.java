package dev.codetrail.desktop.controller;

import javafx.application.Platform;

import java.util.concurrent.CompletionException;

final class UiSupport {
    private UiSupport() {
    }

    static void fx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    static String errorMessage(Throwable failure) {
        if (failure == null) {
            return "The request could not be completed.";
        }
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null || cause.getMessage().isBlank()
                ? "The request could not be completed." : cause.getMessage();
    }
}
