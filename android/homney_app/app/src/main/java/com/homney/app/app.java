package com.homney.app;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.homney.app.webservice.PeticionesRed;

    public class app extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            PeticionesRed.getInstancia(this); // inicializa con Application context
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
            Recordatorios.crearCanal(this);
        }
    }

