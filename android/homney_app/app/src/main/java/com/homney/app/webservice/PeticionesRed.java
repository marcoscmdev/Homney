package com.homney.app.webservice;


import android.content.Context;
import android.graphics.Bitmap;

import androidx.collection.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;


public class PeticionesRed {
    private static PeticionesRed mInstance;
    private static RequestQueue mcolaPeticiones;
    private ImageLoader mImageLoader;
    private static Context mCtx;

        private PeticionesRed(Context context) {
                mCtx = context;
                mcolaPeticiones = getColaPeticiones();

                mImageLoader = new ImageLoader(mcolaPeticiones,
                        new ImageLoader.ImageCache() {
                            private final LruCache<String, Bitmap>
                                    cache = new LruCache<String, Bitmap>(20);

                            @Override
                            public Bitmap getBitmap(String url) {
                                return cache.get(url);
                            }

                            @Override
                            public void putBitmap(String url, Bitmap bitmap) {
                                cache.put(url, bitmap);
                            }
                        });
            }

    public static synchronized PeticionesRed getInstancia(Context context) {
        if (mInstance == null) {
            mInstance = new PeticionesRed(context);
        }
        return mInstance;
    }

    public static RequestQueue getColaPeticiones() {
        if (mcolaPeticiones == null) {
            /* getApplicationContext() es importante, evita perder el objeto si el
              contexto es activity o BroadcastReceiver
             */
            mcolaPeticiones = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mcolaPeticiones;
    }

    public static <T> void anhadirPeticionACola(Request<T> req) {
        getColaPeticiones().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
