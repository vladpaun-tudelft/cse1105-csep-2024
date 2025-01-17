package client.controllers;

import client.scenes.DashboardCtrl;
import com.google.inject.Inject;
import javafx.scene.control.*;

public class SearchCtrl {

    private DashboardCtrl dashboardCtrl;

    // References
    private TextField searchField;

    @Inject
    public SearchCtrl() {}

    /**
     * Set up references for the search bar and collection view.
     *
     * @param searchField    TextField for search input
     */
    public void setReferences(DashboardCtrl dashboardCtrl, TextField searchField) {
        this.dashboardCtrl = dashboardCtrl;
        this.searchField = searchField;
    }

    /**
     * Activates or deactivates the search mode.
     *
     * @param isActive        Whether search is active
     */
    public void setSearchIsActive(boolean isActive) {
        if (!isActive) {
            searchField.clear();
            dashboardCtrl.filter();
        }
    }
}
