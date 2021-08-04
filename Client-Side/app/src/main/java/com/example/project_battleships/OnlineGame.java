package com.example.project_battleships;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.Serializable;

public class OnlineGame implements Serializable {
    private int gameID;
    private Player player;
    private Opponent opponent;
    private int playerNum;
    private String playerName;
    private boolean isPlayerTurn;
    private int status;

    public OnlineGame(String playerName) {
        this.player = null;
        this.playerName = playerName;
        Pair<Integer, Integer> data = getIDAndPlayerNum();
        this.gameID = data.first;
        this.playerNum = data.second;
    }

    public Player getPlayer() { return this.player; }

    public void setPlayer(Player player) { this.player = player; }

    public boolean isPlayerTurn() { return this.isPlayerTurn; }

    public Opponent getOpponent() { return this.opponent; }

    public void setOpponent(Opponent opponent) { this.opponent = opponent; }

    public int getStatus() { return this.status; }

    public int getPlayerNum() { return this.playerNum; }

    public void createInGameMenuDialog(final Context context, final AudioManager audioManager, int maxMusicVolume, final CountDownTimer countDownTimer) {
        /*
        The function creates and shows a dialog where the user can quit in the middle of the game,
        or change some settings in the app.
         */
        final Dialog inGameMenuDialog = new Dialog(context);
        inGameMenuDialog.setContentView(R.layout.dialog_in_game_menu);
        inGameMenuDialog.setCancelable(true);
        inGameMenuDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button btnReturnToStart = (Button) inGameMenuDialog.findViewById(R.id.btnReturnToMain);
        SeekBar sbMusicVolume = (SeekBar) inGameMenuDialog.findViewById(R.id.sbMusicVolume);
        sbMusicVolume.setMax(maxMusicVolume);
        sbMusicVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        btnReturnToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, LoggedMainActivity.class);
                intent.putExtra("Username", playerName);
                intent.putExtra("Request", 1);
                countDownTimer.cancel();
                disconnectGame();
                inGameMenuDialog.dismiss();
                context.startActivity(intent);
                ((Activity)context).finish();
            }
        });
        inGameMenuDialog.show();
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
    }

    public void createGameOverDialog(final Context context, boolean isPlayerWon) {
        /*
        The function creates and shows a dialog which declares the game winner, and from that dialog
        the player can start a new game or move to the main menu (MainActivity).
        */
        final String username = this.playerName;
        final Dialog gameOverDialog = new Dialog(context);
        gameOverDialog.setContentView(R.layout.dialog_game_over);
        gameOverDialog.setCancelable(false);
        gameOverDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView tvGameOver = (TextView) gameOverDialog.findViewById(R.id.tvGameOver);
        TextView tvTurns = (TextView) gameOverDialog.findViewById(R.id.tvTurns);
        TextView tvScore = (TextView) gameOverDialog.findViewById(R.id.tvScore);
        Button btnReturnToStart = (Button) gameOverDialog.findViewById(R.id.btnReturnToMain);
        Button btnStartBattle = (Button) gameOverDialog.findViewById(R.id.btnStartBattle);
        btnStartBattle.setVisibility(View.INVISIBLE);
        btnReturnToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, LoggedMainActivity.class);
                intent.putExtra("Username", username);
                intent.putExtra("Request", 1);
                gameOverDialog.dismiss();
                context.startActivity(intent);
                ((Activity)context).finish();
            }
        });
        if (isPlayerWon) {
            tvGameOver.setText("You Win!");
            tvTurns.setText(String.format("You've sunk the enemy ships in %d turns!", player.getTurns()));
        } else {
            tvGameOver.setText("You Lose!");
            tvTurns.setText(String.format("The enemy has sunk your ships in %d turns!", opponent.getTurns()));
        }
        tvScore.setText(String.format("Your Score: %d", player.getScore()));
        updateLastGameData(isPlayerWon);
        removeGame();
        gameOverDialog.show();
    }

    public void updateLastGameData(boolean isPlayerWon) {
        JSONObject lastGameData = new JSONObject();
        try {
            lastGameData.put("request", "update_last_game_data");
            lastGameData.put("username", this.playerName);
            lastGameData.put("is_win", String.valueOf(isPlayerWon));
            lastGameData.put("score", String.valueOf(this.player.getScore()));
            Client client = new Client(lastGameData);
            JSONObject received = client.execute().get();
            System.out.println("Updated: " + received.getString("response"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Pair<Integer, Integer> getIDAndPlayerNum() {
        JSONObject gameData = new JSONObject();
        try {
            gameData.put("request", "start_online_game");
            Client client = new Client(gameData);
            JSONObject received = client.execute().get();
            int gameID = received.getInt("game_id");
            int player = received.getInt("player");
            this.isPlayerTurn = received.getBoolean("is_player_turn");
            return new Pair<>(gameID, player);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean setShip(int x, int y, int length, boolean horizontal) {
        JSONObject boardShip = new JSONObject();
        try {
            boardShip.put("request", "add_ship");
            boardShip.put("game_id", this.gameID);
            boardShip.put("player", this.playerNum);
            boardShip.put("x", x);
            boardShip.put("y", y);
            boardShip.put("length", length);
            boardShip.put("horizontal", horizontal);
            boardShip.put("RAND_FLAG", false);
            Client client = new Client(boardShip);
            JSONObject received = client.execute().get();
            return received.getBoolean("is_placed");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Triplet<Boolean, Integer, Integer> setRandomShip(int length) {
        JSONObject boardShip = new JSONObject();
        try {
            boardShip.put("request", "add_ship");
            boardShip.put("game_id", this.gameID);
            boardShip.put("player", this.playerNum);
            boardShip.put("length", length);
            boardShip.put("RAND_FLAG", true);
            Client client = new Client(boardShip);
            JSONObject received = client.execute().get();
            int x = received.getInt("x");
            int y = received.getInt("y");
            boolean horizontal = received.getBoolean("horizontal");
            return new Triplet<>(horizontal, x, y);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public char sendTurn(int x, int y) {
        JSONObject turn = new JSONObject();
        char turnResult = '!';
        try {
            turn.put("request", "do_turn");
            turn.put("game_id", this.gameID);
            turn.put("player", this.playerNum);
            turn.put("x", x);
            turn.put("y", y);
            Client client = new Client(turn);
            JSONObject received = client.execute().get();
            if (received.getBoolean("is_done")) {
                turnResult = received.getString("result").charAt(0);
                this.isPlayerTurn = received.getBoolean("player_turn");
                this.status = received.getInt("game_status");
            }
            return turnResult;
        } catch (Exception e) {
            e.printStackTrace();
            return turnResult;
        }
    }

    public Pair<Integer, Integer> getRandomPos() {
        JSONObject turn = new JSONObject();
        Pair<Integer, Integer> pos;
        try {
            turn.put("request", "get_rand_pos");
            turn.put("game_id", this.gameID);
            turn.put("player", this.playerNum);
            Client client = new Client(turn);
            JSONObject received = client.execute().get();
            int x = received.getInt("x");
            int y = received.getInt("y");
            pos = new Pair<>(x, y);
            return pos;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Triplet<Integer, Integer, Character> getOppTurn() {
        JSONObject turn = new JSONObject();
        Triplet<Integer, Integer, Character> oppTurn = null;
        try {
            turn.put("request", "waiting_for_turn");
            turn.put("game_id", this.gameID);
            turn.put("player", this.playerNum);
            Client client = new Client(turn);
            JSONObject received = client.execute().get();
            boolean isTurnDone = received.getBoolean("is_done");
            if (isTurnDone) {
                int xTurn = received.getInt("x");
                int yTurn = received.getInt("y");
                char turnResult = received.getString("result").charAt(0);
                this.isPlayerTurn = received.getBoolean("player_turn");
                oppTurn = new Triplet<>(xTurn, yTurn, turnResult);
            }
            this.status = received.getInt("game_status");
            return oppTurn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isPlayerShipWrecked() {
        JSONObject isShipWrecked = new JSONObject();
        try {
            isShipWrecked.put("request", "is_player_ship_wrecked");
            isShipWrecked.put("game_id", this.gameID);
            isShipWrecked.put("player", this.playerNum);
            Client client = new Client(isShipWrecked);
            JSONObject received = client.execute().get();
            return received.getBoolean("is_wrecked");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONObject isOppShipWrecked() {
        JSONObject isShipWrecked = new JSONObject();
        JSONObject shipData = null;
        try {
            isShipWrecked.put("request", "is_opponent_ship_wrecked");
            isShipWrecked.put("game_id", this.gameID);
            isShipWrecked.put("player", this.playerNum);
            Client client = new Client(isShipWrecked);
            JSONObject received = client.execute().get();
            if (received.getBoolean("is_wrecked")) {
                shipData = new JSONObject();
                shipData.put("x", received.getInt("x"));
                shipData.put("y", received.getInt("y"));
                shipData.put("length", received.getInt("length"));
                shipData.put("horizontal", received.getBoolean("horizontal"));
            }
            return shipData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getScore() {
        JSONObject score = new JSONObject();
        try {
            score.put("request", "get_score");
            score.put("game_id", this.gameID);
            score.put("player", this.playerNum);
            Client client = new Client(score);
            JSONObject received = client.execute().get();
            return received.getInt("score");
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void disconnectGame() {
        JSONObject disconnect = new JSONObject();
        try {
            disconnect.put("request", "disconnect_game");
            disconnect.put("game_id", this.gameID);
            disconnect.put("player", this.playerNum);
            Client client = new Client(disconnect);
            JSONObject received = client.execute().get();
            String response = received.getString("response");
            System.out.println(String.format("Game %d: Player %d has disconnected: %s", this.gameID, this.playerNum, response));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeGame() {
        JSONObject disconnect = new JSONObject();
        try {
            disconnect.put("request", "remove_game");
            disconnect.put("game_id", this.gameID);
            Client client = new Client(disconnect);
            JSONObject received = client.execute().get();
            String response = received.getString("response");
            System.out.println(String.format("Game %d is over: %s", this.gameID, response));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
