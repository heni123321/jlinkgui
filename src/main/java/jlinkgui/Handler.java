package jlinkgui;

import javafx.scene.control.cell.TextFieldTreeCell;

import javafx.scene.control.ContextMenu;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.scene.control.*;

public class Handler extends TextFieldTreeCell<String> {

    @Override
    public void updateItem(String arg0, boolean arg1) {
        super.updateItem(arg0, arg1);
        if (Controler.MAP.get(getTreeItem()) != null) {
            ContextMenu c = new ContextMenu();
            MenuItem m = new MenuItem();
            m.setText("Open folder");
            m.setOnAction(e -> {
                try {
                    Desktop.getDesktop().open(Controler.MAP.get(getTreeItem()).toFile());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            c.getItems().add(m);
            setContextMenu(c);
        }
        if (Controler.MOD.contains(getTreeItem())) {
            ContextMenu c = new ContextMenu();
            MenuItem m = new MenuItem();
            m.setText("Search");
            m.setOnAction(e -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.google.dk/webhp?#q=" + getTreeItem().getValue().replace(' ', '+')));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            });
            c.getItems().add(m);
            setContextMenu(c);
        }

    }
}
