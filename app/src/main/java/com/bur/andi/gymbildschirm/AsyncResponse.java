package com.bur.andi.gymbildschirm;


import android.os.Parcelable;

/**
 * Created by Andi on 06.08.2016.
 */
public interface AsyncResponse extends Parcelable {
    void processFinished(String output);
}