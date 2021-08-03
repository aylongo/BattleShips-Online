package com.example.project_battleships_v4;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class OnlineGame implements Serializable {
    private int gameID;
    private Player player;
    private Opponent opponent;
    private int playerNum;
    private String username;
    private boolean isPlayerTurn;
    private int status;

    public OnlineGame(String username) {
        this.player = null;
        this.username = username;
        Pair<Integer, Integer> data = getIDAndPlayerNum();
        this.gameID = data.first;
        this.playerNum = data.second;
    }

    public int getGameID() { return gameID; }

    public void setGameID(int gameID) { this.gameID = gameID; }

    public Player getPlayer() { return this.player; }

    public void setPlayer(Player player) { this.player = player; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public boolean isPlayerTurn() { return this.isPlayerTurn; }

    public void setPlayerTurn(boolean playerTurn) { this.isPlayerTurn = playerTurn; }

    public Opponent getOpponent() { return opponent; }

    public void setOpponent(Opponent opponent) { this.opponent = opponent; }

    public int getStatus() { return status; }

    public void setStatus(int status) { this.status = status; }

    public int getPlayerNum() { return playerNum; }

    public void setPlayerNum(int playerNum) { this.playerNum = playerNum; }

    public void createInGameMenuDialog(final Context context, final AudioManager audioManager, int maxMusicVolume) {
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
                intent.putExtra("Username", username);
                intent.putExtra("Request", 1);
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
        final String username = this.username;
        final Dialog gameOverDialog = new Dialog(context);
        gameOverDialog.setContentView(R.layout.dialog_game_over);
        gameOverDialog.setCancelable(false);
        gameOverDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView tvGameOver = (TextView) gameOverDialog.findViewById(R.id.tvGameOver);
        TextView tvTurns = (TextView) gameOverDialog.findViewById(R.id.tvTurns);
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
        updateLastGameData(isPlayerWon);
        gameOverDialog.show();
    }

    public void updateLastGameData(boolean isPlayerWon) {
        JSONObject lastGameData = new JSONObject();
        try {
            lastGameData.put("request", "update_last_game_data");
            lastGameData.put("username", this.username);
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
        JSONObject lastGameData = new JSONObject();
        try {
            lastGameData.put("request", "start_online_game");
            Client client = new Client(lastGameData);
            JSONObject received = client.execute().get();
            int gameID = received.getInt("game_id");
            int player = received.getInt("player");
            return new Pair<Integer, Integer>(gameID, player);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendBoard() {
        JSONObject updateBoard = new JSONObject();
        try {
            updateBoard.put("request", "update_board");
            updateBoard.put("game_id", this.gameID);
            updateBoard.put("player", this.playerNum);
            updateBoard.put("board", player.getBoard().getCharactersBoardJA());
            Client client = new Client(updateBoard);
            JSONObject received = client.execute().get();
            this.isPlayerTurn = received.getBoolean("player_turn");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendShipsSetOppShips() {
        JSONObject updateShips = new JSONObject();
        JSONArray jsonShipsList = new JSONArray();
        for (int i = 0; i < Constants.SHIPS_ARRAY_LENGTH; i++) {
            jsonShipsList.put(player.getShips().get(i).jsonObjectShip());
        }
        try {
            updateShips.put("request", "update_ships");
            updateShips.put("game_id", this.gameID);
            updateShips.put("player", this.playerNum);
            updateShips.put("ships", jsonShipsList);
            Client client = new Client(updateShips);
            JSONObject received = client.execute().get();
            JSONArray jsonOppShipsList = received.getJSONArray("opponent_ships");
            ArrayList<Ship> oppShipsList = new ArrayList<>();
            for (int i = 0; i < jsonOppShipsList.length(); i++) {
                int x = jsonOppShipsList.getJSONArray(i).getInt(0);
                int y = jsonOppShipsList.getJSONArray(i).getInt(1);
                int length = jsonOppShipsList.getJSONArray(i).getInt(2);
                boolean horizontal = jsonOppShipsList.getJSONArray(i).getBoolean(3);
                oppShipsList.add(new Ship(x, y, length, horizontal));
            }
            this.opponent.setShips(oppShipsList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendTurn(Opponent opponent) {
        JSONObject turn = new JSONObject();
        try {
            turn.put("request", "do_turn");
            turn.put("game_id", this.gameID);
            turn.put("player", this.playerNum);
            turn.put("board", opponent.getBoard().getCharactersBoardJA());
            Client client = new Client(turn);
            JSONObject received = client.execute().get();
            this.status = received.getInt("game_status");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Character[][] getOppBoard() {
        JSONObject getOppBoard = new JSONObject();
        try {
            getOppBoard.put("request", "get_opponent_board");
            getOppBoard.put("game_id", this.getGameID());
            getOppBoard.put("player", this.getPlayerNum());
            Client client = new Client(getOppBoard);
            JSONObject received = client.execute().get();
            JSONArray oppBoardJA = received.getJSONArray("opponent_board");
            Character[][] oppBoardCA = new Character[Constants.BOARD_ARRAY_LENGTH][Constants.BOARD_ARRAY_LENGTH];
            for(int y = 0; y < oppBoardCA.length; y++){
                for(int x = 0; x < oppBoardCA.length; x++){
                    oppBoardCA[y][x] = oppBoardJA.getJSONArray(y).getString(x).charAt(0);
                }
            }
            return oppBoardCA;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Character[][] getOppTurn() {
        JSONObject turn = new JSONObject();
        try {
            turn.put("request", "waiting_for_turn");
            turn.put("game_id", this.gameID);
            turn.put("player", this.playerNum);
            Client client = new Client(turn);
            JSONObject received = client.execute().get();
            this.status = received.getInt("game_status");
            JSONArray playerBoardJA = received.getJSONArray("board");
            Character[][] playerBoardCA = new Character[Constants.BOARD_ARRAY_LENGTH][Constants.BOARD_ARRAY_LENGTH];
            for (int y = 0; y < playerBoardCA.length; y++) {
                for (int x = 0; x < playerBoardCA.length; x++) {
                    playerBoardCA[y][x] = playerBoardJA.getJSONArray(y).getString(x).charAt(0);
                }
            }
            return playerBoardCA;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
}
