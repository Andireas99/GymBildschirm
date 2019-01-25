package com.bur.andi.gymbildschirm;

import android.os.Bundle;

import java.util.Observable;

public class ServiceObserver extends Observable {
    private static final ServiceObserver instance = new ServiceObserver();

    public static ServiceObserver getInstance() {
        return instance;
    }

    private ServiceObserver() {
    }

    public void addMessagesToTab(Bundle bundle){
        synchronized (this){
            setChanged();
            notifyObservers(bundle);
        }
    }
}
