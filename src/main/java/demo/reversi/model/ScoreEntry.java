package demo.reversi.model;

import java.io.Serializable;

public class ScoreEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int points;
    private final String timestamp;

    public ScoreEntry(int points, String timestamp) {
        this.points = points;
        this.timestamp = timestamp;
    }

    public int getPoints() { return points; }
    public String getTimestamp() { return timestamp; }
}