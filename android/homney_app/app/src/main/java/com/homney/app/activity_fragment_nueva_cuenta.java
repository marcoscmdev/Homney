package com.homney.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class activity_fragment_nueva_cuenta extends Fragment {
EditText reg_nombre, reg_email, reg_telefono, reg_password;
Spinner reg_sexo, reg_modo;

Button btn_registro;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_login_nueva_cuenta, container, false);

        reg_nombre = vista.findViewById(R.id.reg_nombre);
        reg_email  = vista.findViewById(R.id.reg_email);
        reg_telefono = vista.findViewById(R.id.reg_telefono);
        reg_password = vista.findViewById(R.id.reg_password);
        btn_registro = vista.findViewById(R.id.btn_registro);
        reg_sexo = vista.findViewById(R.id.reg_sexo);
        reg_modo = vista.findViewById(R.id.reg_modo);

        ArrayAdapter<CharSequence> adapterSexo = ArrayAdapter.createFromResource(
                getContext(), R.array.sexo_options, R.layout.item_spinner_selected);
        adapterSexo.setDropDownViewResource(R.layout.item_spinner_dropdown);

        ArrayAdapter<CharSequence> adapterModo = ArrayAdapter.createFromResource(
                getContext(), R.array.modo_options, R.layout.item_spinner_selected);
        adapterModo.setDropDownViewResource(R.layout.item_spinner_dropdown);

        reg_sexo.setAdapter(adapterSexo);
        reg_modo.setAdapter(adapterModo);



        return vista;
    }
}
