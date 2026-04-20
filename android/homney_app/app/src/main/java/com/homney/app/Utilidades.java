package com.homney.app;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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

import java.io.File;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
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
            datosDialog.setTitle("Solicitud de permiso");
            datosDialog.setMessage(textoAclaratorio);
            datosDialog.setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
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

    public static void mostrar_error_peticion(Context contexto,String tag,String mensaje, int method, String endPoint,Exception e) {
        String[] str_methods= { "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH" };
        String mensajeError = mensaje + " (" + (e!=null?e.toString():"") + ") " + str_methods[method] + ": " + endPoint ;
        Toast.makeText(contexto, mensajeError, Toast.LENGTH_SHORT).show();
        Log.e(tag,mensajeError + "\n" + (e!=null?Log.getStackTraceString(e):""));
    }
}
