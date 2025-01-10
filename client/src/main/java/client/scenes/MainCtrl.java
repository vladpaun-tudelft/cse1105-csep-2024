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

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Pair;

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
        this.dashboard.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F11 -> primaryStage.setFullScreen(!primaryStage.isFullScreen());
                case ENTER -> {
                    if (event.isAltDown()) {
                        primaryStage.setFullScreen(!primaryStage.isFullScreen());
                    }
                }
                case ESCAPE -> {
                    dashboardCtrl.setSearchIsActive(false);
                }
                default -> {}
            }
        });

        showDashboard();
        primaryStage.show();
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