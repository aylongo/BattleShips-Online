class Ship:
    def __init__(self, x, y, length, horizontal, is_wrecked):
        self.x = x
        self.y = y
        self.length = length
        self.horizontal = horizontal
        self.is_wrecked = is_wrecked

    def get_x(self):
        return self.x

    def set_x(self, x):
        self.x = x

    def get_y(self):
        return self.y

    def set_y(self, y):
        self.y = y

    def get_length(self):
        return self.length

    def set_length(self, length):
        self.length = length

    def get_horizontal(self):
        return self.horizontal

    def set_horizontal(self, horizontal):
        self.horizontal = horizontal

    def get_wrecked(self):
        return self.is_wrecked

    def set_wrecked(self, is_wrecked):
        self.is_wrecked = is_wrecked
