package com.example.project_battleships;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

public class LoggedMainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnStartBattle, btnStartOnlineBattle, btnShowLeaderboard, btnSignOut;
    TextView tvUsername;
    String username;

    OnlineGame game;
    AudioManager audioManager;
    int maxMusicVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_main);
        btnStartBattle = (Button) findViewById(R.id.btnStartBattle);
        btnStartBattle.setOnClickListener(this);
        btnStartOnlineBattle = (Button) findViewById(R.id.btnStartOnlineBattle);
        btnStartOnlineBattle.setOnClickListener(this);
        btnShowLeaderboard = (Button) findViewById(R.id.btnShowLeaderboard);
        btnShowLeaderboard.setOnClickListener(this);
        btnSignOut = (Button) findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(this);
        tvUsername = (TextView) findViewById(R.id.tvUsername);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Intent dataIntent = getIntent();
        int requestCode = dataIntent.getIntExtra("Request", 0);
        if (requestCode == 1) {
            // 1 stands for logging in and returning back to main menu
            username = dataIntent.getStringExtra("Username");
            if (username != null) {
                tvUsername.setText(String.format("Welcome back, %s", username));
            }
        } else if (requestCode == 2) {
            // 2 stands for registering
            username = dataIntent.getStringExtra("Username");
            if (username != null) {
                    tvUsername.setText(String.format("Welcome, %s", username));
            }
        } else if (requestCode == 0) {
            // 0 stands for an error. Usually this situation doesn't occur
            Intent failedIntent = new Intent(this, MainActivity.class);
            Toast.makeText(this, "Activity failed. Bringing you back to Main Menu", Toast.LENGTH_LONG).show();
            startActivity(failedIntent);
            finish();
        }
    }

    @Override
    protected void onResume() { super.onResume(); }

    @Override
    protected void onPause() { super.onPause(); }

    @Override
    public void onClick(View view) {
        if (view == btnStartBattle) {
            Intent intent = new Intent(this, PlaceShipsActivity.class);
            intent.putExtra("Username", username);
            startActivity(intent);
            finish();
        }
        else if (view == btnStartOnlineBattle) {
            final Dialog waitingDialog = new Dialog(this);
            waitingDialog.setContentView(R.layout.dialog_waiting_for_online_game);
            waitingDialog.setCancelable(false);
            waitingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            waitingDialog.show();
            queueThread(waitingDialog).start();
        } else if (view == btnShowLeaderboard) {
            Intent intent = new Intent(this, LeaderboardActivity.class);
            intent.putExtra("Username", username);
            startActivity(intent);
        } else if (view == btnSignOut) {
            Intent intent = new Intent(this, MainActivity.class);
            username = null;
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() { return; }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.meInfo) {
            createInfoDialog(this);
        } else if (item.getItemId() == R.id.meSettings) {
            createSettingsDialog(this);
        }
        return true;
    }

    private void createInfoDialog(Context context) {
        final Dialog infoDialog = new Dialog(context);
        final ArrayList<Integer> listIv = new ArrayList<>();
        listIv.add(R.drawable.instructions_part_2);
        listIv.add(R.drawable.instructions_part_3);
        listIv.add(R.drawable.instructions_part_4);
        Window window = infoDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        infoDialog.setContentView(R.layout.dialog_info);
        infoDialog.setCancelable(false);
        infoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        final ImageView ivInstructions = (ImageView) infoDialog.findViewById(R.id.ivInstructions);
        ivInstructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!listIv.isEmpty()) {
                    ivInstructions.setImageResource(listIv.get(0));
                    listIv.remove(0);
                }
                else {
                    infoDialog.dismiss();
                }
            }
        });
        infoDialog.show();
    }

    private void createSettingsDialog(Context context) {
        Dialog settingsDialog = new Dialog(context);
        settingsDialog.setContentView(R.layout.dialog_settings);
        settingsDialog.setCancelable(true);
        settingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        SeekBar sbMusicVolume = (SeekBar) settingsDialog.findViewById(R.id.sbMusicVolume);
        sbMusicVolume.setMax(maxMusicVolume);
        sbMusicVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        sbMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        settingsDialog.show();
    }

    public Thread queueThread(final Dialog dialog) {
       return new Thread() {
            @Override
            public void run() {
                TextView tvActiveGames = dialog.findViewById(R.id.tvActiveGames);
                tvActiveGames.setText(String.format("Active Games: %s", getActiveGames()));
                game = new OnlineGame(username);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tvWaiting = dialog.findViewById(R.id.tvWaiting);
                        tvWaiting.setText("Game Found!");
                        new CountDownTimer(1000, 1000) {
                            @Override
                            public void onTick(long l) {
                            }
                            @Override
                            public void onFinish() {
                                Intent intent = new Intent(LoggedMainActivity.this, OnlinePlaceShipsActivity.class);
                                intent.putExtra("Game", game);
                                dialog.dismiss();
                                startActivity(intent);
                                finish();
                            }
                        }.start();
                    }
                });
            }
        };
    }

    private String getActiveGames() {
        JSONObject getActiveGames = new JSONObject();
        try {
            getActiveGames.put("request", "get_active_games");
            Client client = new Client(getActiveGames);
            JSONObject received = client.execute().get();
            return received.getString("games");
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }
}