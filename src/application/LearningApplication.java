package application;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import application.backend.BackendApplication;
import application.client.controller.AppController;
import application.client.service.ApiClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class LearningApplication extends Application {
    private ConfigurableApplicationContext applicationContext;
    private AppController controller;

    @Override
    public void init() {
        List<String> backendArguments = new ArrayList<>();
        backendArguments.add("--server.port=0");
        backendArguments.addAll(getParameters().getRaw());

        applicationContext = new SpringApplicationBuilder(BackendApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .headless(false)
                .web(WebApplicationType.SERVLET)
                .run(backendArguments.toArray(String[]::new));

        int port = ((ServletWebServerApplicationContext) applicationContext).getWebServer().getPort();
        applicationContext.getBean(ApiClient.class)
                .setBaseUri(URI.create("http://127.0.0.1:" + port + "/api/"));
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/MainView.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Scene scene = new Scene(loader.load(), 1280, 820);
        scene.getStylesheets().add(getClass().getResource("/resources/css/application.css").toExternalForm());
        controller = loader.getController();

        stage.setTitle("CodeTrail - Learning Platform");
        stage.setMinWidth(1040);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
