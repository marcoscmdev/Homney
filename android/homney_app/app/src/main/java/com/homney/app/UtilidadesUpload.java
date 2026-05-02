package com.homney.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UtilidadesUpload {
    /* Upload fichero */

    public static Bitmap getBitMapReducido(String ruta_fichero,
                                           int dimension_requerida){
        InputStream entrada_fichero = null;
        try {
            entrada_fichero = new FileInputStream(new File(ruta_fichero));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        BitmapFactory.Options opicionesBitmap = new BitmapFactory.Options();
        opicionesBitmap.inJustDecodeBounds = true;
         /* Con esta opción a true, no se carga la imagen en memoria y se obtienen
         sus dimensiones, que se guardarán en el propio objeto de opciones,
         en los atributos outWidth y outHeight
         */

        // Obtenemos el tamaño original de la imagen, sin cargarla en memoria
        BitmapFactory.decodeStream(entrada_fichero,null,opicionesBitmap);

         /* Encontramos la escala correcta según dimensión reducida requerida.
         Por razones de eficiencia debe ser una potencia de 2.
         La escala indica 1/2 del tamaño original, 1/4, 1/8...
         */

        int ancho_tmp=opicionesBitmap.outWidth, alto_tmp=opicionesBitmap.outHeight;
        int escala=1;
        while((ancho_tmp >=dimension_requerida) && (alto_tmp >= dimension_requerida)){
            escala=escala*2;
            ancho_tmp=ancho_tmp/2;
            alto_tmp=alto_tmp/2;
        }

        opicionesBitmap.inJustDecodeBounds=false; // Cargamos la imagen en memoria
        opicionesBitmap.inSampleSize=escala; // Reducimos su tamaño antes de cargarla
        try { // Es necesario volver a abrir el fichero
            entrada_fichero = new FileInputStream(new File(ruta_fichero));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return BitmapFactory.decodeStream(entrada_fichero,null,opicionesBitmap);
    }


    /*
     * El método toma Bitmap como argumento
     * luego devolverá el array de bytes [] para el mapa de bits dado
     * aquí estamos usando Compresión PNG con calidad% de calidad
     * puedes dar calidad entre 0 y 100
     * 0 significa peor calidad
     * 100 significa la mejor calidad
     * */
    public static byte[] getBytesPNGDeBitmap(Bitmap bitmap, int calidad /* 80 es una buena medida */) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, calidad, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    public static String getRutaRealDeImagenURI(Uri contentUri, ContentResolver contentResolver) {

        String [] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = contentResolver.query( contentUri,
                proj, // columnas a entregar
                null,       // WHERE filas
                null,       // WHERE argumentos
                null); // order by
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    /* Precondición: Permiso de escritura en almacenamiento externo concedido */
    public static String crearFicheroExternoTemporal(String extension, String carpeta, boolean publico, Context contexto) throws IOException {
        // Creamos un nombre de fichero único.
        String instanteActual = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreFichero = instanteActual+"_";
        File fcarpeta=null;
        if (publico)
            fcarpeta = Environment.getExternalStoragePublicDirectory(carpeta);
        else
            fcarpeta = contexto.getExternalFilesDir(carpeta);

        File fichero = File.createTempFile( nombreFichero, extension, fcarpeta  );

        return  fichero.getAbsolutePath();
    }

    public static void anhadirAGaleria(String rutaFicheo, Context contexto) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(rutaFicheo);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        contexto.sendBroadcast(mediaScanIntent);
    }
}
