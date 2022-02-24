package com.adastra.pogodimesto2;


        import android.app.Activity;
        import android.app.ActivityOptions;
        import android.app.Dialog;
        import android.content.Intent;
        import android.content.pm.PackageInfo;
        import android.content.pm.PackageManager;
        import android.os.Build;
        import android.os.Bundle;
        import android.support.v4.view.ViewPager;
        import android.util.Log;
        import android.util.Pair;
        import android.view.View;
        import android.view.Window;
        import android.view.WindowManager;
        import android.widget.Button;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.RelativeLayout;
        import android.widget.TextView;
        import android.widget.Toast;


        import com.adastra.pogodimesto2.gameplay.ActivitySingleGame;
        import com.google.android.gms.games.Games;
        import com.google.example.games.basegameutils.GameHelper;



@SuppressWarnings("unchecked")
public class ActivityMainMenu extends Activity implements
        GameHelper.GameHelperListener {


    /**
     * ***********************
     * Lista bagova     *
     * ***********************
     *
     * (sredjeno) Kad se u dialogu klikne na praznu belu povrsinu (da nije switch ili "o nama"), on se zatvara
     * (sredjeno) Klik na "o nama" stvara crash
     * (sredjeno) SingleGameActivity: klik na back dugme nakon nekog vremena stvara crash ako se ostane u glavnom meniju (tajmer NE staje pritom)
     * Cioda (slajder) se ne resetuje kad se predje na sledeci nivo
     *
     *
     * Da li da se greske prilikom prijave na google play ispisuju u dialogu ili toastu?
     * Da li ce aplikacija odmah da pokusa da prijavi korisnika, ili ce to da se desi tek kad hoce da otvori rang liste?
     *
     *
     *
     * ***********************
     * To-Do list       *
     * ***********************
     *
     * Napraviti da se prikaze nesto kad igrac mnogo omasi pa ne dobije poene (trenutno nema nista)
     * Achievement za predjen 1. nivo
     * Achievement Hunter - za osvojene sve achievemente
     *
     */


    // Views
    RelativeLayout mBtnNewGame;
    RelativeLayout mBtnLeaderboards;
    RelativeLayout mBtnSettings;
    RelativeLayout mBtnAchievements;
    ImageView mImgMap;
    ImageView mImgPin;


    long mBackPressed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Uklanja status bar na starijim verzijama
        if (Build.VERSION.SDK_INT < 21) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        PogodiMesto.mGameHelper = new GameHelper(this, GameHelper.CLIENT_GAMES);
        PogodiMesto.mGameHelper.setup(this);
        PogodiMesto.mGameHelper.setMaxAutoSignInAttempts(1);
        PogodiMesto.mGameHelper.setConnectOnStart(true);


        setContentView(R.layout.activity_main);

        //vraća normalan prikaz ako je dimmovan navbar
        View decorView = ActivityMainMenu.this.getWindow().getDecorView();
        decorView.setSystemUiVisibility(0);

        mBtnNewGame         = (RelativeLayout)  findViewById(R.id.act_main_menu_btn_NewGame);
        mBtnLeaderboards    = (RelativeLayout)  findViewById(R.id.act_main_menu_btn_Leaderboards);
        mBtnSettings        = (RelativeLayout)  findViewById(R.id.act_main_menu_btn_Settings);
        mBtnAchievements    = (RelativeLayout)  findViewById(R.id.act_main_menu_btn_Achievements);
        mImgMap             = (ImageView)       findViewById(R.id.act_main_menu_img_map);
        mImgPin             = (ImageView)       findViewById(R.id.act_main_menu_img_pin);
    }


    @Override
    protected void onStart() {
        super.onStart();
        PogodiMesto.mGameHelper.onStart(this);
    }


    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        PogodiMesto.mGameHelper.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onSignInFailed() {
        // TODO Auto-generated method stub

    }
    @Override
    public void onSignInSucceeded() {
        // TODO Auto-generated method stub

    }


    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() < mBackPressed + 2000) {
            super.onBackPressed();
            return;
        }

        Toast.makeText(this, R.string.act_main_menu_double_back_to_exit, Toast.LENGTH_SHORT).show();
        mBackPressed = System.currentTimeMillis();
    }


    public void onClick_NewGame(View v) {
        Intent intent = new Intent(this, ActivitySingleGame.class);

        if (Build.VERSION.SDK_INT >= 21) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    new Pair<View, String>(mImgMap, PogodiMesto.PACKAGE + ".imageMap"),
                    new Pair<View, String>(mImgPin, PogodiMesto.PACKAGE + ".imagePin"));

            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }


    public void onClick_Leaderboards(View v) {
        if (PogodiMesto.mGameHelper.isSignedIn()) {
            startActivityForResult(
                    Games.Leaderboards.getAllLeaderboardsIntent(PogodiMesto.mGameHelper.getApiClient()),
                    9001); // 9001 = RC_UNUSED
        }
        else {
            PogodiMesto.mGameHelper.beginUserInitiatedSignIn();
        }
    }


    public void onClick_Achievements(View v) {
        if (PogodiMesto.mGameHelper.isSignedIn()) {
            startActivityForResult(Games.Achievements.getAchievementsIntent(PogodiMesto.mGameHelper.getApiClient()), 9001);
        }
        else {
            PogodiMesto.mGameHelper.beginUserInitiatedSignIn();
        }
    }

    public void onClick_Settings(View v) {
        //otvara Settings activity
        startActivity(new Intent(this, ActivitySettings.class));
    }
}