import copy

from ship import Ship


class Game:
    def __init__(self):
        self.player1_board = None
        self.player1_ships = []
        self.player2_board = None
        self.player2_ships = []
        self.player1_turn = None
        self.status = 0
        self.player1_connected = None
        self.player2_connected = None
        self.id = None

    def get_player1_board(self):
        return self.player1_board

    def get_player2_board(self):
        return self.player2_board

    def add_new_ship(self, player, ship):
        if player == 1:
            self.player1_ships.append(ship)
        else:
            self.player2_ships.append(ship)

    def get_ships(self, player):
        if player == 1:
            return self.player1_ships
        else:
            return self.player2_ships

    def set_ships(self, player, json_ships_list):
        ships = []
        for json_ship in json_ships_list:
            x = json_ship["x"]
            y = json_ship["y"]
            length = json_ship["length"]
            horizontal = json_ship["horizontal"]
            ships.append(Ship(x, y, length, horizontal))
        if player == 1:
            self.player1_ships = ships
        else:
            self.player2_ships = ships

    def get_status(self):
        return self.status

    def set_status(self, status):
        self.status = status

    def get_id(self):
        return self.id

    def get_player1_turn(self):
        return self.player1_turn

    def set_player1_turn(self, player1_turn):
        self.player1_turn = player1_turn

    def generate_id(self, current_games):
        self.id = current_games

    def update_player1_board(self, player1_board):
        self.player1_board = player1_board

    def update_player2_board(self, player2_board):
        self.player2_board = player2_board

    def get_player1_connected(self):
        return self.player1_connected

    def set_player1_connected(self, player1_connected):
        self.player1_connected = player1_connected

    def get_player2_connected(self):
        return self.player2_connected

    def set_player2_connected(self, player2_connected):
        self.player2_connected = player2_connected

    def check_game_over(self):
        player1_won = True
        player2_won = True
        for y in range(10):
            for x in range(10):
                if copy.deepcopy(self.get_player1_board())[y][x] == 's':
                    player2_won = False
                if copy.deepcopy(self.get_player2_board())[y][x] == 's':
                    player1_won = False
        if player1_won:
            self.status = 1
        elif player2_won:
            self.status = 2

        return self.status

    def get_opponent_ships_list(self, player):
        transferable_ships_list = []
        if player == 1:
            ships_list = self.get_ships(2)
        else:
            ships_list = self.get_ships(1)
        for ship in ships_list:
            transferable_ships_list.append([ship.get_x(), ship.get_y(), ship.get_length(), ship.get_horizontal()])
        return transferable_ships_list
