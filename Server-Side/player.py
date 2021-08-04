import random

from ship import Ship


class Player:
    def __init__(self):
        self.board = generate_empty_board()
        self.ships = generate_ships()
        self.ships_index = 0
        self.score = 0
        self.turns = 0
        self.turn_till_wreck = 0
        self.is_connected = False
        self.last_turn = [0, 0, "o"]

    def get_board(self):
        return self.board

    def get_ships(self):
        return self.ships

    def get_connected(self):
        return self.is_connected

    def set_is_connected(self, is_connected):
        self.is_connected = is_connected

    def get_last_turn(self):
        return self.last_turn

    def set_last_turn(self, x, y, result):
        self.last_turn[0] = x
        self.last_turn[1] = y
        self.last_turn[2] = result

    def get_ship(self, x, y):
        for ship in self.ships:
            if ship.get_horizontal():
                if y == ship.get_y() and ship.get_x() <= x < ship.get_x() + ship.get_length():
                    return ship
            else:
                if x == ship.get_x() and ship.get_y() <= y < ship.get_y() + ship.get_length():
                    return ship

    def set_ship(self, x, y, length, horizontal):
        ship = self.ships[self.ships_index]
        if length == ship.get_length() and is_legal_place(self.board, x, y, length, horizontal):
            ship.set_x(x)
            ship.set_y(y)
            ship.set_horizontal(horizontal)
            place_ship_on_board(self.board, ship)
            self.ships_index += 1
            return True
        else:
            return False

    def set_rand_ship(self, length):
        ship = self.ships[self.ships_index]
        is_placed = False
        while not is_placed:
            x = random.randrange(10)
            y = random.randrange(10)
            horizontal = random.choice([True, False])
            if length == ship.get_length() and is_legal_place(self.board, x, y, length, horizontal):
                ship.set_x(x)
                ship.set_y(y)
                ship.set_horizontal(horizontal)
                place_ship_on_board(self.board, ship)
                is_placed = True
                self.ships_index += 1
        return is_placed, ship.get_x(), ship.get_y(), ship.get_horizontal()

    def get_turns(self):
        return self.turns

    def inc_turns(self):
        self.turns += 1
        self.turn_till_wreck += 1

    def get_score(self):
        return self.score

    def update_score(self):
        self.score += (100 - self.turn_till_wreck)
        self.turn_till_wreck = 0


def generate_empty_board():
    board = []
    for y in range(10):
        board_line = []
        for x in range(10):
            board_line.append('o')
        board.append(board_line)
    return board


def generate_ships():
    ships = [Ship(x=-1, y=-1, length=2, horizontal=True, is_wrecked=False),
             Ship(x=-1, y=-1, length=3, horizontal=True, is_wrecked=False),
             Ship(x=-1, y=-1, length=3, horizontal=True, is_wrecked=False),
             Ship(x=-1, y=-1, length=4, horizontal=True, is_wrecked=False),
             Ship(x=-1, y=-1, length=5, horizontal=True, is_wrecked=False)]
    return ships


def is_legal_place(board, x, y, length, horizontal):
    if horizontal:
        if x + length <= 10:
            for x_ship in range(x, x + length):
                if board[y][x_ship] != 'o':
                    return False
            return True
        else:
            return False
    else:
        if y + length <= 10:
            for y_ship in range(y, y + length):
                if board[y_ship][x] != 'o':
                    return False
            return True
        else:
            return False


def place_ship_on_board(board, ship):
    length = ship.get_length()
    x = ship.get_x()
    y = ship.get_y()
    if ship.get_horizontal():
        for x_ship in range(x, x + length):
            board[y][x_ship] = 's'
    else:
        for y_ship in range(y, y + length):
            board[y_ship][x] = 's'
