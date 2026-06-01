package demo.reversi.game;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReversiGame {
    public static final int BOARD_SIZE = 8;
    private final Disc[][] board;
    private Disc currentPlayer;
    private final GameListener listener;
    private boolean gameOver;

    private final int gameMode; 
    private final Random random = new Random();

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    private static final int[][] POSITION_WEIGHTS = {
            { 100, -20,  10,   5,   5,  10, -20, 100},
            { -20, -50,  -2,  -2,  -2,  -2, -50, -20},
            {  10,  -2,  -1,  -1,  -1,  -1,  -2,  10},
            {   5,  -2,  -1,  -1,  -1,  -1,  -2,   5},
            {   5,  -2,  -1,  -1,  -1,  -1,  -2,   5},
            {  10,  -2,  -1,  -1,  -1,  -1,  -2,  10},
            { -20, -50,  -2,  -2,  -2,  -2, -50, -20},
            { 100, -20,  10,   5,   5,  10, -20, 100}
    };

    public ReversiGame(GameListener listener, int gameMode) {
        this.listener = listener;
        this.gameMode = gameMode;
        this.board = new Disc[BOARD_SIZE][BOARD_SIZE];
        initBoard();
    }

    private void initBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = Disc.EMPTY;
            }
        }
        board[3][3] = Disc.WHITE;
        board[3][4] = Disc.BLACK;
        board[4][3] = Disc.BLACK;
        board[4][4] = Disc.WHITE;

        currentPlayer = Disc.BLACK; 
        gameOver = false;
        notifyBoardUpdated();
    }

    public Disc getDiscAt(int row, int col) { return board[row][col]; }
    public Disc getCurrentPlayer() { return currentPlayer; }
    public boolean isGameOver() { return gameOver; }

    public int getScore(Disc player) {
        int score = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == player) score++;
            }
        }
        return score;
    }

    public List<int[]> getValidMoves(Disc player) {
        List<int[]> validMoves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, player)) validMoves.add(new int[]{i, j});
            }
        }
        return validMoves;
    }

    public boolean isValidMove(int row, int col, Disc player) {
        if (board[row][col] != Disc.EMPTY) return false;
        for (int[] dir : DIRECTIONS) {
            if (checkDirection(row, col, dir[0], dir[1], player)) return true;
        }
        return false;
    }

    private boolean checkDirection(int row, int col, int rowDir, int colDir, Disc player) {
        int r = row + rowDir, c = col + colDir;
        boolean hasOpponentBetween = false;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
            if (board[r][c] == Disc.EMPTY) return false;
            if (board[r][c] == player.opposite()) hasOpponentBetween = true;
            else if (board[r][c] == player) return hasOpponentBetween;
            r += rowDir; c += colDir;
        }
        return false;
    }

    public void makeMove(int row, int col) {
        if (gameOver || !isValidMove(row, col, currentPlayer)) return;
        board[row][col] = currentPlayer;
        flipDiscs(row, col, currentPlayer);
        switchTurn();
    }

    private void switchTurn() {
        currentPlayer = currentPlayer.opposite();

        if (getValidMoves(currentPlayer).isEmpty()) {
            currentPlayer = currentPlayer.opposite();
            if (getValidMoves(currentPlayer).isEmpty()) {
                gameOver = true;
                if (listener != null) listener.onGameOver(getScore(Disc.BLACK), getScore(Disc.WHITE));
                notifyBoardUpdated();
                return;
            }
        }

        notifyBoardUpdated();

        if (gameMode > 0 && currentPlayer == Disc.WHITE && !gameOver) {
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> makeAIMove());
            pause.play();
        }
    }

    private void flipDiscs(int row, int col, Disc player) {
        for (int[] dir : DIRECTIONS) {
            if (checkDirection(row, col, dir[0], dir[1], player)) {
                int r = row + dir[0], c = col + dir[1];
                while (board[r][c] == player.opposite()) {
                    board[r][c] = player;
                    r += dir[0]; c += dir[1];
                }
            }
        }
    }

    private void makeAIMove() {
        List<int[]> moves = getValidMoves(currentPlayer);
        if (moves.isEmpty()) return;

        int[] selectedMove = null;
        if (gameMode == 1) { 
            selectedMove = moves.get(random.nextInt(moves.size()));
        } else if (gameMode == 2) { 
            int bestScore = Integer.MIN_VALUE;
            for (int[] move : moves) {
                int moveScore = POSITION_WEIGHTS[move[0]][move[1]];
                if (moveScore > bestScore) {
                    bestScore = moveScore;
                    selectedMove = move;
                }
            }
        }

        if (selectedMove != null) {
            makeMove(selectedMove[0], selectedMove[1]);
        }
    }

    private void notifyBoardUpdated() {
        if (listener != null) listener.onBoardUpdated();
    }
}