package com.adastra.pogodimesto2.gameplay;

import android.content.Context;
import android.util.Log;

import com.adastra.pogodimesto2.ObscuredSharedPreferences;
import com.adastra.pogodimesto2.PogodiMesto;
import com.adastra.pogodimesto2.R;
import com.google.android.gms.games.Games;


public class PlayGames {

    public final String TAG = "PlayGames";

    public final String PREF_L_HIGHSCORE = "leaderboard_highscore";
    public final String PREF_L_AVG_TIME = "leaderboard_average_time";
    public final String PREF_L_AVG_ACCURACY = "leaderboard_average_accuracy";
    public final String PREF_A_ODLICAN_ODGOVOR = "achievement_odlican_odgovor";
    public final String PREF_A_MAX_PRECIZNOST = "achievement_maksimalna_preciznost";
    public final String PREF_A_BRZINA = "achievement_brzinom_svetlosti";
    public final String PREF_A_MAX_POENA_NIVO = "achievement_maksimalno_poena_u_nivou";
    public final String PREF_A_PREDJENA_IGRA = "achievement_predjena_igra";

    // Unix timestamp vremena prve igre
    public final String PREF_TIME_FIRST = "time_first_play";

    // Unix timestamp vremena poslednje igre
    public final String PREF_TIME_LAST = "time_last_play";

    // TREBA LI prikazati poruku o povlacenju prsta na gore za nastavak igre (swipe up stats) ?? default = true = da
    public final String PREF_SWIPE_MSG = "swipe_message";



    /** Ucitane vrednosti iz SharedPreferences */
    public int mSaved_L_Highscore;
    public float mSaved_L_AvgTime;
    public float mSaved_L_AvgAccuracy;
    public boolean mSaved_A_OdlicanOdgovor;
    public boolean mSaved_A_MaxPreciznost;
    public boolean mSaved_A_Brzina;
    public boolean mSaved_A_MaxPoenaNivo;
    public boolean mSaved_A_PredjenaIgra;
    public long mSaved_Time_First;
    public long mSaved_Time_Last;
    public boolean mSaved_Swipe_Msg;


    private Context mContext;
    private ObscuredSharedPreferences mSharedPrefs;


    PlayGames(Context c) {
        mContext = c;
        mSharedPrefs = ObscuredSharedPreferences.getPrefs(mContext, PogodiMesto.PACKAGE, Context.MODE_APPEND);

        LoadAchievements();
        LoadLeaderboards();
        LoadEverythingElse();
        DebugSharedPrefs();
    }


    /**
     * Učitava status dostignuća iz SharedPreferences (ima/nema)
     */
    private void LoadAchievements() {
        try {
            mSaved_A_OdlicanOdgovor = mSharedPrefs.getBoolean(PREF_A_ODLICAN_ODGOVOR, false);
            mSaved_A_MaxPreciznost = mSharedPrefs.getBoolean(PREF_A_MAX_PRECIZNOST, false);
            mSaved_A_Brzina = mSharedPrefs.getBoolean(PREF_A_BRZINA, false);
            mSaved_A_MaxPoenaNivo= mSharedPrefs.getBoolean(PREF_A_MAX_POENA_NIVO, false);
            mSaved_A_PredjenaIgra = mSharedPrefs.getBoolean(PREF_A_PREDJENA_IGRA, false);
        }
        catch (Exception e) {
            Log.w(TAG, "Failed loading achievements: "+e);
        }
    }


    /**
     * Učitava skorove iz SharedPreferences (-1 ako još nije upisan skor)
     */
    private void LoadLeaderboards() {
        try {
            mSaved_L_Highscore = mSharedPrefs.getInt(PREF_L_HIGHSCORE, -1);
            mSaved_L_AvgTime = mSharedPrefs.getFloat(PREF_L_AVG_TIME, -1);
            mSaved_L_AvgAccuracy = mSharedPrefs.getInt(PREF_L_AVG_ACCURACY, -1);
        }
        catch (Exception e) {
            Log.w(TAG, "Failed loading leaderboards: "+e);
        }
    }


    /**
     * Učitava sve ostalo što nisu dostignuća/skorovi
     */
    private void LoadEverythingElse() {
        try {
            mSaved_Time_First = mSharedPrefs.getLong(PREF_TIME_FIRST, -1);
            mSaved_Time_Last = mSharedPrefs.getLong(PREF_TIME_LAST, -1);
            mSaved_Swipe_Msg = mSharedPrefs.getBoolean(PREF_SWIPE_MSG, true);
        }
        catch (Exception e) {
            Log.w(TAG, "Failed loading everything else: "+e);
        }


        // Izmena first i last vremena
        long timestamp = System.currentTimeMillis();
        ObscuredSharedPreferences.Editor editor = mSharedPrefs.edit();
        if (mSaved_Time_First == -1) {
            editor.putLong(PREF_TIME_FIRST, timestamp);
            mSaved_Time_First = timestamp;
        }
        editor.putLong(PREF_TIME_LAST, timestamp); // TODO: Time last sacuvati nakon zavrsetka svake igre
        mSaved_Time_Last = timestamp;
        editor.apply();
    }


    /**
     * Proverava da li igrač ispunjava uslove da dobije određeni achievement, da li ga već poseduje,
     * čuva informacije lokalno i šalje ih na Google Play Games.
     * TODO: Da se iscrta toast za one igrače koji nisu ulogovani
     *
     * @param achievement određuje koji se achievement čuva
     * @param valueToCompare vrednost koju treba uporediti sa nekom drugom, kako bismo odredili da li je igrač ispunio uslove da dobije ovaj achievement
     */
    public void SaveAchievement(String achievement, String valueToCompare) {
        ObscuredSharedPreferences.Editor editor = mSharedPrefs.edit();
        boolean signed_in = PogodiMesto.mGameHelper.isSignedIn();

        if (achievement == PREF_A_ODLICAN_ODGOVOR) { // Achievement: Odlican odgovor
            if (Integer.parseInt(valueToCompare) == 1000 && !mSaved_A_OdlicanOdgovor) {
                editor.putBoolean(PREF_A_ODLICAN_ODGOVOR, true);
                mSaved_A_OdlicanOdgovor = true;

                if (signed_in) {
                    Games.Achievements.unlock(PogodiMesto.mGameHelper.getApiClient(),
                            PogodiMesto.mContext.getResources().getString(R.string.achievement_odlican_odgovor));
                }
            }
        }


        if (achievement == PREF_A_MAX_PRECIZNOST) { // Achievement: Maksimalna preciznost
            if (Float.parseFloat(valueToCompare) <= 1.0 && !mSaved_A_MaxPreciznost) {
                editor.putBoolean(PREF_A_MAX_PRECIZNOST, true);
                mSaved_A_MaxPreciznost = true;

                if (signed_in) {
                    Games.Achievements.unlock(PogodiMesto.mGameHelper.getApiClient(),
                            PogodiMesto.mContext.getResources().getString(R.string.achievement_maksimalna_preciznost));
                }
            }
        }


        if (achievement == PREF_A_BRZINA) { // Achievement: Brzina
            if (Float.parseFloat(valueToCompare) <= 0.1 && !mSaved_A_Brzina) {
                editor.putBoolean(PREF_A_BRZINA, true);
                mSaved_A_Brzina = true;

                if (signed_in) {
                    Games.Achievements.unlock(PogodiMesto.mGameHelper.getApiClient(),
                            PogodiMesto.mContext.getResources().getString(R.string.achievement_brzinom_svetlosti));
                }
            }
        }


        if (achievement == PREF_A_MAX_POENA_NIVO) { // Achievement: maksimalan broj poena u nivou
            if (!mSaved_A_MaxPoenaNivo) { // Ovde ne treba nikakva druga provera, jer se ona vrsi u SingleGameActivity
                editor.putBoolean(PREF_A_MAX_POENA_NIVO, true);
                mSaved_A_MaxPoenaNivo = true;

                if (signed_in) {
                    Games.Achievements.unlock(PogodiMesto.mGameHelper.getApiClient(),
                            PogodiMesto.mContext.getResources().getString(R.string.achievement_maks_poena_u_nivou));
                }
            }
        }


        if (achievement == PREF_A_PREDJENA_IGRA) { // Achievement: Predjena igra
            if (!mSaved_A_PredjenaIgra) {
                Log.d("PlayGames DEBUG", "ACHIEVEMENT: PREDJENA IGRA!");
                editor.putBoolean(PREF_A_PREDJENA_IGRA, true);
                mSaved_A_PredjenaIgra = true;

                if (signed_in) {
                    Games.Achievements.unlock(PogodiMesto.mGameHelper.getApiClient(),
                            PogodiMesto.mContext.getResources().getString(R.string.achievement_predjena_igra));
                }
            }
        }


        editor.apply();
    }


    /**
     * Proverava da li igrač ispunjava uslove da mu se upiše novi najbolji skor iz bilo koje oblasti.
     * Čuva informacije lokalno i šalje ih na Google Play Games.
     * TODO: Da se ispiše nekakvo obaveštenje za sve igrače
     *
     * @param score određuje koji se skor čuva
     * @param newScore novi skor koji se upoređuje sa već sačuvanom vrednošću
     */
    public void SaveScore(String score, String newScore) {
        ObscuredSharedPreferences.Editor editor = mSharedPrefs.edit();
        boolean signed_in = PogodiMesto.mGameHelper.isSignedIn();

        if (score == PREF_L_HIGHSCORE) { // Skor: Ukupan broj poena
            if (Integer.parseInt(newScore) > mSaved_L_Highscore || mSaved_L_Highscore == -1) {
                editor.putInt(PREF_L_HIGHSCORE, Integer.parseInt(newScore));
                mSaved_L_Highscore = Integer.parseInt(newScore);

                if (signed_in) {
                    Games.Leaderboards.submitScore(PogodiMesto.mGameHelper.getApiClient(),
                            PogodiMesto.mContext.getResources().getString(R.string.leaderboard_najuspesniji_igraci),
                            Integer.parseInt(newScore));
                }
            }
        }

        if (score == PREF_L_AVG_ACCURACY) { // Skor: Najbolja prosečna preciznost
            if (Float.parseFloat(newScore) < mSaved_L_AvgAccuracy || mSaved_L_AvgAccuracy == -1) {
                editor.putFloat(PREF_L_AVG_ACCURACY, Float.parseFloat(newScore));
                mSaved_L_AvgAccuracy = Float.parseFloat(newScore);

                if (signed_in) {
                    Games.Leaderboards.submitScore(PogodiMesto.mGameHelper.getApiClient(),
                            PogodiMesto.mContext.getResources().getString(R.string.leaderboard_najbolja_prosecna_preciznost),
                            (long) Float.parseFloat(newScore));
                }
            }
        }


        editor.apply();
    }


    /**
     * Čuva određenu preferencu, koja nije niti achievement niti leaderboard
     *
     * @param t     Vrsta preference (string/int..)
     * @param key   Naziv preference
     * @param value Vrednost koja će se upisati, ali u formi stringa
     */
    public void SavePref(Class<?> t, String key, String value) {

        ObscuredSharedPreferences.Editor editor = mSharedPrefs.edit();

        if (t == String.class)
            editor.putString(key, value);

        else if (t == Integer.class)
            editor.putInt(key, Integer.parseInt(value));

        else if (t == Float.class)
            editor.putFloat(key, Float.parseFloat(value));

        else if (t == Boolean.class)
            editor.putBoolean(key, Boolean.parseBoolean(value));

        else if (t == Long.class)
            editor.putLong(key, Long.parseLong(value));

        //editor.apply();
    }


    private void DebugSharedPrefs() {
        Log.d("DEBUG", "L_Highscore:        "+mSaved_L_Highscore);
        Log.d("DEBUG", "L_AvgTime:          "+mSaved_L_AvgTime);
        Log.d("DEBUG", "L_AvgAccuracy:      "+mSaved_L_AvgAccuracy);
        Log.d("DEBUG", "A_OdlicanOdgovor:   "+mSaved_A_OdlicanOdgovor);
        Log.d("DEBUG", "A_MaxPreciznost:    "+mSaved_A_MaxPreciznost);
        Log.d("DEBUG", "A_Brzina:           "+mSaved_A_Brzina);
        Log.d("DEBUG", "A_MaxPoenaNivo:     "+mSaved_A_MaxPoenaNivo);
        Log.d("DEBUG", "A_PredjenaIgra:     "+mSaved_A_PredjenaIgra);
        Log.d("DEBUG", "Time_First:         "+mSaved_Time_First);
        Log.d("DEBUG", "Time_Last:          "+mSaved_Time_Last);
        Log.d("DEBUG", "Swipe_Msg:          "+mSaved_Swipe_Msg);
    }
}
