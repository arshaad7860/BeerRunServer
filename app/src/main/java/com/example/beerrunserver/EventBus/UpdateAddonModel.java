package com.example.beerrunserver.EventBus;

import com.example.beerrunserver.Model.AddonModel;
import com.example.beerrunserver.Model.SizeModel;

import java.util.List;

public class UpdateAddonModel {
    private List<AddonModel> addonModelList;

    public UpdateAddonModel() {
    }

    public UpdateAddonModel(List<AddonModel> addonModelList) {
        this.addonModelList = addonModelList;
    }

    public List<AddonModel> getAddonModelList() {
        return addonModelList;
    }

    public void setAddonModelList(List<AddonModel> addonModelList) {
        this.addonModelList = addonModelList;
    }
}
