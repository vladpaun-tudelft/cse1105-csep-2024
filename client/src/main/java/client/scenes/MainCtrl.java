/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package client.scenes;

import commons.Note;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Pair;

import javafx.scene.input.KeyEvent;

public class MainCtrl {

    private Stage primaryStage;

    private DashboardCtrl dashboardCtrl;
    private Scene dashboard;


    public void initialize(Stage primaryStage,
                           Pair<DashboardCtrl, Parent> dashboard) {
        this.primaryStage = primaryStage;

        this.dashboardCtrl = dashboard.getKey();
        this.dashboard = new Scene(dashboard.getValue());
        this.dashboard.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        configureKeyboardShortcuts();

        showDashboard();
        primaryStage.show();
    }

    /**
     * Configures various keyboard shortcuts used in the dashboard
     */
    private void configureKeyboardShortcuts() {
        this.dashboard.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {

                // NAVIGATION
                case  RIGHT -> {
                    if (event.isAltDown()) {
                        dashboardCtrl.selectNextCollection();
                        event.consume();
                    }
                }
                case LEFT -> {
                    if (event.isAltDown()) {
                        dashboardCtrl.selectPreviousCollection();
                        event.consume();
                    }
                }
                case DOWN -> {
                    if (event.isAltDown()) {
                        dashboardCtrl.selectNextNote();
                        event.consume();
                    }
                }
                case UP -> {
                    if (event.isAltDown()){
                        dashboardCtrl.selectPreviousNote();
                        event.consume();
                    }
                }
                case TAB -> {

                }

                // DELETING NOTES
                case DELETE -> {
                    if (event.isAltDown()) {
                        dashboardCtrl.deleteSelectedNote();
                        event.consume();
                    }
                }

                // EDITING
                case E -> {
                    if (event.isAltDown() || event.isControlDown()) {
                        if (event.isShiftDown()) {
                            dashboardCtrl.openEditCollections();
                            event.consume();
                        } else {
                            dashboardCtrl.editCurrentNoteName();
                            event.consume();
                        }
                    }
                }

                // ADDING NOTES AND COLLECTIONS
                case N -> {
                    if (event.isControlDown() || event.isAltDown()) {
                        if (event.isShiftDown()) {
                            dashboardCtrl.addCollection();
                            event.consume();
                        } else {
                            dashboardCtrl.addNote();
                            event.consume();
                        }
                    }
                }

                // SEARCHING
                case ESCAPE -> {
                    dashboardCtrl.clearSearch();
                    Platform.runLater(() -> {dashboardCtrl.getSearchField().requestFocus();});
                    event.consume();
                }

                // REFRESH
                case F5 -> {
                    dashboardCtrl.refresh();
                    event.consume();
                }

                // FULLSCREEN
                case F11 -> primaryStage.setFullScreen(!primaryStage.isFullScreen());
                case ENTER -> {
                    if (event.isAltDown()) {
                        primaryStage.setFullScreen(!primaryStage.isFullScreen());
                        event.consume();
                    }
                }

                default -> {}
            }
        });
    }

    /**
     * Shows the dashboard scene and sets the title
     */
    public void showDashboard() {
        primaryStage.setTitle("NetNote");
        Image icon = new Image(getClass().getResourceAsStream("/css/icons/netnote-icon.png"));
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(dashboard);
    }

    public void onClose() {
        dashboardCtrl.onClose();
    }
}