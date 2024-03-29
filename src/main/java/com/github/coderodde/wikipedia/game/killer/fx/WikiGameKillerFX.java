package com.github.coderodde.wikipedia.game.killer.fx;

import com.github.coderodde.graph.pathfinding.delayed.AbstractDelayedGraphPathFinder;
import com.github.coderodde.graph.pathfinding.delayed.AbstractNodeExpander;
import com.github.coderodde.graph.pathfinding.delayed.impl.ThreadPoolBidirectionalBFSPathFinder;
import com.github.coderodde.graph.pathfinding.delayed.impl.ThreadPoolBidirectionalBFSPathFinderBuilder;
import com.github.coderodde.graph.pathfinding.delayed.impl.ThreadPoolBidirectionalBFSPathFinderSearchBuilder;
import com.github.coderodde.wikipedia.graph.expansion.BackwardWikipediaGraphNodeExpander;
import com.github.coderodde.wikipedia.graph.expansion.ForwardWikipediaGraphNodeExpander;
import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public final class WikiGameKillerFX extends Application {

    /**
     * Specifies the HTML file format.
     */
    private static final String HTML_TEMPLATE = 
            """
            <!DOCTYPE html>
            <html>
                <head>
                    <title>WikiGameKillerFX.java</title>
                    <style>
                        body {
                            font: normal 12pt Monospaced;
                        }
                    </style>
                </head>
                <body>
                    <div>
                        <h3>Statistics</h3>
                        %s
                    </div>
                    <div>
                        <h3>Shortest path</h3>
                        <table>
            %s            </table>
                    </div>
                    <div>
                        <h3>Search parameters:</h3>
                        <table>
                            <tr><td><b>Number of threads:     </b></td><td>%d</td><tr>
                            <tr><td><b>Expansion duration:    </b></td><td>%d</td><tr>
                            <tr><td><b>Wait timeout:          </b></td><td>%d</td><tr>
                            <tr><td><b>Master trials:         </b></td><td>%d</td><tr>
                            <tr><td><b>Master sleep duration: </b></td><td>%d</td><tr>
                            <tr><td><b>Slave sleep duration:  </b></td><td>%d</td><tr>
                        </table>
                    </div>
                <body>
            </html>
            """;
    /**
     * The Wikipedia URL format.
     */
    private static final String WIKIPEDIA_URL_FORMAT =
            "^((http:\\/\\/)|(https:\\/\\/))?..\\.wikipedia\\.org\\/wiki\\/.+$";
    
    private static final String SEPARATOR_CSS = 
            ".separator {\n" + 
            "  -fx-padding: 0px;\n" +
            "  -fx-border-insets: 0px;\n" + 
            "  -fx-border-width: 0px;\n" + 
            "}";
    
    /**
     * The Wikipedia URL regular expression pattern object.
     */
    private static final Pattern WIKIPEDIA_URL_FORMAT_PATTERN = 
            Pattern.compile(WIKIPEDIA_URL_FORMAT);
    
    /**
     * The application font.
     */
    private static final Font FONT = 
            Font.font("monospaced", FontWeight.BOLD, 11);
    
    /**
     * Warning text field warning.
     */
    private static final Border WARNING_BORDER = 
                new Border(
                        new BorderStroke(
                                Color.RED, 
                                BorderStrokeStyle.SOLID, 
                                CornerRadii.EMPTY, 
                                BorderWidths.DEFAULT));
        
    private final TextField sourceTextField             = new TextField();
    private final TextField targetTextField             = new TextField();
    private final TextField threadsTextField            = new TextField();
    private final TextField expansionoDurationTextField = new TextField();
    private final TextField waitTimeoutTextField        = new TextField();
    private final TextField masterTrialsTextField       = new TextField();
    private final TextField masterSleepTextField        = new TextField();
    private final TextField slaveSleepTextField         = new TextField();
        
    private final Button searchButton          = new Button("Search");
    private final Button haltButton            = new Button("Halt");
    private final Button defaultSettingsButton = new Button("Set defaults");
    private final Button saveResultsButton     = new Button("Save");
    
    private final ProgressBar progressBar = new ProgressBar(100.0);
    
    private volatile ThreadPoolBidirectionalBFSPathFinder<String> finder;
    
    private Stage resultsStage;
    private Stage primaryStage;
    private Scene primaryScene;
    
    private final HBox statusBarHBox = new HBox();
    private final Label statusBarLabel = new Label();
    
    private final List<TextField> textFieldList = new ArrayList<>();
    
    private final Border textFieldWarningBorder = 
                new Border(
                        new BorderStroke(
                                Color.RED, 
                                BorderStrokeStyle.SOLID, 
                                CornerRadii.EMPTY, 
                                BorderWidths.DEFAULT));
    
    private volatile List<String> resultUrls = new ArrayList<>();
    private volatile int duration;
    private volatile int numberOfExpandedNodes;
     
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(final Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        primaryStage.setTitle(
                "WikiGameKillerFX.java 1.0.0 (by Rodion \"rodde\" Efremov)");
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });
        
        final VBox mainBox = new VBox();
        
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
        
        sourceTextField             .setPrefWidth(300);
        targetTextField             .setPrefWidth(300);
        threadsTextField            .setPrefWidth(300);
        expansionoDurationTextField .setPrefWidth(300);
        waitTimeoutTextField        .setPrefWidth(300);
        masterTrialsTextField       .setPrefWidth(300);
        masterSleepTextField        .setPrefWidth(300);
        slaveSleepTextField         .setPrefWidth(300);
        
        sourceTextField             .textProperty().addListener(new StringTextFieldChangeListener(sourceTextField));
        targetTextField             .textProperty().addListener(new StringTextFieldChangeListener(targetTextField));
        threadsTextField            .textProperty().addListener(new IntegerTextFieldChangeListener(threadsTextField));
        expansionoDurationTextField .textProperty().addListener(new IntegerTextFieldChangeListener(expansionoDurationTextField));
        waitTimeoutTextField        .textProperty().addListener(new IntegerTextFieldChangeListener(waitTimeoutTextField));
        masterTrialsTextField       .textProperty().addListener(new IntegerTextFieldChangeListener(masterTrialsTextField));
        masterSleepTextField        .textProperty().addListener(new IntegerTextFieldChangeListener(masterSleepTextField));
        slaveSleepTextField         .textProperty().addListener(new IntegerTextFieldChangeListener(slaveSleepTextField));
        
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
        
        setDefaultSettings();
        
        searchButton.setDisable(true);
        haltButton.setDisable(true);
        saveResultsButton.setDisable(true);
        
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
                                           saveResultsButton,
                                           haltButton);
        
        defaultSettingsButton.setOnAction((ActionEvent t) -> {
            setDefaultSettings();
        });
        
        saveResultsButton.setOnAction((t) -> {
            if (resultUrls.isEmpty()) {
                // Once here, there is no results to save. Alert and go back to GUI:
                final Alert alert = 
                        new Alert(
                                AlertType.INFORMATION,
                                "No results to save.",
                                ButtonType.OK);

                alert.showAndWait();
                return;
            }
            
            final FileChooser fileChooser = new FileChooser();
            
            fileChooser.setTitle("Choose an HTML file");
            fileChooser.getExtensionFilters()
                       .addAll(new ExtensionFilter("HTML files", "*.html"),
                               new ExtensionFilter("HTML files", "*.htm"));
            
            final File file = fileChooser.showSaveDialog(resultsStage);
            
            if (file == null) {
                return;
            }
            
            try {
                saveFile(file);
            } catch (final RuntimeException ex) {
                final Alert alert = 
                        new Alert(
                                AlertType.ERROR,
                                ex.getMessage(),
                                ButtonType.CLOSE);
                
                alert.showAndWait();
            }
        });
        
        searchButton.setOnAction((ActionEvent actionEvent) -> {
            Platform.runLater(() -> {
                searchButton.setDisable(true);
                haltButton.setDisable(false);
                saveResultsButton.setDisable(true);
            });
            
            final boolean ok = validateInputForm();
            
            if (ok) {
                final String sourceUrl = sourceTextField.getText();
                final String targetUrl = targetTextField.getText();
                
                final String sourceLanguageCode = getLanguageCode(sourceUrl);
                final String targetLanguageCode = getLanguageCode(targetUrl);
                
                final int threads           = Integer.parseInt(threadsTextField.getText());
                final int lockWaitDuration  = Integer.parseInt(waitTimeoutTextField.getText());
                final int expansionDuration = Integer.parseInt(expansionoDurationTextField.getText());
                final int masterTrials      = Integer.parseInt(masterTrialsTextField.getText());
                final int masterSleep       = Integer.parseInt(masterSleepTextField.getText());
                final int slaveSleep        = Integer.parseInt(slaveSleepTextField.getText());
                
                finder = 
                        ThreadPoolBidirectionalBFSPathFinderBuilder
                        .<String>begin()
                        .withJoinDurationMillis(expansionDuration)
                        .withLockWaitMillis(lockWaitDuration)
                        .withMasterThreadSleepDurationMillis(masterSleep)
                        .withNumberOfMasterTrials(masterTrials)
                        .withNumberOfRequestedThreads(threads)
                        .withSlaveThreadSleepDurationMillis(slaveSleep)
                        .end();
                
                final AbstractNodeExpander<String> forwardNodeExpander = 
                        new ForwardLinkExpander(sourceLanguageCode);
                
                final AbstractNodeExpander<String> backwardNodeExpander = 
                        new BackwardLinkExpander(targetLanguageCode);
                
                haltButton.setDisable(false);
                searchButton.setDisable(true);
                defaultSettingsButton.setDisable(true);
                
                final SearchTask searchTask = new SearchTask();

                searchTask.finder = finder;
                searchTask.source = stripHostFromURL(sourceUrl);
                searchTask.target = stripHostFromURL(targetUrl);
                searchTask.forwardExpander = forwardNodeExpander;
                searchTask.backwardExpander = backwardNodeExpander;
                
                searchTask.setOnSucceeded(
                        (final WorkerStateEvent workerStateEvent) -> {
                    try {
                        reportResults(
                                searchTask.get(),
                                sourceLanguageCode,
                                (int) searchTask.finder.getDuration(),
                                searchTask.finder
                                        .getNumberOfExpandedNodes());
                    } catch (final InterruptedException | ExecutionException ex) {
                        final Alert alert =
                                new Alert(
                                        AlertType.ERROR, 
                                        String.format("Search failed: %s", ex.getMessage()),
                                        ButtonType.CLOSE);
                        
                        alert.showAndWait();
                        return;
                    }
                    
                    enableInputForm();
                    haltButton.setDisable(true);
                    searchButton.setDisable(false);
                    defaultSettingsButton.setDisable(false);
                });

                final Thread searchThread = new Thread(searchTask);
                searchThread.start();
                
                searchButton.setDisable(true);
                defaultSettingsButton.setDisable(true);
                disableInputForm();
            }
        });
        
        haltButton.setOnAction((ActionEvent actionEvent) -> {
            if (finder != null) {
                finder.halt();
                searchButton.setDisable(false);
                haltButton.setDisable(true);
               
                reportHalt(finder);
                
                System.out.printf(
                        "Search halted after %s milliseconds " + 
                        "expanding %d nodes.\n", 
                        finder.getDuration(), 
                        finder.getNumberOfExpandedNodes());
                
                finder = null;
                
                enableInputForm();
                defaultSettingsButton.setDefaultButton(false);
            }
        });
        
        loadTextFieldList();
        
        setTextFieldWarning(sourceTextField);
        setTextFieldWarning(targetTextField);
        
        progressBar.setPrefSize(statusBarHBox.getWidth(), 25);
        
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
                       progressBar,
                       statusBarHBox);
        
        final StackPane root = new StackPane();
        root.getChildren().add(mainBox);
        
        primaryStage.setScene(this.primaryScene = new Scene(root));
        primaryStage.show();
    }
    
    private void reportHalt(
            final AbstractDelayedGraphPathFinder<String> finder) {
         final Text statisticsText = 
                new Text(
                    String.format(
                        "[STATISTICS] Duration: %d milliseconds.\n" +
                        "[STATISTICS] Number of expanded nodes: %d.", 
                        finder.getDuration(),
                        finder.getNumberOfExpandedNodes()));

        statisticsText.setFont(FONT);
           
        final Text haltText = new Text("Search halted.");
        haltText.setFont(FONT);
        haltText.setStyle("-fx-text-fill: #ff4444;");
        
        final VBox box = new VBox();
        box.getChildren().addAll(statisticsText, haltText);
        
        Platform.runLater(() -> {
            
            if (resultsStage != null) {
                resultsStage.close();
            }
            
            resultsStage = new Stage();
            
            final StackPane resultsRoot = new StackPane(box);
            final Scene resultsScene = new Scene(resultsRoot,
                                                 400, 
                                                 primaryScene.getHeight());
            resultsScene.getStylesheets()
                        .add(SEPARATOR_CSS);
            
            resultsStage.setScene(resultsScene);
            
            resultsStage.setY(primaryStage.getY());
            resultsStage.setX(primaryStage.getX() +
                              primaryStage.getWidth() + 10);
            
            resultsStage.setTitle("Search results");
            resultsStage.show();
        });
    }
    
    private void disableInputForm() {
        for (final TextField textField : textFieldList) {
            textField.setDisable(true);
        }
    }
    
    private void enableInputForm() {
        for (final TextField textField : textFieldList) {
            textField.setDisable(false);
        }
    }
    
    private void setDefaultSettings() {
        
        threadsTextField.setText("128");
        
        expansionoDurationTextField.setText("4000");
        
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
    }
    
    static String toTableRowHtml(final int lineNumber, 
                                 final String url, 
                                 final String title) {
        return new StringBuilder().append("                ") // Align.
                                  .append("<tr><td>")
                                  .append(lineNumber)
                                  .append(".</td><td><a href=\"")
                                  .append(url)
                                  .append("\">")
                                  .append(url)
                                  .append("</a></td><td>")
                                  .append(title)
                                  .append("</td></tr>\n")   
                                  .toString();
    }
    
    private void setTextFieldWarning(final TextField textField) {
        textField.setBorder(WARNING_BORDER);
        
        for (final TextField tf : textFieldList) {
            if (tf.getText().isEmpty()) {
                tf.setBorder(textFieldWarningBorder);
            } else {
                tf.setBorder(null);
            }
        }
        
        final TextField topmostWarningTextField = getTopmostEmptyTextField();
        
        if (topmostWarningTextField == null) {
            return;
        }
        
        statusBarLabel.setStyle("-fx-text-inner-color: red;");
        statusBarLabel.setText(
                String.format(
                        "%s cannot be empty.",
                        getParameterName(topmostWarningTextField)));
    }
    
    private void unsetTextFieldWarning(final TextField textField) {
        textField.setBorder(null);
    }
    
    private void loadTextFieldList() {
        textFieldList.addAll(Arrays.asList(sourceTextField,
                                           targetTextField,
                                           threadsTextField,
                                           expansionoDurationTextField,
                                           waitTimeoutTextField,
                                           masterTrialsTextField,
                                           masterSleepTextField,
                                           slaveSleepTextField));
    }
    
    private TextField getTopmostEmptyTextField() {
        for (final TextField textField : textFieldList) {
            if (textField.getText().isEmpty()) {
                return textField;
            }
        }
        
        return null;
    }
    
    private String getParameterName(final TextField textField) {
        if (textField == sourceTextField) {
            return "Source URL";
        }
        
        if (textField == targetTextField) {
            return "Target URL";
        }
        
        if (textField == expansionoDurationTextField) {
            return "Expansion duration";
        }
        
        if (textField == masterSleepTextField) {
            return "Master sleep duration";
        }
        
        if (textField == masterTrialsTextField) {
            return "Master trials";
        }
        
        if (textField == slaveSleepTextField) {
            return "Slave sleep duration";
        }
        
        if (textField == threadsTextField) {
            return "Threads";
        }
        
        if (textField == waitTimeoutTextField) {
            return "Wait timeout";
        }
        
        throw new IllegalStateException("Should not get here.");
    }
    
    /**
     * Validates the input form. If there are at least one error in the input 
     * form, returns {@code false}. Returns {@code true} if the input form is in
     * order.
     * 
     * @return {@code true} only if the input form is correctly filled.
     */
    private boolean validateInputForm() {
        final TextField topmostWarningTextField = 
                getTopmostEmptyTextField();

        TextField failingTextField = null;
        String failingTextFieldText = null;
        
        if (topmostWarningTextField == null) {
                        
            String sourceUrlLanguageCode = null;
            String targetUrlLanguageCode = null;
            
            try {
                final String sourceUrl = sourceTextField.getText();
                
                if (sourceUrl.trim().isEmpty()) {
                    setTextFieldWarning(sourceTextField);
                    
                    statusBarLabel.setText(
                            String.format(
                                    "%s cannot be empty.", 
                                    getParameterName(sourceTextField)));
                    
                    searchButton.setDisable(true);
                    haltButton.setDisable(true);
                    return false;
                }
                
                checkSourceUrl(sourceUrl);
                
                sourceUrlLanguageCode = getLanguageCode(sourceUrl);
            } catch (final IllegalArgumentException ex) {
                setTextFieldWarning(sourceTextField);
                failingTextField = sourceTextField;
                failingTextFieldText = ex.getMessage();
                searchButton.setDisable(true);
                haltButton.setDisable(true);
            }
            
            try {
                final String targetUrl = targetTextField.getText();
                
                if (targetUrl.trim().isEmpty()) {
                    setTextFieldWarning(targetTextField);
                    searchButton.setDisable(true);
                    haltButton.setDisable(true);
                    
                    statusBarLabel.setText(
                            String.format(
                                    "%s cannot be empty.", 
                                    getParameterName(targetTextField)));
                    return false;
                }
                
                checkTargetUrl(targetUrl);
                
                targetUrlLanguageCode = getLanguageCode(targetUrl);
            } catch (final IllegalArgumentException ex) {
                setTextFieldWarning(targetTextField);
                searchButton.setDisable(true);
                haltButton.setDisable(true);
                
                if (failingTextField == null) {
                    failingTextField = targetTextField;
                    failingTextFieldText = ex.getMessage();
                }
            }
            
            if (failingTextField != null) {
                setTextFieldWarning(failingTextField);
                statusBarLabel.setText(failingTextFieldText);
                return false;
            }
            
            if (!sourceUrlLanguageCode.equals(targetUrlLanguageCode)) {
                setTextFieldWarning(sourceTextField);
                setTextFieldWarning(targetTextField);
                searchButton.setDisable(true);
                haltButton.setDisable(true);
                
                statusBarLabel.setText(
                        String.format(
                                "Language mismatch: \"%s\" vs \"%s\".", 
                                sourceUrlLanguageCode, 
                                targetUrlLanguageCode));
                return false;
            } else {
                searchButton.setDisable(false);
                haltButton.setDisable(true);
                return true;
            }
        }
        
        searchButton.setDisable(true);
        haltButton.setDisable(true);
        
        statusBarLabel.setText(
                String.format(
                        "%s cannot be empty.", 
                        getParameterName(topmostWarningTextField)));
        
        return false;
    }
    
    private void trySetHint(final TextField textField) {
        if (textField == expansionoDurationTextField ||
            textField == waitTimeoutTextField ||
            textField == masterSleepTextField ||
            textField == slaveSleepTextField) {
            
            textField.setPromptText("Milliseconds");
            textField.getParent().requestFocus();
        } else if (textField == threadsTextField) {
            textField.setPromptText("Number of threads");
            textField.getParent().requestFocus();
        } else if (textField == masterTrialsTextField) {
            textField.setPromptText("Master trials");
            textField.getParent().requestFocus();
        }
    }
    
    private final class StringTextFieldChangeListener 
            implements ChangeListener<String> {

        private final TextField textField;

        public StringTextFieldChangeListener(final TextField textField) {
            this.textField = textField;
        }
        
        @Override
        public void changed(
                final ObservableValue<? extends String> observableValue, 
                final String oldValue, 
                final String newValue) {
            
            if (newValue.isEmpty()) {
                validateInputForm();
                setTextFieldWarning(textField);
                return;
            }
            
            textField.setText(newValue);
            unsetTextFieldWarning(textField);
            
            if (validateInputForm()) {
                statusBarLabel.setText("");
                searchButton.setDisable(false);
            }
        }
    }
    
    private final class IntegerTextFieldChangeListener 
            implements ChangeListener<String> {
        
        private final TextField textField;

        public IntegerTextFieldChangeListener(final TextField textField) {
            this.textField = textField;
        }
        
        @Override
        public void changed(
                final ObservableValue<? extends String> observableValue, 
                final String oldValue, 
                final String newValue) {
            
            if (newValue.trim().equals("")) {   
                textField.setText("");
                trySetHint(textField);
                setTextFieldWarning(textField);
                validateInputForm();
                return;
            }
            
            try {
                Integer.parseInt(newValue);
                textField.setText(newValue);
                unsetTextFieldWarning(textField);
                
                if (validateInputForm()) {
                    statusBarLabel.setText("");
                    searchButton.setDisable(false);
                }
            } catch (final NumberFormatException ex) {
                textField.setText(oldValue);
            }
        }
    }
    
    /**
     * Checks that the source article URL conforms to a Wikipedia URL regular 
     * language.
     * 
     * @param sourceUrl the source article URL.
     */
    private static void checkSourceUrl(final String sourceUrl) {
        if (!WIKIPEDIA_URL_FORMAT_PATTERN.matcher(sourceUrl).find()) {
            throw new IllegalArgumentException(
                    String.format(
                            "The source URL \"%s\" is invalid.", 
                            sourceUrl));
        }
    }
    
    /**
     * Checks that the target article URL conforms to a Wikipedia URL regular 
     * language.
     * 
     * @param sourceUrl the target article URL.
     */
    private static void checkTargetUrl(final String targetUrl) {
        if (!WIKIPEDIA_URL_FORMAT_PATTERN.matcher(targetUrl).find()) {
            throw new IllegalArgumentException(
                    String.format(
                            "The target URL \"%s\" is invalid.", 
                            targetUrl));
        }
    }
    
    /**
     * Returns the ISO language code used in the input URL {@code url}.
     * 
     * @param url the URL to extract the country code from.
     * 
     * @return the language code.
     * 
     * @throws CommandLineException if the resulting language code does not 
     *                              conform to ISO.
     */
    private static String getLanguageCode(String url) {
        final String secureProtocol = "https://";
        final String insecureProtocol = "http://";
        
        // Strip the possible protocol (https or http):
        if (url.startsWith(secureProtocol)) {
            url = url.substring(secureProtocol.length());
        } else if (url.startsWith(insecureProtocol)) {
            url = url.substring(insecureProtocol.length());
        }
        
        // Get the language code. We don't (yet) support simple.wikipedia.org.
        final String languageCode = url.substring(0, 2);
        
        // Check that the language code is in the list of all possible ISO 
        // country codes:
        if (!Arrays.asList(Locale.getISOLanguages()).contains(languageCode)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unknown language code: %s",
                            languageCode));
        }
        
        return languageCode;
    }
    
    /**
     * Strips the protocol, host name and {@code wiki} path from each URL in
     * the {@code urlList}. For example, 
     * {@code https://en.wikipedia.org/en/Hiisi} becomes simply {@code Hiisi}.
     * 
     * @param urlList the list of URLs.
     * @return the list of article titles.
     */
    private static List<String> stripHostAddress(final List<String> urlList) {
        List<String> result = new ArrayList<>(urlList.size());
        
        for (final String url : urlList) {
            result.add(stripHostFromURL(url));
        }
        
        return result;
    }
    
    /**
     * Strips the host from the input URL. For example, 
     * {@code en.wikipedia.org/wiki/Audi} becomes simply {@code Audi}.
     * 
     * @param url the URL from which to strip the host address.
     * 
     * @return the title of the Wikipedia article. 
     */
    private static String stripHostFromURL(final String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
    
    /**
     * Reports the search results.
     * 
     * @param titles                the list of titles in the article path.
     * @param languageCode          the language code of the search process.
     * @param duration              the duration of search in milliseconds.
     * @param numberOfExpandedNodes the number of expanded nodes during search.
     */
    private void reportResults(final List<String> titles,
                               final String languageCode,
                               final int duration,
                               final int numberOfExpandedNodes) {
        
        // Get the path of full URLs:
        final List<String> urls = addHosts(titles, languageCode);
        
        // Update the most recent search result:
        this.resultUrls.clear();
        this.resultUrls.addAll(urls);
        this.duration = duration;
        this.numberOfExpandedNodes = numberOfExpandedNodes;
        
        // Convert the URLs to the instances of Hyperlink class:
        final List<Hyperlink> hyperlinks = getHyperlinks(urls);
        
        // Get the statistics message text:
        final Text statisticsText = 
                new Text(
                        String.format(
                                "[STATISTICS] Duration: %d milliseconds.\n" +
                                "[STATISTICS] Number of expanded nodes: %d.", 
                                duration,
                                numberOfExpandedNodes));
        
        statisticsText.setFont(FONT);
        
        final Node[] nodes = new Node[1 + 2 * titles.size()];
        
        nodes[0] = statisticsText;
        
        int index = 1;
        
        for (int i = 0; i < hyperlinks.size(); i++) {
            // We cannot reuse the same Separator. Need to make different 
            // instances:
            final Separator separator = new Separator(Orientation.VERTICAL);
            separator.setPadding(new Insets(-5, 0, -5, 0));
            nodes[index] = separator;
            nodes[index + 1] = hyperlinks.get(i);   
            hyperlinks.get(i).setFont(FONT);
            index += 2;
        }
        
        Platform.runLater(() -> {
            
            if (resultsStage != null) {
                // Close the previous result window:
                resultsStage.close();
            }
            
            resultsStage = new Stage();
            
            final VBox vbox = new VBox();
            vbox.getChildren().addAll(nodes);
            
            final StackPane resultsRoot = new StackPane(vbox);
            final Scene resultsScene = new Scene(resultsRoot, 
                                                 400, 
                                                 primaryScene.getHeight());
            resultsScene.getStylesheets()
                        .add(SEPARATOR_CSS);
            
            resultsStage.setScene(resultsScene);
            
            resultsStage.setY(primaryStage.getY());
            resultsStage.setX(primaryStage.getX() +
                              primaryStage.getWidth() + 10);
            
            resultsStage.setTitle("Search results");
            resultsStage.show();
            
            saveResultsButton.setDisable(false);
        });
    }
     
    /**
     * Returns a block of HTML code describing the actual contents of the result
     * table.
     * 
     * @param urlList the list of URLs in the article path.
     * 
     * @return HTML code for the result &lt;table&gt;.
     */
    private static String getPathTableHtml(
            final List<String> urlList) {
        
        StringBuilder stringBuilder = new StringBuilder();
        
        int lineNumber = 1;
        
        for (final String url : urlList) {
            stringBuilder.append("                <tr><td>") // Add also indent.
                         .append(lineNumber++)
                         .append(".</td><td><a href=\"")
                         .append(url)
                         .append("\">")
                         .append(url)
                         .append("</a></td><td>")
                         .append(stripHostFromURL(url))
                         .append("</td></tr>\n");
        }
        
        return stringBuilder.toString();
    }
    
    /**
     * Attempts to save the most recent results into the file {@code file}.
     * 
     * @param file the file object describing the target file to save to.
     */
    private void saveFile(final File file) {
        if (file.exists()) {
            // Need to delete first.
            if (!file.delete()) {
                // Once here, could not delete.
                throw new RuntimeException(
                        String.format(
                                "Could not delete the file \"%s\".", 
                                file.getName()));
            }
        }
        
        // Get the HTML code for the result file:
        final String html = String.format(
                    HTML_TEMPLATE,
                    String.format(
                            "Duration: %d milliseconds, expanded %d nodes.", 
                            duration,
                            numberOfExpandedNodes),
                    getPathTableHtml(resultUrls),
                    finder.getNumberOfThreads(),
                    finder.getExpansionJoinDurationMillis(),
                    finder.getLockWaitDurationMillis(),
                    finder.getMasterThreadTrials(),
                    finder.getMasterThreadSleepDurationMillis(),
                    finder.getSlaveThreadSleepDurationMillis());
        
        // Attempt to save:
        try (BufferedWriter bufferedWriter =
                new BufferedWriter(new FileWriter(file))) {
            
            bufferedWriter.write(html);
            bufferedWriter.close();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Could not create a buffered writer.");
        }
    }
    
    private static List<Hyperlink> getHyperlinks(final List<String> urls) {
        final List<Hyperlink> hyperlinkList = new ArrayList<>(urls.size());
        
        for (final String url : urls) {
            final Hyperlink hyperlink = new Hyperlink();
            hyperlink.setText(url);
            hyperlink.setOnAction((actionEvent) -> {
                if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop()
                           .isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (URISyntaxException | IOException ex) {
                        final Alert alert = 
                                new Alert(
                                        Alert.AlertType.ERROR, 
                                        String.format(
                                                "Could not open %s!",
                                                url), 
                                        ButtonType.OK);
                        
                        alert.showAndWait();
                    }
                }
            });
            
            hyperlinkList.add(hyperlink);
        }
        
        return hyperlinkList;
    }
    
    private static List<String> addHosts(final List<String> titles,
                                         final String languageCode) {
        final List<String> result = new ArrayList<>(titles.size());
        
        for (final String title : titles) {
            result.add(
                    String.format(
                            "https://%s.wikipedia.org/wiki/%s", 
                            languageCode, 
                            title));
        }
        
        return result;
    }
    
    /**
     * This class implements the forward link expander.
     */
    private static final class ForwardLinkExpander 
            extends AbstractNodeExpander<String> {

        private final ForwardWikipediaGraphNodeExpander expander;
        
        public ForwardLinkExpander(final String languageCode) {
            this.expander = new ForwardWikipediaGraphNodeExpander(languageCode);
        }
        
        /**
         * Generate all the links that this article links to.
         * 
         * @param article the source article of each link.
         * 
         * @return all the article titles that {@code article} links to.
         */
        @Override
        public List<String> generateSuccessors(final String article) {
            try {
                return stripHostAddress(expander.getNeighbors(article));
            } catch (Exception ex) {
                return Collections.<String>emptyList();
            }
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public boolean isValidNode(final String article) {
            try {
                return expander.isValidNode(article);
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    /**
     * This class implements the backward link expander. 
     */
    private static final class BackwardLinkExpander 
            extends AbstractNodeExpander<String> {

        private final BackwardWikipediaGraphNodeExpander expander;
        
        public BackwardLinkExpander(final String languageCode) {
            this.expander = 
                    new BackwardWikipediaGraphNodeExpander(languageCode);
        }
        
        /**
         * Generate all the links pointing to the article {@code article}.
         * 
         * @param article the target article of each link.
         * 
         * @return all the article titles linking to {@code article}.
         * 
         * @throws java.lang.Exception if something fails.
         */
        @Override
        public List<String> generateSuccessors(final String article) {
            try {
                return stripHostAddress(expander.getNeighbors(article));
            } catch (Exception ex) {
                return Collections.<String>emptyList();
            }
        }
        
        /**
         * {@inheritDoc }
         */
        @Override
        public boolean isValidNode(final String article) {
            try {
                return expander.isValidNode(article);
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    private final class SearchTask extends Task<List<String>> {

        String source;
        String target;
        AbstractDelayedGraphPathFinder<String> finder;
        AbstractNodeExpander<String> forwardExpander;
        AbstractNodeExpander<String> backwardExpander;
        
        @Override
        protected List<String> call() throws Exception {
            return ThreadPoolBidirectionalBFSPathFinderSearchBuilder
                    .<String>withPathFinder(finder)
                    .withSourceNode(source)
                    .withTargetNode(target)
                    .withForwardNodeExpander(forwardExpander)
                    .withBackwardNodeExpander(backwardExpander)
                    .search();
        }
    }
}
