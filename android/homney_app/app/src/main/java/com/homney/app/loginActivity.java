package com.homney.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.homney.app.login_registro.activity_fragment_nueva_cuenta;

public class loginActivity extends AppCompatActivity {

    Button btn_login;
    TextView tab_login, tab_registro;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    boolean remember;
    String mail;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btn_login = findViewById(R.id.btn_login);
        tab_login = findViewById(R.id.tab_login);
        tab_registro = findViewById(R.id.tab_registro);

        activity_fragment_registro fragment_registro = new activity_fragment_registro();
        activity_fragment_nueva_cuenta fragment_nueva_cuenta = new activity_fragment_nueva_cuenta();

        getSupportFragmentManager().beginTransaction().replace(R.id.contenedor_fragmento, fragment_registro).commit();

        tab_registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tab_registro.setBackgroundResource(R.drawable.bg_tab_active);
                tab_login.setBackgroundResource(android.R.color.transparent);
                getSupportFragmentManager().beginTransaction().replace(R.id.contenedor_fragmento, fragment_nueva_cuenta).commit();
            }
        });

        tab_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tab_login.setBackgroundResource(R.drawable.bg_tab_active);
                tab_registro.setBackgroundResource(android.R.color.transparent);
                getSupportFragmentManager().beginTransaction().replace(R.id.contenedor_fragmento, fragment_registro).commit();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        preferences = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        try {
            remember = preferences.getBoolean("remember", false);
            mail = preferences.getString("mail", "");
            if (remember) {
                Intent intent = new Intent(loginActivity.this, MainActivity.class);
                intent.putExtra("mail", mail);
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}