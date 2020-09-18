package com.example.beerrunserver.EventBus;

import com.example.beerrunserver.Model.AddonModel;
import com.example.beerrunserver.Model.SizeModel;

public class SelectAddonModel {
    private AddonModel addonModel;

    public SelectAddonModel(AddonModel addonModel) {
        this.addonModel = addonModel;
    }

    public AddonModel getAddonModel() {
        return addonModel;
    }

    public void setAddonModel(AddonModel addonModel) {
        this.addonModel = addonModel;
    }
}
