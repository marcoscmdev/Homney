package com.homney.app.ui.fragmento4_cartera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.homney.app.R;

public class Fragmento4_cartera extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment4_cartera, container, false);

        Bundle argumentos = getArguments();
        if (argumentos!=null) {
            String dato = argumentos.getString("dato");
            Toast.makeText(getContext(), "Dato recibido: " + dato, Toast.LENGTH_LONG).show();
        }

        return root;
    }
}