package com.homney.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class loginActivity extends AppCompatActivity {

Button btn_login;
TextView tab_login, tab_registro;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btn_login = findViewById(R.id.btn_login);
        tab_login   =   findViewById(R.id.tab_login);
        tab_registro = findViewById(R.id.tab_registro);

        activity_fragment_registro fragment_registro = new activity_fragment_registro();
        activity_fragment_nueva_cuenta fragment_nueva_cuenta = new activity_fragment_nueva_cuenta();

        getSupportFragmentManager().beginTransaction().replace(R.id.contenedor_fragmento,fragment_registro).commit();

       tab_registro.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               tab_registro.setBackgroundResource(R.drawable.bg_tab_active);
               tab_login.setBackgroundResource(android.R.color.transparent);

               getSupportFragmentManager().beginTransaction().replace(R.id.contenedor_fragmento,fragment_nueva_cuenta).commit();
           }
       });

       tab_login.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               tab_login.setBackgroundResource(R.drawable.bg_tab_active);
               tab_registro.setBackgroundResource(android.R.color.transparent);

               getSupportFragmentManager().beginTransaction().replace(R.id.contenedor_fragmento,fragment_registro).commit();
           }
       });


    }
}