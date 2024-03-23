package com.github.coderodde.wikipedia.game.killer.fx;

import com.github.coderodde.graph.pathfinding.delayed.impl.ThreadPoolBidirectionalBFSPathFinder;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public final class WikiGameKillerFX extends Application {

    private static final Font FONT = 
            Font.font("monospaced", FontWeight.BOLD, 11);
        
    private final TextField sourceTextField             = new TextField();
    private final TextField targetTextField             = new TextField();
    private final TextField threadsTextField            = new TextField();
    private final TextField expansionoDurationTextField = new TextField();
    private final TextField waitTimeoutTextField        = new TextField();
    private final TextField masterTrialsTextField       = new TextField();
    private final TextField masterSleepTextField        = new TextField();
    private final TextField slaveSleepTextField         = new TextField();
    
    private final TextArea resultTextArea  = new TextArea();
    
    private final HBox statusBarHBox = new HBox();
    private final Label statusBarLabel = new Label();
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        final VBox mainBox = new VBox();;
        
        final Label sourceLabel            = new Label("Source article:        ");
        final Label targetLabel            = new Label("Target article:        ");
        final Label threadsLabel           = new Label("Number of threads:     ");
        final Label expansionDurationLabel = new Label("Expansion duration:    ");
        final Label waitTimeoutLabel       = new Label("Wait timeout:          ");
        final Label masterTrialsLabel      = new Label("Master trials:         ");
        final Label masterSleepLabel       = new Label("Master sleep duration: ");
        final Label slaveSleepLabel        = new Label("Slave sleep duration:  ");
        
        sourceLabel            .setFont(FONT);
        targetLabel            .setFont(FONT);
        threadsLabel           .setFont(FONT);
        expansionDurationLabel .setFont(FONT);
        waitTimeoutLabel       .setFont(FONT);
        masterTrialsLabel      .setFont(FONT);
        masterSleepLabel       .setFont(FONT);
        slaveSleepLabel        .setFont(FONT);
        
        sourceTextField             .setFont(FONT);
        targetTextField             .setFont(FONT);
        threadsTextField            .setFont(FONT);
        expansionoDurationTextField .setFont(FONT);
        waitTimeoutTextField        .setFont(FONT);
        masterTrialsTextField       .setFont(FONT);
        masterSleepTextField        .setFont(FONT);
        slaveSleepTextField         .setFont(FONT);
        
        threadsTextField            .textProperty().addListener(new TextFieldChangeListener(threadsTextField));
        expansionoDurationTextField .textProperty().addListener(new TextFieldChangeListener(expansionoDurationTextField));
        waitTimeoutTextField        .textProperty().addListener(new TextFieldChangeListener(waitTimeoutTextField));
        masterTrialsTextField       .textProperty().addListener(new TextFieldChangeListener(masterTrialsTextField));
        masterSleepTextField        .textProperty().addListener(new TextFieldChangeListener(masterSleepTextField));
        slaveSleepTextField         .textProperty().addListener(new TextFieldChangeListener(slaveSleepTextField));
        
        final HBox sourceRowBox            = new HBox();
        final HBox targetRowBox            = new HBox();
        final HBox threadsRowBox           = new HBox();
        final HBox expansionDurationRowBox = new HBox();
        final HBox waitTimeoutRowBox       = new HBox();
        final HBox masterTrialsRowBox      = new HBox();
        final HBox masterSleepRowBox       = new HBox();
        final HBox slaveSleepRowBox        = new HBox();
        final HBox buttonsRowBox           = new HBox();
        
        final Insets rowBoxInsets = new Insets(3.0);
        
        sourceRowBox            .setPadding(rowBoxInsets);
        targetRowBox            .setPadding(rowBoxInsets);
        threadsRowBox           .setPadding(rowBoxInsets);
        expansionDurationRowBox .setPadding(rowBoxInsets);
        waitTimeoutRowBox       .setPadding(rowBoxInsets);
        masterTrialsRowBox      .setPadding(rowBoxInsets);
        masterSleepRowBox       .setPadding(rowBoxInsets);
        slaveSleepRowBox        .setPadding(rowBoxInsets);
        buttonsRowBox           .setPadding(rowBoxInsets);
        
        sourceRowBox.setAlignment            (Pos.CENTER_LEFT);
        targetRowBox.setAlignment            (Pos.CENTER_LEFT);
        threadsRowBox.setAlignment           (Pos.CENTER_LEFT);
        expansionDurationRowBox.setAlignment (Pos.CENTER_LEFT);
        waitTimeoutRowBox.setAlignment       (Pos.CENTER_LEFT);
        masterTrialsRowBox.setAlignment      (Pos.CENTER_LEFT);
        masterSleepRowBox.setAlignment       (Pos.CENTER_LEFT);
        slaveSleepRowBox.setAlignment        (Pos.CENTER_LEFT);
        
        sourceRowBox.getChildren().addAll(sourceLabel, sourceTextField);
        targetRowBox.getChildren().addAll(targetLabel, targetTextField);
        threadsRowBox.getChildren().addAll(threadsLabel, threadsTextField);
        expansionDurationRowBox.getChildren()
                               .addAll(expansionDurationLabel,
                                       expansionoDurationTextField);
        
        waitTimeoutRowBox.getChildren().addAll(waitTimeoutLabel, 
                                               waitTimeoutTextField);
        
        masterTrialsRowBox.getChildren().addAll(masterTrialsLabel,
                                                masterTrialsTextField);
        
        masterSleepRowBox.getChildren().addAll(masterSleepLabel,
                                               masterSleepTextField);
        
        slaveSleepRowBox.getChildren().addAll(slaveSleepLabel, 
                                              slaveSleepTextField);
//        mainBox.setPadding(new Insets(7.0));
        
        setDefaultSettings();
        
        final Button searchButton          = new Button("Search");
        final Button haltButton            = new Button("Halt");
        final Button defaultSettingsButton = new Button("Set defaults");
        
        statusBarHBox.setMaxHeight(30.0);
        
        final BorderStroke borderStroke =
                new BorderStroke(
                        Color.GRAY, 
                        null,
                        null, 
                        null,
                        BorderStrokeStyle.SOLID,
                        BorderStrokeStyle.NONE, 
                        BorderStrokeStyle.NONE, 
                        BorderStrokeStyle.NONE, 
                        CornerRadii.EMPTY,
                        BorderWidths.DEFAULT, 
                        new Insets(10.0, 0.0, 0.0, 0.0));
        
        final Border statusBarBorder = new Border(borderStroke);
        
        statusBarHBox.setBorder(statusBarBorder);
        statusBarHBox.getChildren().add(statusBarLabel);
        statusBarLabel.setFont(FONT);
        
        buttonsRowBox.getChildren().addAll(searchButton,
                                           defaultSettingsButton,
                                           haltButton);
        
        defaultSettingsButton.setOnAction((ActionEvent t) -> {
            setDefaultSettings();
        });
        
        
        
        mainBox.getChildren()
               .addAll(sourceRowBox,
                       targetRowBox,
                       threadsRowBox,
                       expansionDurationRowBox,
                       waitTimeoutRowBox,
                       masterTrialsRowBox,
                       masterSleepRowBox,
                       slaveSleepRowBox,
                       buttonsRowBox,
                       statusBarHBox);
        
        final StackPane root = new StackPane();
        root.getChildren().add(mainBox);
        
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
    
    private void setDefaultSettings() {
        
        threadsTextField.setText(
                Integer.toString(
                        ThreadPoolBidirectionalBFSPathFinder
                                .DEFAULT_NUMBER_OF_THREADS));
        
        expansionoDurationTextField.setText(
                Integer.toString(
                        ThreadPoolBidirectionalBFSPathFinder
                                .DEFAULT_EXPANSION_JOIN_DURATION_MILLIS));
        
        waitTimeoutTextField.setText(
                Integer.toString(
                        ThreadPoolBidirectionalBFSPathFinder
                                .DEFAULT_LOCK_WAIT_MILLIS));
        
        masterTrialsTextField.setText(
                Integer.toString(
                        ThreadPoolBidirectionalBFSPathFinder
                                .DEFAULT_NUMBER_OF_MASTER_TRIALS));
        
        masterSleepTextField.setText(
                Integer.toString(
                        ThreadPoolBidirectionalBFSPathFinder
                                .DEFAULT_MASTER_THREAD_SLEEP_DURATION_MILLIS));
        
        slaveSleepTextField.setText(
                Integer.toString(
                        ThreadPoolBidirectionalBFSPathFinder
                                .DEFAULT_SLAVE_THREAD_SLEEP_DURATION_MILLIS));
        
        setWarning(slaveSleepTextField, "Hello wolrd!");
    }
    
    private void setWarning(final TextField textField, 
                            final String warningMessage) {
        Border border = 
                new Border(
                        new BorderStroke(
                                Color.RED, 
                                BorderStrokeStyle.SOLID, 
                                CornerRadii.EMPTY, 
                                BorderWidths.DEFAULT));
        
        textField.setBorder(border);
        statusBarLabel.setStyle("-fx-text-inner-color: red;");
        statusBarLabel.setText(warningMessage);
    }
    
    private void unsetWarning(final TextField textField) {
        textField.setBorder(null);
        statusBarLabel.setText("");
    }
    
    private static final class TextFieldChangeListener 
            implements ChangeListener<String> {
        
        private final TextField textField;

        public TextFieldChangeListener(final TextField textField) {
            this.textField = textField;
        }
        
        @Override
        public void changed(final ObservableValue<? extends String> observableValue, 
                            final String oldValue, 
                            final String newValue) {
            
            if (newValue.trim().equals("")) {
                textField.setText("");
                return;
            }
            
            try {
                Integer.parseInt(newValue);
                textField.setText(newValue);
            } catch (final NumberFormatException ex) {
                textField.setText(oldValue);
            }
        }
    }
}
