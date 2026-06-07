package com.homney.app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Fragment de login con:
 *  - Inicio de sesión email + contraseña (Homney backend)
 *  - Continuar con Google (Firebase Auth + Homney backend)
 *  - Continuar con Apple  (Firebase Auth + Homney backend)
 *  - ¿Olvidaste tu contraseña? (Firebase Auth sendPasswordResetEmail)
 *
 * Flujo de login social:
 *  1. Firebase autentica al usuario (Google/Apple)
 *  2. Buscamos su email en la BD Homney (login_social.php)
 *  3a. Si existe → guardamos sesión y vamos a MainActivity
 *  3b. Si no existe → vamos al tab Registro con el email/nombre pre-rellenado
 */
public class activity_fragment_registro extends Fragment {

    /* ── Vistas ── */
    EditText  et_mail, et_pass;
    CheckBox  cb_recordar_sesion;
    Button    btn_login, btn_google;
    TextView  tv_olvidar_pass;

    /* ── Estado ── */
    String tagLogCat = "WS";
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    LoadingDialog loadingDialog;

    /* ── Firebase ── */
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    /* ════════════════════════════════════════════════════════
       LAUNCHER para Google Sign-In (registrado antes de onCreate)
    ════════════════════════════════════════════════════════ */

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        loadingDialog.dismiss(); // siempre cerramos el loading al volver
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                Log.d("GOOGLE_AUTH", "idToken: " + (account.getIdToken() != null ? "OK" : "NULL"));
                                Log.d("GOOGLE_AUTH", "email: " + account.getEmail());
                                // idToken null → SHA-1 no registrado en Firebase o Google Auth no habilitado
                                if (account.getIdToken() == null) {
                                    mostrarError("Token nulo. Verifica que el SHA-1 está en Firebase console.");
                                    return;
                                }
                                loadingDialog.show(); // volvemos a mostrar para la fase Firebase
                                firebaseAuthWithGoogle(
                                        account.getIdToken(),
                                        account.getDisplayName(),
                                        account.getEmail());
                            } catch (ApiException e) {
                                // Código 10 = DEVELOPER_ERROR → SHA-1 no registrado
                                // Código 12501 = usuario canceló
                                Log.e("GOOGLE_AUTH", "ApiException código: " + e.getStatusCode(), e);
                                if (e.getStatusCode() != 12501) { // 12501 = cancelado por el usuario
                                    mostrarError("Error Google Sign-In (código " + e.getStatusCode()
                                            + "). Si es 10, falta el SHA-1 en Firebase.");
                                }
                            }
                        } else {
                            Log.d("GOOGLE_AUTH", "Resultado no OK: " + result.getResultCode());
                            // RESULT_CANCELED (0) puede ser:
                            //  a) Usuario pulsó atrás en el selector → no mostrar error
                            //  b) SHA-1 no registrado en Firebase → Google cancela silenciosamente
                            // Intentamos obtener el Task para ver si hay ApiException
                            if (result.getData() != null) {
                                Task<GoogleSignInAccount> task =
                                        GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                                try {
                                    task.getResult(ApiException.class);
                                } catch (ApiException e) {
                                    Log.e("GOOGLE_AUTH", "ApiException código: " + e.getStatusCode());
                                    String hint = e.getStatusCode() == 10
                                            ? " (código 10 = SHA-1 no registrado en Firebase)"
                                            : "";
                                    mostrarError("Error Google Sign-In: código " + e.getStatusCode() + hint);
                                }
                            }
                            // Si getData() es null → el usuario canceló → no mostrar nada
                        }
                    });

    /* ════════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View vista = inflater.inflate(R.layout.fragment_login_registro, container, false);

        // Vistas
        et_mail           = vista.findViewById(R.id.et_mail);
        et_pass           = vista.findViewById(R.id.et_pass);
        cb_recordar_sesion = vista.findViewById(R.id.cb_recordar_sesion);
        btn_login         = vista.findViewById(R.id.btn_login);
        btn_google        = vista.findViewById(R.id.btn_google);
        tv_olvidar_pass   = vista.findViewById(R.id.tv_olvidar_pass);

        preferences = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        editor      = preferences.edit();
        loadingDialog = new LoadingDialog(requireContext());

        // ── Firebase Auth ──────────────────────────────────────────
        mAuth = FirebaseAuth.getInstance();

        // Google Sign-In: requestIdToken obtiene el token para Firebase.
        // R.string.default_web_client_id es auto-generado por el plugin google-services
        // a partir del google-services.json (client_type 3 = web client).
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // ── Listeners ─────────────────────────────────────────────
        btn_login.setOnClickListener(v -> {
            String mail = et_mail.getText().toString().trim();
            String pass = et_pass.getText().toString().trim();
            if (mail.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.rellena_todos_los_campos), Toast.LENGTH_SHORT).show();
            } else {
                iniciarSesion(mail, Utilidades.encriptaMD5(pass));
            }
        });

        btn_google.setOnClickListener(v -> iniciarSesionGoogle());

        tv_olvidar_pass.setOnClickListener(v -> mostrarDialogResetPassword());

        return vista;
    }

    /* ════════════════════════════════════════════════════════
       LOGIN CLÁSICO (email + contraseña Homney)
    ════════════════════════════════════════════════════════ */

    void iniciarSesion(String email, String password) {
        if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), "No existe conexión a INTERNET", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject credenciales = new JSONObject();
        try {
            credenciales.put("email",    email);
            credenciales.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        loadingDialog.show();

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.POST, WebService.URL_Login, credenciales,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS).equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Usuario>>(){}.getType();
                            RespuestaLista<Usuario> resp = gson.fromJson(response.toString(), tipo);

                            if (resp.data != null && !resp.data.isEmpty()) {
                                Usuario u = resp.data.get(0);
                                if (cb_recordar_sesion.isChecked()) {
                                    editor.putBoolean("remember", true);
                                    editor.putString("mail", u.getEmail());
                                } else {
                                    editor.putBoolean("remember", false);
                                }
                                guardarSesionYEntrar(u);
                            } else {
                                loadingDialog.dismiss();
                                Toast.makeText(requireContext(),
                                        "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            loadingDialog.dismiss();
                            String msg = response.has("message")
                                    ? response.getString("message") : "Error de login";
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        loadingDialog.dismiss();
                        Toast.makeText(requireContext(),
                                getString(R.string.error_procesar_respuesta), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    Utilidades.mostrar_error_peticion(requireContext(), tagLogCat,
                            "Error en la red", Request.Method.POST, WebService.URL_Login, error);
                }
        ));
    }

    /* ════════════════════════════════════════════════════════
       GOOGLE SIGN-IN
    ════════════════════════════════════════════════════════ */

    private void iniciarSesionGoogle() {
        if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.sin_conexion_internet), Toast.LENGTH_SHORT).show();
            return;
        }
        loadingDialog.show();
        // signOut previo para que siempre muestre el selector de cuenta
        mGoogleSignInClient.signOut().addOnCompleteListener(t -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    /**
     * Paso 2 del flujo Google: Firebase verifica el idToken de Google
     * y crea/actualiza la sesión Firebase del usuario.
     */
    private void firebaseAuthWithGoogle(String idToken, String displayName, String email) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        Log.d("GOOGLE_AUTH", "Firebase auth OK");
                        FirebaseUser fbUser = mAuth.getCurrentUser();
                        String emailFinal  = email != null ? email
                                : (fbUser != null ? fbUser.getEmail() : "");
                        String nombreFinal = displayName != null ? displayName
                                : (fbUser != null && fbUser.getDisplayName() != null
                                   ? fbUser.getDisplayName() : "");
                        // Buscar en la BD de Homney
                        loginSocialEnHomney(emailFinal, nombreFinal);
                    } else {
                        String err = task.getException() != null
                                ? task.getException().getMessage() : "Error desconocido";
                        Log.e("GOOGLE_AUTH", "Firebase signInWithCredential falló: " + err);
                        mostrarError("Error Firebase: " + err);
                    }
                });
    }
    /* ════════════════════════════════════════════════════════
       VERIFICACIÓN EN HOMNEY TRAS LOGIN SOCIAL
    ════════════════════════════════════════════════════════ */

    /**
     * Tras autenticarse con Firebase (Google/Apple), comprobamos si el email
     * ya tiene cuenta en Homney.
     *  - Si SÍ → guardamos sesión y vamos a MainActivity.
     *  - Si NO → vamos al tab "Registro" con email y nombre pre-rellenados
     *            para que el usuario complete el proceso.
     */
    private void loginSocialEnHomney(String email, String nombre) {
        Log.d("GOOGLE_AUTH", "Buscando en Homney DB: " + email);
        loadingDialog.show();
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);

            PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                    Request.Method.POST, WebService.URL_LoginSocial, body,
                    response -> {
                        loadingDialog.dismiss();
                        try {
                            Log.d("GOOGLE_AUTH", "login_social.php respuesta: " + response);
                            if (WebService.JSON.SUCCESS.equals(
                                    response.getString(WebService.JSON.STATUS))) {
                                // ── Usuario encontrado → login directo ──────
                                Gson gson = new GsonBuilder().create();
                                Type tipo = new TypeToken<RespuestaLista<Usuario>>(){}.getType();
                                RespuestaLista<Usuario> resp =
                                        gson.fromJson(response.toString(), tipo);
                                if (resp.data != null && !resp.data.isEmpty()) {
                                    guardarSesionYEntrar(resp.data.get(0));
                                }
                            } else {
                                // ── Usuario NO encontrado → ir a formulario de registro ──
                                Log.d("GOOGLE_AUTH", "Usuario nuevo → redirigiendo a registro");
                                requireActivity().runOnUiThread(() -> {
                                    mostrarError("¡Bienvenido! Completa tu perfil para entrar 🐝");
                                    if (requireActivity() instanceof loginActivity) {
                                        ((loginActivity) requireActivity())
                                                .irARegistroConDatos(nombre, email);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e("GOOGLE_AUTH", "Error parseando login_social: " + e.getMessage());
                            mostrarError("Error al verificar la cuenta en el servidor");
                        }
                    },
                    error -> {
                        loadingDialog.dismiss();
                        // Si login_social.php no está subido al servidor dará 404
                        Log.e("GOOGLE_AUTH", "Error Volley login_social: " + error);
                        mostrarError("¿Subiste login_social.php al servidor? Error de red.");
                    }
            ));
        } catch (JSONException e) {
            loadingDialog.dismiss();
        }
    }

    /** Muestra un Toast de forma segura aunque el Fragment esté en segundo plano. */
    private void mostrarError(String msg) {
        Activity act = getActivity();
        if (act != null && !act.isFinishing()) {
            act.runOnUiThread(() ->
                    Toast.makeText(act, msg, Toast.LENGTH_LONG).show());
        }
        Log.e("GOOGLE_AUTH", msg);
    }

    /* ════════════════════════════════════════════════════════
       RESTABLECER CONTRASEÑA (Firebase Auth)
       ¡IMPORTANTE!: Solo funciona para cuentas creadas con email/password
       en Firebase Auth. Envía un email de restablecimiento desde Firebase.
    ════════════════════════════════════════════════════════ */

    private void mostrarDialogResetPassword() {
        EditText etEmail = new EditText(requireContext());
        etEmail.setHint(getString(R.string.hint_correo_recuperar));
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setPadding(50, 30, 50, 30);
        // Pre-rellena con lo que haya escrito en el campo de login
        String emailActual = et_mail.getText().toString().trim();
        if (!emailActual.isEmpty()) etEmail.setText(emailActual);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_recuperar_pass_titulo))
                .setMessage(getString(R.string.dialog_recuperar_pass_msg))
                .setView(etEmail)
                .setPositiveButton(getString(R.string.enviar), (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Introduce tu email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(getContext(),
                                "Introduce un email válido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    enviarEmailReset(email);
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    private void enviarEmailReset(String email) {
        loadingDialog.show();
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "📧 Email enviado a " + email + ". Revisa tu bandeja de entrada.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "No se pudo enviar el email";
                        Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /* ════════════════════════════════════════════════════════
       HELPER: guardar sesión y navegar a MainActivity
    ════════════════════════════════════════════════════════ */

    private void guardarSesionYEntrar(Usuario u) {
        editor.putInt("id_usuario",    u.getId_usuario());
        editor.putString("nombre",     u.getNombre());
        editor.putString("email",      u.getEmail());
        editor.putString("rol",        u.getRol());
        editor.putInt("id_hogar",      u.getId_hogar());
        editor.putString("avatar",     u.getAvatar());
        editor.apply();

        startActivity(new Intent(requireActivity(), MainActivity.class));
        requireActivity().finish();
    }
}
