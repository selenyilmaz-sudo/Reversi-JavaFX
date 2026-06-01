package demo.reversi.ui;

import demo.reversi.model.User;
import demo.reversi.persistence.PasswordHasher;
import demo.reversi.persistence.UserStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

public class LoginView {
    private final VBox root;

    public LoginView(UserStore userStore, Consumer<User> loginHandler, Runnable registerHandler) {
        Label titleLabel = new Label("Reversi Giriş");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        TextField emailField = new TextField();
        emailField.setPromptText("E-posta adresiniz");
        emailField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Şifreniz");
        passwordField.setMaxWidth(250);

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;"); // Hata mesajları kırmızı görünsün

        Button loginButton = new Button("Giriş Yap");
        
        emailField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });

        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                loginButton.fire();
            }
        });
        
        Button registerButton = new Button("Yeni Kayıt Oluştur");
        
        
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        
        HBox buttons = new HBox(15, loginButton, registerButton);
        buttons.setAlignment(Pos.CENTER);

        root = new VBox(15, titleLabel, emailField, passwordField, buttons, messageLabel);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);

        loginButton.setOnAction(e -> {
            try {
                String email = emailField.getText().trim();
                String password = passwordField.getText();
                
                if (email.isEmpty() || password.isEmpty()) {
                    messageLabel.setText("Lütfen e-posta ve şifrenizi girin.");
                    return;
                }

                User user = userStore.findByEmail(email);
                if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
                    messageLabel.setText("Hatalı e-posta veya şifre!");
                    return;
                }
                
                loginHandler.accept(user);
            } catch (Exception ex) {
                messageLabel.setText("Giriş hatası: " + ex.getMessage());
            }
        });

        registerButton.setOnAction(e -> registerHandler.run());
    }

    public Parent getRoot() {
        return root;
    }
}