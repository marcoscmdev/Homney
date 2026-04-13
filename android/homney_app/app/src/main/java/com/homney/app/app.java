package com.homney.app;

import android.app.Application;
import com.homney.app.webservice.PeticionesRed;

    public class app extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            PeticionesRed.getInstancia(this); // inicializa con Application context
        }
    }

