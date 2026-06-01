package demo.reversi.game;

public interface GameListener {
    void onBoardUpdated();
    void onGameOver(int blackScore, int whiteScore);
}