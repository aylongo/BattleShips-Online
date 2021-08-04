package com.example.project_battleships;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.Serializable;

public class OnlinePlayerTurnActivity extends AppCompatActivity implements View.OnClickListener {
    OnlineGame game;
    LinearLayout boardLinearLayout;
    Buttons buttons;
    Player player;
    Opponent opponent;
    CharactersBoard oppBoard;
    TextView tvScore;
    TextView tvTimeLeft;

    int timeLeft = 5;

    AudioManager audioManager;
    int maxMusicVolume;
    CountDownTimer turnTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_turn);
        boardLinearLayout = (LinearLayout) findViewById(R.id.board_linear_layout);
        buttons = new Buttons(this, boardLinearLayout);
        tvScore = (TextView) findViewById(R.id.tvScore);
        tvTimeLeft = (TextView) findViewById(R.id.tvTimeLeft);
        tvTimeLeft.setText(String.valueOf(timeLeft));
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        game = (OnlineGame) intent.getSerializableExtra("Game");
        if (game != null) {
            player = game.getPlayer();
            opponent = game.getOpponent();
            oppBoard = opponent.getBoard();
            int playerScore = player.getScore();
            tvScore.setText(String.format("Your Score: %d", playerScore));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        buttons.setClickable(true);
        buttons.setListener();
        updateOppButtonsBoard();
        turnTimer = new CountDownTimer(5000, 1000) {

            @Override
            public void onTick(long l) {
                tvTimeLeft.setText(String.valueOf(timeLeft));
                timeLeft--;
            }

            @Override
            public void onFinish() {
                tvTimeLeft.setText("0");
                if (game.isPlayerTurn()) {
                    Pair<Integer, Integer> pos = game.getRandomPos();
                    int x = pos.first, y = pos.second;
                    handleTurn(opponent, x, y);
                    updateOppButtonsBoard();
                    buttons.setClickable(false);
                    player.setScore(game.getScore());
                    int playerScore = player.getScore();
                    tvScore.setText(String.format("Your Score: %d", playerScore));
                    if (game.getStatus() == -1) {
                        Intent battleIntent = new Intent(OnlinePlayerTurnActivity.this, OnlineOpponentTurnActivity.class);
                        battleIntent.putExtra("Game", (Serializable) game);
                        startActivity(battleIntent);
                        finish();
                    } else {
                        int gameStatus = game.getStatus();
                        int player = game.getPlayerNum();
                        game.createGameOverDialog(OnlinePlayerTurnActivity.this, (gameStatus == 1 && player == 1) || (gameStatus == 2 && player == 2));
                    }
                }
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        game.createInGameMenuDialog(this, audioManager, maxMusicVolume, turnTimer);
    }

    @Override
    public void onClick(View view) {
        Pair<Integer, Integer> point = buttons.getButtonPos((Button) view);
        int x = point.first, y = point.second;
        if (!handleTurn(opponent, x, y)) {
            Toast.makeText(this, "Please press on a valid place", Toast.LENGTH_LONG).show();
        } if (!game.isPlayerTurn()) {
            updateOppButtonsBoard();
            buttons.setClickable(false);
            player.setScore(game.getScore());
            int playerScore = player.getScore();
            tvScore.setText(String.format("Your Score: %d", playerScore));
            if (game.getStatus() == -1) {
                Intent battleIntent = new Intent(this, OnlineOpponentTurnActivity.class);
                battleIntent.putExtra("Game", (Serializable) game);
                turnTimer.cancel();
                startActivity(battleIntent);
                finish();
            } else {
                int gameStatus = game.getStatus();
                int player = game.getPlayerNum();
                game.createGameOverDialog(this, (gameStatus == 1 && player == 1) || (gameStatus == 2 && player == 2));
            }
        }
    }

    public boolean handleTurn(Opponent opponent, int x, int y) {
        char turnResult = game.sendTurn(x, y);
        try {
            if (turnResult != '!') {
                opponent.getBoard().getCharactersBoard()[y][x] = turnResult;
                player.incTurns();
                if (turnResult == 'h') {
                    JSONObject shipData = game.isOppShipWrecked();
                    if (shipData != null) {
                        int shipX = shipData.getInt("x");
                        int shipY = shipData.getInt("y");
                        int shipLength = shipData.getInt("length");
                        boolean horizontal = shipData.getBoolean("horizontal");
                        oppBoard.updateWreckedShip(shipX, shipY, shipLength, horizontal);
                    }
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateOppButtonsBoard() {
        /*
        The function updates the computer's buttons board
        in order to let the player to watch the changes in the game.
        */
        for (int y = 0; y < Constants.BOARD_ARRAY_LENGTH; y++) {
            for (int x = 0; x < Constants.BOARD_ARRAY_LENGTH; x++) {
                switch (oppBoard.getCharactersBoard()[y][x]) {
                    case 'w':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonWrecked));
                        break;
                    case 'h':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonHit));
                        break;
                    case 'm':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonMiss));
                        break;
                     /*
                    case 's':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonShip));
                        break;
                     */
                    case 'o':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonBackground));
                        break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.game_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        game.createInGameMenuDialog(this, audioManager, maxMusicVolume, turnTimer);
        return true;
    }
}
