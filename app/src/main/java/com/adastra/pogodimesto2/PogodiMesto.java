/**
 ****************************************
 * Imenovanje fajlova:
 ****************************************
 *
 * Za .java activity fajlove: prefix "Activity" (primer: ActivitySingleGame.java)
 * Ukoliko activity sadrži više komponenti (kao što je slučaj sa ActivitySingleGame), grupisati ih u poseban direktorijum
 *
 *
 * Za layout.xml fajlove:
 ** Activity:  prefix "activity_" (primer: activity_single_game.xml)
 ** Dialog:    prefix "dialog_"
 ** Fragment:  prefix "fragment_"
 ** Include:   prefix "include_"
 *
 *
 ****************************************
 * Imenovanje ID-eva za stringove i view-e
 ****************************************
 *
 * Globalni stringovi imaju prefix "global_"
 * Ostali stringovi i ID-evi imaju skraćeni prefix sa nazivom activity-ja kome pripadaju (primer: "act_single_game_")
 * Ako pripadaju nekom "dialog_" fajlu, umesto naziva activity-ja mogu imati naziv dialoga ("dlg_endgame_")
 *
 * Viewi se imenuju na sledeći način:
 * prefix activity-ja/dialoga + skraćeni naziv view-a + bliži opis view-a
 * Primeri:
 ** Activity Single Game + TextView + Points popup = act_single_game_text_points_popup
 ** Dialog Endgame + Button + cancel = dlg_endgame_btn_cancel
 *
 * Skraćenice (act, dlg, inc) se koriste kao prefixi za ID-eve, dakle unutar fajlova, a *NE* za imenovanje fajlova.
 *
 *
 *
 ****************************************
 * Skraćeni nazivi view-a:
 ****************************************
 * Button - btn
 * EditText - et
 * TextView - text
 * Checkbox - chk
 * RadioButton - rb
 * ToggleButton - tb
 * Spinner - spn
 * Menu - mnu
 * ListView - lv
 * GalleryView - gv
 * LinearLayout -ll
 * RelativeLayout - rl
 * TextSwitcher - tsw
 *
 */

package com.adastra.pogodimesto2;

import com.adastra.pogodimesto2.gameplay.Mesto;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.example.games.basegameutils.GameHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class PogodiMesto extends Application {

    public static final String PACKAGE        = "com.adastra.pogodimesto";
    public static final String CONTACT_EMAIL  = "adastra.apps@gmail.com";
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;
    public static int SCREEN_HEIGHT_REAL; // Prava visina, uzimajuci u obzir i nav bar
    public static int SCREEN_WIDTH_REAL;  // Prava sirina
    public static PogodiMesto mContext;
    public static GameHelper mGameHelper;


    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
        getScreenSize();
    }


    private static void getScreenSize() {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        SCREEN_WIDTH  = displayMetrics.widthPixels;
        SCREEN_HEIGHT = displayMetrics.heightPixels;


        // Kod za dobijanje stvarne velicine ekrana!
        /*if (Build.VERSION.SDK_INT >= 11) {
            Point size = new Point();
            try {
                ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(size);
                SCREEN_WIDTH_REAL = size.x;
                SCREEN_HEIGHT_REAL = size.y;
            } catch (NoSuchMethodError e) {
                Log.i("error", "error");
                SCREEN_WIDTH_REAL = SCREEN_WIDTH;
                SCREEN_HEIGHT_REAL = SCREEN_HEIGHT;
            }
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
            SCREEN_WIDTH_REAL = metrics.widthPixels;
            SCREEN_HEIGHT_REAL = metrics.heightPixels;
        }*/
    }


    public static double pixelsToKilometers(double horizontalDist, double verticalDist) {
        double WIDTH  = 329970.44;
        double HEIGHT = 481972.98;

        double X_cof = WIDTH/Mesto.mapWidth;       // Odnos između piksela na ekranu i veličine srbije
        double Y_cof = HEIGHT/Mesto.mapHeight;

        return Math.hypot(horizontalDist * X_cof, verticalDist * Y_cof)/1000;
    }



    public static void writeToLog(String data){

        File root = android.os.Environment.getExternalStorageDirectory();

        File dir = new File (root.getAbsolutePath() + "/Android/data/" + PACKAGE);
        if(!dir.mkdirs())
            Log.e("ERROR", "Ne mogu da kreiram folder: " + dir);

        Log.d("DEBUG", "dir: "+dir);
        File file = new File(dir, "debug.log");
        if (!file.exists()) {
            data  = "Device:            " + Build.DEVICE +"\n"+
                    "Model:             " + Build.MODEL + " ("+ Build.PRODUCT +")\n" +
                    "OS Version:        " + System.getProperty("os.version") + " ("+ Build.VERSION.INCREMENTAL +")\n" +
                    "OS API Level:      " + Build.VERSION.SDK_INT + "\n" +
                    "Screen Resolution: " + SCREEN_WIDTH + "x" + SCREEN_HEIGHT + "\n" +
                    "-----------------------------------------------\n\n\n" +
                    data;
        }

        try {
            FileOutputStream f = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(f);
            pw.println(data);
            pw.flush();
            pw.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void setMargins (View v, int left, int top, int right, int bottom) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
    }



    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return rand.nextInt((max - min) + 1) + min;
    }

    /*private void getDeviceSuperInfo() {
        Log.i("DEBUG", "getDeviceSuperInfo");

        try {

            String s = "Debug-infos:";
            s += "\n OS Version: "      + System.getProperty("os.version")      + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
            s += "\n OS API Level: "    + android.os.Build.VERSION.SDK_INT;
            s += "\n Device: "          + android.os.Build.DEVICE;
            s += "\n Model (and Product): " + android.os.Build.MODEL            + " ("+ android.os.Build.PRODUCT + ")";

            s += "\n RELEASE: "         + android.os.Build.VERSION.RELEASE;
            s += "\n BRAND: "           + android.os.Build.BRAND;
            s += "\n DISPLAY: "         + android.os.Build.DISPLAY;
            s += "\n CPU_ABI: "         + android.os.Build.CPU_ABI;
            s += "\n CPU_ABI2: "        + android.os.Build.CPU_ABI2;
            s += "\n UNKNOWN: "         + android.os.Build.UNKNOWN;
            s += "\n HARDWARE: "        + android.os.Build.HARDWARE;
            s += "\n Build ID: "        + android.os.Build.ID;
            s += "\n MANUFACTURER: "    + android.os.Build.MANUFACTURER;
            s += "\n SERIAL: "          + android.os.Build.SERIAL;
            s += "\n USER: "            + android.os.Build.USER;
            s += "\n HOST: "            + android.os.Build.HOST;


            Log.i("DEBUG" + " | Device Info > ", s);

        } catch (Exception e) {
            Log.e("DEBUG", "Error getting Device INFO");
        }
    }*/



    public static void changeViewIndex(View child, int index) {
        final ViewGroup parent = (ViewGroup) child.getParent();
        if (parent != null) {
            parent.removeView(child);
            parent.addView(child, index);
        }
    }
}