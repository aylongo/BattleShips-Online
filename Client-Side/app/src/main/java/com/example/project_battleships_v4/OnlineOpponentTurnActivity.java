package com.example.project_battleships_v4;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;

public class OnlineOpponentTurnActivity extends AppCompatActivity {
    OnlineGame game;
    LinearLayout boardLinearLayout;
    Buttons buttons;
    Player player;
    Opponent opponent;
    CharactersBoard playerBoard;
    TextView tvScore;

    AudioManager audioManager;
    int maxMusicVolume;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_computer_turn);
        boardLinearLayout = (LinearLayout) findViewById(R.id.board_linear_layout);
        buttons = new Buttons(this, boardLinearLayout);
        tvScore = (TextView) findViewById(R.id.tvScore);
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
            playerBoard = player.getBoard();
            int playerScore = player.getScore();
            tvScore.setText(String.format("Your Score: %d", playerScore));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePlayerButtonsBoard();
        // CountDownTimer Class delays the computer's turn so the player can see the computer's selection.
        new CountDownTimer(300, 500) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                playerBoard = player.getBoard();
                Character[][] playerBoardCA = game.getOppTurn();
                if (playerBoardCA != null) {
                    playerBoard.setBoard(playerBoardCA);
                    updatePlayerButtonsBoard();
                    opponent.incTurns();
                }
                if (game.getStatus() == -1) {
                    game.setPlayerTurn(true);
                    Intent battleIntent = new Intent(OnlineOpponentTurnActivity.this, OnlinePlayerTurnActivity.class);
                    battleIntent.putExtra("Game", (Serializable) game);
                    startActivity(battleIntent);
                    finish();
                } else {
                    int gameStatus = game.getStatus();
                    int player = game.getPlayerNum();
                    if ((gameStatus == 1 && player == 1) || (gameStatus == 2 && player == 2)) {
                        game.createGameOverDialog(OnlineOpponentTurnActivity.this, true);
                    } else {
                        game.createGameOverDialog(OnlineOpponentTurnActivity.this, false);
                    }
                }
            }
        }.start();
    }

    @Override
    public void onBackPressed() { return; }

    public void updatePlayerButtonsBoard() {
        /*
        The function updates the player's buttons board
        in order to let the player to watch the changes in the game.
        */
        for (int y = 0; y < Constants.BOARD_ARRAY_LENGTH; y++) {
            for (int x = 0; x < Constants.BOARD_ARRAY_LENGTH; x++) {
                switch (playerBoard.getCharactersBoard()[y][x]) {
                    case 'w':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonWrecked));
                        break;
                    case 'h':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonHit));
                        break;
                    case 'm':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonMiss));
                        break;
                    case 's':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonShip));
                        break;
                    case 'o':
                        buttons.getButtons()[y][x].setBackgroundColor(getResources().getColor(R.color.colorButtonBackground));
                        break;
                }
            }
        }
    }
}
