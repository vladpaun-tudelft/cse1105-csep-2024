package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;

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
