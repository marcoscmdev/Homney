package com.homney.app;

import android.app.Activity;
import android.content.ContentResolver;
import com.homney.app.R;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


public class Utilidades {


    public static boolean hayConexionInternet(Context contexto){

        ConnectivityManager cm =
                (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork==null || !activeNetwork.isConnectedOrConnecting()) {
            return false;
        }

        return true;
    }



    /* Genera los parámatros a añadir a una url teniendo en cuenta símbolos ?, & y condificación utf-8 */
    public static String generaParametrosURL(HashMap<String,String> listaParametros) {

       String parametrosURL = "";

       if (!listaParametros.isEmpty()) {
           parametrosURL += "?";
           try {
               Iterator it = listaParametros.entrySet().iterator();
               while (it.hasNext()) {
                   Map.Entry p = (Map.Entry) it.next();

                   String parametroString = p.getKey() + "="
                           + URLEncoder.encode(p.getValue().toString(), "UTF-8");
                   if (parametrosURL.length() > 1) {
                       parametrosURL += "&" + parametroString;
                   } else {
                       parametrosURL += parametroString;
                   }
               }
           }
           catch(Exception e){
               e.printStackTrace();
           }
       }
       return parametrosURL;
   }


    public static final SimpleDateFormat FMT_ENTRADA =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public static final SimpleDateFormat FMT_SALIDA  =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    public static final SimpleDateFormat FMT_ENTRADA_DATETIME =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    public static final SimpleDateFormat FMT_SALIDA_DATETIME  =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    /** Convierte fecha de formato BD (yyyy-MM-dd o yyyy-MM-dd HH:mm:ss) a formato visual (dd/MM/yyyy). */
    public static String fechaEntradaASalida(String fechaEntrada) {
        if (fechaEntrada == null || fechaEntrada.isEmpty()) return "";
        try {
            // Recortar a los primeros 10 caracteres (yyyy-MM-dd) tanto si viene
            // con separador 'T' (ISO 8601) como con espacio (MySQL datetime)
            String limpia = fechaEntrada.substring(0, Math.min(10, fechaEntrada.length()));
            Date d = FMT_ENTRADA.parse(limpia);
            return d != null ? FMT_SALIDA.format(d) : fechaEntrada;
        } catch (Exception e) {
            return fechaEntrada;
        }
    }

    /** Convierte fecha de formato visual (dd/MM/yyyy) a formato BD (yyyy-MM-dd). */
    public static String fechaSalidaAEntrada(String fechaSalida) {
        if (fechaSalida == null || fechaSalida.isEmpty()) return "";
        try {
            Date d = FMT_SALIDA.parse(fechaSalida);
            return d != null ? FMT_ENTRADA.format(d) : fechaSalida;
        } catch (Exception e) {
            return fechaSalida;
        }
    }

    /** Devuelve la fecha de hoy en formato BD (yyyy-MM-dd). */
    public static String fechaHoyEntrada() {
        return FMT_ENTRADA.format(new Date());
    }

    /** Convierte fecha de BD ("yyyy-MM-dd HH:mm:ss" o "yyyy-MM-dd") → "dd/MM/yyyy" sin hora. */
    public static String formatearFechaMuro(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            // Recortar a yyyy-MM-dd e ignorar la hora
            String limpia = raw.substring(0, Math.min(10, raw.length()));
            Date d = FMT_ENTRADA.parse(limpia);
            return d != null ? FMT_SALIDA.format(d) : raw;
        } catch (Exception e) {
            return raw;
        }
    }

    /* Convierte un array de bytes en su representación Hexadecimal String */
    public static String bytesAHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }

    /* Encripta los bytes UTF-8 de un String a MD5 hexadecimal  */
    public static String encriptaMD5(String s)
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes("UTF-8"));
            return bytesAHexString(digest.digest());
        }
        catch(Exception e){
            return "";
        }

    }

    /**** Otros métodos útiles ****/

    // Oculta el Teclado virtual
    public static void ocultarTeclado(Activity activity){
        InputMethodManager inputMethodManager = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
				}

    // Prerequisito: Usar Justo antes de setContentView
    public static void  eliminarBarraSuperior(AppCompatActivity a){
        a.requestWindowFeature(Window.FEATURE_NO_TITLE); // Sin ActionBar
        ActionBar actionBar = a.getSupportActionBar(); // Con ActionBar
        if (actionBar!=null) actionBar.hide();
    }

    // Evita que la Activity se cierre y se vuelva a abrir (perdiendo los datos en memoria) cuando el dispositivo gira a posición horiontal
    public static void evitarCambioOrientacionApaisado(Activity a){
        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    public static void visualizar_url(Context contexto, String url){
        Uri uri_url = Uri.parse(url);
        Intent it = new Intent(Intent.ACTION_VIEW, uri_url);
        contexto.startActivity(it);
    }
	

    public static void enviaEmail(Context contexto, File fichero, String destinatario, String asunto, String cuerpo){
        Uri uri_fichero = Uri.fromFile(fichero); // Obtenemos ruta en formato uri
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        String to[] = {destinatario};
        emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
        // adjuntamos el archivo
        emailIntent .putExtra(Intent.EXTRA_STREAM, uri_fichero);
        // el asunto y el cuerpo
        emailIntent .putExtra(Intent.EXTRA_SUBJECT, asunto);
        emailIntent.putExtra(Intent.EXTRA_TEXT, cuerpo);

        // Buscamos Aplicaciones que puedan enviar emails
        contexto.startActivity(Intent.createChooser(emailIntent , "Enviar email..."));
    }

    /* Prerequisito en Activity:
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            // requestCode = Código de Petición, permissions=Permisos solicitados, grantResults=array paralelo de concesiones o rechazos de permisos.
            if (requestCode == CODIGO_PETICION) {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido.
                     llamadaAMetodo();
                }
                else { // El usuario ha rechazado el permiso.
                    Toast.makeText(getApplicationContext(),"Permiso para XXXX  rechazado",Toast.LENGTH_LONG).show();
                }
            }
        }

     */
                                                            // Manifest.permission.
    public boolean PermisoConcedido(final Activity a, final String permiso, final String textoAclaratorio, final int codigoSolicitudPermiso) {
        // Build.VERSION.SDK_INT  => Versión Android del dispositivo en el que se está ejecutando la app.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // Build.VERSION_CODES.M => Versión 6 (M - Marshmallow) de Android. https://developer.android.com/reference/android/os/Build.VERSION_CODES.html
            return true;
        }
        if (ActivityCompat.checkSelfPermission(a,permiso) == PackageManager.PERMISSION_GRANTED) { // Permiso ya previamente concedido.
            return true;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(a,permiso)) { // Es necesario dar una explicación (textoAclaratorio) acerca de por qué nuestra aplicacion necesita el permiso.
            AlertDialog.Builder datosDialog = new AlertDialog.Builder(a);
            datosDialog.setTitle(a.getString(R.string.solicitud_permiso));
            datosDialog.setMessage(textoAclaratorio);
            datosDialog.setPositiveButton(a.getString(R.string.entendido), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String[] arrayPermisos = {permiso}; // Se usa un array porque es posible solicitar varios permisos al mismo tiempo
                    ActivityCompat.requestPermissions(a,arrayPermisos, codigoSolicitudPermiso); // muestra una ventana al usuario
                }
            });
            datosDialog.show();

        } else {
            // new String[]{permiso} ==> Atajo para crear e inicilizar el array en una sola instrucción
            ActivityCompat.requestPermissions(a, new String[]{permiso}, codigoSolicitudPermiso);  // muestra una ventana al usuario
        }
        return false;
    }

    /* ══════════════════════════════════════════════
       UTILIDADES DE IMAGEN
    ══════════════════════════════════════════════ */

    /**
     * Lee una imagen desde una URI (galería / cámara) y la devuelve como array de bytes JPEG
     * comprimido hasta el tamaño máximo indicado.
     *
     * Uso típico:
     *   byte[] bytes = Utilidades.uriABytesJpeg(context, uri, 800, 80);
     *   if (bytes != null) { ... usar VolleyMultipartRequest ... }
     *
     * @param context     Contexto de la aplicación/fragmento
     * @param uri         URI de la imagen seleccionada (content:// o file://)
     * @param maxPx       Dimensión máxima (ancho o alto). 0 = sin escalar
     * @param calidad     Calidad JPEG 0-100 (recomendado 80 para avatares, 70 para muro)
     * @return bytes del JPEG, o null si falla
     */
    public static byte[] uriABytesJpeg(Context context, Uri uri, int maxPx, int calidad) {
        try {
            ContentResolver cr = context.getContentResolver();
            InputStream is = cr.openInputStream(uri);
            if (is == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            if (bitmap == null) return null;

            // Escalar si supera maxPx
            if (maxPx > 0) {
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                if (w > maxPx || h > maxPx) {
                    float ratio = Math.min((float) maxPx / w, (float) maxPx / h);
                    bitmap = Bitmap.createScaledBitmap(
                            bitmap, Math.round(w * ratio), Math.round(h * ratio), true);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, bos);
            return bos.toByteArray();

        } catch (Exception e) {
            Log.e("Utilidades", "uriABytesJpeg error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Devuelve un nombre de fichero único para subir al servidor.
     * Ejemplo: "usuario_5.jpg" o "pub_12.jpg"
     *
     * @param prefijo  "usuario" | "pub"
     * @param id       id del recurso
     * @return nombre del fichero
     */
    public static String nombreFicheroImagen(String prefijo, int id) {
        return prefijo + "_" + id + ".jpg";
    }

    /**
     * Muestra un mensaje genérico al usuario y registra el detalle técnico completo en Logcat.
     * <p>
     * De cara al usuario: sólo el parámetro {@code mensaje} (sin URL ni stack trace).
     * De cara al desarrollador: método HTTP, endpoint completo y stack trace en Log.e().
     */
    public static void mostrar_error_peticion(Context contexto, String tag, String mensaje,
                                              int method, String endPoint, Exception e) {
        // — Toast: mensaje legible para el usuario, sin datos técnicos internos —
        Toast.makeText(contexto, mensaje, Toast.LENGTH_SHORT).show();

        // — Logcat: detalle completo para el desarrollador —
        String[] str_methods = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH"};
        String detalle = mensaje
                + " | " + str_methods[method] + " " + endPoint
                + (e != null ? " | " + e.toString() : "");
        Log.e(tag, detalle + (e != null ? "\n" + Log.getStackTraceString(e) : ""));
    }
}
