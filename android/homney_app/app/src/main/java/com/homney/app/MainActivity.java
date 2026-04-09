package com.homney.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private boolean pulsadoUnaVezAtrasParaSalir = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable cancelarSalida = () -> pulsadoUnaVezAtrasParaSalir = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        /* Barra de acción */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        /* Botón flotante */
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout); // Contenedor principal que incluye toda la interfaz con el menú deslizante
        NavigationView navigationView = findViewById(R.id.nav_view); // Menú deslizante

       // IDs fragments(mobile_navigation.xml) y eb menu deslizante(activity_main_drawer.xml)
        // a tener en cuenta para visualizar en la barra de acción
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.fragmento2, R.id.fragmento3)
                .setOpenableLayout(drawer)
                .build();

        /* Enlazamos cargador de fragments del menú deslizante (NavController) con barra de acción */
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                NavigationView navigationView = findViewById(R.id.nav_view);
                // Buscamos id menú/fragmento Actual
                long idMenuPrevioSeleccionado=-1;
                for (int i=0; i<navigationView.getMenu().size(); i++) {
                    if (navigationView.getMenu().getItem(i).isChecked()) {
                        idMenuPrevioSeleccionado = navigationView.getMenu().getItem(i).getItemId();
                    }
                }

                if (menuItem.getItemId() == R.id.nav_salir) {
                    finish();
                }
                else {
                    Bundle argumentos=null;
                    // Ejemplo para enviar argumentos a un fragmento
                    if (idMenuPrevioSeleccionado == R.id.nav_home && menuItem.getItemId()==R.id.fragmento2) {
                        argumentos=new Bundle();
                        argumentos.putString("dato","X-100");
                    }


                    NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);
                    NavOptions.Builder opcionesBuilder = new NavOptions.Builder();
                    opcionesBuilder.setPopUpTo(menuItem.getItemId(), true);
                    navController.navigate(menuItem.getItemId(),argumentos, opcionesBuilder.build());

                    ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawers();
                }

                return true;
            }
        });

        UtilidadesNavigationDrawer.cambiarCabecera(navigationView,R.drawable.ic_cara_sonriente,"Nombre app", "subtitulo");


        // Eventos del menú deslizante
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                    // Menú deslizante abierto
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                    // Menú deslizante cerrado
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });




        // Confirmacion de cierre de aplicación
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int num_fragmentos_historial = getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment)
                        .getChildFragmentManager()
                        .getBackStackEntryCount();

                if (num_fragmentos_historial == 0) {
                    if (pulsadoUnaVezAtrasParaSalir) {
                        finish(); // ← mucho más simple
                        return;
                    }

                    pulsadoUnaVezAtrasParaSalir = true;
                    Toast.makeText(getApplicationContext(),
                            "Por favor, presione ATRÁS otra vez para SALIR",
                            Toast.LENGTH_LONG).show();

                    handler.removeCallbacks(cancelarSalida);
                    handler.postDelayed(cancelarSalida, 2000); // 2 segundos

                } else { // Mostramos fragmento anterior
                    NavController navController = Navigation.findNavController(
                            MainActivity.this, R.id.nav_host_fragment);
                    navController.popBackStack();
                }
            }
        });

    }

    /* Menú principal */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
                Toast.makeText(getApplicationContext(),"Clic menú principal", Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {  // Manejo del historial de framentos
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }



}