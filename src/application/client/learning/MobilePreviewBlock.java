package application.client.learning;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.InputStream;

/**
 * MobilePreviewBlock provides a realistic mobile phone device frame preview
 * rendering mobile UI components, interactive states, and component hierarchy inspection.
 */
public class MobilePreviewBlock implements LearningBlock {
    private final String id;
    private final String title;
    private final String framework;
    private final String appTitle;
    private final String stateDebugNote;

    public MobilePreviewBlock(String id, String title, String framework, String appTitle, String stateDebugNote) {
        this.id = id;
        this.title = title;
        this.framework = framework != null ? framework : "Flutter";
        this.appTitle = appTitle != null ? appTitle : "Mobile App";
        this.stateDebugNote = stateDebugNote;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public CourseCategory category() {
        return CourseCategory.APP_DEV;
    }

    @Override
    public Node render(boolean isDark) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 20, 16, 20));

        card.setStyle(
                "-fx-background-color: " + (isDark ? "#121926" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#223147" : "#e2e8f0") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView phoneIconView = application.client.util.LucideIcons.icon("smartphone", 18, isDark);
        HBox phoneIconBadge = new HBox(phoneIconView);
        phoneIconBadge.setAlignment(Pos.CENTER);
        phoneIconBadge.setPadding(new Insets(3, 4, 3, 4));
        phoneIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#3b0764" : "#f3e8ff") + ";" +
                "-fx-border-color: " + (isDark ? "#7c3aed" : "#a855f7") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("MOBILE DEVICE PREVIEW: " + title.toUpperCase());
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #a855f7; -fx-letter-spacing: 1.1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(framework + " Viewport (375 × 667)");
        badge.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #a855f7; " +
                "-fx-background-color: " + (isDark ? "#3b0764" : "#f3e8ff") + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;"
        );

        header.getChildren().addAll(phoneIconBadge, heading, spacer, badge);

        // Center Container for the Phone Mockup
        VBox phoneCenter = new VBox();
        phoneCenter.setAlignment(Pos.CENTER);
        phoneCenter.setPadding(new Insets(10, 0, 10, 0));

        // Phone Outer Chassis
        VBox phone = new VBox();
        phone.setMaxWidth(340);
        phone.setPrefWidth(340);
        phone.setStyle(
                "-fx-background-color: #0b0f17;" +
                "-fx-border-color: #334155;" +
                "-fx-border-width: 4px;" +
                "-fx-border-radius: 30px;" +
                "-fx-background-radius: 30px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.35), 14, 0, 0, 6);"
        );

        // Phone Top Notch / Island
        HBox topNotchBar = new HBox();
        topNotchBar.setAlignment(Pos.CENTER);
        topNotchBar.setPadding(new Insets(8, 14, 4, 14));

        Label timeLabel = new Label("9:41");
        timeLabel.setStyle("-fx-font-family: -apple-system, sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        Region notchSpacerL = new Region();
        HBox.setHgrow(notchSpacerL, Priority.ALWAYS);

        // Dynamic Island
        Region island = new Region();
        island.setPrefWidth(64);
        island.setPrefHeight(14);
        island.setStyle("-fx-background-color: #000000; -fx-background-radius: 12px;");

        Region notchSpacerR = new Region();
        HBox.setHgrow(notchSpacerR, Priority.ALWAYS);

        Label battery = new Label("100%");
        battery.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        topNotchBar.getChildren().addAll(timeLabel, notchSpacerL, island, notchSpacerR, battery);

        // Mobile Screen Area
        VBox screen = new VBox();
        screen.setPrefHeight(380);
        screen.setStyle(
                "-fx-background-color: " + (isDark ? "#141c2b" : "#f8fafc") + ";" +
                "-fx-border-radius: 0 0 24px 24px; -fx-background-radius: 0 0 24px 24px;"
        );

        // Mobile AppBar
        HBox appBar = new HBox(8);
        appBar.setAlignment(Pos.CENTER_LEFT);
        appBar.setPadding(new Insets(10, 14, 10, 14));
        appBar.setStyle(
                "-fx-background-color: " + (isDark ? "#1e293b" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#334155" : "#e2e8f0") + "; -fx-border-width: 0 0 1px 0;"
        );

        Label backGlyph = new Label("←");
        backGlyph.setStyle("-fx-font-size: 14px; -fx-text-fill: #a855f7;");

        Label appTitleLabel = new Label(appTitle);
        appTitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + ";");

        Region appSpacer = new Region();
        HBox.setHgrow(appSpacer, Priority.ALWAYS);

        Label menuGlyph = new Label("⋮");
        menuGlyph.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        appBar.getChildren().addAll(backGlyph, appTitleLabel, appSpacer, menuGlyph);

        // Screen Body: Interactive Demo Component
        VBox screenContent = new VBox(10);
        screenContent.setPadding(new Insets(14, 16, 14, 16));
        VBox.setVgrow(screenContent, Priority.ALWAYS);

        Label welcomeMsg = new Label("Flutter Reactive State Demo");
        welcomeMsg.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + ";");

        // Counter Card inside mobile screen
        VBox counterCard = new VBox(8);
        counterCard.setAlignment(Pos.CENTER);
        counterCard.setPadding(new Insets(14, 12, 14, 12));
        counterCard.setStyle(
                "-fx-background-color: " + (isDark ? "#1e293b" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#334155" : "#cbd5e1") + ";" +
                "-fx-border-radius: 12px; -fx-background-radius: 12px;"
        );

        Label countTitle = new Label("Current Counter Value:");
        countTitle.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#475569") + ";");

        final int[] counter = { 0 };
        Label countDisplay = new Label("0");
        countDisplay.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #a855f7;");

        // State inspector badge below
        Label stateBadge = new Label("State: { counter: 0 }");
        stateBadge.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 10px; -fx-text-fill: #38bdf8;");

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER);

        Button decBtn = new Button("−");
        decBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #ffffff; -fx-font-weight: 800; -fx-font-size: 13px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 4 12;");
        decBtn.setOnAction(e -> {
            counter[0]--;
            countDisplay.setText(String.valueOf(counter[0]));
            stateBadge.setText("State: { counter: " + counter[0] + " }");
        });

        Button incBtn = new Button("+");
        incBtn.setStyle("-fx-background-color: #a855f7; -fx-text-fill: #ffffff; -fx-font-weight: 800; -fx-font-size: 13px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 4 12;");
        incBtn.setOnAction(e -> {
            counter[0]++;
            countDisplay.setText(String.valueOf(counter[0]));
            stateBadge.setText("State: { counter: " + counter[0] + " }");
        });

        btnRow.getChildren().addAll(decBtn, incBtn);
        counterCard.getChildren().addAll(countTitle, countDisplay, btnRow);

        // Task Item Checklist
        VBox taskList = new VBox(6);
        Label listTitle = new Label("Screen Component Hierarchy:");
        listTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + ";");

        CheckBox cb1 = new CheckBox("Scaffold & SafeArea");
        cb1.setSelected(true);
        cb1.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + ";");

        CheckBox cb2 = new CheckBox("StatefulWidget: CounterCard");
        cb2.setSelected(true);
        cb2.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + ";");

        CheckBox cb3 = new CheckBox("BottomNavigationBar");
        cb3.setSelected(true);
        cb3.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + ";");

        taskList.getChildren().addAll(listTitle, cb1, cb2, cb3);

        screenContent.getChildren().addAll(welcomeMsg, counterCard, taskList);

        // Mobile Bottom Nav Bar
        HBox bottomNav = new HBox(20);
        bottomNav.setAlignment(Pos.CENTER);
        bottomNav.setPadding(new Insets(8, 14, 8, 14));
        bottomNav.setStyle(
                "-fx-background-color: " + (isDark ? "#1e293b" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#334155" : "#e2e8f0") + "; -fx-border-width: 1px 0 0 0;"
        );

        Label nav1 = new Label("Home");
        nav1.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #a855f7;");

        Label nav2 = new Label("Search");
        nav2.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

        Label nav3 = new Label("Profile");
        nav3.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");

        bottomNav.getChildren().addAll(nav1, nav2, nav3);

        // Home Indicator Line
        HBox homeIndicatorBar = new HBox();
        homeIndicatorBar.setAlignment(Pos.CENTER);
        homeIndicatorBar.setPadding(new Insets(4, 0, 6, 0));
        Region homeBar = new Region();
        homeBar.setPrefWidth(90);
        homeBar.setPrefHeight(4);
        homeBar.setStyle("-fx-background-color: #64748b; -fx-background-radius: 2px;");
        homeIndicatorBar.getChildren().add(homeBar);

        screen.getChildren().addAll(appBar, screenContent, bottomNav, homeIndicatorBar);
        phone.getChildren().addAll(topNotchBar, screen);
        phoneCenter.getChildren().add(phone);

        card.getChildren().addAll(header, phoneCenter);

        // State Debugger Callout
        if (stateDebugNote != null && !stateDebugNote.isBlank()) {
            VBox debugBox = new VBox(4);
            debugBox.setPadding(new Insets(10, 12, 10, 12));
            debugBox.setStyle(
                    "-fx-background-color: " + (isDark ? "#2e1065" : "#faf5ff") + ";" +
                    "-fx-border-color: " + (isDark ? "#581c87" : "#e9d5ff") + ";" +
                    "-fx-border-radius: 6px; -fx-background-radius: 6px;"
            );

            ImageView bugIconView = new ImageView();
            String bugIconPath = isDark ? "/resources/images/icon-debug-dark.png" : "/resources/images/icon-debug.png";
            try {
                InputStream is = getClass().getResourceAsStream(bugIconPath);
                if (is != null) {
                    bugIconView.setImage(new Image(is));
                    bugIconView.setFitWidth(16);
                    bugIconView.setFitHeight(16);
                    bugIconView.setSmooth(true);
                }
            } catch (Throwable ignored) {}

            Label dbgTitle = new Label("STATE & COMPONENT ARCHITECTURE");
            dbgTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#d8b4fe" : "#7e22ce") + "; -fx-letter-spacing: 0.8px;");

            HBox debugHeader = new HBox(8, bugIconView, dbgTitle, stateBadge);
            debugHeader.setAlignment(Pos.CENTER_LEFT);

            Label dbgText = new Label(stateDebugNote);
            dbgText.setWrapText(true);
            dbgText.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#f3e8ff" : "#581c87") + "; -fx-line-spacing: 2px;");

            debugBox.getChildren().addAll(debugHeader, dbgText);
            card.getChildren().add(debugBox);
        }

        return card;
    }
}
