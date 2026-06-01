package demo.reversi.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_SCORES = 3;

    private final String name;
    private final String surname;
    private final String nickname;
    private final String email;
    private final String passwordHash;
    private final List<ScoreEntry> highScores = new ArrayList<>();

    public User(String name, String surname, String nickname, String email, String passwordHash) {
        this.name = name;
        this.surname = surname;
        this.nickname = nickname;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }

    public List<ScoreEntry> getHighScores() {
        return Collections.unmodifiableList(highScores);
    }

    public void addScore(int points) {
        highScores.add(new ScoreEntry(
                points,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
        highScores.sort(Comparator.comparingInt(ScoreEntry::getPoints).reversed());
        while (highScores.size() > MAX_SCORES) {
            highScores.remove(highScores.size() - 1);
        }
    }
}