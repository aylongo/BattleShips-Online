import sqlite3
import socket
import threading
import json

from game import Game

GAMES = []
current_games = 0

CREATE_TABLE_QUERY = """CREATE TABLE IF NOT EXISTS players(Username text UNIQUE, Password text, HighestScore INT DEFAULT 0, 
Wins INT DEFAULT 0)"""


def new_connection(sock, ip_address):
    print(f"new connection has received from {ip_address}")
    db_connection = sqlite3.connect('database.sql', check_same_thread=False)
    db_cursor = db_connection.cursor()
    client_request = sock.recv(1024).decode('utf-8')
    response = request_handler(db_connection, db_cursor, json.loads(client_request))
    sock.send(response.encode('utf-8'))
    print(f"sent {response}")
    sock.close()
    db_connection.close()
    print(f"closed connection with {ip_address}\n")
    return response


def request_handler(connection, cursor, request):

    if request["request"] == "start_online_game":
        player, game = search_free_game()
        game.set_player_connected(player, True)
        while True:
            if game.get_status() == -1:
                break
        is_player_turn = game.get_player_turn(player)
        return json.dumps({"is_connected": True, "game_id": game.get_id(), "player": player, "is_player_turn": is_player_turn})

    elif request["request"] == "add_ship":
        game_id = request["game_id"]
        player = request["player"]
        rand_flag = request["RAND_FLAG"]
        length = request["length"]
        game = search_game_by_id(game_id)
        if rand_flag:
            is_ship_placed, x, y, horizontal = game.set_rand_player_ship(player, length)
            return json.dumps({"is_placed": is_ship_placed, "x": x, "y": y, "horizontal": horizontal})
        else:
            x = request["x"]
            y = request["y"]
            horizontal = request["horizontal"]
            is_ship_placed = game.set_player_ship(player, x, y, length, horizontal)
            return json.dumps({"is_placed": is_ship_placed})

    elif request["request"] == "do_turn":
        game_id = request["game_id"]
        player = request["player"]
        x = request["x"]
        y = request["y"]
        game = search_game_by_id(game_id)
        game_status = game.get_status()
        is_player_turn = game.get_player_turn(player)
        turn_done_flag, result = game.handle_turn(player, is_player_turn, x, y)
        if turn_done_flag:
            game_status = game.check_game_over()
            game.set_last_turn(player, x, y, result)
            game.update_turn(player)
            is_player_turn = game.get_player_turn(player)
        print("game " + str(game_id) + ": " + "player " + str(player) + "'s turn is being updated")
        return json.dumps({"is_done": turn_done_flag, "result": result, "player_turn": is_player_turn, "game_status": game_status})

    elif request["request"] == "get_rand_pos":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        x, y = game.get_random_pos(player)
        return json.dumps({"x": x, "y": y})

    elif request["request"] == "waiting_for_turn":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        is_player_turn = game.get_player_turn(player)
        is_opponent_connected = game.get_opp_connected(player)
        while not is_player_turn and is_opponent_connected:
            is_player_turn = game.get_player_turn(player)
            is_opponent_connected = game.get_opp_connected(player)
        game_status = game.get_status()
        last_turn = game.get_opp_last_turn(player)
        print("game " + str(game_id) + ": " + "sending back to player " + str(player) + " his opponent's turn")
        if not is_opponent_connected:
            return json.dumps({"is_done": False, "game_status": game_status})
        return json.dumps({"is_done": True, "x": last_turn[0], "y": last_turn[1], "result": last_turn[2], "player_turn": is_player_turn, "game_status": game_status})

    elif request["request"] == "is_player_ship_wrecked":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        last_turn = game.get_opp_last_turn(player)
        ship = game.get_player_ship(player, last_turn[0], last_turn[1])
        return json.dumps({"is_wrecked": ship.get_wrecked()})

    elif request["request"] == "is_opponent_ship_wrecked":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        last_turn = game.get_player_last_turn(player)
        ship = game.get_opp_ship(player, last_turn[0], last_turn[1])
        return json.dumps({"is_wrecked": ship.get_wrecked(), "x": ship.get_x(), "y": ship.get_y(), "length": ship.get_length(), "horizontal": ship.get_horizontal()})

    elif request["request"] == "get_score":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        return json.dumps({"score": game.get_player_score(player)})

    elif request["request"] == "disconnect_game":
        game_id = request["game_id"]
        player = request["player"]
        game = search_game_by_id(game_id)
        game.set_player_connected(player, False)
        if game.get_opp_connected(player):
            if player == 1:
                game.set_status(2)
            else:
                game.set_status(1)
        else:
            remove_game(game_id)
        print("game " + str(game_id) + ": " + "player " + str(player) + " has disconnected")
        return json.dumps({"response": True})

    elif request["request"] == "remove_game":
        game_id = request["game_id"]
        game = search_game_by_id(game_id)
        if game is not None:
            remove_game(game_id)
            print(GAMES)
        return json.dumps({"response": True})

    elif request["request"] == "get_active_games":
        active_games = 0
        for game in GAMES:
            if game.get_status() != 0:
                active_games += 1
        return json.dumps({"games": active_games})

    elif request["request"] == "get_players_by_wins":
        print("Getting players ordered by Wins")
        get_players_by_wins = f"SELECT Username, HighestScore, Wins FROM players ORDER BY Wins DESC, HighestScore DESC"
        try:
            cursor.execute(get_players_by_wins)
            players_order = cursor.fetchmany(20)  # returns the first 20 users by most wins and highest score
            connection.commit()
            print(players_order)
            return json.dumps({"response": True, "players_order": players_order})
        except Exception as e:
            print(e)
        return json.dumps({"response": False})

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
                update_wins_query = f"UPDATE players SET Wins = {wins + 1} WHERE Username = '{username}'"
                cursor.execute(update_wins_query)
                connection.commit()
            get_highest_score_query = f"SELECT HighestScore FROM players WHERE Username = '{username}'"
            cursor.execute(get_highest_score_query)
            highest_score = cursor.fetchone()[0]
            connection.commit()
            update_score_query = f"UPDATE players SET HighestScore = {score} WHERE Username = '{username}' AND {highest_score} < {score}"
            cursor.execute(update_score_query)
            connection.commit()
            return json.dumps({"response": True})
        except Exception as e:
            print(e)
        return json.dumps({"response": False})

    elif request["request"] == "register":
        username = request["username"]
        password = request["password"]
        insert_data_query = f"INSERT INTO players (Username, Password) VALUES ('{username}', '{password}')"
        try:
            cursor.execute(insert_data_query)
            connection.commit()
            print("Registering")
            return json.dumps({"response": True})
        except Exception as e:
            print(e)
        return json.dumps({"response": False})

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
                return json.dumps({"response": True})
        except Exception as e:
            print(e)
        return json.dumps({"response": False})


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
                game.set_status(-1)
                game.generate_id(current_games)
                current_games += 1
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
