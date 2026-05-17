package com.homney.app.webservice;

import android.util.Log;

import com.android.volley.toolbox.HurlStack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HurlStack personalizado que acepta el certificado SSL de AwardSpace
 * sin validar la cadena de CAs intermedias.
 */
public class TrustAllHurlStack extends HurlStack {

    private static final String TAG = "TrustAllHurlStack";

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection connection = super.createConnection(url);

        if (connection instanceof HttpsURLConnection) {
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                        }
                }, new SecureRandom());

                HttpsURLConnection https = (HttpsURLConnection) connection;
                https.setSSLSocketFactory(sc.getSocketFactory());
                // Acepta cualquier hostname (necesario en AwardSpace shared hosting)
                https.setHostnameVerifier((hostname, session) -> true);

            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                Log.e(TAG, "Error configurando SSL", e);
            }
        }

        return connection;
    }
}
