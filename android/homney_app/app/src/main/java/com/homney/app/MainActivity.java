package com.homney.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.homney.app.ui.fragmento2_mihogar.HogarAIBottomSheet;
import com.homney.app.webservice.WebService;

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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.fragmento2, R.id.fragmento3, R.id.fragmento4, R.id.fragmento5,
                R.id.nav_crear_tarea, R.id.nav_crear_gasto, R.id.nav_crear_publicacion)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        /* ── FAB dinámico: cambia icono y acción según el fragment activo ── */
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();

            if (id == R.id.fragmento3) {
                // Tareas → añadir tarea
                fab.show();
                fab.setImageResource(R.drawable.ic_add);
                fab.setContentDescription("Nueva tarea");
                fab.setOnClickListener(v ->
                        navController.navigate(R.id.nav_crear_tarea));

            } else if (id == R.id.fragmento4) {
                // Cartera → añadir gasto
                fab.show();
                fab.setImageResource(R.drawable.ic_add);
                fab.setContentDescription("Nuevo gasto");
                fab.setOnClickListener(v ->
                        navController.navigate(R.id.nav_crear_gasto));

            } else if (id == R.id.fragmento5) {
                // Muro → nueva publicación
                fab.show();
                fab.setImageResource(R.drawable.ic_add);
                fab.setContentDescription("Nueva publicación");
                fab.setOnClickListener(v ->
                        navController.navigate(R.id.nav_crear_publicacion));

            } else if (id == R.id.nav_home) {
                // Panel → acceso rápido: diálogo con las 3 opciones de creación
                fab.show();
                fab.setImageResource(R.drawable.ic_add);
                fab.setContentDescription("Crear...");
                fab.setOnClickListener(v -> mostrarDialogoCrearRapido(navController));

            } else if (id == R.id.fragmento2) {
                // Mi Hogar → Asistente IA
                fab.show();
                fab.setImageResource(R.drawable.homney_mate);
                fab.setContentDescription("Tu asistente con IA");
                fab.setOnClickListener(v -> {
                    HogarAIBottomSheet sheet = new HogarAIBottomSheet();
                    sheet.show(getSupportFragmentManager(), "hogar_ai");
                });

            } else {
                // Formularios de creación y el resto → FAB oculto
                fab.hide();
            }
        });


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                NavigationView navigationView = findViewById(R.id.nav_view);
                long idMenuPrevioSeleccionado = -1;
                for (int i = 0; i < navigationView.getMenu().size(); i++) {
                    if (navigationView.getMenu().getItem(i).isChecked()) {
                        idMenuPrevioSeleccionado = navigationView.getMenu().getItem(i).getItemId();
                    }
                }

                if (menuItem.getItemId() == R.id.nav_view) {
                    finish();
                } else {
                    Bundle argumentos = null;
                    if (idMenuPrevioSeleccionado == R.id.nav_home && menuItem.getItemId() == R.id.fragmento2) {
                        argumentos = new Bundle();
                        argumentos.putString("dato", "X-100");
                    }
                    if (idMenuPrevioSeleccionado == R.id.fragmento2 && menuItem.getItemId() == R.id.fragmento2) {
                        argumentos = new Bundle();
                        argumentos.putString("dato", "X-100");
                    }

                    NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);
                    NavOptions.Builder opcionesBuilder = new NavOptions.Builder();
                    opcionesBuilder.setPopUpTo(menuItem.getItemId(), true);
                    navController.navigate(menuItem.getItemId(), argumentos, opcionesBuilder.build());

                    ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawers();
                }

                return true;
            }
        });

        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
            @Override public void onDrawerOpened(@NonNull View drawerView) {}
            @Override public void onDrawerClosed(@NonNull View drawerView) {}
            @Override public void onDrawerStateChanged(int newState) {}
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int num_fragmentos_historial = getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment)
                        .getChildFragmentManager()
                        .getBackStackEntryCount();

                if (num_fragmentos_historial == 0) {
                    if (pulsadoUnaVezAtrasParaSalir) {
                        finish();
                        return;
                    }
                    pulsadoUnaVezAtrasParaSalir = true;
                    Toast.makeText(getApplicationContext(),
                            "Por favor, presione ATRÁS otra vez para SALIR",
                            Toast.LENGTH_LONG).show();
                    handler.removeCallbacks(cancelarSalida);
                    handler.postDelayed(cancelarSalida, 2000);
                } else {
                    NavController navController = Navigation.findNavController(
                            MainActivity.this, R.id.nav_host_fragment);
                    navController.popBackStack();
                }
            }
        });
    }

    /* ════════════════════════════════════════════
       MENÚ PRINCIPAL — incluye el avatar de perfil
    ════════════════════════════════════════════ */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        // Cargar el avatar en el action view del ítem de perfil
        MenuItem profileItem = menu.findItem(R.id.action_profile);
        if (profileItem != null) {
            View actionView = profileItem.getActionView();
            if (actionView != null) {
                ImageView ivAvatar = actionView.findViewById(R.id.iv_toolbar_avatar);
                if (ivAvatar != null) {
                    cargarAvatarToolbar(ivAvatar);
                    // Al pulsar el avatar se abre un PopupMenu anclado al propio avatar
                    actionView.setOnClickListener(v -> mostrarPopupPerfil(v));
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_mi_perfil) {
            Navigation.findNavController(this, R.id.nav_host_fragment)
                    .navigate(R.id.nav_mi_perfil);
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            Toast.makeText(getApplicationContext(), "Ajustes", Toast.LENGTH_LONG).show();
        }
        if (item.getItemId() == R.id.menu_cerrar_sesion) {
            deleteSharedPreferences("sesion");
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Diálogo de acceso rápido desde el Panel.
     * Muestra las tres acciones de creación en un AlertDialog de lista.
     */
    private void mostrarDialogoCrearRapido(NavController navController) {
        String[] opciones = {"📋  Nueva tarea", "💰  Nuevo gasto", "📣  Nueva publicación"};
        new AlertDialog.Builder(this)
                .setTitle("¿Qué quieres crear?")
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0: navController.navigate(R.id.nav_crear_tarea);        break;
                        case 1: navController.navigate(R.id.nav_crear_gasto);        break;
                        case 2: navController.navigate(R.id.nav_crear_publicacion);  break;
                    }
                })
                .show();
    }

    /**
     * PopupMenu anclado al avatar.
     * Sustituye al menú de desbordamiento (3 puntos) para que éste no aparezca.
     * Reutiliza la lógica de onOptionsItemSelected para cada ítem.
     */
    private void mostrarPopupPerfil(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.popup_perfil, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
        popup.show();
    }

    /* ════════════════════════════════════════════
       CARGA DEL AVATAR CON GLIDE
       - Si el usuario tiene foto real → Glide la carga en círculo
       - Si tiene la foto por defecto (default.png) o no tiene
         → se genera un Bitmap con la inicial sobre fondo accent
    ════════════════════════════════════════════ */

    private void cargarAvatarToolbar(ImageView ivAvatar) {
        SharedPreferences prefs = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "?");
        String avatar = prefs.getString("avatar", null);

        char inicial = (nombre != null && !nombre.isEmpty())
                ? Character.toUpperCase(nombre.charAt(0)) : '?';

        // Fallback: círculo con la inicial (fondo accent #F5C518)
        Drawable avatarFallback = crearAvatarInicial(inicial, 0xFFF5C518);

        boolean tieneAvatarReal = avatar != null
                && !avatar.isEmpty()
                && !avatar.contains("default.png");

        if (tieneAvatarReal) {
            // URL completa de la imagen: mismo base que el backend
            String avatarUrl = WebService.PROTOCOLO + WebService.SERVIDOR
                    + WebService.CARPETA + "/" + avatar;

            Glide.with(this)
                    .load(avatarUrl)
                    .apply(new RequestOptions()
                            .circleCrop()
                            .placeholder(avatarFallback)
                            .error(avatarFallback))
                    .into(ivAvatar);
        } else {
            // Sin foto real → mostrar inicial directamente
            ivAvatar.setImageDrawable(avatarFallback);
            // Clip circular para la imagen con inicial
            ivAvatar.setClipToOutline(true);
        }
    }

    /**
     * Genera un Drawable circular con la inicial del usuario.
     * Equivale al avatar por defecto de la web:
     *   <div class="avatar" style="background:${color}">${inicial}</div>
     */
    private Drawable crearAvatarInicial(char inicial, int colorFondo) {
        int size = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics()));

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int marginRight = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));

        // Fondo circular
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(colorFondo);
        float offsetRight = 8f;
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint);


        // Texto de la inicial centrado
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size * 0.43f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        float yPos = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(inicial), size / 2f, yPos, textPaint);

        return new BitmapDrawable(getResources(), bitmap);
    }
}
