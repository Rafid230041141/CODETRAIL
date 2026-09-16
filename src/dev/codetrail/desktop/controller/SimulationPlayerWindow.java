package dev.codetrail.desktop.controller;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Dedicated popup/modal window manager for the full VisuAlgo-grade
 * Simulation Studio.
 */
public final class SimulationPlayerWindow {
    private SimulationPlayerWindow() {
    }

    public static Stage open(String type, String configJson, boolean darkMode, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SimulationPlayerWindow.class.getResource("/dev/codetrail/desktop/fxml/SimulationPlayerScreen.fxml"));
            Parent root = loader.load();
            SimulationPlayerController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Simulation Studio - " + type);
            if (owner != null) {
                stage.initOwner(owner);
            }

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenW = screenBounds.getWidth();
            double screenH = screenBounds.getHeight();
            Scene scene = new Scene(root, screenW, screenH);

            if (darkMode) {
                root.getStyleClass().add("dark-theme");
                controller.setDarkMode(true);
            }
            stage.setScene(scene);
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenW);
            stage.setHeight(screenH);
            stage.setMaximized(true);

            controller.setOnBackAction(stage::close);
            controller.load(type, configJson);

            stage.setOnCloseRequest(event -> controller.stopPlayback());
            stage.show();
            return stage;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
