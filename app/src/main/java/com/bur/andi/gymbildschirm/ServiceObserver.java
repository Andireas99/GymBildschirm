package com.bur.andi.gymbildschirm;

import android.os.Bundle;

import java.util.Observable;

class ServiceObserver extends Observable {
    private static final ServiceObserver instance = new ServiceObserver();

    static ServiceObserver getInstance() {
        return instance;
    }

    private ServiceObserver() {
    }

    void addMessagesToTab(Bundle bundle){
        synchronized (this){
            setChanged();
            notifyObservers(bundle);
        }
    }
}
