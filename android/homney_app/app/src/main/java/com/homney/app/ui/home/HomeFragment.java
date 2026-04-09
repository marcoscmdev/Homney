package com.homney.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.homney.app.MainActivity;
import com.homney.app.R;
import com.homney.app.UtilidadesNavigationDrawer;

public class HomeFragment extends Fragment {
Button button_pruebas;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        button_pruebas =  root.findViewById(R.id.button_pruebas);
        button_pruebas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // En evento propio. Cambiar de fragmento, ennviando parámetros (opcional) y sin iniciar una nueva instancia del fragmento (esté o no en el menú)

                Bundle bundleActitivityReservas = new Bundle();
                bundleActitivityReservas.putSerializable("dato","X-100");
                NavOptions.Builder opcionesBuilder = new NavOptions.Builder();
                opcionesBuilder.setPopUpTo(R.id.fragmento2, true);

                Navigation.findNavController(button_pruebas).navigate(R.id.fragmento2,bundleActitivityReservas,opcionesBuilder.build());

            }
        });
        return root;

    }
}