package com.homney.app.ui.nav_menu_secundario;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.homney.app.R;

public class fragment_ayuda extends Fragment {
Button btn_contactar;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_ayuda, container, false);
        btn_contactar = v.findViewById(R.id.btn_contactar);

        btn_contactar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri  envioMail = Uri.parse("mailto:marcoscmdev@info.com");
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, envioMail);
                startActivity(emailIntent);
            }
        });

        return v;
    }
}
