module jlinkgui {
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	requires java.desktop;
	exports jlinkgui to javafx.graphics, javafx.fxml;
	opens jlinkgui to javafx.fxml;
}