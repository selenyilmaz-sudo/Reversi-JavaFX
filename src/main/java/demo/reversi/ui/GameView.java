package demo.reversi.ui;

import demo.reversi.game.Disc;
import demo.reversi.game.GameListener;
import demo.reversi.game.ReversiGame;
import demo.reversi.model.User;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class GameView implements GameListener {
    private static final int TILE_SIZE = 60;
    
    private final BorderPane root;
    private final GridPane boardGrid;
    
    private final Circle[][] discNodes = new Circle[ReversiGame.BOARD_SIZE][ReversiGame.BOARD_SIZE];
    private final Circle[][] hintNodes = new Circle[ReversiGame.BOARD_SIZE][ReversiGame.BOARD_SIZE];

    private final Label scoreLabel;
    private final Label turnLabel;
    private final Label messageLabel;

    private ReversiGame game;
    private final User loggedInUser;
    private final Consumer<Integer> onGameOverHandler;
    private final Runnable onLogoutHandler;
    
    private AudioClip placeSound;
    private MediaPlayer bgMusicPlayer;
    private boolean soundEnabled = true;
    private boolean isFirstDraw = true;

    public GameView(User user, Consumer<Integer> onGameOver, Runnable onLogout) {
        this.loggedInUser = user;
        this.onGameOverHandler = onGameOver;
        this.onLogoutHandler = onLogout;

        try {
            var url = getClass().getResource("/sounds/place.wav");
            if (url != null) placeSound = new AudioClip(url.toExternalForm());
        } catch (Exception e) {}

        try {
            var bgmUrl = getClass().getResource("/sounds/bgm.mp3");
            if (bgmUrl != null) {
                Media media = new Media(bgmUrl.toExternalForm());
                bgMusicPlayer = new MediaPlayer(media);
                bgMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgMusicPlayer.setVolume(0.5); 
                if (soundEnabled) bgMusicPlayer.play();
            }
        } catch (Exception e) {}

        boardGrid = new GridPane();
        boardGrid.setStyle("-fx-background-color: #222222; -fx-padding: 5;");
        boardGrid.setHgap(2); 
        boardGrid.setVgap(2);
        boardGrid.setAlignment(Pos.CENTER);
        
        initBoardUI();

        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(25));
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setPrefWidth(280); 
        leftPanel.setStyle("-fx-background-color: #2b2b2b;");

        Label welcomeLabel = new Label("Oyuncu:\n" + user.getNickname());
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        welcomeLabel.setTextFill(Color.WHITE);
        welcomeLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        turnLabel = new Label("Sıra: Pembe");
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        turnLabel.setTextFill(Color.DEEPPINK);
        turnLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        scoreLabel = new Label("Pembe: 2\nBeyaz: 2");
        scoreLabel.setFont(Font.font("Arial", 16));
        scoreLabel.setTextFill(Color.LIGHTGRAY);
        scoreLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        messageLabel = new Label("");
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Button btn2P = styleButton("İki Kişilik Oyna", "#4CAF50");
        Button btnAIEasy = styleButton("Bilgisayar (Normal)", "#2196F3");
        Button btnAIHard = styleButton("Bilgisayar (Zor)", "#f44336");
        
        Button soundToggleBtn = styleButton("Sesi Kapat", "#ff9800");
        Button logoutButton = styleButton("Çıkış Yap", "#757575");

        btn2P.setOnAction(e -> startNewGame(0));
        btnAIEasy.setOnAction(e -> startNewGame(1));
        btnAIHard.setOnAction(e -> startNewGame(2));
        
        soundToggleBtn.setOnAction(e -> {
            soundEnabled = !soundEnabled;
            soundToggleBtn.setText(soundEnabled ? "Sesi Kapat" : "Sesi Aç");
            if (bgMusicPlayer != null) {
                if (soundEnabled) bgMusicPlayer.play();
                else bgMusicPlayer.pause();
            }
        });

        logoutButton.setOnAction(e -> {
            if (bgMusicPlayer != null) bgMusicPlayer.stop();
            onLogoutHandler.run();
        });

        leftPanel.getChildren().addAll(
            welcomeLabel, new Label(""), turnLabel, scoreLabel, messageLabel, 
            new Label(""), btn2P, btnAIEasy, btnAIHard, new Label(""), soundToggleBtn, logoutButton
        );

        root = new BorderPane();
        root.setLeft(leftPanel);
        root.setCenter(boardGrid);

        startNewGame(0);
    }

    private Button styleButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        return btn;
    }

    public Parent getRoot() { return root; }

    private void initBoardUI() {
        for (int row = 0; row < ReversiGame.BOARD_SIZE; row++) {
            for (int col = 0; col < ReversiGame.BOARD_SIZE; col++) {
                StackPane cell = new StackPane();
                Rectangle bg = new Rectangle(TILE_SIZE, TILE_SIZE, Color.DARKGREEN);
                Circle hint = new Circle(10, Color.TRANSPARENT);
                hintNodes[row][col] = hint;
                
                Circle disc = new Circle(TILE_SIZE / 2.0 - 5);
                disc.setVisible(false);
                discNodes[row][col] = disc;

                cell.getChildren().addAll(bg, hint, disc);
                
                final int r = row; final int c = col;
                cell.setOnMouseClicked(e -> {
                    if (game != null && !game.isGameOver()) game.makeMove(r, c);
                });
                
                boardGrid.add(cell, col, row);
            }
        }
    }

    private void startNewGame(int mode) {
    	isFirstDraw = true;
        game = new ReversiGame(this, mode);
        messageLabel.setText("");
        onBoardUpdated(); 
    }

    @Override
    public void onBoardUpdated() {
        if (game == null) return;
        boolean moveMade = false;

        List<int[]> validMoves = game.getValidMoves(game.getCurrentPlayer());
        for (int r = 0; r < ReversiGame.BOARD_SIZE; r++) {
            for (int c = 0; c < ReversiGame.BOARD_SIZE; c++) {
                hintNodes[r][c].setFill(Color.TRANSPARENT);
            }
        
            if (moveMade && soundEnabled && placeSound != null && !isFirstDraw) {
                placeSound.play();
            }
            isFirstDraw = false;
            updateLabels();
        }
        if (!game.isGameOver()) {
            Color hintColor = (game.getCurrentPlayer() == Disc.BLACK) ? Color.rgb(255, 20, 147, 0.4) : Color.rgb(255, 255, 255, 0.4);
            for (int[] move : validMoves) hintNodes[move[0]][move[1]].setFill(hintColor);
        }

        for (int r = 0; r < ReversiGame.BOARD_SIZE; r++) {
            for (int c = 0; c < ReversiGame.BOARD_SIZE; c++) {
                Disc engineDisc = game.getDiscAt(r, c);
                Circle uiDisc = discNodes[r][c];

                if (engineDisc != Disc.EMPTY) {
                    Color targetColor = (engineDisc == Disc.BLACK) ? Color.DEEPPINK : Color.WHITE;
                    if (!uiDisc.isVisible()) {
                        uiDisc.setFill(targetColor);
                        uiDisc.setVisible(true);
                        moveMade = true;
                    } else if (!uiDisc.getFill().equals(targetColor)) {
                        animateFlip(uiDisc, targetColor);
                    }
                } else {
                    uiDisc.setVisible(false);
                }
            }
        }

        if (moveMade && soundEnabled && placeSound != null) placeSound.play();
        updateLabels();
    }

    private void animateFlip(Circle disc, Color newColor) {
        ScaleTransition shrink = new ScaleTransition(Duration.millis(150), disc);
        shrink.setToX(0); 
        ScaleTransition grow = new ScaleTransition(Duration.millis(150), disc);
        grow.setToX(1);   
        shrink.setOnFinished(e -> {
            disc.setFill(newColor);
            grow.play();
        });
        shrink.play();
    }

    private void updateLabels() {
        if (game == null) return; 
        int pinkScore = game.getScore(Disc.BLACK);
        int whiteScore = game.getScore(Disc.WHITE);
        scoreLabel.setText("Pembe: " + pinkScore + "\nBeyaz: " + whiteScore);

        if (game.getCurrentPlayer() == Disc.BLACK) {
            turnLabel.setText("Sıra: Pembe");
            turnLabel.setTextFill(Color.DEEPPINK);
        } else {
            turnLabel.setText("Sıra: Beyaz");
            turnLabel.setTextFill(Color.WHITE);
        }
    }

    @Override
    public void onGameOver(int blackScore, int whiteScore) {
        turnLabel.setText("Oyun Bitti!");
        turnLabel.setTextFill(Color.YELLOW);
        if (blackScore > whiteScore) {
            messageLabel.setText("Kazanan: Pembe!");
            messageLabel.setTextFill(Color.DEEPPINK);
        } else if (whiteScore > blackScore) {
            messageLabel.setText("Kazanan: Beyaz!");
            messageLabel.setTextFill(Color.WHITE);
        } else {
            messageLabel.setText("Berabere!");
            messageLabel.setTextFill(Color.LIGHTBLUE);
        }
        onGameOverHandler.accept(blackScore); 
    }
}