package com.adastra.pogodimesto2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class ActivitySettings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_settings);
    }

    public void onClick_About(View v) {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().getAttributes().windowAnimations = R.style.SettingsDialogAnimation;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_about);

        if (Build.VERSION.SDK_INT < 21)
            // Na verzijama <21 ima okvir oko dialoga; ovo uklanja taj okvir, ali je dialog sirok koliko i ekran
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        Button btnSendEmail = (Button) dialog.findViewById(R.id.dlg_about_btn_sendEmail);
        btnSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick_SendEmail(v);
            }
        });
    }

    public void onClick_SendEmail(View v) {
        String versionName = "N/A";
        int versionCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
            versionCode = pInfo.versionCode;
        }catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("Error", "PackageInfo error (onClick_SendEmail)");
        }

        String extraSubject = getResources().getString(R.string.global_app_name) + " - контакт";
        String extraText = "Напишите своју поруку овде.\n\n\n" +
                "--------\n" +
                "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.PRODUCT + ")\n" +
                "OS: " + Build.VERSION.RELEASE + " (API: " + Build.VERSION.SDK_INT + ")\n" +
                "Description: " + Build.DISPLAY + " (" + PogodiMesto.SCREEN_WIDTH + "x" + PogodiMesto.SCREEN_HEIGHT + ")\n" +
                "Kernel: " + System.getProperty("os.version") + " ("+ Build.VERSION.INCREMENTAL +")\n" +
                "App Version: " + versionName + " (" + versionCode + ")";

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822"); // Filtrira aplikacije koje podrzavaju send intent
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{PogodiMesto.CONTACT_EMAIL});
        i.putExtra(Intent.EXTRA_SUBJECT, extraSubject);
        i.putExtra(Intent.EXTRA_TEXT, extraText);
        try {
            startActivity(Intent.createChooser(i, "Пошаљи имејл..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Није пронађена ниједна апликација за слање имејлова.", Toast.LENGTH_LONG).show();
        }
    }
}
