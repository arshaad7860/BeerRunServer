package com.example.beerrunserver.EventBus;

import com.example.beerrunserver.Model.ShipperModel;

public class UpdateShipperEvent {
    private ShipperModel shipperModel;
    private boolean active;

    public UpdateShipperEvent(ShipperModel shipperModel, boolean active) {
        this.shipperModel = shipperModel;
        this.active = active;
    }

    public ShipperModel getShipperModel() {
        return shipperModel;
    }

    public void setShipperModel(ShipperModel shipperModel) {
        this.shipperModel = shipperModel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
