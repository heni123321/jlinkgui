module jlinkgui {
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	requires java.desktop;
	//have to export this to all because FXMLLoader is invoking methods from the unnamed module
	exports jlinkgui;
}