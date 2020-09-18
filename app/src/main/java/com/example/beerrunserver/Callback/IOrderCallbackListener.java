package com.example.beerrunserver.Callback;

import com.example.beerrunserver.Model.CategoryModel;
import com.example.beerrunserver.Model.OrderModel;

import java.util.List;

public interface IOrderCallbackListener {
    void onOrderLoadSuccess(List<OrderModel> orderModels);
    void onOrderLoadFailed(String message);
}
