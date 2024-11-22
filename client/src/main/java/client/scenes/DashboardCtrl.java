package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;
import commons.Person;
import commons.Quote;
import jakarta.ws.rs.WebApplicationException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;

/**
 * Controlls all logic for the main dashboard.
 */
public class DashboardCtrl {

    private final ServerUtils server;
    private final MainCtrl mainCtrl;

    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

}
