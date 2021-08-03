import random
import sqlite3
import socket
import threading
import json

import numpy as np

from game import Game

GAMES = []
current_games = 0

CREATE_TABLE_QUERY = """CREATE TABLE IF NOT EXISTS players(Username text UNIQUE, Password text, HighestScore text DEFAULT '0', 
Wins text DEFAULT '0')"""


def new_connection(sock, ip_address):
    print(f"new connection has received from {ip_address}")
    db_connection = sqlite3.connect('database.sql', check_same_thread=False)
    db_cursor = db_connection.cursor()
    client_request = sock.recv(2048).decode()
    response = request_handler(db_connection, db_cursor, json.loads(client_request))
    sock.send(response.encode())
    print(f"sent {response}")
    sock.close()
    db_connection.close()
    print(f"closed connection with {ip_address}\n")
    return response


def request_handler(connection, cursor, request):

    if request["request"] == "start_online_game":
        player, game = search_free_game()
        if player == 1:
            game.set_player1_connected(True)
        else:
            game.set_player2_connected(True)
        while True:
            if game.get_status() == -1:
                break
        return json.dumps({"response": "true", "game_id": game.get_id(), "player": player})

    elif request["request"] == "update_board":
        game_id = request["game_id"]
        player = request["player"]
        board = np.array(request["board"])
        game = search_game_by_id(game_id)
        if player == 1:
            game.update_player1_board(board)
            player_turn = game.get_player1_turn()
        else:
            game.update_player2_board(board)
            player_turn = not game.get_player1_turn()
        while True:
            if game.get_player1_board() is not None and game.get_player2_board() is not None:
                break
        return json.dumps({"response": "true", "player_turn": player_turn})

    elif request["request"] == "update_ships":
        game_id = request["game_id"]
        player = request["player"]
        json_ships_list = request["ships"]
        game = search_game_by_id(game_id)
        game.set_ships(player, json_ships_list)
        while True:
            if game.get_ships(1) and game.get_ships(2):
                break
        opp_ships = game.get_opponent_ships_list(player)
        print(player, ":", opp_ships)
        return json.dumps({"response": "true", "opponent_ships": opp_ships})

    elif request["request"] == "get_opponent_board":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        if player == 1:
            opp_board = game.get_player2_board()
        else:
            opp_board = game.get_player1_board()
        return json.dumps({"response": "true", "opponent_board": opp_board.tolist()})

    elif request["request"] == "do_turn":
        game_id = request["game_id"]
        player = request["player"]
        board = np.array(request["board"])
        game = search_game_by_id(game_id)
        print("game " + str(game_id) + ": " + "player " + str(player) + "'s turn is being updated")
        if player == 1:
            game.update_player2_board(board)
            game_status = game.check_game_over()
            game.set_player1_turn(False)
            player_turn = game.get_player1_turn()
        else:
            game.update_player1_board(board)
            game_status = game.check_game_over()
            game.set_player1_turn(True)
            player_turn = not game.get_player1_turn
        if game_status > 0:
            remove_game(game_id)
        return json.dumps({"response": "true", "player_turn": player_turn, "game_status": game_status})

    elif request["request"] == "waiting_for_turn":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        player_turn = False
        opponent_connected = True
        while not player_turn and opponent_connected:
            if player == 1:
                player_turn = game.get_player1_turn()
                board = game.get_player1_board()
                opponent_connected = game.get_player2_connected()
            else:
                player_turn = not game.get_player1_turn()
                board = game.get_player2_board()
                opponent_connected = game.get_player1_connected()
        game_status = game.get_status()
        if not opponent_connected:
            remove_game(game_id)
            return json.dumps({"response": "true", "game_status": game_status})
        return json.dumps({"response": "true", "player_turn": player_turn, "game_status": game_status, "board": board.tolist()})

    elif request["request"] == "disconnect_game":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        if player == 1:
            game.set_player1_connected(False)
            game.set_status(2)
        else:
            game.set_player2_connected(False)
            game.set_status(1)
        print("game " + str(game_id) + ": " + "player " + str(player) + "disconnected")
        return json.dumps({"response": "true"})

    elif request["request"] == "get_players_by_wins":
        print("Getting players ordered by Wins")
        get_players_by_wins = f"SELECT Username, HighestScore, Wins FROM players ORDER BY Wins DESC, HighestScore DESC"
        try:
            cursor.execute(get_players_by_wins)
            players_order = cursor.fetchmany(20)  # returns the first 20 users by most wins and highest score
            connection.commit()
            print(players_order)
            return json.dumps({"response": "true", "players_order": players_order})
        except Exception as e:
            print(e)
        return json.dumps({"response": "false"})

    elif request["request"] == "update_last_game_data":
        username = request["username"]
        is_win = request["is_win"]
        score = request["score"]
        print("Updating data")
        try:
            if is_win == "true":
                get_wins_query = f"SELECT Wins FROM players WHERE Username = '{username}'"
                cursor.execute(get_wins_query)
                wins = int(cursor.fetchone()[0])
                connection.commit()
                update_wins_query = f"UPDATE players SET Wins = '{wins + 1}' WHERE Username = '{username}'"
                cursor.execute(update_wins_query)
                connection.commit()
            get_highest_score_query = f"SELECT HighestScore FROM players WHERE Username = '{username}'"
            cursor.execute(get_highest_score_query)
            highest_score = cursor.fetchone()[0]
            connection.commit()
            update_score_query = f"UPDATE players SET HighestScore = '{score}' WHERE Username = '{username}' AND {highest_score} < {score}"
            cursor.execute(update_score_query)
            connection.commit()
            return json.dumps({"response": "true"})
        except Exception as e:
            print(e)
        return json.dumps({"response": "false"})

    elif request["request"] == "register":
        username = request["username"]
        password = request["password"]
        insert_data_query = f"INSERT INTO players (Username, Password) VALUES ('{username}', '{password}')"
        try:
            cursor.execute(insert_data_query)
            connection.commit()
            print("Registering")
            return json.dumps({"response": "true"})
        except Exception as e:
            print(e)
        return json.dumps({"response": "false"})

    elif request["request"] == "login":
        username = request["username"]
        password = request["password"]
        data_exists_query = f"SELECT * FROM players WHERE Username = '{username}' AND Password = '{password}'"
        try:
            cursor.execute(data_exists_query)
            data = cursor.fetchone()
            connection.commit()
            if data is not None:
                print("Logging in")
                return json.dumps({"response": "true"})
        except Exception as e:
            print(e)
        return json.dumps({"response": "false"})


def remove_game(game_id):
    for game in GAMES:
        if game.get_id() == game_id:
            GAMES.remove(game)


def search_free_game():
    print(GAMES)
    global current_games
    if not GAMES:
        game = Game()
        GAMES.append(game)
        return 1, game
    else:
        for game in GAMES:
            if game.get_status() == 0:
                current_games += 1
                game.set_status(-1)
                game.set_player1_turn(random.choice([True, False]))
                game.generate_id(current_games)
                return 2, game
        game = Game()
        GAMES.append(game)
        return 1, game


def search_game_by_id(game_id):
    for game in GAMES:
        if game.get_id() == game_id:
            return game


def main():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(("0.0.0.0", 1225))
    print("server started")
    server_socket.listen()

    with sqlite3.connect('database.sql', check_same_thread=False) as connection:
        cursor = connection.cursor()
        cursor.execute(CREATE_TABLE_QUERY)
        connection.commit()

    while True:
        new_client = server_socket.accept()
        new_thread = threading.Thread(target=new_connection, args=new_client)
        new_thread.start()


if __name__ == '__main__':
    main()
