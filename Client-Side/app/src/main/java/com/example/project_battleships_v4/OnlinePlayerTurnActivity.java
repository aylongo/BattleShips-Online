package com.example.project_battleships_v4;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
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

import java.io.Serializable;

public class OnlinePlayerTurnActivity extends AppCompatActivity implements View.OnClickListener {
    OnlineGame game;
    LinearLayout boardLinearLayout;
    Buttons buttons;
    Player player;
    Opponent opponent;
    CharactersBoard oppBoard;
    TextView tvScore;

    AudioManager audioManager;
    int maxMusicVolume;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_turn);
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
            oppBoard = opponent.getBoard();
            Character[][] oppBoardCA = game.getOppBoard();
            if (oppBoardCA != null) {
                oppBoard.setBoard(oppBoardCA);
            }
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
    }

    @Override
    public void onBackPressed() {
        game.createInGameMenuDialog(this, audioManager, maxMusicVolume);
    }

    @Override
    public void onClick(View view) {
        Pair<Integer, Integer> point = buttons.getButtonPos((Button) view);
        int x = point.first, y = point.second;
        if (player.handleTurn(opponent, x, y)) {
            game.setPlayerTurn(false);
            game.sendTurn(opponent);
        } else {
            Toast.makeText(this, "Please press on a valid place", Toast.LENGTH_LONG).show();
        }
        if (!game.isPlayerTurn()) {
            updateOppButtonsBoard();
            buttons.setClickable(false);
            int playerScore = player.getScore();
            tvScore.setText(String.format("Your Score: %d", playerScore));
            if (game.getStatus() == -1) {
                Intent battleIntent = new Intent(this, OnlineOpponentTurnActivity.class);
                battleIntent.putExtra("Game", (Serializable) game);
                startActivity(battleIntent);
                finish();
            } else {
                int gameStatus = game.getStatus();
                int player = game.getPlayerNum();
                if ((gameStatus == 1 && player == 1) || (gameStatus == 2 && player == 2)) {
                    game.createGameOverDialog(this, true);
                } else {
                    game.createGameOverDialog(this, false);
                }
            }
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
                    case 's':
                        /*
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
        game.createInGameMenuDialog(this, audioManager, maxMusicVolume);
        return true;
    }
}
