package com.homney.app.login_registro;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anychart.scales.Linear;
import com.homney.app.R;
import com.homney.app.Utilidades;

public class activity_fragment_nueva_cuenta extends Fragment {
EditText reg_nombre, reg_email, reg_telefono, reg_password, reg_password2, reg_clave;
Spinner reg_sexo, reg_modo;
LinearLayout reg_clave_wrap;

    Button btn_registro;
    /* ── Sesión ──────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar   = -1;

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
        reg_password2 = vista.findViewById(R.id.reg_password2);
        reg_clave = vista.findViewById(R.id.reg_clave);
        reg_clave_wrap = vista.findViewById(R.id.reg_clave_wrap);

        reg_modo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int seleccion = i;
                modoRegistro(seleccion);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btn_registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            try {
                int claveleng = Integer.parseInt(reg_clave.getText().toString());
                String clave = reg_clave.getText().toString();
                String clave2 = reg_password2.getText().toString();
                if(claveleng<=5){
                    Toast.makeText(getContext(), "La clave deve contener al menos 5 caracteres", Toast.LENGTH_SHORT).show();
                }
                if(!clave.equalsIgnoreCase(clave2)){
                    Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                }else{
                    String claveHash =  Utilidades.encriptaMD5(clave);


                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            }
        });

        return vista;
    }

    void modoRegistro(int modo){
        if(modo==1){
            reg_clave.setVisibility(View.VISIBLE);
            reg_clave_wrap.setVisibility(View.VISIBLE);
        }else{
            reg_clave.setVisibility(View.GONE);
            reg_clave_wrap.setVisibility(View.GONE);
        }
    }

}
