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

import java.io.Serializable;
import java.util.ArrayList;

public class OnlinePlaceShipsActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout boardLinearLayout;
    OnlineGame game;
    Player player;
    Opponent opponent;
    Buttons buttons;

    int playerShipsIndex = 0;
    ArrayList<Ship> playerShips;

    int shipLength;
    int timeLeft = 10;

    TextView tvShipsLeft;
    TextView tvShipLength;
    TextView tvTimeLeft;
    Button btnRotateShip;

    AudioManager audioManager;
    int maxMusicVolume;
    CountDownTimer placeShipsTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_ships);
        boardLinearLayout = (LinearLayout) findViewById(R.id.board_linear_layout);
        game = (OnlineGame) getIntent().getSerializableExtra("Game");
        player = new Player();
        opponent = new Opponent();
        playerShips = player.getShips();
        buttons = new Buttons(this, boardLinearLayout);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStart() {
        super.onStart();
        buttons.setClickable(true);
        buttons.setListener();

        tvShipsLeft = (TextView) findViewById(R.id.tvShipsLeft);
        tvShipLength = (TextView) findViewById(R.id.tvShipLength);
        tvTimeLeft = (TextView) findViewById(R.id.tvTimeLeft);
        btnRotateShip = (Button) findViewById(R.id.btnRotateShip);

        tvShipsLeft.setText(Constants.SHIPS_ARRAY_LENGTH + " Ships Left");
        shipLength = playerShips.get(playerShipsIndex).getLength();
        tvShipLength.setText("Current Ship's Length: " + shipLength);
        tvTimeLeft.setText(String.valueOf(timeLeft));
        btnRotateShip.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        placeShipsTimer = new CountDownTimer(10000, 1000) {

            @Override
            public void onTick(long l) {
                tvTimeLeft.setText(String.valueOf(timeLeft));
                timeLeft--;
            }

            @Override
            public void onFinish() {
                tvTimeLeft.setText("0");
                if (playerShipsIndex < Constants.SHIPS_ARRAY_LENGTH) {
                    while (playerShipsIndex < Constants.SHIPS_ARRAY_LENGTH) {
                        Ship ship = playerShips.get(playerShipsIndex);
                        Triplet<Boolean, Integer, Integer> shipData = game.setRandomShip(ship.getLength());
                        if (shipData != null) {
                            boolean horizontal = shipData.getFirst();
                            int x = shipData.getSecond(), y = shipData.getThird();
                            ship.setX(x); ship.setY(y); ship.setHorizontal(horizontal);
                            ship.setPlaced(true);
                            placeShipOnCharBoard(ship, player.getBoard().getCharactersBoard());
                            placeShipOnButtonsBoard(ship);
                            playerShipsIndex++;
                        }
                    }
                    buttons.setClickable(false);
                    btnRotateShip.setEnabled(false);
                }
                game.setPlayer(player);
                game.setOpponent(opponent);
                Intent battleIntent;
                if (game.isPlayerTurn()) {
                    battleIntent = new Intent(OnlinePlaceShipsActivity.this, OnlinePlayerTurnActivity.class);
                } else {
                    battleIntent = new Intent(OnlinePlaceShipsActivity.this, OnlineOpponentTurnActivity.class);
                }
                battleIntent.putExtra("Game", (Serializable) game);
                startActivity(battleIntent);
                finish();
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        game.createInGameMenuDialog(this, audioManager, maxMusicVolume, placeShipsTimer);
    }

    @Override
    public void onClick(View view) {
        if (view == btnRotateShip) {
            boolean shipRotate = playerShips.get(playerShipsIndex).isHorizontal();
            if (shipRotate) {
                playerShips.get(playerShipsIndex).setHorizontal(false);
                btnRotateShip.setHint("Vertical");
            } else {
                playerShips.get(playerShipsIndex).setHorizontal(true);
                btnRotateShip.setHint("Horizontal");
            }
        } else {
            Ship ship = playerShips.get(playerShipsIndex);
            Pair<Integer, Integer> point = buttons.getButtonPos((Button) view);
            int x = point.first, y = point.second;
            if (handlePlacePlayerShip(x, y, ship)) {
                tvShipsLeft.setText(Constants.SHIPS_ARRAY_LENGTH - playerShipsIndex + " Ships Left");
                if (playerShipsIndex == Constants.SHIPS_ARRAY_LENGTH) {
                    buttons.setClickable(false);
                    btnRotateShip.setEnabled(false);
                } else {
                    shipLength = playerShips.get(playerShipsIndex).getLength();
                    tvShipLength.setText("Current Ship's Length: " + shipLength);
                    btnRotateShip.setHint("Horizontal");
                }
            }
        }
    }

    public boolean handlePlacePlayerShip(int x, int y, Ship ship) {
        int length = ship.getLength();
        boolean horizontal = ship.isHorizontal();
        if (game.setShip(x, y, length, horizontal)) {
            ship.setX(x); ship.setY(y); ship.setHorizontal(horizontal);
            ship.setPlaced(true);
            placeShipOnCharBoard(ship, player.getBoard().getCharactersBoard());
            placeShipOnButtonsBoard(ship);
            playerShipsIndex++;
            return true;
        } else {
            Toast.makeText(this, "Please place the ships on a legal place", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public void placeShipOnCharBoard(Ship ship, Character[][] board) {
        /*
        The function gets the ship and the characters board which the ship will be placed on.
        It fills the places which the ship is placed on, according to its length and rotation,
        with the character 's'.
        */
        int xStart = ship.getX(), yStart = ship.getY(), shipLength = ship.getLength();
        if (ship.isHorizontal()) { // For a horizontal ship
            for (int x = xStart; x < xStart + shipLength; x++) {
                board[yStart][x] = 's';
            }
        } else { // For a vertical ship
            for (int y = yStart; y < yStart + shipLength; y++) {
                board[y][xStart] = 's';
            }
        }
    }

    public void placeShipOnButtonsBoard(Ship ship) {
        /*
        The function gets the ship which was placed.
        It fills the places which the ship is placed on, according to its length and rotation,
        with a gray color.
        */
        int xStart = ship.getX(), yStart = ship.getY(), shipLength = ship.getLength();;
        Button[][] buttons = this.buttons.getButtons();
        if (ship.isHorizontal()) { // For a horizontal ship
            for (int x = ship.getX(); x < xStart + shipLength; x++) {
                buttons[yStart][x].setBackgroundColor(getResources().getColor(R.color.colorButtonShip));
            }
        } else { // For a vertical ship
            for (int y = ship.getY(); y < yStart + shipLength; y++) {
                buttons[y][xStart].setBackgroundColor(getResources().getColor(R.color.colorButtonShip));
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
        game.createInGameMenuDialog(this, audioManager, maxMusicVolume, placeShipsTimer);
        return true;
    }
}