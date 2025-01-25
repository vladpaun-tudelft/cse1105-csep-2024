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

import client.LanguageManager;
import client.MyFXML;
import client.MyModule;
import client.utils.Config;
import com.google.inject.Inject;
import com.google.inject.Injector;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.util.ResourceBundle;

import static com.google.inject.Guice.createInjector;

public class MainCtrl {

    private Stage primaryStage;

    private DashboardCtrl dashboardCtrl;
    private Scene dashboard;
    @Inject
    private Config config;
    private LanguageManager manager;

    private static final Injector INJECTOR = createInjector(new MyModule());
    private static final MyFXML FXML = new MyFXML(INJECTOR);

    public void initialize(Stage primaryStage,
                           Pair<DashboardCtrl, Parent> dashboard) {
        this.primaryStage = primaryStage;

        this.dashboardCtrl = dashboard.getKey();
        this.dashboard = new Scene(dashboard.getValue());
        this.dashboard.getStylesheets().add(getClass().getResource("/css/color-styles.css").toExternalForm());
        this.dashboard.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        manager = LanguageManager.getInstance(config);

        configureKeyboardShortcuts();

        showDashboard();
        primaryStage.show();
    }

    /**
     * Method to load scenes without caching.
     */
    public <T> T loadScene(Class<T> controllerClass, ResourceBundle bundle, String... fxmlPath) {
        var fxml = FXML.load(controllerClass, bundle, fxmlPath);
        Scene scene = new Scene(fxml.getValue());
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        return controllerClass.cast(fxml.getKey());
    }

    /**
     * Opens the EditCollections scene as a popup.
     */
    public EditCollectionsCtrl showEditCollections() {
        var editCollectionsCtrl = loadScene(EditCollectionsCtrl.class, manager.getBundle(), "client", "scenes", "EditCollections.fxml");

        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.setTitle(manager.getBundle().getString("editCollections.text"));

        Scene scene = new Scene(FXML.load(EditCollectionsCtrl.class, manager.getBundle(), "client", "scenes", "EditCollections.fxml").getValue());
        scene.getStylesheets().add(dashboardCtrl.getCurrentCss());
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);

        editCollectionsCtrl.setReferences(
                popupStage,
                dashboardCtrl.getCollectionCtrl(),
                dashboardCtrl,
                dashboardCtrl.getServer(),
                dashboardCtrl.getNoteCtrl(),
                dashboardCtrl.getConfig(),
                dashboardCtrl.getDialogStyler()
        );
        editCollectionsCtrl.setCollectionList(dashboardCtrl.getCollections());

        configureCollectionsKeyboardShortcuts(scene, popupStage, editCollectionsCtrl);

        Platform.runLater(() -> popupStage.showAndWait());

        return editCollectionsCtrl;
    }


    /**
     * Configures various keyboard shortcuts used in the edit collections stage
     */
    private void configureCollectionsKeyboardShortcuts(Scene scene, Stage popupStage, EditCollectionsCtrl editCollectionsCtrl) {
        // Add keyboard shortcuts for the new stage
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {


                // CLOSE POPUP
                case ESCAPE -> {
                    popupStage.close(); // Close the stage on ESC
                    event.consume();
                }

                // NAVIGATION
                case DOWN -> {
                    editCollectionsCtrl.selectNextCollection();
                    event.consume();
                }
                case UP -> {
                    editCollectionsCtrl.selectPreviousCollection();
                    event.consume();
                }
                case TAB -> {
                    if (event.isShiftDown()) {
                        editCollectionsCtrl.selectPreviousJFXElement();;
                    } else {
                        editCollectionsCtrl.selectNextJFXElement();
                    }
                    event.consume();
                }

                case N -> {
                    if (event.isControlDown() || event.isAltDown()) {
                        editCollectionsCtrl.addCollection();
                        event.consume();
                    }
                }
                case S -> {
                    if (event.isControlDown() || event.isAltDown()) {
                        editCollectionsCtrl.saveCollection();
                        event.consume();
                    }
                }
                case C -> {
                    if (event.isAltDown()) {
                        editCollectionsCtrl.connectToCollection();
                        editCollectionsCtrl.createCollection();
                        event.consume();
                    }
                }
                case DELETE -> {
                    if (event.isAltDown()) {
                        if (event.isShiftDown()) {
                            editCollectionsCtrl.forgetCollection();
                        } else {
                            editCollectionsCtrl.deleteCollection();
                        }
                        event.consume();
                    }
                }

                default -> {
                }
            }
        });

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
                    if (event.isAltDown() && !event.isShiftDown()) {
                        dashboardCtrl.collectionView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                        dashboardCtrl.allNotesView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                        dashboardCtrl.selectNextNote();
                        dashboardCtrl.allNotesView.getSelectionModel().clearSelection
                                (dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex()-1);
                        dashboardCtrl.allNotesView.scrollTo(dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex()-1);

                    } else if (event.isAltDown() && event.isShiftDown()) {
                        dashboardCtrl.collectionView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        dashboardCtrl.allNotesView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        dashboardCtrl.selectNextNote();
                        dashboardCtrl.allNotesView.scrollTo(dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex()-1);
                    }
                    dashboardCtrl.collectionView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    dashboardCtrl.allNotesView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    event.consume();
                }
                case UP -> {

                    if (event.isAltDown() && !event.isShiftDown()) {
                        dashboardCtrl.collectionView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                        dashboardCtrl.allNotesView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                        dashboardCtrl.selectPreviousNote();
                        dashboardCtrl.allNotesView.getSelectionModel().clearSelection
                                (dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex()+1);
                        dashboardCtrl.allNotesView.scrollTo(dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex()-1);

                    } else if (event.isAltDown() && event.isShiftDown()) {
                        dashboardCtrl.collectionView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        dashboardCtrl.allNotesView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        dashboardCtrl.selectPreviousNote();
                        dashboardCtrl.allNotesView.scrollTo(dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex()-1);

                    }
                    dashboardCtrl.collectionView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    dashboardCtrl.allNotesView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    event.consume();
                }
                case TAB -> {

                }

                // DELETING NOTES
                case DELETE -> {
                    if (event.isAltDown()) {
                        if (dashboardCtrl.getCurrentCollection() == null) {
                            dashboardCtrl.getNoteCtrl().deleteMultipleNotesInTreeView(
                                    dashboardCtrl.getAllNotes(),
                                    dashboardCtrl.getAllNotesView().getSelectionModel().getSelectedItems(),
                                    dashboardCtrl.getCollectionNotes()
                            );
                        } else {
                            dashboardCtrl.deleteMultipleNotes(dashboardCtrl.collectionView.getSelectionModel().getSelectedItems());
                        }

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

                // UNDO
                case Z -> {
                    if (event.isControlDown() || event.isAltDown()) {
                        dashboardCtrl.undoLastAction(event);
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