module demo.reversi {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;

    opens demo.reversi to javafx.fxml;
    
    exports demo.reversi;
    exports demo.reversi.ui;
    exports demo.reversi.game;
    exports demo.reversi.model;
    exports demo.reversi.persistence;
}