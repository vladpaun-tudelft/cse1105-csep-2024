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
package client;

import client.scenes.DashboardCtrl;
import client.scenes.MainCtrl;
import client.utils.Config;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.google.inject.Guice.createInjector;

public class Main extends Application {

	private DashboardCtrl dashboardCtrl;

	public DashboardCtrl getDashboardCtrl() {
		return dashboardCtrl;
	}

	public static void main(String[] args) throws URISyntaxException, IOException {
		launch();
	}

	@Override
	public void start(Stage primaryStage) {
		Injector injector = createInjector(new MyModule());
		MyFXML FXML = new MyFXML(injector);

		Config config = injector.getInstance(Config.class);
		LanguageManager languageManager = LanguageManager.getInstance(config);

		var dashboard = FXML.load(DashboardCtrl.class, languageManager.getBundle(), "client", "scenes", "Dashboard.fxml");
		dashboardCtrl = dashboard.getKey();

		var mainCtrl = injector.getInstance(MainCtrl.class);
		mainCtrl.initialize(primaryStage, dashboard);

		// Add close request handler
		primaryStage.setOnCloseRequest(event -> {
			// Call the onClose method in the controller
			mainCtrl.onClose();

			Platform.exit();
			System.exit(0);
		});
	}
}