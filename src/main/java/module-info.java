module jlinkgui {
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	exports jlinkgui to javafx.graphics, javafx.fxml;
}