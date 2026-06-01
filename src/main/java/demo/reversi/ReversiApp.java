package demo.reversi;

import demo.reversi.model.User;
import demo.reversi.persistence.UserStore;
import demo.reversi.ui.GameView;
import demo.reversi.ui.LoginView;
import demo.reversi.ui.RegisterView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ReversiApp extends Application {
    private Stage primaryStage;
    private UserStore userStore;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        try {
            userStore = new UserStore();
        } catch (Exception e) {
            System.err.println("Veritabanı başlatılamadı: " + e.getMessage());
            return;
        }

        primaryStage.setTitle("Reversi (Othello) - Pembe & Beyaz");
        primaryStage.setResizable(false);
        
        showLoginScreen();
        primaryStage.show();
    }

    private void showLoginScreen() {
        LoginView loginView = new LoginView(
            userStore,
            user -> showGameScreen(user),
            () -> showRegisterScreen()
        );
        
        Scene scene = new Scene(loginView.getRoot(), 450, 350);
        primaryStage.setScene(scene);
    }

    private void showRegisterScreen() {
        RegisterView registerView = new RegisterView(
            userStore,
            user -> showGameScreen(user),
            () -> showLoginScreen()
        );
        
        Scene scene = new Scene(registerView.getRoot(), 450, 450);
        primaryStage.setScene(scene);
    }

    private void showGameScreen(User user) {
        GameView gameView = new GameView(
            user,
            
            blackScore -> {
                try {
                    user.addScore(blackScore); 
                    userStore.save(user);      
                } catch (Exception e) {
                    System.err.println("Skor kaydedilemedi: " + e.getMessage());
                }
            },
            
            () -> showLoginScreen()
        );
        
        Scene scene = new Scene(gameView.getRoot(), 820, 520);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}