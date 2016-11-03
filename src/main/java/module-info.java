module jlinkgui {
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	//have to export this to all becouse fxmlloader is invoking the controler from the unamed module
	exports jlinkgui;
}