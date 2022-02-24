package com.adastra.pogodimesto2.gameplay;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.adastra.pogodimesto2.PogodiMesto;
import com.adastra.pogodimesto2.R;
import com.google.android.gms.games.Games;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("unchecked")
public class ActivitySingleGame extends Activity {

    // Da li ispisuje debug informacije?
    private static final boolean DEBUG = true;

    // Ukupan broj nivoa
    private static final int TOTAL_LEVELS = 8;

    // Ukupan broj poena koji moze da se dobije u nivou
    private static final int MAX_LEVEL_BOUNTY = 1000;

    // Vreme za odgovor na postavljeno pitanje  (u milisekundama)
    private static final int QUESTION_TIME = 7000;

    // Kad ce tekst za preostalo vreme da promeni boju
    private static final int CRITICAL_TIME = 2500;

    // Nakon koliko vremena se uklanjaju cioda i kruzic, i zadaje sledece mesto
    private static final int RESTING_TIME = 2500;

    // Ako je vreme odgovora <= od ovoga, igrac dobija max broj bodova (500)
    private static final int MAX_BOUNTY_TIME = 300;

    // Ako je udaljenost <= od ovoga, igrac dobija max broj bodova (500)
    private static final int MAX_BOUNTY_DIST = 10;

    // Koliko km moze da omasi, a da dobije poene (vise od ovoga -> ne dobija nista)
    private static final double MAX_DISTANCE = 100.0;

    // Tipovi nivoa
    private static final int CITY = 0;
    private static final int VILLAGE = 1;
    private static final int MONASTERY = 2;
    private static final int LANDMARK = 3;

    // Tezine nivoa
    private static final int EASY = 0;
    private static final int MEDIUM = 1;
    private static final int HARD = 2;
    private static final int ULTRA = 3;

    // index u nizu "mLevels" gde je upisana tezina
    private static final int INDEX_DIFFICULTY = 0;

    // index gde je upisan naziv nivoa (gradovi/sela, itd)
    private static final int INDEX_TYPE = 1;

    // index gde je upisana duzina  nivoa (broj pitanja)
    private static final int INDEX_LENGTH = 2;

    // Index gde je upisan minimalan broj bodova za prelazak nivoa
    private static final int INDEX_MIN = 3;

    private static final String SHAREDPREF_SHOW_STATS_CLOSE_HINT = "show_stats_close_hint";

    // Koliko px ce cioda da pada (kad igrac klikne na mapu da bi dao odgovor)
    private static final int PIN_FALL_DISTANCE = 200;

    /*****************************************************/

    // Views
    ImageView mImgMap;
    ImageView mImgPin;
    TextView mNewPlaceTextTitle;
    TextView mTextTimeContent;
    TextView mTextPointsPopUp;
    TextView mStatsTextTimeLeft;
    TextView mStatsTextAccuracy;
    TextView mStatsTextPoints;
    TextView mStatsTextCloseHint;
    RelativeLayout mMainLayout;
    RelativeLayout mDlgBackground;
    LinearLayout mStatsDropdown;
    TextView mTextTarget;
    ProgressBar mStatsProgressBar;
    View mBlackShade;
    TextView mTextPoints;
    TextSwitcher mTswTarget;
    TextSwitcher mTswPoints;
    ImageView mTempImagePin;
    ImageView mTempImageFingerprint;
    QuestionProgress mQuestionProgress;

    // Iako je LinearLayout, stoji View zbog biblioteke za kompatibilnost za CircularReveal
    View mNewPlaceWrapper;

    /*****************************************************/

    // U promenljivoj se skladisti vrednost koja je za 1 manja od stvarnog levela (level 1 -> 0, 2 -> 1, itd)
    int mLevel = 0;

    // Ukupan broj poena
    int mPoints = 0;

    // Ukupan broj poena u trenutnom levelu
    int mPointsInLevel = 0;

    // Broj poena od zadnjeg odgovora
    int mPointsLast = 0;

    // Koje mesto je na redu u trenutnom nivou
    int mLevelProgress = -1;

    // Koliko je lakih/srednjih/...   gradova/sela/...   bilo prikazano tokom cele igre
    // Level Progress (Extended)   [tip mesta][tezina]
    int mLevelProgressEx[][] = new int[4][4];

    // Ukupan broj poena u tipovima nivoa (ukupno za gradove, ukupno za sela, ...)
    int mPointsTypeTotal[] = new int[4];

    // Tip trenutnog nivoa (gradovi/sela...)
    int mCurrentType;

    // Tezina trenutnog nivoa
    int mCurrentDifficulty;

    // Duzina (broj pitanja) u trenutnom nivou
    int mCurrentLength;

    // Da li je igra pocela
    boolean mGameInProgress = false;

    boolean mCanClickMap = false;

    // Vreme preostalo za odgovor (sekunde)
    double mTimeLeftSecs;

    // Vreme preostalo za odgovor (milisekunde)
    double mTimeLeftMillis;

    // Preciznost za prethodno mesto
    double mLastAccuracy;

    // Vreme prethodnog odgovora
    double mLastTime;

    // Mesto koje treba da nadje
    Mesto mTargetPlace;

    // Zbir vremena svih odgovora, koji će se deliti promenljivom mAvgValuesDivider kako bi se dobilo prosečno vreme
    float mTimeAvgSum;

    // Zbir udaljenosti/preciznosti svih odgovora, koji će se deliti promenljivom mAvgValuesDivider kako bi se dobila prosečna preciznost
    float mDistanceAvgSum;

    // Brojac (sa ovim se deli zbir)
    int mAvgCounter = 0;

    // Brojac za povlacenje stats bara na gore (kad je >3, onda se cuva u shared prefs i ne prikazuje se uputstvo)
    int mSwipeUpCounter = 0;

    // Da li je stats dropdown spusten/prikazan?
    boolean mIsStatsDown = false;

    // Prikazati tekst "Prevuci prstom na gore" u stats dropdown-u?
    // Treba da bude prikazan samo tokom 1. nivoa ikada, i da nakon toga nikad vise ne bude prikazan
    boolean mShowStatsDropdownCloseHint = true;

    int mOldHighscore = 0;
    float mOldHighscoreAccuracy = 0f;
    float mOldHighscoreTime = 0f;

    /*****************************************************/

    ArrayList<Mesto>[][] mPlaces = new ArrayList[4][4]; // [tip mesta][tezina]
    int mLevels[][] = new int[20][4];
    CountDownTimer mTimerMain;
    CountDownTimer mTimerRest;
    CountDownTimer mTimerStartGame;
    CountDownTimer mTimerRemoveTransition;
    String mLevelStrings[] = new String[4];
    String mDifficultyStrings[] = new String[4];
    PlayGames playGames;
    Animation closeAnim;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playGames = new PlayGames(this);


        // ******************************** Dizajn *********************************
        setContentView(R.layout.activity_single_game);

        //dimovanje navbar-a
        View decorView = ActivitySingleGame.this.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        decorView.setSystemUiVisibility(uiOptions);

        mImgMap                 = (ImageView)       findViewById(R.id.act_single_game_img_map);
        mImgPin                 = (ImageView)       findViewById(R.id.act_single_game_img_pin);
        mNewPlaceTextTitle      = (TextView)        findViewById(R.id.act_single_game_newPlace_text_title);
        mTextTimeContent        = (TextView)        findViewById(R.id.act_single_game_text_time_left);
        mTextPointsPopUp        = (TextView)        findViewById(R.id.act_single_game_text_points_popup);
        mStatsTextTimeLeft      = (TextView)        findViewById(R.id.act_single_game_stats_text_time_left);
        mStatsTextAccuracy      = (TextView)        findViewById(R.id.act_single_game_stats_text_accuracy);
        mStatsTextPoints        = (TextView)        findViewById(R.id.act_single_game_stats_text_points);
        mStatsTextCloseHint     = (TextView)        findViewById(R.id.act_single_game_stats_text_close_hint);
        mMainLayout             = (RelativeLayout)  findViewById(R.id.act_single_game_main_layout);
        mDlgBackground          = (RelativeLayout)  findViewById(R.id.act_single_game_background_shade);
        mStatsDropdown          = (LinearLayout)    findViewById(R.id.act_single_game_stats_dropdown);
        mTswTarget              = (TextSwitcher)    findViewById(R.id.act_single_game_tsw_target);
        mTswPoints              = (TextSwitcher)    findViewById(R.id.act_single_game_tsw_points);
        mStatsProgressBar       = (ProgressBar)     findViewById(R.id.act_single_game_stats_progressbar);
        mBlackShade             =                   findViewById(R.id.act_single_game_black_shade);
        mNewPlaceWrapper        =                   findViewById(R.id.act_single_game_newPlace_wrapper);
        mQuestionProgress       = (QuestionProgress)findViewById(R.id.act_single_game_mQuestionProgress);
        //touchEventSound         = MediaPlayer.create(this, R.raw.beep);
        //TODO smanjiti lag (ako on postoji) kada se puštaju zvukovi. možda neki pre-buffer ili tako nešto

        if (Build.VERSION.SDK_INT >= 21) {
            ViewCompat.setTransitionName(mImgMap, PogodiMesto.PACKAGE + ".imageMap");
            ViewCompat.setTransitionName(mImgPin, PogodiMesto.PACKAGE + ".imagePin");
        }


        // ****************** TARGET SWITCHER ******************
        mTswTarget.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        mTswTarget.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
        mTswTarget.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                float spSize = getResources().getDimension(R.dimen.font_statusbar_big);
                float density = getResources().getDisplayMetrics().density;

                mTextTarget = new TextView(ActivitySingleGame.this);
                mTextTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP, spSize / density);
                mTextTarget.setTextColor(getResources().getColor(R.color.primary_dark));
                mTextTarget.setGravity(Gravity.END);
                return mTextTarget;
            }
        });
        // *****************************************************



        // ****************** POINTS SWITCHER ******************
        mTswPoints.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        mTswPoints.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
        mTswPoints.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                float spSize = getResources().getDimension(R.dimen.act_single_game_font_statusbar_small);
                float density = getResources().getDisplayMetrics().density;

                mTextPoints = new TextView(ActivitySingleGame.this);
                mTextPoints.setTextSize(TypedValue.COMPLEX_UNIT_SP, spSize / density);
                mTextPoints.setTextColor(getResources().getColor(R.color.primary_dark));
                mTextPoints.setGravity(Gravity.END);
                return mTextPoints;
            }
        });
        // *****************************************************


        mTextPointsPopUp.setPadding(0, (int) getResources().getDimension(R.dimen.act_single_game_points_popup_displacement), 0, 0);
        // *************************************************************************


        /**
         * mTimerRest: inicijalizacija
         *
         * Nakon odredjenog vremena, cioda i tacka se uklanjaju sa mape, i zadaje se novo mesto
         */
        mTimerRest = new CountDownTimer(RESTING_TIME, 500) {
            public void onFinish() {
                // Prikazuje informacije o preciznosti, vremenu i poenima nakon odgovora
                mCanClickMap = false;
                showStats();
            }

            public void onTick(long millisUntilFinished) {}
        };


        // Pocetak igre nakon jedne sekunde
        mTimerStartGame = new CountDownTimer(1000, 1000) {
            public void onFinish() {
                initializeLevels();
                showStartGameDialog();

                mOldHighscore = playGames.mSaved_L_Highscore;
                mOldHighscoreTime = playGames.mSaved_L_AvgTime;
                mOldHighscoreAccuracy = playGames.mSaved_L_AvgAccuracy;
            }

            public void onTick(long millisUntilFinished) {}
        }.start();


        ViewTreeObserver vto = mStatsDropdown.getViewTreeObserver();
        final SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mStatsDropdown.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mShowStatsDropdownCloseHint = sharedPref.getBoolean(SHAREDPREF_SHOW_STATS_CLOSE_HINT, true);

                // Stats dropdown se pomera gore, van ekrana
                // Za koliko se pomera, zavisi od toga da li je prikazan "close hint" ili nije
                if (mShowStatsDropdownCloseHint) {
                    PogodiMesto.setMargins(mStatsDropdown, 0, -mStatsDropdown.getHeight(), 0, 0);
                }
                else {
                    removeStatsDropdownCloseHint(); // Osim sto brise, takodje i pomera na gore.
                }
            }
        });


        if (!mShowStatsDropdownCloseHint) {
            mStatsTextCloseHint.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        PogodiMesto.mGameHelper.onStart(this);
    }
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        PogodiMesto.mGameHelper.onActivityResult(requestCode, resultCode, data);
    }


    public void onBackPressed() {
        if (mIsStatsDown) {
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(mStatsDropdown, "translationY", 0-mStatsDropdown.getHeight());
            slideUp.setDuration(300);
            slideUp.setInterpolator(new AccelerateInterpolator());
            slideUp.start();

            closeStats(); // Ovo treba da se pojavi 300ms kasnije, ali izgleda da se ne vidi razlika tokom igre, tako da nije problem
        }
        else {
            new AlertDialog.Builder(this)
                    .setTitle("Излаз")
                    .setMessage("Да ли заиста желиш да напустиш игру?")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mTextTimeContent.setText("--");

                            showEndGameDialog(mPoints > playGames.mSaved_L_Highscore,
                                    false,
                                    mOldHighscoreAccuracy > playGames.mSaved_L_AvgAccuracy,
                                    PogodiMesto.mGameHelper.isSignedIn(),
                                    false);
                        }
                    })
                    .setNegativeButton("Не", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }


        /*if (mTimerMain != null) mTimerMain.cancel();
        if (mTimerRest != null) mTimerRest.cancel();
        if (mTimerStartGame != null) mTimerStartGame.cancel();
        if (mTimerRemoveTransition != null) mTimerRemoveTransition.cancel();*/
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCanClickMap && event.getAction() == MotionEvent.ACTION_UP) {
            mCanClickMap = false;
            mTimerMain.cancel();

            Drawable res;
            String   uri        = "@drawable/ic_cioda";
            int imageResource   = getResources().getIdentifier(uri, null, getPackageName());
            int pinWidth        = (int) (36 * Resources.getSystem().getDisplayMetrics().density);
            int pinHeight       = (int) (36 * Resources.getSystem().getDisplayMetrics().density);
            int x_fix           = (PogodiMesto.SCREEN_WIDTH - Mesto.mapWidth)/2;
            int y_fix           = (PogodiMesto.SCREEN_HEIGHT - Mesto.mapHeight)/2;
            int pinFinalPosX    = mTargetPlace.x /*+ x_fix*/ - pinWidth/2;
            int pinFinalPosY    = mTargetPlace.y + y_fix - pinHeight;

            int     x           = (int) (event.getX());
            int     y           = (int) (event.getY());
            double  distance    = PogodiMesto.pixelsToKilometers(
                    Math.abs(mTargetPlace.x/*+x_fix*/ - x),
                    Math.abs(mTargetPlace.y + y_fix - y));
            int     time        = (int) (QUESTION_TIME-mTimeLeftMillis);


            // ************************ Računanje poena ************************
            double pct_time; // procenti
            double pct_dist;

            if (distance > MAX_DISTANCE)
                pct_dist = 0;
            else if (distance <= MAX_BOUNTY_DIST)
                pct_dist = 1;
            else
                pct_dist = 1 / (MAX_DISTANCE - MAX_BOUNTY_DIST) * (MAX_DISTANCE - distance);


            if (time <= MAX_BOUNTY_TIME)
                pct_time = 1;
            else
                pct_time = 1 / ((double) (QUESTION_TIME - MAX_BOUNTY_TIME)) * (mTimeLeftMillis);


            // Update korisnickih varijabli
            mPointsLast      = (int) Math.round(pct_dist * pct_time * MAX_LEVEL_BOUNTY);
            mPoints         += mPointsLast;
            mPointsInLevel  += mPointsLast;
            mLastAccuracy    = distance;
            mLastTime        = Math.round((QUESTION_TIME - mTimeLeftMillis)/1000 * 10.0)/10.0;
            mPointsTypeTotal[mCurrentType] += mPointsLast;
            mDistanceAvgSum += distance;
            mTimeAvgSum     += mLastTime;
            mAvgCounter++;

            // Dostignuca
            playGames.SaveAchievement(playGames.PREF_A_ODLICAN_ODGOVOR, Integer.toString(mPointsLast));
            playGames.SaveAchievement(playGames.PREF_A_MAX_PRECIZNOST, Double.toString(distance));
            if (mPointsLast > 800) playGames.SaveAchievement(playGames.PREF_A_BRZINA, Double.toString(mLastTime));

            // *****************************************************************


            if (DEBUG) {
                /*Log.d("DEBUG", "Place name:             "+mTargetPlace.name);
                Log.d("DEBUG", "Place X:                "+mTargetPlace.x);
                Log.d("DEBUG", "Place Y:                "+(mTargetPlace.y+y_fix));
                Log.d("DEBUG", "Touch X:                "+x);
                Log.d("DEBUG", "Touch Y:                "+y);
                Log.d("DEBUG", "QUESTION_TIME:          "+QUESTION_TIME);
                Log.d("DEBUG", "MAX_BOUNTY_TIME:        "+MAX_BOUNTY_TIME);
                Log.d("DEBUG", "Distance:               "+distance);
                Log.d("DEBUG", "Time (s):               "+mLastTime);
                Log.d("DEBUG", "Time (ms):              "+time);
                Log.d("DEBUG", "Time left (s):          "+mTimeLeftSecs);
                Log.d("DEBUG", "Time left (ms):         "+mTimeLeftMillis);
                Log.d("DEBUG", "Time pct:               "+pct_time*100+"%");
                Log.d("DEBUG", "Distance pct:           "+pct_dist*100+"%");
                Log.d("DEBUG", "Points last:            "+mPointsLast);
                Log.d("DEBUG", "Points (total):         "+mPoints);
                Log.d("DEBUG", "Points (cities):        "+mPointsTypeTotal[CITY]);
                Log.d("DEBUG", "Points (villages):      "+mPointsTypeTotal[VILLAGE]);
                Log.d("DEBUG", "Points (monasteries):   "+mPointsTypeTotal[MONASTERY]);
                Log.d("DEBUG", "Points (landmarks):     "+mPointsTypeTotal[LANDMARK]);
                Log.d("DEBUG", "Width/height:           "+PogodiMesto.SCREEN_WIDTH +" x "+PogodiMesto.SCREEN_HEIGHT);*/

                String log = "" +
                        "Time:                  "+ Calendar.HOUR + ":" + Calendar.MINUTE + ":" + Calendar.SECOND + "\n"+
                        "Place name:            "+mTargetPlace.name+"\n"+
                        "Place X:               "+mTargetPlace.x+"\n"+
                        "Place Y:               "+(mTargetPlace.y+y_fix)+"\n"+
                        "Touch X:               "+x+"\n"+
                        "Touch Y:               "+y+"\n"+
                        "QUESTION_TIME:         "+QUESTION_TIME+"\n"+
                        "MAX_BOUNTY_TIME:       "+MAX_BOUNTY_TIME+"\n"+
                        "Distance:              "+distance+"\n"+
                        "Time (s):              "+mLastTime+"\n"+
                        "Time (ms):             "+time+"\n"+
                        "Time left (s):         "+mTimeLeftSecs+"\n"+
                        "Time left (ms):        "+mTimeLeftMillis+"\n"+
                        "Time pct:              "+pct_time*100+"%"+"\n"+
                        "Distance pct:          "+pct_dist*100+"%"+"\n"+
                        "Points last:           "+mPointsLast+"\n"+
                        "Points (total):        "+mPoints+"\n"+
                        "Points (cities):       "+mPointsTypeTotal[CITY]+"\n"+
                        "Points (villages):     "+mPointsTypeTotal[VILLAGE]+"\n"+
                        "Points (monasteries):  "+mPointsTypeTotal[MONASTERY]+"\n"+
                        "Points (landmarks):    "+mPointsTypeTotal[LANDMARK]+"\n"+
                        "-----------------------------------------------\n";
                PogodiMesto.writeToLog(log);
            }


            // ************************ Iscrtavanje čiode ************************
            if (Build.VERSION.SDK_INT >= 21)
                res = getResources().getDrawable(imageResource, null);
            else
                res = getResources().getDrawable(imageResource);

            mTempImagePin = new ImageView(ActivitySingleGame.this);
            mTempImagePin.setImageDrawable(res);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pinWidth, pinHeight);
            params.leftMargin = pinFinalPosX;
            params.topMargin  = pinFinalPosY - PIN_FALL_DISTANCE;
            mMainLayout.addView(mTempImagePin, params);
            if (Build.VERSION.SDK_INT < 21)
                PogodiMesto.changeViewIndex(mTempImagePin, mMainLayout.indexOfChild(mTempImagePin) - 2);

            TranslateAnimation pinFall = new TranslateAnimation(0, 0, 0, PIN_FALL_DISTANCE);
            pinFall.setDuration(300);
            pinFall.setFillAfter(true);
            mTempImagePin.startAnimation(pinFall);
            // *******************************************************************



            // ******************** Iscrtavanje dodirne tacke ********************
            Drawable drawableFingerprint = ContextCompat.getDrawable(this, R.drawable.ic_fingerprint);
            BitmapDrawable bdFingerprint = (BitmapDrawable) drawableFingerprint;
            mTempImageFingerprint = new ImageView(this);
            mTempImageFingerprint.setId((int) System.currentTimeMillis());
            mTempImageFingerprint.setBackground(drawableFingerprint);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = x-(bdFingerprint.getBitmap().getWidth()/2);
            params.topMargin = y - (bdFingerprint.getBitmap().getHeight()/2);
            mMainLayout.addView(mTempImageFingerprint, params);
            if (Build.VERSION.SDK_INT < 21)
                PogodiMesto.changeViewIndex(mTempImageFingerprint, mMainLayout.indexOfChild(mTempImageFingerprint) - 2);
            // *******************************************************************

            showPoints(mPointsLast);
            mTimerRest.start();
        }
        return false;
    }


    private void showPoints(int pts) {
        if (pts == 0) {
            mTextPointsPopUp.setText("0");
            mTextPointsPopUp.setTextColor(getResources().getColor(R.color.red_400));
        }
        else if (pts == 1000) {
            mTextPointsPopUp.setText("+" + pts);
            mTextPointsPopUp.setTextColor(getResources().getColor(R.color.green_500));
        }
        else {
            mTextPointsPopUp.setText("+" + pts);
            mTextPointsPopUp.setTextColor(getResources().getColor(R.color.amber_500));
        }


        // Animacije
        TranslateAnimation textPosition = new TranslateAnimation(0, 0, 0, -(int) getResources().getDimension(R.dimen.act_single_game_points_popup_displacement));
        textPosition.setFillAfter(true);
        textPosition.setDuration(800);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setFillAfter(true);
        fadeIn.setDuration(800);

        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setFillAfter(true);
        fadeOut.setDuration(300);
        fadeOut.setStartOffset(RESTING_TIME);

        AnimationSet animatePointsText = new AnimationSet(false);
        animatePointsText.addAnimation(textPosition);
        animatePointsText.addAnimation(fadeIn);
        animatePointsText.addAnimation(fadeOut);
        mTextPointsPopUp.startAnimation(animatePointsText);

        animatePointsText.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                mTextPointsPopUp.setVisibility(View.INVISIBLE);
                mTextPointsPopUp.setPadding(0, (int) getResources().getDimension(R.dimen.act_single_game_points_popup_displacement), 0, 0);
            }


            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
    }


    private void assignNextPlace() {
        mGameInProgress = true;
        mCanClickMap = false;

        if(mLevels[mLevel][INDEX_LENGTH] == mLevelProgress+1) { // Sva pitanja iz nivoa su potrosena

            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            if (mLevel == 0 && mShowStatsDropdownCloseHint) {
                editor.putBoolean(SHAREDPREF_SHOW_STATS_CLOSE_HINT, false);
                removeStatsDropdownCloseHint();
            }

            if (mLevel == TOTAL_LEVELS-1) {
                // TODO Stigao do kraja igre


                // Shared preferences - azuriranje
                playGames.SaveAchievement(playGames.PREF_A_PREDJENA_IGRA, "");
                playGames.SaveScore(playGames.PREF_L_HIGHSCORE, Integer.toString(mPoints));
                playGames.SaveScore(playGames.PREF_L_AVG_ACCURACY, Float.toString(mDistanceAvgSum/ mAvgCounter));


                if (PogodiMesto.mGameHelper.isSignedIn()) {
                    //Games.Leaderboards.submitScore(PogodiMesto.mGoogleApiClient, );
                }
                else {
                    // TODO: da bude samo dugme za login umesto da se odmah otvara dialog
                    PogodiMesto.mGameHelper.beginUserInitiatedSignIn();
                }

                return;
            }


            if (mPointsInLevel >= mLevels[mLevel][INDEX_MIN]) {
                // Ima dovoljno poena da bi isao dalje



                if (mPointsInLevel == (mLevels[mLevel][INDEX_LENGTH]*1000)) { // Achievement: maksimalan broj poena u nivou
                    playGames.SaveAchievement(playGames.PREF_A_MAX_POENA_NIVO, "");
                }

                mTswPoints.setText(Integer.toString(mPoints));
                showNewLevelDialog();
                //TODO ovde treba da se vrši promena broja polja QuestionProgress-a, ako postoji potreba za time


                mPointsInLevel = 0; // resetuje se
            }
            else {
                // Nema dovoljno poena -> kraj igre

                showEndGameDialog(mPoints > playGames.mSaved_L_Highscore,
                        false,
                        mOldHighscoreAccuracy > playGames.mSaved_L_AvgAccuracy,
                        PogodiMesto.mGameHelper.isSignedIn(),
                        false);
            }

            editor.apply();
        }
        else {
            mLevelProgress++;
            mCurrentType        = mLevels[mLevel][INDEX_TYPE];
            mCurrentDifficulty  = mLevels[mLevel][INDEX_DIFFICULTY];
            mCurrentLength      = mLevels[mLevel][INDEX_LENGTH];
            mLevelProgressEx[mCurrentType][mCurrentDifficulty]++;
            mTargetPlace = mPlaces[mCurrentType][mCurrentDifficulty].get(mLevelProgressEx[mCurrentType][mCurrentDifficulty]);

            // Prikazuje traku preko sredine ekrana sa nazivom zadatog mesta
            showNewPlaceName();
        }
    }


    private void showStartGameDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().getAttributes().windowAnimations = R.style.StartGameDialogAnimation;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_startgame);

        TextView textType       = (TextView) dialog.findViewById(R.id.dlg_newlevel_text_type);
        TextView textDifficulty = (TextView) dialog.findViewById(R.id.dlg_newlevel_text_difficulty);
        Button btnCancel        = (Button) dialog.findViewById(R.id.dlg_newlevel_btn_cancel);
        Button btnStart         = (Button) dialog.findViewById(R.id.buttonStart);
        textType.setText(mLevelStrings[mLevels[mLevel + 1][INDEX_TYPE]]);
        textDifficulty.setText(mDifficultyStrings[mLevels[mLevel + 1][INDEX_DIFFICULTY]]);

        if (Build.VERSION.SDK_INT < 21) {
            // Na verzijama <21 ima okvir oko dialoga; ovo uklanja taj okvir, ali je dialog sirok koliko i ekran
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.hide();
                initializePlaces();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }


    private void showNewLevelDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().getAttributes().windowAnimations = R.style.NewLevelDialogAnimation;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_newlevel);
        dialog.setCanceledOnTouchOutside(false);

        TextView textPointsLevel    = (TextView)    dialog.findViewById(R.id.dlg_newlevel_text_points_in_level);
        TextView textPointsTotal    = (TextView)    dialog.findViewById(R.id.dlg_newlevel_text_points_total);
        TextView textNextType       = (TextView)    dialog.findViewById(R.id.dlg_newlevel_text_type);
        TextView textNextDifficulty = (TextView)    dialog.findViewById(R.id.dlg_newlevel_text_difficulty);
        Button btnCancel            = (Button)      dialog.findViewById(R.id.dlg_newlevel_btn_cancel);
        Button btnContinue          = (Button)      dialog.findViewById(R.id.dlg_newlevel_btn_continue);

        textPointsLevel.setText(Integer.toString(mPointsInLevel));
        textPointsTotal.setText(Integer.toString(mPoints));
        textNextType.setText(mLevelStrings[mLevels[mLevel + 1][INDEX_TYPE]]);
        textNextDifficulty.setText(mDifficultyStrings[mLevels[mLevel + 1][INDEX_DIFFICULTY]]);

        if (Build.VERSION.SDK_INT < 21)
            // Na verzijama <21 ima okvir oko dialoga; ovo uklanja taj okvir, ali je dialog sirok koliko i ekran
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.hide();
                mLevel++;
                mLevelProgress = -1;
                assignNextPlace();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showEndGameDialog(mPoints > playGames.mSaved_L_Highscore,
                        false,
                        mOldHighscoreAccuracy > playGames.mSaved_L_AvgAccuracy,
                        PogodiMesto.mGameHelper.isSignedIn(),
                        false);
            }
        });


        // Resetovanje progressbar-a
        ObjectAnimator anim;
        anim = ObjectAnimator.ofInt(mStatsProgressBar, "progress", mStatsProgressBar.getProgress(), 0);
        anim.setDuration(1);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
        mStatsProgressBar.getProgressDrawable().clearColorFilter();

        mQuestionProgress.reset();
    }


    /**
     * Prikazuje dialog na kraju igre sa ukupnim brojem poena i ostalim informacijama
     * @param highscore Da li je postigao najbolji skor?
     * @param highscore_time Da li je postigao najbolje prosečno vreme?
     * @param highscore_accuracy Da li je postigao najbolju prosečnu preciznost?
     * @param is_signed_in Da li je ulogovan (false -> prikaži dugme)
     * @param finished TODO Da li je presao celu igru?
     */
    private void showEndGameDialog(boolean highscore, boolean highscore_time, boolean highscore_accuracy, boolean is_signed_in, boolean finished) {

        // Iskljucivanje svih tajmera
        if (mTimerMain != null) mTimerMain.cancel();
        if (mTimerRest != null) mTimerRest.cancel();
        if (mTimerStartGame != null) mTimerStartGame.cancel();
        if (mTimerRemoveTransition != null) mTimerRemoveTransition.cancel();

        // Sredjivanje promenljivih + skor
        mOldHighscore = playGames.mSaved_L_Highscore;
        mOldHighscoreTime = playGames.mSaved_L_AvgTime;
        mOldHighscoreAccuracy = playGames.mSaved_L_AvgAccuracy;
        playGames.SaveScore(playGames.PREF_L_HIGHSCORE, Integer.toString(mPoints));
        playGames.SaveScore(playGames.PREF_L_AVG_ACCURACY, Float.toString(mDistanceAvgSum / mAvgCounter));

        final Dialog dialog = new Dialog(this);
        dialog.getWindow().getAttributes().windowAnimations = R.style.EndGameDialogAnimation;
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); // Ovo MORA da ima da bi dimAmount funkcionisao
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = 0.7f;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_endgame);

        // TextView: ukupna broj poena, prosecno vreme, prosecna preciznost
        TextView tPoints = (TextView) dialog.findViewById(R.id.dlg_endgame_text_points_total);
        TextView tBestScore = (TextView) dialog.findViewById(R.id.dlg_endgame_text_bestscore);
        TextView tAvgTime = (TextView) dialog.findViewById(R.id.dlg_endgame_avg_time);
        TextView tAvgAccuracy = (TextView) dialog.findViewById(R.id.dlg_endgame_avg_accuracy);

        // Dugmici (krune + tekst)
        LinearLayout buttonHighscore = (LinearLayout) dialog.findViewById(R.id.dlg_endgame_inc_highscore);
        LinearLayout buttonHighscoreTime = (LinearLayout) dialog.findViewById(R.id.dlg_endgame_inc_highscore_time);
        LinearLayout buttonHighscoreAccuracy = (LinearLayout) dialog.findViewById(R.id.dlg_endgame_inc_highscore_accuracy);
        LinearLayout buttonSignIn = (LinearLayout) dialog.findViewById(R.id.dlg_endgame_inc_signin);

        // Pripremanje vrednosti
        String average_time = String.format("%.2f", mTimeAvgSum / mAvgCounter);
        String average_accuracy = String.format("%.2f", mDistanceAvgSum / mAvgCounter);
        String res_average_time = getResources().getString(R.string.act_single_game_endgame_avg_time);
        String res_average_accuracy = getResources().getString(R.string.act_single_game_endgame_avg_accuracy);
        String res_best_score = getResources().getString(R.string.act_single_game_endgame_bestscore);

        // Postavljanje vrednosti
        tPoints.setText(Integer.toString(mPoints));
        tBestScore.setText(String.format(res_best_score, playGames.mSaved_L_Highscore));
        tAvgTime.setText(String.format(res_average_time, average_time));
        tAvgAccuracy.setText(String.format(res_average_accuracy, average_accuracy));

        if (!highscore)
            buttonHighscore.setVisibility(View.GONE);
        else {
            TextView t = (TextView) buttonHighscore.findViewById(R.id.inc_dlg_endgame_text_highscore);
            t.setText(String.format(getResources().getString(R.string.act_single_game_endgame_new_highscore), mOldHighscore));
        }

        if (!highscore_time)
            buttonHighscoreTime.setVisibility(View.GONE);
        else {
            TextView t = (TextView) buttonHighscoreTime.findViewById(R.id.inc_dlg_endgame_text_highscore_time);
            t.setText(String.format(getResources().getString(R.string.act_single_game_endgame_new_avg_time), mOldHighscoreTime));
        }

        if (!highscore_accuracy)
            buttonHighscoreAccuracy.setVisibility(View.GONE);
        else {
            TextView t = (TextView) buttonHighscoreAccuracy.findViewById(R.id.inc_dlg_endgame_text_highscore_accuracy);
            t.setText(String.format(getResources().getString(R.string.act_single_game_endgame_new_avg_accuracy), mOldHighscoreAccuracy));
        }
        if (is_signed_in)
            buttonSignIn.setVisibility(View.GONE);


        if (Build.VERSION.SDK_INT < 21)
            // Na verzijama <21 ima okvir oko dialoga; ovo uklanja taj okvir, ali je dialog sirok koliko i ekran
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        Button btnMainMenu = (Button) dialog.findViewById(R.id.dlg_endgame_btn_main_menu);
        Button btnNewGame  = (Button) dialog.findViewById(R.id.dlg_endgame_btn_new_game);

        btnMainMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.hide();
                recreate();
            }
        });
        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PogodiMesto.mGameHelper.isSignedIn())
                    PogodiMesto.mGameHelper.beginUserInitiatedSignIn();
            }
        });
        buttonHighscore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PogodiMesto.mGameHelper.isSignedIn()) {
                    startActivityForResult(
                            Games.Leaderboards.getLeaderboardIntent(PogodiMesto.mGameHelper.getApiClient(),
                                    getResources().getString(R.string.leaderboard_najuspesniji_igraci)),
                                    9001); // 9001 = RC_UNUSED
                }
                else {
                    PogodiMesto.mGameHelper.beginUserInitiatedSignIn();
                }
            }
        });
        /*buttonHighscoreTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PogodiMesto.mGameHelper.isSignedIn()) {
                    startActivityForResult(
                            Games.Leaderboards.getLeaderboardIntent(PogodiMesto.mGameHelper.getApiClient(),
                                    getResources().getString(R.string.leaderboard_najbolje_prosecno_vreme)),
                                    9001); // 9001 = RC_UNUSED
                }
                else {
                    PogodiMesto.mGameHelper.beginUserInitiatedSignIn();
                }
            }
        });*/
        buttonHighscoreAccuracy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PogodiMesto.mGameHelper.isSignedIn()) {
                    startActivityForResult(
                            Games.Leaderboards.getLeaderboardIntent(PogodiMesto.mGameHelper.getApiClient(),
                                    getResources().getString(R.string.leaderboard_najbolja_prosecna_preciznost)),
                                    9001); // 9001 = RC_UNUSED
                }
                else {
                    PogodiMesto.mGameHelper.beginUserInitiatedSignIn();
                }
            }
        });


        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }


    private void showStats() {
        if (Build.VERSION.SDK_INT < 21) {
            // Kad je API < 21, setFillAfter ne funkcionise iz nekog razloga, tako da cioda mora da se pomeri za
            // ...200px na dole, da bi ostala na mestu gde padne.
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTempImagePin.getLayoutParams();
            params.topMargin += PIN_FALL_DISTANCE;
            mTempImagePin.setLayoutParams(params);
        }

        mStatsTextTimeLeft.setText(mLastTime + " секунди");
        mStatsTextAccuracy.setText((Math.round(mLastAccuracy * 10.00) / 10.00) + " километара");
        //mStatsTextPoints.setText(String.valueOf(mPoints));

        ObjectAnimator slideDown = ObjectAnimator.ofFloat(mStatsDropdown, "translationY", mStatsDropdown.getHeight());
        slideDown.setDuration(300);
        slideDown.setInterpolator(new DecelerateInterpolator());

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(300);
        fadeIn.setFillAfter(true);

        AlphaAnimation partialFadeOut = new AlphaAnimation(1f, 0.3f);
        partialFadeOut.setDuration(300);
        partialFadeOut.setFillAfter(true);


        mStatsDropdown.setVisibility(View.VISIBLE);
        mStatsDropdown.setAlpha(1f);
        mDlgBackground.setVisibility(View.VISIBLE);
        mDlgBackground.startAnimation(fadeIn);
        mTempImageFingerprint.startAnimation(partialFadeOut);
        mTempImagePin.startAnimation(partialFadeOut);
        slideDown.start();

        fadeIn.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsStatsDown = true;
                ObjectAnimator anim;

                //odabir boje za question progress
                Integer toggleColor;
                if (mPointsLast == 0) toggleColor = getResources().getColor(R.color.red_500);
                else if (mPointsLast != 1000)
                    toggleColor = getResources().getColor(R.color.yellow_600);
                else toggleColor = getResources().getColor(R.color.green_600);
                mQuestionProgress.toggle(mLevelProgress, toggleColor);

                if (mPointsInLevel > 0) {
                    if (mStatsProgressBar.getProgress() < mStatsProgressBar.getSecondaryProgress()
                            && mPointsInLevel >= mStatsProgressBar.getSecondaryProgress()) {
                        /**ako će progressbar da pređe "granicu" desiće se prva animacija,
                         * onda promena boje, pa druga animacija
                         */

                        Integer poeni = mPointsInLevel - mStatsProgressBar.getProgress();
                        Integer animTime1 = 1000 * (mStatsProgressBar.getSecondaryProgress() - mStatsProgressBar.getProgress()) / poeni;
                        final Integer animTime2 = 1000 * (mPointsInLevel - mStatsProgressBar.getSecondaryProgress()) / poeni;
                        anim = ObjectAnimator.ofInt(mStatsProgressBar, "progress", mStatsProgressBar.getProgress(),
                                mStatsProgressBar.getSecondaryProgress());
                        anim.setInterpolator(new LinearInterpolator());
                        anim.setDuration(animTime1);
                        anim.start();

                        anim.addListener(new Animator.AnimatorListener() {

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mStatsProgressBar.getProgressDrawable().setColorFilter(getResources()
                                        .getColor(R.color.green_500), PorterDuff.Mode.SRC_IN);

                                ObjectAnimator anim2;
                                anim2 = ObjectAnimator.ofInt(mStatsProgressBar, "progress",
                                        mStatsProgressBar.getProgress(),
                                        mPointsInLevel);
                                anim2.setDuration(animTime2);
                                anim2.setInterpolator(new DecelerateInterpolator());
                                anim2.start();
                            }


                            @Override
                            public void onAnimationStart(Animator animation) {}

                            @Override
                            public void onAnimationCancel(Animator animation) {}

                            @Override
                            public void onAnimationRepeat(Animator animation) {}
                        });


                    } else {
                        anim = ObjectAnimator.ofInt(mStatsProgressBar, "progress",
                                mStatsProgressBar.getProgress(),
                                mPointsInLevel);
                        anim.setDuration(1000);
                        anim.setInterpolator(new DecelerateInterpolator());
                        anim.start();
                    }
                    graduallyIncreaseNumber(mStatsTextPoints, mPointsInLevel - mPointsLast, mPointsLast, MAX_LEVEL_BOUNTY, 800);
                }

                if (playGames.mSaved_Swipe_Msg) {
                    mSwipeUpCounter++;
                    closeAnim = new AlphaAnimation(1.0f, 0.0f);
                    closeAnim.setDuration(1000);
                    closeAnim.setStartOffset(1000);
                    closeAnim.setRepeatMode(Animation.REVERSE);
                    closeAnim.setRepeatCount(Animation.INFINITE);
                    mStatsTextCloseHint.startAnimation(closeAnim);
                }

                mMainLayout.setOnTouchListener(new SwipeDismissListener(mStatsDropdown, new SwipeDismissListener.DismissCallbacks() {

                    public void onDismiss(View view) {
                        closeStats();
                        mMainLayout.setOnTouchListener(null);
                    }

                }));
            }


            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }


    private void closeStats() {

        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(300);
        fadeOut.setFillAfter(true);
        mDlgBackground.startAnimation(fadeOut);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                mMainLayout.removeView(mTempImagePin);
                mMainLayout.removeView(mTempImageFingerprint);
                assignNextPlace();
                mIsStatsDown = false;
            }


            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }


    private void showNewPlaceName() {
        // ***************** Izmena teksta gde je potrebno *****************
        mNewPlaceTextTitle.setText(mTargetPlace.name);
        if (mLevel == 0 && mLevelProgress == 0)  // Ako je pocetak igre, onda da postavi bez animacije
            mTswTarget.setCurrentText(mTargetPlace.name);
        else
            mTswTarget.setText(mTargetPlace.name);
        mTswPoints.setText(Integer.toString(mPoints));
        mTextTimeContent.setText(Math.round((QUESTION_TIME / 1000) * 10.0) / 10.0 + " сек");
        mTextTimeContent.setTextColor(getResources().getColor(R.color.primary_dark));
        // *****************************************************************



        // *********************** Prikazivanje trake i pozadine ***********************
        mNewPlaceWrapper.setVisibility(View.VISIBLE);
        int startX = ((int) (mNewPlaceTextTitle.getX())) + mNewPlaceTextTitle.getWidth()/2;
        int startY = mNewPlaceTextTitle.getHeight();

        if (Build.VERSION.SDK_INT >= 21) {
            Animator animator = ViewAnimationUtils.createCircularReveal(mNewPlaceWrapper, startX, startY, 0,
                    PogodiMesto.SCREEN_WIDTH / 2);
            animator.setDuration(300);
            animator.start();
        }
        else {
            Animator animator = ViewAnimationUtils.createCircularReveal(mNewPlaceWrapper, startX, startY, 0, PogodiMesto.SCREEN_WIDTH / 2);
            animator.setDuration(300);
            animator.start();
        }

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(300);
        fadeIn.setFillAfter(true);
        mBlackShade.startAnimation(fadeIn);
        // *****************************************************************************


        /** Prvi tajmer: uklanja se crna poluprovidna pozadina i traka (onFinish) */
        mTimerRemoveTransition = new CountDownTimer(1500, 1000) {
            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setDuration(350);
                mNewPlaceWrapper.startAnimation(fadeOut);
                mBlackShade.startAnimation(fadeOut);

                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}


                    // Zavrsene fadeOut animacije
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mNewPlaceWrapper.setVisibility(View.INVISIBLE);
                        mBlackShade.setVisibility(View.INVISIBLE);
                        mCanClickMap    = true;
                        mTimeLeftMillis = QUESTION_TIME;


                        /** Drugi tajmer: meri vreme koje je preostalo za davanje odgovora, otkucava na 100ms */
                        mTimerMain = new CountDownTimer(QUESTION_TIME, 100) {

                            /**
                             * onTick:
                             * Azurira se TextView koji prikazuje preostalo vreme za odgovor
                             */
                            public void onTick(long millisUntilFinished) {

                                mTimeLeftSecs   = (double) Math.round(((double)millisUntilFinished/1000) * 10)/10;
                                mTimeLeftMillis = millisUntilFinished;

                                mTextTimeContent.setText(mTimeLeftSecs + " сек");
                                if (millisUntilFinished <= CRITICAL_TIME)
                                    mTextTimeContent.setTextColor(getResources().getColor(R.color.red_400));
                            }

                            /**
                             * onFinish:
                             * Zapocinje novi tajmer, po cijem se zavrsetku uklanjaju cioda i tacka, i zadaje novo mesto
                             */
                            public void onFinish() {
                                mCanClickMap = false;

                                Drawable res;
                                String   uri        = "@drawable/ic_cioda";
                                int imageResource   = getResources().getIdentifier(uri, null, getPackageName());
                                int pinWidth        = (int) (36 * Resources.getSystem().getDisplayMetrics().density);
                                int pinHeight       = (int) (36 * Resources.getSystem().getDisplayMetrics().density);
                                int x_fix           = (PogodiMesto.SCREEN_WIDTH - Mesto.mapWidth)/2;
                                int y_fix           = (PogodiMesto.SCREEN_HEIGHT - Mesto.mapHeight)/2;
                                int pinFinalPosX    = mTargetPlace.x /*+ x_fix*/ - pinWidth/2;
                                int pinFinalPosY    = mTargetPlace.y + y_fix - pinHeight;

                                double  distance    = 200;
                                int     time        = QUESTION_TIME;


                                // Update korisnickih varijabli
                                mPointsLast      = 0;
                                mPoints         += mPointsLast;
                                mPointsInLevel  += mPointsLast;
                                mLastAccuracy    = distance;
                                mLastTime        = 0.0;
                                mPointsTypeTotal[mCurrentType] += mPointsLast;
                                mDistanceAvgSum += distance;
                                mTimeAvgSum     += mLastTime;
                                mAvgCounter++;

                                // ************************ Iscrtavanje čiode ************************
                                if (Build.VERSION.SDK_INT >= 21)
                                    res = getResources().getDrawable(imageResource, null);
                                else
                                    res = getResources().getDrawable(imageResource);

                                mTempImagePin = new ImageView(ActivitySingleGame.this);
                                mTempImagePin.setImageDrawable(res);
                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pinWidth, pinHeight);
                                params.leftMargin = pinFinalPosX;
                                params.topMargin  = pinFinalPosY - PIN_FALL_DISTANCE;
                                mMainLayout.addView(mTempImagePin, params);
                                if (Build.VERSION.SDK_INT < 21)
                                    PogodiMesto.changeViewIndex(mTempImagePin, mMainLayout.indexOfChild(mTempImagePin) - 2);

                                TranslateAnimation pinFall = new TranslateAnimation(0, 0, 0, PIN_FALL_DISTANCE);
                                pinFall.setDuration(300);
                                pinFall.setFillAfter(true);
                                mTempImagePin.startAnimation(pinFall);
                                // *******************************************************************

                                showPoints(mPointsLast);
                                mTimerRest.start();
                                mTextTimeContent.setText("0 сек");
                            }
                        };
                        mTimerMain.start();
                    }


                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            }
        }.start();
    }




    /**
     * initializePlaces
     *
     * Inicijalizacija mesta + zadavanje prvog mesta (skroz dole)
     */
    private void initializePlaces() {
        new Mesto(mImgMap.getWidth(), mImgMap.getHeight());


        /**************************************************************
         *
         * GRADOVI
         *
         **************************************************************/
        mPlaces[CITY][EASY] = new ArrayList<>(); // Laki gradovi
        mPlaces[CITY][EASY].add(new Mesto("Београд ",       0.38444385566717987, 0.3267275254744064));
        mPlaces[CITY][EASY].add(new Mesto("Крагујевац",     0.49789405991704483, 0.5115292334931216));
        mPlaces[CITY][EASY].add(new Mesto("Лесковац",       0.7457015159815006, 0.7420496302451279));
        mPlaces[CITY][EASY].add(new Mesto("Ниш",            0.7331473177377785, 0.6699763871473432));
        mPlaces[CITY][EASY].add(new Mesto("Нови Сад",       0.23935199788538028, 0.22285308916363394));
        mPlaces[CITY][EASY].add(new Mesto("Приштина",       0.5568124602098194, 0.8185154774448192));
        mPlaces[CITY][EASY].add(new Mesto("Призрен",        0.45692133862714046, 0.9201910785850121));
        mPlaces[CITY][EASY].add(new Mesto("Ваљево",         0.2538751443085593, 0.45155326068739965));
        mPlaces[CITY][EASY].add(new Mesto("Врање",          0.7326354990936956, 0.8429512570957285));
        mPlaces[CITY][EASY].add(new Mesto("Гњилане",        0.632733276577403, 0.8637543029127175));
        mPlaces[CITY][EASY].add(new Mesto("Зрењанин",       0.3712438435361438, 0.19197383440619895));
        mPlaces[CITY][EASY].add(new Mesto("Краљево",        0.44216072831046943, 0.577522593802941));
        mPlaces[CITY][EASY].add(new Mesto("Крушевац",       0.5990955166230665, 0.6107878074209084));
        mPlaces[CITY][EASY].add(new Mesto("Нови Пазар",     0.40169501508040867, 0.7115157854591425));
        mPlaces[CITY][EASY].add(new Mesto("Панчево",        0.4340241141468457, 0.313634274005258));
        mPlaces[CITY][EASY].add(new Mesto("Смедерево",      0.501369519737172, 0.35973478743510295));
        mPlaces[CITY][EASY].add(new Mesto("Суботица",       0.19794692704773756, 0.02234215597365109));
        mPlaces[CITY][EASY].add(new Mesto("Ужице",          0.24042716420342577, 0.5479866292766338));
        mPlaces[CITY][EASY].add(new Mesto("Чачак",          0.36431931332293355, 0.5411364859085113));
        mPlaces[CITY][EASY].add(new Mesto("Шабац",          0.2044050399723271, 0.3402152545870131));
        mPlaces[CITY][EASY].add(new Mesto("Врњачка Бања",   0.4919774693781219, 0.6000193427107265)); // <20k
        mPlaces[CITY][EASY].add(new Mesto("Јагодина",       0.580289675389645, 0.5193888789070358)); // <40k
        mPlaces[CITY][EASY].add(new Mesto("Лозница",        0.09326590857026665, 0.39082359910081743)); // <20k
        mPlaces[CITY][EASY].add(new Mesto("Пирот",          0.8991352983455555, 0.7132195716928141)); // <40k
        mPlaces[CITY][EASY].add(new Mesto("Пожаревац",      0.5615249654728052, 0.36937777156918616)); // <50k
        mPlaces[CITY][EASY].add(new Mesto("Сомбор",         0.06478808140411023, 0.09998839180950023)); // <50k
        mPlaces[CITY][EASY].add(new Mesto("Зајечар",        0.8243960901525053, 0.5373622791879945)); // <40k


        mPlaces[CITY][MEDIUM] = new ArrayList<>(); // Srednje-teski gradovi (do 40k)
        mPlaces[CITY][MEDIUM].add(new Mesto("Борча",                0.38509753239376476, 0.31030317284565095)); // 40-50k
        mPlaces[CITY][MEDIUM].add(new Mesto("Бор",                  0.7815105941143556, 0.5004252967020598));
        mPlaces[CITY][MEDIUM].add(new Mesto("Вршац",                0.5911742837159861, 0.2538006084837149));
        mPlaces[CITY][MEDIUM].add(new Mesto("Кикинда",              0.16905287430560034, 0.08718277718701459));
        mPlaces[CITY][MEDIUM].add(new Mesto("Рума",                 0.23641150708427544, 0.2820648024248956));
        mPlaces[CITY][MEDIUM].add(new Mesto("Сремска Митровица",    0.1891823272311592, 0.2883908252415987));
        mPlaces[CITY][MEDIUM].add(new Mesto("Аранђеловац",          0.41057562197094266, 0.4421025292173269));
        mPlaces[CITY][MEDIUM].add(new Mesto("Бачка Паланка",        0.13361415655556802, 0.2237137042346377));
        mPlaces[CITY][MEDIUM].add(new Mesto("Бечеј",                0.28920682103264744, 0.13551242813325046));
        mPlaces[CITY][MEDIUM].add(new Mesto("Врбас",                0.19297808296767116, 0.1481608819663483));
        mPlaces[CITY][MEDIUM].add(new Mesto("Горњи Милановац",      0.38687346911528664, 0.508199865727981));
        mPlaces[CITY][MEDIUM].add(new Mesto("Инђија",               0.2965489210611774, 0.2704327915944753));
        mPlaces[CITY][MEDIUM].add(new Mesto("Лазаревац",            0.3387343451830377, 0.42567604347373134));
        mPlaces[CITY][MEDIUM].add(new Mesto("Младеновац",           0.44391721993896816, 0.41223408922253346));
        mPlaces[CITY][MEDIUM].add(new Mesto("Обреновац",            0.32601792275276664, 0.36078546699444297));
        mPlaces[CITY][MEDIUM].add(new Mesto("Параћин",              0.6159835170910068, 0.547013473822512));
        mPlaces[CITY][MEDIUM].add(new Mesto("Прокупље",             0.6636735438757938, 0.688502804488773));
        mPlaces[CITY][MEDIUM].add(new Mesto("Смедеревска Паланка",  0.5080387523099607, 0.4290118803864571));
        mPlaces[CITY][MEDIUM].add(new Mesto("Алексинац",            0.6906926078449447, 0.6207161189962858));
        mPlaces[CITY][MEDIUM].add(new Mesto("Апатин",               0.03264290882927878, 0.12253864585848988));
        mPlaces[CITY][MEDIUM].add(new Mesto("Бачка Топола",         0.19044901889404423, 0.09047116362245308));
        mPlaces[CITY][MEDIUM].add(new Mesto("Бујановац",            0.6999472752661668, 0.8638010394320713));
        mPlaces[CITY][MEDIUM].add(new Mesto("Велика Плана",         0.535366603578079, 0.6291760920920417));
        mPlaces[CITY][MEDIUM].add(new Mesto("Власотинце",           0.7877764647420193, 0.7509517057582208));
        mPlaces[CITY][MEDIUM].add(new Mesto("Ивањица",              0.3344687136333084, 0.6115456315263461));
        mPlaces[CITY][MEDIUM].add(new Mesto("Књажевац",             0.8168907565227089, 0.6128106211333291));
        mPlaces[CITY][MEDIUM].add(new Mesto("Ковин",                0.5123629867219737, 0.34195744736381073));
        mPlaces[CITY][MEDIUM].add(new Mesto("Кула",                 0.1673967591135933, 0.1385322752039992));
        mPlaces[CITY][MEDIUM].add(new Mesto("Куршумлија",           0.5867393969570552, 0.7103661396659922));
        mPlaces[CITY][MEDIUM].add(new Mesto("Куршумлијска Бања",    0.5811673951751863, 0.7293141670854032)); // <5k
        mPlaces[CITY][MEDIUM].add(new Mesto("Неготин",              0.8856655686152218, 0.46085202826490135));
        mPlaces[CITY][MEDIUM].add(new Mesto("Нови Бечеј",           0.31245420761173204, 0.14084404719148758));
        mPlaces[CITY][MEDIUM].add(new Mesto("Нишка Бања",           0.7594124558928372, 0.6752383392296998));
        mPlaces[CITY][MEDIUM].add(new Mesto("Петроварадин",         0.25015843640899493, 0.2298907607613518));
        mPlaces[CITY][MEDIUM].add(new Mesto("Пожега",               0.2870093544825088, 0.5506210699946955));
        mPlaces[CITY][MEDIUM].add(new Mesto("Прибој",               0.16209534639454792, 0.6136273908106333));
        mPlaces[CITY][MEDIUM].add(new Mesto("Пријепоље",            0.18758855717970857, 0.6538157754585621));
        mPlaces[CITY][MEDIUM].add(new Mesto("Сента",                0.2992494607824196, 0.0647972131777577));
        mPlaces[CITY][MEDIUM].add(new Mesto("Сјеница",              0.28153874681533386, 0.6804610514826779));
        mPlaces[CITY][MEDIUM].add(new Mesto("Србобран",             0.22886751631651897, 0.15350926425165864));
        mPlaces[CITY][MEDIUM].add(new Mesto("Сремска Каменица",     0.24070604037197704, 0.2343866458144096));
        mPlaces[CITY][MEDIUM].add(new Mesto("Стара Пазова",         0.3149381669579436, 0.2845091513717493));
        mPlaces[CITY][MEDIUM].add(new Mesto("Сурдулица",            0.7984338155472919, 0.8137572179337936));
        mPlaces[CITY][MEDIUM].add(new Mesto("Сурчин",               0.34111401195834096, 0.3295838092457997));
        mPlaces[CITY][MEDIUM].add(new Mesto("Темерин",              0.25306109726699755, 0.18285203034643635));
        mPlaces[CITY][MEDIUM].add(new Mesto("Трстеник",             0.518263364938839, 0.6017066403525908));
        mPlaces[CITY][MEDIUM].add(new Mesto("Тутин",                0.35597734351424776, 0.7462128854602769));
        mPlaces[CITY][MEDIUM].add(new Mesto("Ћуприја",              0.6074022966608542, 0.5308121675480435));
        mPlaces[CITY][MEDIUM].add(new Mesto("Футог",                0.2086425055237727, 0.22802674385190344));
        mPlaces[CITY][MEDIUM].add(new Mesto("Топола",               0.4407338024796419, 0.4552616002299884)); // <5k
        mPlaces[CITY][MEDIUM].add(new Mesto("Пећ",                  0.3475214896902192, 0.8173402573850166)); // <50k (kim)
        mPlaces[CITY][MEDIUM].add(new Mesto("Ђаковица",             0.3801222661382252, 0.8815219028837162)); // <50k (kim)
        mPlaces[CITY][MEDIUM].add(new Mesto("Глоговац",             0.49246886585564936, 0.8284959293595545)); // <70k (kim)
        mPlaces[CITY][MEDIUM].add(new Mesto("Косовска Митровица",   0.48447272588586066, 0.7692837300787246)); // <70k (kim)
        mPlaces[CITY][MEDIUM].add(new Mesto("Косовска Каменица",    0.6588308728153622, 0.8354734318066437)); // <70k (kim)
        mPlaces[CITY][MEDIUM].add(new Mesto("Урошевац",             0.5538602185219652, 0.8856859565388862)); // kim
        mPlaces[CITY][MEDIUM].add(new Mesto("Миријево",             0.4021884419027929, 0.32867504732069774));
        mPlaces[CITY][MEDIUM].add(new Mesto("Обилић",               0.5325973730616975, 0.8120816554599601)); // <30k
        mPlaces[CITY][MEDIUM].add(new Mesto("Косово Поље",          0.5387694333153022, 0.8238659758800828)); // <30k
        mPlaces[CITY][MEDIUM].add(new Mesto("Сремчица",             0.37147276804261775, 0.35755198986733816)); //21к
        mPlaces[CITY][MEDIUM].add(new Mesto("Ветерник",             0.22011781059560565, 0.2266753746416817));
        mPlaces[CITY][MEDIUM].add(new Mesto("Подујево",             0.5667704701276365, 0.7633119417670106));
        mPlaces[CITY][MEDIUM].add(new Mesto("Ораховац",             0.4350959856286969, 0.8780721635301137));
        mPlaces[CITY][MEDIUM].add(new Mesto("Прешево",              0.671633356965742, 0.8989396678967821));
        mPlaces[CITY][MEDIUM].add(new Mesto("Шид",                  0.0944340087204777, 0.25211160673170085));
        mPlaces[CITY][MEDIUM].add(new Mesto("Нова Пазова",          0.3302795539629484, 0.2942473830002542));
        mPlaces[CITY][MEDIUM].add(new Mesto("Златибор",             0.20391952056971163, 0.5778976425512989)); // <5k


        mPlaces[CITY][HARD] = new ArrayList<>(); // Teski gradovi (do 10k)
        mPlaces[CITY][HARD].add(new Mesto("Ада",                0.30705612134370586, 0.09199521715873492));
        mPlaces[CITY][HARD].add(new Mesto("Александровац",      0.5298308982503901, 0.6380281758940657));
        mPlaces[CITY][HARD].add(new Mesto("Ариље",              0.30196509551795725, 0.5719009909286946));
        mPlaces[CITY][HARD].add(new Mesto("Бабушница",          0.856554978563819, 0.7275130836606323));
        mPlaces[CITY][HARD].add(new Mesto("Бајина Башта",       0.17319553433447385, 0.5221203695744062));
        mPlaces[CITY][HARD].add(new Mesto("Бајмок",             0.13977823130936615, 0.051885269033132356));
        mPlaces[CITY][HARD].add(new Mesto("Банатски Карловац",  0.5255596705508165, 0.2693170083035278));
        mPlaces[CITY][HARD].add(new Mesto("Бања Ковиљача",      0.0766808624836822, 0.3947950316573649));
        mPlaces[CITY][HARD].add(new Mesto("Бач",                0.0951730248472927, 0.18936500663010705));
        mPlaces[CITY][HARD].add(new Mesto("Бачки Јарак",        0.24807543377704055, 0.199516851379913));
        mPlaces[CITY][HARD].add(new Mesto("Бачки Петровац",     0.18086504209931994, 0.20093807428512847));
        mPlaces[CITY][HARD].add(new Mesto("Бела Паланка",       0.8318066439522613, 0.6921434214128939));
        mPlaces[CITY][HARD].add(new Mesto("Бела Црква",         0.15334978198243907, 0.42319172631861945));
        mPlaces[CITY][HARD].add(new Mesto("Беочин",             0.2104349044765101, 0.23559195918478595));
        mPlaces[CITY][HARD].add(new Mesto("Блаце",              0.5904968807431311, 0.673848466069135));
        mPlaces[CITY][HARD].add(new Mesto("Велико Градиште",    0.6411787304385049, 0.337199861850271));
        mPlaces[CITY][HARD].add(new Mesto("Владичин Хан",       0.7723128261304948, 0.8060300627481641));
        mPlaces[CITY][HARD].add(new Mesto("Врањска Бања",       0.7579327248694555, 0.8429081664736597));
        mPlaces[CITY][HARD].add(new Mesto("Гроцка",             0.4502486894264216, 0.357894131161402));
        mPlaces[CITY][HARD].add(new Mesto("Димитровград",       0.9452528145859045, 0.7380743592521872));
        mPlaces[CITY][HARD].add(new Mesto("Добановци",          0.3299907960588703, 0.3216944910251242));
        mPlaces[CITY][HARD].add(new Mesto("Жабаљ",              0.29609782583208644, 0.19297156315114936));
        mPlaces[CITY][HARD].add(new Mesto("Кањижа",             0.2918727145354099, 0.032685135569327936));
        mPlaces[CITY][HARD].add(new Mesto("Качарево",           0.4445559713311121, 0.28891933348940524));
        mPlaces[CITY][HARD].add(new Mesto("Кладово",            0.906182457757532, 0.37391421202316527));
        mPlaces[CITY][HARD].add(new Mesto("Ковачица",           0.4267211236653549, 0.25578127584577093));
        mPlaces[CITY][HARD].add(new Mesto("Костолац",           0.5586622287498741, 0.34601840136196826));
        mPlaces[CITY][HARD].add(new Mesto("Лапово",             0.5408267911136341, 0.47163926226730896));
        mPlaces[CITY][HARD].add(new Mesto("Лебане",             0.693983713577115, 0.7604453010338553));
        mPlaces[CITY][HARD].add(new Mesto("Мајданпек",          0.7442721198031013, 0.417419455651379));
        mPlaces[CITY][HARD].add(new Mesto("Мол",                0.09007409267090562, 0.10158973352791534));
        mPlaces[CITY][HARD].add(new Mesto("Нова Варош",         0.23112152692963275, 0.638255206195139));
        mPlaces[CITY][HARD].add(new Mesto("Нови Кнежевац",      0.30344723758560116, 0.03814137926250127));
        mPlaces[CITY][HARD].add(new Mesto("Оџаци",              0.10217293037991607, 0.1625087608664263));
        mPlaces[CITY][HARD].add(new Mesto("Палић",              0.218766514147234, 0.021127465154465536));
        mPlaces[CITY][HARD].add(new Mesto("Петровац",           0.618018417221601, 0.6184030423902966));
        mPlaces[CITY][HARD].add(new Mesto("Рашка",              0.4260248126146104, 0.678453079351063));
        mPlaces[CITY][HARD].add(new Mesto("Свилајнац",          0.564686650880757, 0.6500138414188522));
        mPlaces[CITY][HARD].add(new Mesto("Сврљиг",             0.7867781152203009, 0.6481376076903622));
        mPlaces[CITY][HARD].add(new Mesto("Севојно",            0.2528181855543237, 0.5511700656813391));
        mPlaces[CITY][HARD].add(new Mesto("Сокобања",           0.7311306060491567, 0.5965337531767381));
        mPlaces[CITY][HARD].add(new Mesto("Сремски Карловци",   0.264854528322061, 0.2427686950288791));
        mPlaces[CITY][HARD].add(new Mesto("Старчево",           0.4482706539085035, 0.3264409853042947));
        mPlaces[CITY][HARD].add(new Mesto("Тител",              0.34880805023370254, 0.2335195443201795));
        mPlaces[CITY][HARD].add(new Mesto("Уб",                 0.2936918036971234, 0.4076706986183627));
        mPlaces[CITY][HARD].add(new Mesto("Умка",               0.35157107584202246, 0.3553052610150455));
        mPlaces[CITY][HARD].add(new Mesto("Вучитрн",            0.511561412597484, 0.7820511266310982)); // 30k (kim)
        mPlaces[CITY][HARD].add(new Mesto("Љубовија",           0.1276811122382394, 0.47096213555449845)); // <4к
        mPlaces[CITY][HARD].add(new Mesto("Угриновци",          0.3227326332998709, 0.31200746192683887));
        mPlaces[CITY][HARD].add(new Mesto("Барајево",           0.3829149399377805, 0.3751517600617514));
        mPlaces[CITY][HARD].add(new Mesto("Чуруг",              0.2931121181228647, 0.1705124280295191));
        mPlaces[CITY][HARD].add(new Mesto("Руменка",            0.2165284133815884, 0.2149872145649786));
        mPlaces[CITY][HARD].add(new Mesto("Ковиљ",              0.2819756359227027, 0.23152812141365486));
        mPlaces[CITY][HARD].add(new Mesto("Каћ",                0.263827702431367, 0.21503244208356648));
        mPlaces[CITY][HARD].add(new Mesto("Ђенерал Јанковић",   0.59059643903896, 0.9362672100270326));
        mPlaces[CITY][HARD].add(new Mesto("Штрпце",             0.5279700650937345, 0.9146789228906499));
        mPlaces[CITY][HARD].add(new Mesto("Ајвалија",           0.5611166262816524, 0.8277616212231422));
        mPlaces[CITY][HARD].add(new Mesto("Богатић",            0.15508249214855332, 0.31876710498296945));


        mPlaces[CITY][ULTRA] = new ArrayList<>(); // Mnogo teski gradovi (do 5k)
        mPlaces[CITY][ULTRA].add(new Mesto("Алексиначки Рудник",    0.6865643048076401, 0.6170348894351567));
        mPlaces[CITY][ULTRA].add(new Mesto("Алибунар",              0.5137243788097161, 0.2624988353969397));
        mPlaces[CITY][ULTRA].add(new Mesto("Баточина",              0.5354714712296069, 0.47922196695715147));
        mPlaces[CITY][ULTRA].add(new Mesto("Баљевац",               0.3423600527276526, 0.3828726991352421));
        mPlaces[CITY][ULTRA].add(new Mesto("Белановица",            0.3749478048324705, 0.45530739612238025));
        mPlaces[CITY][ULTRA].add(new Mesto("Бели Поток",            0.39963191355886996, 0.351018605101878));
        mPlaces[CITY][ULTRA].add(new Mesto("Бело Поље",             0.3235204679556485, 0.3644028386187778));
        mPlaces[CITY][ULTRA].add(new Mesto("Боговина",              0.7446225661568534, 0.5404854675587618));
        mPlaces[CITY][ULTRA].add(new Mesto("Бољевац",               0.7513501649355262, 0.5534349984542228));
        mPlaces[CITY][ULTRA].add(new Mesto("Босилеград",            0.8740564938420594, 0.8565296913788615));
        mPlaces[CITY][ULTRA].add(new Mesto("Брза Паланка",          0.8668552228249125, 0.4061291096233958));
        mPlaces[CITY][ULTRA].add(new Mesto("Брус",                  0.5259698392462846, 0.6552729308141292));
        mPlaces[CITY][ULTRA].add(new Mesto("Велики Црљени",         0.3453164398449169, 0.4044746275536485));
        mPlaces[CITY][ULTRA].add(new Mesto("Вучје",                 0.7376061882919626, 0.7720061809148971));
        mPlaces[CITY][ULTRA].add(new Mesto("Грделица",              0.7744545675490321, 0.7681755688841487));
        mPlaces[CITY][ULTRA].add(new Mesto("Гуча",                  0.33343332249601226, 0.5662711606997288));
        mPlaces[CITY][ULTRA].add(new Mesto("Деспотовац",            0.6229586408229972, 0.492248239187021));
        mPlaces[CITY][ULTRA].add(new Mesto("Дивчибаре",             0.2765163384936296, 0.4871664278389661));
        mPlaces[CITY][ULTRA].add(new Mesto("Доњи Милановац",        0.7953885323569625, 0.4045171649128317));
        mPlaces[CITY][ULTRA].add(new Mesto("Житиште",               0.4091001772348599, 0.1675007054338915));
        mPlaces[CITY][ULTRA].add(new Mesto("Ириг",                  0.24336743631174373, 0.26098445157852374));
        mPlaces[CITY][ULTRA].add(new Mesto("Јаша Томић",            0.48149017705512365, 0.17692892109438593));
        mPlaces[CITY][ULTRA].add(new Mesto("Јошаничка Бања",        0.45651017388108095, 0.653980212152096));
        mPlaces[CITY][ULTRA].add(new Mesto("Косјерић",              0.2573835439375894, 0.5141846399587522));
        mPlaces[CITY][ULTRA].add(new Mesto("Крупањ",                0.12579893599309927, 0.42993708330582275));
        mPlaces[CITY][ULTRA].add(new Mesto("Кучево",                0.6792823469103002, 0.4027547439476611));
        mPlaces[CITY][ULTRA].add(new Mesto("Лајковац",              0.3182615993751852, 0.42790507993816684));
        mPlaces[CITY][ULTRA].add(new Mesto("Лучани",                0.3117365988437111, 0.5477731678368388));
        mPlaces[CITY][ULTRA].add(new Mesto("Љиг",                   0.33592276262625004, 0.460667085287338));
        mPlaces[CITY][ULTRA].add(new Mesto("Мали Зворник",          0.0653140950417275, 0.42349840662401267));
        mPlaces[CITY][ULTRA].add(new Mesto("Матарушка Бања",        0.4233837486001375, 0.5855437149711359));
        mPlaces[CITY][ULTRA].add(new Mesto("Мачванска Митровица",   0.18192743240191364, 0.2902236600591024));
        mPlaces[CITY][ULTRA].add(new Mesto("Медвеђа",               0.6569583115355184, 0.7785623454121957));
        mPlaces[CITY][ULTRA].add(new Mesto("Мионица",               0.298310857969674, 0.4546778000184369));
        mPlaces[CITY][ULTRA].add(new Mesto("Овча",                  0.40487849274512283, 0.30778047314369833));
        mPlaces[CITY][ULTRA].add(new Mesto("Опово",                 0.38260183259595887, 0.2701334186159286));
        mPlaces[CITY][ULTRA].add(new Mesto("Остружница",            0.354575120268196, 0.3466016607406676));
        mPlaces[CITY][ULTRA].add(new Mesto("Пиносава",              0.39325227924868184, 0.3535314294373871));
        mPlaces[CITY][ULTRA].add(new Mesto("Рача",                  0.5126237787889668, 0.4608786713195352));
        mPlaces[CITY][ULTRA].add(new Mesto("Ресавица",              0.6579079391954531, 0.5055444035220823));
        mPlaces[CITY][ULTRA].add(new Mesto("Рибница",               0.4427534339402591, 0.5888291415049128));
        mPlaces[CITY][ULTRA].add(new Mesto("Рудовци",               0.3753207116492894, 0.4266157762434163));
        mPlaces[CITY][ULTRA].add(new Mesto("Руцка",                 0.3501789982841246, 0.3603896847136604));
        mPlaces[CITY][ULTRA].add(new Mesto("Сијаринска Бања",       0.6634004903901612, 0.7946922627456282));
        mPlaces[CITY][ULTRA].add(new Mesto("Сопот",                 0.414021442086055, 0.39186363559695353));
        mPlaces[CITY][ULTRA].add(new Mesto("Ћићевац",               0.6249980407996255, 0.5787771543118357));
        mPlaces[CITY][ULTRA].add(new Mesto("Уљма",                  0.5580200470538447, 0.27038132799669057));
        mPlaces[CITY][ULTRA].add(new Mesto("Црвенка",               0.14920903555164441, 0.12725049890441092));
        mPlaces[CITY][ULTRA].add(new Mesto("Чајетина",              0.20820323182197448, 0.5735863397151578));
        mPlaces[CITY][ULTRA].add(new Mesto("Чока",                  0.3116376983413051, 0.06136894652220168));
        mPlaces[CITY][ULTRA].add(new Mesto("Челарево",              0.16550247406813318, 0.22121006469822233));
        mPlaces[CITY][ULTRA].add(new Mesto("Липљан",                0.546841431617431, 0.8502636187545908)); // 15k (kim)



        /**************************************************************
         *
         * SELA
         *
         **************************************************************/
        mPlaces[VILLAGE][EASY] = new ArrayList<>(); // Laka sela (do 5, 6k)
       /* mPlaces[VILLAGE][EASY].add(new Mesto("Велики Црљени", 0.3457627240665713, 0.4036909466879836));
        mPlaces[VILLAGE][EASY].add(new Mesto("Братмиловце", 0.7548764708341433, 0.7406046525193711));
        mPlaces[VILLAGE][EASY].add(new Mesto("Шајкаш", 0.2987080414342963, 0.22205119281112237));
        mPlaces[VILLAGE][EASY].add(new Mesto("Госпођинци", 0.275296393897889, 0.18574125966806712));
        mPlaces[VILLAGE][EASY].add(new Mesto("Кисач", 0.21362776262179173, 0.2016267981083271));
        mPlaces[VILLAGE][EASY].add(new Mesto("Дебељача", 0.4231944222888363, 0.26427506607634754));
        mPlaces[VILLAGE][EASY].add(new Mesto("Лешница", 0.11300145148479582, 0.36202859578613056));
        mPlaces[VILLAGE][EASY].add(new Mesto("Коцељева", 0.2389067249160107, 0.40561195642161313));
        mPlaces[VILLAGE][EASY].add(new Mesto("Бадовинци", 0.12859383365005703, 0.33383512997171366));
        mPlaces[VILLAGE][EASY].add(new Mesto("Брзи Брод", 0.7498791180771953, 0.6710720549049961));*/


        mPlaces[VILLAGE][EASY].add(new Mesto("Поцерски Причиновић", 0.20811241550217796, 0.3466039527861267)); //<6k
        mPlaces[VILLAGE][EASY].add(new Mesto("Мокрин", 0.15449140294322516, 0.06470378759965482)); //<6k
        mPlaces[VILLAGE][EASY].add(new Mesto("Хоргош", 0.26525795885843145, 0.013873655809658143)); //<6k
        mPlaces[VILLAGE][EASY].add(new Mesto("Бешка", 0.2948332752803899, 0.25454367949332996)); //<6k
        mPlaces[VILLAGE][EASY].add(new Mesto("Бачко Градиште", 0.2851270528039084, 0.15633862162159542)); //<5k
        mPlaces[VILLAGE][EASY].add(new Mesto("Српска Црња", 0.44183941707687846, 0.11169175244742169)); //<5k
        mPlaces[VILLAGE][EASY].add(new Mesto("Мали Иђош", 0.19724843088455046, 0.11318029489041025)); //<5k
        mPlaces[VILLAGE][EASY].add(new Mesto("Ечка", 0.38836134723290183, 0.20676624302770763)); //<4k
        mPlaces[VILLAGE][EASY].add(new Mesto("Војка", 0.31193627089299714, 0.2966602296238006)); //<4k
        mPlaces[VILLAGE][EASY].add(new Mesto("Шимановци", 0.2984640791899439, 0.31020867222768367)); // <3k
        mPlaces[VILLAGE][EASY].add(new Mesto("Житорађа", 0.6853752379250089, 0.701488577737435)); // <3k
        mPlaces[VILLAGE][EASY].add(new Mesto("Пландиште", 0.5494680261812143, 0.22780015865662565)); //<3k
        mPlaces[VILLAGE][EASY].add(new Mesto("Нови Сланкамен", 0.3329457005043016, 0.25525155258328275)); //<3k
        mPlaces[VILLAGE][EASY].add(new Mesto("Голубац", 0.6692624715015492, 0.36234360289177264)); //<2k
        mPlaces[VILLAGE][EASY].add(new Mesto("Турија", 0.24224690486628617, 0.15742349851978907)); //<2k
        mPlaces[VILLAGE][EASY].add(new Mesto("Баранда", 0.3855649373805294, 0.26115239636978427)); //<2k
        mPlaces[VILLAGE][EASY].add(new Mesto("Варварин", 0.6049518940004687, 0.5780636798328231)); //<2k
        mPlaces[VILLAGE][EASY].add(new Mesto("Кнић", 0.4502763292679243, 0.5307648290937956)); //<2k
        mPlaces[VILLAGE][EASY].add(new Mesto("Орашац", 0.4181066266057748, 0.4340449148357393));//<1.5k
        mPlaces[VILLAGE][EASY].add(new Mesto("Бачки Брег", 0.02174784284039681, 0.06509986322395678));//<1.5k
        mPlaces[VILLAGE][EASY].add(new Mesto("Ражањ", 0.650961148156946, 0.5900356375771514));//<1k
        mPlaces[VILLAGE][EASY].add(new Mesto("Банатски Двор", 0.3998696978350115, 0.15805829912077807));//<1k
        mPlaces[VILLAGE][EASY].add(new Mesto("Јазак", 0.22089336899856352, 0.260226807655899));//<1k
        mPlaces[VILLAGE][EASY].add(new Mesto("Мачкат", 0.22424086606402155, 0.5618448803828465));//<1k
        mPlaces[VILLAGE][EASY].add(new Mesto("Црна Трава", 0.8294022005660607, 0.7858179805656064));//<500
        mPlaces[VILLAGE][EASY].add(new Mesto("Таково", 0.3718878424971764, 0.5031703927571007));//<500
        mPlaces[VILLAGE][EASY].add(new Mesto("Стари Сланкамен", 0.33881408535967417, 0.2506361465510559));//<500
        mPlaces[VILLAGE][EASY].add(new Mesto("Мокра Гора", 0.159108729768837, 0.5602084339015483));//<500
        mPlaces[VILLAGE][EASY].add(new Mesto("Грачаница", 0.56411383226223, 0.8322888543406901));
        mPlaces[VILLAGE][EASY].add(new Mesto("Тршић", 0.10396489877392112, 0.3995579665357764));
        mPlaces[VILLAGE][EASY].add(new Mesto("Сталаћ", 0.620562764175726, 0.5873370846017074));
        mPlaces[VILLAGE][EASY].add(new Mesto("Сирогојно", 0.24924198560414945, 0.5861334370348584));


        mPlaces[VILLAGE][MEDIUM] = new ArrayList<>(); // Laka sela (do 5, 6k)
        mPlaces[VILLAGE][HARD] = new ArrayList<>(); // Laka sela (do 5, 6k)



        /**************************************************************
         *
         * MANASTIRI
         *
         **************************************************************/
        mPlaces[MONASTERY][EASY] = new ArrayList<>(); // Laki manastiri
        mPlaces[MONASTERY][EASY].add(new Mesto("Жича",              0.4314973524894096, 0.5840837582231663));
        mPlaces[MONASTERY][EASY].add(new Mesto("Студеница",         0.4435502036280077, 0.5778791235767655));
        mPlaces[MONASTERY][EASY].add(new Mesto("Грачаница",         0.5641976183500842, 0.8328145516312893));
        mPlaces[MONASTERY][EASY].add(new Mesto("Високи Дечани",     0.339436086824378, 0.8435064515451353));
        mPlaces[MONASTERY][EASY].add(new Mesto("Сопоћани",          0.36602755888594785, 0.7167957248160933));
        mPlaces[MONASTERY][EASY].add(new Mesto("Пећка патријаршија",0.3394084001972972, 0.8171505922589003));
        mPlaces[MONASTERY][EASY].add(new Mesto("Ново Хопово",       0.24020241608658705, 0.25479649717292535));
        mPlaces[MONASTERY][EASY].add(new Mesto("Старо Хопово",      0.24312775787119006, 0.2531523316462653));
        mPlaces[MONASTERY][EASY].add(new Mesto("Крушедол",          0.2624707248597137, 0.2570562341234532));
        mPlaces[MONASTERY][EASY].add(new Mesto("Манасија",          0.6305048957966802, 0.49088639295987896));
        mPlaces[MONASTERY][EASY].add(new Mesto("Ђурђеви Ступови",   0.39676573640872753, 0.7058430513272229));
        mPlaces[MONASTERY][EASY].add(new Mesto("Гргетег",           0.2535272181060224, 0.25267920615938266));
        mPlaces[MONASTERY][EASY].add(new Mesto("Милешева",          0.20420228842746532, 0.6587455127449768));
        mPlaces[MONASTERY][EASY].add(new Mesto("Раковица",          0.3828799092111194, 0.34406746564918844));
        mPlaces[MONASTERY][EASY].add(new Mesto("Раваница",          0.6374072778330399, 0.5204898992792739));


        mPlaces[MONASTERY][MEDIUM] = new ArrayList<>(); // Srednje-teski manastiri
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Девич",          0.4680918951502793, 0.8112822735451611));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Љубостиња",      0.5172523945591727, 0.5936152298114709));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Ћелије",         0.24691503742186377, 0.45809959374472875));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Троноша",        0.10726856394474336, 0.4079491442402369));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Бањска",         0.46525741996799697, 0.74821807970068));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Горњак",         0.6486919623559169, 0.6439789136999977));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Витовница",      0.6482206240061499, 0.6181343535860602));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Радовашница",    0.15529179094584034, 0.3694897878262608));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Градац",         0.4072782730647629, 0.6598113750105314));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Поганово",       0.9170262226305673, 0.7492238969263267));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Војловица",      0.442325230635046, 0.3217227177871317));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Свети Арханђели",0.46250610954193627, 0.9237195304182136));
        mPlaces[MONASTERY][MEDIUM].add(new Mesto("Богородица Љевишка",0.45559970311060205, 0.9212191955167377));


        mPlaces[MONASTERY][HARD] = new ArrayList<>(); // Teski manastiri
        mPlaces[MONASTERY][HARD].add(new Mesto("Месић",            0.6152496668671333, 0.2565088239981538));
        mPlaces[MONASTERY][HARD].add(new Mesto("Свете Меланије",   0.37698856145722237, 0.18832569562128498));
        mPlaces[MONASTERY][HARD].add(new Mesto("Хајдучица",        0.5083354906464831, 0.2229606821881161));
        mPlaces[MONASTERY][HARD].add(new Mesto("Средиште",         0.6162071400263859, 0.24741207625714862));
        mPlaces[MONASTERY][HARD].add(new Mesto("Баваниште",        0.488207478774641, 0.32412143599776216));
        mPlaces[MONASTERY][HARD].add(new Mesto("Бођани",           0.06366670228071884, 0.18783880609000211));
        mPlaces[MONASTERY][HARD].add(new Mesto("Ковиљ",             0.2855346837520621, 0.23541625649904213));


        /**************************************************************
         *
         * ZNAMENITOSTI
         *
         **************************************************************/
        mPlaces[LANDMARK][EASY] = new ArrayList<>(); // Lake znamenitosti
        mPlaces[LANDMARK][EASY].add(new Mesto("Гамзиград",          0.8012938843785936, 0.5368079308867657));
        mPlaces[LANDMARK][EASY].add(new Mesto("Дрвенград",          0.15807464181202352, 0.5624171135478343));
        mPlaces[LANDMARK][EASY].add(new Mesto("Калемегдан",         0.38402692229609176, 0.32266933076981424));
        mPlaces[LANDMARK][EASY].add(new Mesto("Царичин град",       0.677562306047091, 0.7528337793608377));
        mPlaces[LANDMARK][EASY].add(new Mesto("Сирмијум",           0.18602844217405243, 0.2871957191827568));
        mPlaces[LANDMARK][EASY].add(new Mesto("Виминацијум",        0.5729469246172684, 0.3436467159296505));
        mPlaces[LANDMARK][EASY].add(new Mesto("Авалски торањ",      0.40001150780666006, 0.3530417896822493));
        mPlaces[LANDMARK][EASY].add(new Mesto("Ћеле Kула",          0.7391223641892816, 0.6719299254984017));
        mPlaces[LANDMARK][EASY].add(new Mesto("Винча",              0.420575357886679, 0.335277933089493));
        mPlaces[LANDMARK][EASY].add(new Mesto("Лудошко језеро",     0.23496748771192139, 0.022783933396043683));
        mPlaces[LANDMARK][EASY].add(new Mesto("Феликс Ромулијана",  0.8034092032047798, 0.5386925283918284));


        mPlaces[LANDMARK][MEDIUM] = new ArrayList<>(); // Srednje-teske znamenitosti
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Улпијана",                 0.5593992989374257, 0.8332190474707013));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Медијана",                 0.7451570503587917, 0.6723702299144549));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Музеј воштаних фигура",    0.5812216405416526, 0.5217770786076086));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Чегар",                    0.7435239327229428, 0.6595224819035997));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Бели Двор",                0.3844950834919314, 0.3359516363973435));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Музеј Војводине",          0.242023621234932, 0.2252129075589649));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Трајанова табла",          0.8323327221300234, 0.364270066081266));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Кадињача",                 0.2159265299159302, 0.536597283343143));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Опленац",                  0.4407338024796419, 0.4552616002299884));
        mPlaces[LANDMARK][MEDIUM].add(new Mesto("Дворац \"Каштел\"",        0.38339152202538673, 0.20680798512284526));


        mPlaces[LANDMARK][HARD] = new ArrayList<>(); // Teske znamenitosti
        mPlaces[LANDMARK][HARD].add(new Mesto("Бојчинска шума",         0.3092801009501924, 0.34116054129568474));
        mPlaces[LANDMARK][HARD].add(new Mesto("Крагујевачки Октобар",   0.4905410969365766, 0.5092498702891147));
        mPlaces[LANDMARK][HARD].add(new Mesto("Јајинци",                0.3929392324240778, 0.344089975675641));
        mPlaces[LANDMARK][HARD].add(new Mesto("Чачалица",               0.5659910151018861, 0.3711475990591904));
        mPlaces[LANDMARK][HARD].add(new Mesto("Старчево",               0.4519983027062715, 0.32297400555255035));
        mPlaces[LANDMARK][HARD].add(new Mesto("Трајанов мост",          0.9193603239724822, 0.37223533748797305));
        mPlaces[LANDMARK][HARD].add(new Mesto("Лудошко језеро",         0.23496748771192139, 0.022783933396043683));
        mPlaces[LANDMARK][HARD].add(new Mesto("Царска Бара",            0.37446589810951497, 0.2197955235099607));



        Collections.shuffle(mPlaces[CITY][EASY]);
        Collections.shuffle(mPlaces[CITY][MEDIUM]);
        Collections.shuffle(mPlaces[CITY][HARD]);
        Collections.shuffle(mPlaces[CITY][ULTRA]);
        Collections.shuffle(mPlaces[VILLAGE][EASY]);
        Collections.shuffle(mPlaces[VILLAGE][MEDIUM]);
        Collections.shuffle(mPlaces[VILLAGE][HARD]);
        Collections.shuffle(mPlaces[MONASTERY][EASY]);
        Collections.shuffle(mPlaces[MONASTERY][MEDIUM]);
        Collections.shuffle(mPlaces[MONASTERY][HARD]);
        Collections.shuffle(mPlaces[LANDMARK][EASY]);
        Collections.shuffle(mPlaces[LANDMARK][MEDIUM]);
        Collections.shuffle(mPlaces[LANDMARK][HARD]);


        assignNextPlace();
    }


    /**
     * Inicijalizacija nivoa
     */
    private void initializeLevels() {

        mLevelStrings[CITY]         = getResources().getString(R.string.act_single_game_type_cities);
        mLevelStrings[VILLAGE]      = getResources().getString(R.string.act_single_game_type_villages);
        mLevelStrings[MONASTERY]    = getResources().getString(R.string.act_single_game_type_monasteries);
        mLevelStrings[LANDMARK]     = getResources().getString(R.string.act_single_game_type_landmarks);
        mDifficultyStrings[EASY]    = getResources().getString(R.string.act_single_game_diff_easy);
        mDifficultyStrings[MEDIUM]  = getResources().getString(R.string.act_single_game_diff_medium);
        mDifficultyStrings[HARD]    = getResources().getString(R.string.act_single_game_diff_hard);
        mDifficultyStrings[ULTRA]   = getResources().getString(R.string.act_single_game_diff_ultra);


        mLevels[0][INDEX_TYPE] = CITY;
        mLevels[0][INDEX_DIFFICULTY] = EASY;
        mLevels[0][INDEX_LENGTH] = 7;
        mLevels[0][INDEX_MIN] = 4000; // >50%

        mLevels[1][INDEX_TYPE] = CITY;
        mLevels[1][INDEX_DIFFICULTY] = EASY;
        mLevels[1][INDEX_LENGTH] = 7;
        mLevels[1][INDEX_MIN] = 4500; // >50%

        mLevels[2][INDEX_TYPE] = LANDMARK;
        mLevels[2][INDEX_DIFFICULTY] = EASY;
        mLevels[2][INDEX_LENGTH] = 6;
        mLevels[2][INDEX_MIN] = 3500; // >50%

        mLevels[3][INDEX_TYPE] = MONASTERY;
        mLevels[3][INDEX_DIFFICULTY] = EASY;
        mLevels[3][INDEX_LENGTH] = 7;
        mLevels[3][INDEX_MIN] = 3500; // 50%

        mLevels[4][INDEX_TYPE] = CITY;
        mLevels[4][INDEX_DIFFICULTY] = MEDIUM;
        mLevels[4][INDEX_LENGTH] = 7;
        mLevels[4][INDEX_MIN] = 4000; // >50%

        mLevels[5][INDEX_TYPE] = CITY;
        mLevels[5][INDEX_DIFFICULTY] = MEDIUM;
        mLevels[5][INDEX_LENGTH] = 7;
        mLevels[5][INDEX_MIN] = 4500; // >50%

        mLevels[6][INDEX_TYPE] = VILLAGE;
        mLevels[6][INDEX_DIFFICULTY] = EASY;
        mLevels[6][INDEX_LENGTH] = 7;
        mLevels[6][INDEX_MIN] = 3500; // 50%

        mLevels[7][INDEX_TYPE] = MONASTERY;
        mLevels[7][INDEX_DIFFICULTY] = MEDIUM;
        mLevels[7][INDEX_LENGTH] = 7;
        mLevels[7][INDEX_MIN] = 3500; // 50%

        mLevels[8][INDEX_TYPE] = LANDMARK;
        mLevels[8][INDEX_DIFFICULTY] = MEDIUM;
        mLevels[8][INDEX_LENGTH] = 7;
        mLevels[8][INDEX_MIN] = 4000; // >50%

        mLevels[9][INDEX_TYPE] = CITY;
        mLevels[9][INDEX_DIFFICULTY] = HARD;
        mLevels[9][INDEX_LENGTH] = 7;
        mLevels[9][INDEX_MIN] = 4000; // >50%

        mLevels[10][INDEX_TYPE] = CITY;
        mLevels[10][INDEX_DIFFICULTY] = HARD;
        mLevels[10][INDEX_LENGTH] = 7;
        mLevels[10][INDEX_MIN] = 4000; // >50%

        mLevels[11][INDEX_TYPE] = MONASTERY;
        mLevels[11][INDEX_DIFFICULTY] = HARD;
        mLevels[11][INDEX_LENGTH] = 5;
        mLevels[11][INDEX_MIN] = 2500; // 50%

        mLevels[12][INDEX_TYPE] = LANDMARK;
        mLevels[12][INDEX_DIFFICULTY] = HARD;
        mLevels[12][INDEX_LENGTH] = 6;
        mLevels[12][INDEX_MIN] = 3000; // 50%

        mLevels[13][INDEX_TYPE] = VILLAGE;
        mLevels[13][INDEX_DIFFICULTY] = HARD;
        mLevels[13][INDEX_LENGTH] = 7;
        mLevels[13][INDEX_MIN] = 3500; // 50%

        mLevels[14][INDEX_TYPE] = CITY;
        mLevels[14][INDEX_DIFFICULTY] = ULTRA;
        mLevels[14][INDEX_LENGTH] = 7;
        mLevels[14][INDEX_MIN] = 3500; // 50%

        mLevels[15][INDEX_TYPE] = CITY;
        mLevels[15][INDEX_DIFFICULTY] = ULTRA;
        mLevels[15][INDEX_LENGTH] = 7;
        mLevels[15][INDEX_MIN] = 3500; // 50%



        mLevelProgressEx[CITY][EASY]        = -1;
        mLevelProgressEx[CITY][MEDIUM]      = -1;
        mLevelProgressEx[CITY][HARD]        = -1;
        mLevelProgressEx[CITY][ULTRA]       = -1;

        mLevelProgressEx[VILLAGE][EASY]     = -1;
        mLevelProgressEx[VILLAGE][MEDIUM]   = -1;
        mLevelProgressEx[VILLAGE][HARD]     = -1;

        mLevelProgressEx[MONASTERY][EASY]   = -1;
        mLevelProgressEx[MONASTERY][MEDIUM] = -1;
        mLevelProgressEx[MONASTERY][HARD]   = -1;

        mLevelProgressEx[LANDMARK][EASY]    = -1;
        mLevelProgressEx[LANDMARK][MEDIUM]  = -1;
        mLevelProgressEx[LANDMARK][HARD]    = -1;
    }

    /** graduallyIncreaseNumber
     *
     * @param v             TextView koji se menja
     * @param start         Početna vrednost (od koje se kreće dodavanje)
     * @param to_add        Vrednost koja se dodaje na početnu (već postojeću) vrednost
     * @param max_number    Maksimalni broj do koga može da se vrši povećavanje
     * @param max_time      Maksimalno vreme (u milisekundama) koliko može da traje povećavanje
     *                      Vreme povećavanja će biti srazmerno broju koji se dodaje (42% od max_number = 42% od max_time)
     */
    public void graduallyIncreaseNumber(final TextView v, final int start, final int to_add, int max_number, int max_time) {
        v.setText(Integer.toString(start));
        if (to_add == 0) return;

        // Vreme za koje će se izvršiti povećavanje
        final int time = Math.round((max_time * to_add) / max_number);

        // Broj otkucaja
        // Ako rezultat zaokruzanja bude 0, prebaciti da bude 1 kako bi se izbeglo deljenje nulom
        final int ticks = Math.round((to_add*100f)/max_number)==0 ? 1 : Math.round((to_add*100f)/max_number);

        // Vremenski interval između 2 otkucaja
        final int interval = Math.round(time/ticks);

        // Prosečni broj koji će da se doda
        // Ako je min = 20, max = 80 ---> avg = 55 [(max-min)/2 + min = avg  --->  (80-20)/2 + 20 = 55]
        /**
         * Dobijanje min i max vrednosti:
         *
         * min + (max-min)/2 = avg    / *2
         * min + max = 2avg
         *
         * Zbir min+max može da bude jednak 2*avg
         * Optimalno je za min uzeti 1/4 od 2avg, a za max 3/4 od 2avg
         */
        final int add_avg = Math.round(to_add/ticks); // 100

        // Najmanji broj koji se može dodati u jednom otkucaju (1/4 od 2avg)
        final int add_min = Math.round(add_avg/2);

        // Najveći broj koji se može dodati u jednom otkucaju (3/4 od 2avg)
        final int add_max = add_min * 3;


        // DEBUG
        /*Log.d("DEBUG", "Time:       " + time);
        Log.d("DEBUG", "Ticks:      " + ticks);
        Log.d("DEBUG", "Interval:   " + interval);
        Log.d("DEBUG", "Avg:        " + add_avg);
        Log.d("DEBUG", "Min:        " + add_min);
        Log.d("DEBUG", "Max:        " + add_max);*/


        new Timer().scheduleAtFixedRate(new TimerTask() {
            // Beleži broj otkucaja da bismo znali kad da prekinemo sa dodavanjem
            int howmanyticks = 0;

            // Pamti kolika je vrednost dodata na broj nakon svakog otkucaja
            int total_added = 0;

            @Override
            public void run() {
                int points_add = PogodiMesto.randInt(add_min, add_max);
                total_added += points_add;


                howmanyticks++;
                if (howmanyticks >= ticks) {
                    this.cancel();
                    total_added = to_add;
                }

                // View mora da se menja na ovaj način, u suprotnom se dobija crash:
                // Only the original thread that created a view hierarchy can touch its views.
                // ...zbog toga se view manja u UI thread-u
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        v.setText(Integer.toString(start + total_added));
                    }
                });
            }
        }, 0, interval);
    }

    // Uklanjanje teksta, izmena visine layouta i pomeranje iznad ekrana
    private void removeStatsDropdownCloseHint() {
        mStatsDropdown.removeView(mStatsTextCloseHint);

        // Mora setMargins da bi se statsdropdown uklonio sa ekrana
        // Ako se nekad ova linija ukloni, mora da se izmeni deo u onCreate koji se oslanja na ovaj metod.
        PogodiMesto.setMargins(mStatsDropdown, 0, -mStatsDropdown.getHeight() + mStatsTextCloseHint.getHeight(), 0, 0);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mStatsDropdown.getLayoutParams();
        params.height = mStatsDropdown.getHeight() - mStatsTextCloseHint.getHeight();
        mStatsDropdown.setLayoutParams(params);
    }
}