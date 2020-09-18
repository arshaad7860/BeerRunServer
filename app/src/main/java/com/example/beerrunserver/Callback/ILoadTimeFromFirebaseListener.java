package com.example.beerrunserver.Callback;

import com.example.beerrunserver.Model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadOnlyTimeSuccess(long estimatedTimeInMs);
    void onLoadTimeFailed(String message);

}
