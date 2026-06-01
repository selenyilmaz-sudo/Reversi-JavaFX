package demo.reversi.game;

public enum Disc {
    EMPTY,
    BLACK,
    WHITE;

    public Disc opposite() {
        if (this == BLACK) return WHITE;
        if (this == WHITE) return BLACK;
        return EMPTY;
    }
}