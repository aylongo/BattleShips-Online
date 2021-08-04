import random

from player import Player


class Game:
    def __init__(self):
        self.p1 = Player()
        self.p2 = Player()
        self.p1_turn = random.choice([True, False])
        self.status = 0
        self.id = None

    def get_player(self, player):
        if player == 1:
            return self.p1
        else:
            return self.p2

    def get_board(self, player):
        if player == 1:
            return self.p1.get_board()
        else:
            return self.p2.get_board()

    def inc_turns(self, player):
        if player == 1:
            self.p1.inc_turns()
        else:
            self.p2.inc_turns()

    def set_player_ship(self, player, x, y, length, horizontal):
        if player == 1:
            return self.p1.set_ship(x, y, length, horizontal)
        else:
            return self.p2.set_ship(x, y, length, horizontal)

    def set_rand_player_ship(self, player, length):
        if player == 1:
            return self.p1.set_rand_ship(length)
        else:
            return self.p2.set_rand_ship(length)

    def get_ships(self, player):
        if player == 1:
            return self.p1.get_ships()
        else:
            return self.p2.get_ships()

    def get_status(self):
        return self.status

    def set_status(self, status):
        self.status = status

    def get_id(self):
        return self.id

    def generate_id(self, current_games):
        self.id = current_games

    def get_p1_turn(self):
        return self.p1_turn

    def get_player_turn(self, player):
        if player == 1:
            return self.p1_turn
        else:
            return not self.p1_turn

    def update_turn(self, player):
        if player == 1:
            self.p1_turn = False
        else:
            self.p1_turn = True

    def get_opp_connected(self, player):
        if player == 1:
            return self.p2.get_connected()
        else:
            return self.p1.get_connected()

    def get_player_connected(self, player):
        if player == 1:
            return self.p1.get_connected()
        else:
            return self.p2.get_connected()

    def set_player_connected(self, player, is_connected):
        if player == 1:
            self.p1.set_is_connected(is_connected)
        else:
            self.p2.set_is_connected(is_connected)

    def update_score(self, player):
        if player == 1:
            self.p1.update_score()
        else:
            self.p2.update_score()

    def get_player_last_turn(self, player):
        if player == 1:
            return self.p1.get_last_turn()
        else:
            return self.p2.get_last_turn()

    def get_opp_last_turn(self, player):
        if player == 1:
            return self.p2.get_last_turn()
        else:
            return self.p1.get_last_turn()

    def set_last_turn(self, player, x, y, result):
        if player == 1:
            self.p1.set_last_turn(x, y, result)
        else:
            self.p2.set_last_turn(x, y, result)

    def check_game_over(self):
        p1_wrecked_ships = 0
        p2_wrecked_ships = 0

        for i in range(5):
            p1_ship = self.p1.get_ships()[i]
            p2_ship = self.p2.get_ships()[i]
            if p1_ship.get_wrecked():
                p1_wrecked_ships += 1
            if p2_ship.get_wrecked():
                p2_wrecked_ships += 1

        if p1_wrecked_ships == 5:
            self.status = 2
        elif p2_wrecked_ships == 5:
            self.status = 1

        return self.status

    def get_player_ship(self, player, x, y):
        if player == 1:
            ship = self.p1.get_ship(x, y)
        else:
            ship = self.p2.get_ship(x, y)
        return ship

    def get_opp_ship(self, player, x, y):
        if player == 1:
            ship = self.p2.get_ship(x, y)
        else:
            ship = self.p1.get_ship(x, y)
        return ship

    def get_player_score(self, player):
        if player == 1:
            return self.p1.get_score()
        else:
            return self.p2.get_score()

    def get_opp_board(self, player):
        if player == 1:
            board = self.p2.get_board()
        else:
            board = self.p1.get_board()
        return board

    def get_player_turns(self, player):
        if player == 1:
            return self.p1.get_turns()
        else:
            return self.p2.get_turns()

    def get_opp_turns(self, player):
        if player == 1:
            return self.p2.get_turns()
        else:
            return self.p1.get_turns()

    def handle_turn(self, player, is_player_turn, x, y):
        if is_player_turn:
            board = self.get_opp_board(player)
            if board[y][x] == 'o':
                board[y][x] = 'm'
                self.inc_turns(player)
                return True, 'm'
            elif board[y][x] == 's':
                board[y][x] = 'h'
                self.inc_turns(player)
                ship = self.get_opp_ship(player, x, y)
                if is_ship_wrecked(board, ship):
                    update_wrecked_ship(board, ship)
                    self.update_score(player)
                return True, 'h'
        return False, None

    def get_random_pos(self, player):
        board = self.get_opp_board(player)
        while True:
            x = random.randrange(0, 10)
            y = random.randrange(0, 10)
            if board[y][x] == 'o' or board[y][x] == 's':
                return x, y


def update_wrecked_ship(board, ship):
    x_start = ship.get_x()
    y_start = ship.get_y()
    length = ship.get_length()
    if ship.get_horizontal():
        for x in range(x_start, x_start + length):
            board[y_start][x] = 'w'
    else:
        for y in range(y_start, y_start + length):
            board[y][x_start] = 'w'


def is_ship_wrecked(board, ship):
    x_start = ship.get_x()
    y_start = ship.get_y()
    length = ship.get_length()
    if ship.get_horizontal():
        for x in range(x_start, x_start + length):
            if board[y_start][x] != 'h':
                ship.set_wrecked(False)
                return False
    else:
        for y in range(y_start, y_start + length):
            if board[y][x_start] != 'h':
                ship.set_wrecked(False)
                return False
    ship.set_wrecked(True)
    return True
