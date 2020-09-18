package com.example.beerrunserver.EventBus;

import com.example.beerrunserver.Model.OrderModel;

public class PrintOrderEvent {
    private String path;
    private OrderModel orderModel;

    public PrintOrderEvent(String path, OrderModel orderModel) {
        this.path = path;
        this.orderModel = orderModel;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public OrderModel getOrderModel() {
        return orderModel;
    }

    public void setOrderModel(OrderModel orderModel) {
        this.orderModel = orderModel;
    }
}
