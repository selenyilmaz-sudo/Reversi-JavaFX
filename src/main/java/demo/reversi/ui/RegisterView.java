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

import java.util.Locale;
import java.util.function.Consumer;

public class RegisterView {
    private final VBox root;

    public RegisterView(UserStore userStore, Consumer<User> registrationHandler, Runnable backHandler) {
        Label titleLabel = new Label("Yeni Kullanıcı Kaydı");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        TextField nameField = new TextField();
        nameField.setPromptText("Adınız");
        nameField.setMaxWidth(250);
        
        TextField surnameField = new TextField();
        surnameField.setPromptText("Soyadınız");
        surnameField.setMaxWidth(250);
        
        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Kullanıcı Adı (Nickname)");
        nicknameField.setMaxWidth(250);
        
        TextField emailField = new TextField();
        emailField.setPromptText("E-posta");
        emailField.setMaxWidth(250);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Şifre");
        passwordField.setMaxWidth(250);
        
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        Button saveButton = new Button("Kayıt Ol");
        saveButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Button backButton = new Button("Geri Dön");
        
        HBox buttons = new HBox(15, saveButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        root = new VBox(12, titleLabel, nameField, surnameField, nicknameField, emailField, passwordField, buttons, messageLabel);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        saveButton.setOnAction(e -> {
            try {
                String email = emailField.getText().trim().toLowerCase(Locale.ROOT);
                if (nameField.getText().isBlank() || surnameField.getText().isBlank() || 
                    nicknameField.getText().isBlank() || email.isBlank() || passwordField.getText().isBlank()) {
                    messageLabel.setText("Lütfen tüm alanları doldurun.");
                    return;
                }
                if (!email.contains("@")) {
                    messageLabel.setText("Geçerli bir e-posta adresi girin.");
                    return;
                }
                if (userStore.findByEmail(email) != null) {
                    messageLabel.setText("Bu e-posta adresi zaten kayıtlı.");
                    return;
                }

                User user = new User(
                        nameField.getText().trim(),
                        surnameField.getText().trim(),
                        nicknameField.getText().trim(),
                        email,
                        PasswordHasher.hash(passwordField.getText())
                );
                
                userStore.save(user);
                registrationHandler.accept(user);
            } catch (Exception ex) {
                messageLabel.setText("Kayıt hatası: " + ex.getMessage());
            }
        });

        backButton.setOnAction(e -> backHandler.run());
    }

    public Parent getRoot() {
        return root;
    }
}