package jlinkgui;

import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;

public class Controler {

    @FXML
	private TreeView<String> tv;
	private DirectoryChooser outputChooser;
    private DirectoryChooser mlibChooser;
	private TreeItem<String> jmods;
	public static Stage stage;
    private Set<Path> mlibs = new HashSet<>();
    private ToolProvider jlink = ToolProvider.findFirst("jlink").orElseThrow(() -> new RuntimeException("jlink not found"));
    private StringProperty outputprop = new SimpleStringProperty(this, "output");
	private boolean cfn;
    private Path output;
    @FXML
    private ComboBox<String> comboBox;
    @FXML
    private Label l;
    @FXML
    private ChoiceBox<String> compression;
    @FXML
    private CheckBox c;
    @FXML
    private CheckBox cl;


    @FXML
    void initialize() {
    	this.jmods = new TreeItem<>();
    	setup();
    	tv.setShowRoot(false);
    	tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    	tv.setRoot(jmods);
        l.textProperty().bind(outputprop);
    }

    
	public void addlib() {
		File showDialog = mlibChooser.showDialog(stage);
		if (showDialog == null) {
			return;
		}
		Path path = showDialog.toPath();
		calculateModules(path);
		mlibs.add(path);
	}
	
	public void setoutput() {
		this.output = outputChooser.showDialog(stage).toPath();
	}
	
	public void link() {
		try {
            runprosses(getargs());
        } catch (Exception e1) {
            e1.printStackTrace();
            String collect = Stream.of(e1.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n"));
            l.setText(e1.toString() + "\n" + collect);
        }
	}
	
	 @FXML
	 void compresion(ActionEvent event) {
		 compression.setDisable(c.isPressed());
	 }

	 @FXML
	 void cfnm(ActionEvent event) {
		 cfn = cl.isSelected();
	 }
	 
	private void calculateModules(Path path) {
		if (mlibs.contains(path)) {
			return;
		}
		TreeItem<String> item = new TreeItem<>(
				path.toString().substring(path.toString().lastIndexOf(File.separator) + 1));
		ModuleFinder mf = ModuleFinder.of(path);
		try {
			Field field = mf.getClass().getDeclaredField("isLinkPhase");
			field.setAccessible(true);
			field.set(mf, true);
			List<Item> items = parcestrings(
					mf.findAll().stream().map(mr -> mr.descriptor().name()).sorted().collect(Collectors.toList()));
			fillsubtree(items, item, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		jmods.getChildren().add(item);
	}

	private void fillsubtree(List<Item> items, TreeItem<String> titem, Item module) {
		if (module != null) {
			titem.getChildren().add(new TreeItem<String>(module.toString()));
		}
		Collections.sort(items);
		for (Item item : items) {
			if (item.getChildrens().size() == 0) {
				titem.getChildren().add(new TreeItem<String>(item.toString()));
			} else {
				TreeItem<String> ti = new TreeItem<String>(item.getName());
				titem.getChildren().add(ti);
				if (item.isModule()) {
					fillsubtree(item.getChildrens(), ti, item);
				} else {
					fillsubtree(item.getChildrens(), ti, null);
				}
			}
		}
	}

	private List<Item> parcestrings(List<String> moduleNames) {
		List<Item> items = new ArrayList<>();
		moduleNames.sort(null);
		for (String name : moduleNames) {
			Item currentItem = null;
			if (!name.contains(".")) {
				currentItem = new Item(name, 0, true);
				if (items.contains(currentItem))
					continue;
				else
					items.add(currentItem);
			} else {
				String[] split = name.split("\\.");
				Item parent = null;
				for (int i = 0; i < split.length; i++) {
					String itemName = split[i];
					if (i == 0) {
						currentItem = items.stream().filter(i2 -> i2.getName().equals(itemName)).findFirst()
								.orElse(null);
						if (currentItem == null) {
							currentItem = new Item(itemName, i, i + 1 == split.length);
							items.add(currentItem);
						}
					} else {
						parent = currentItem;
						currentItem = parent.getChildrens().stream().filter(i2 -> i2.getName().equals(itemName))
								.findFirst().orElse(null);
						if (currentItem == null) {
							currentItem = new Item(itemName, i, i + 1 == split.length);
							currentItem.setParent(parent);
							parent.getChildrens().add(currentItem);
						}
					}
				}
			}
		}
		return items;
	}

	private void setup() {
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Output");
		dc.setInitialDirectory(new File(System.getProperty("user.dir")));
		outputChooser = dc;
		DirectoryChooser dc1 = new DirectoryChooser();
		dc1.setTitle("Output");
		dc1.setInitialDirectory(new File(System.getProperty("java.home")));
		mlibChooser = dc1;
	}
	
	private List<String> getargs() {
        List<String> l = new ArrayList<>();
        l.add("--module-path");
        l.add(mlibs.stream().map(Path::toString).collect(Collectors.joining(";")));
        l.add("--add-modules");
        if (tv.getSelectionModel().getSelectedItems().size() == 1) {
            l.add(tv.getSelectionModel().getSelectedItems().get(0).getValue());
        } else {
            l.add(tv.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.joining(",")));
        }
        l.add("--output");
        try {
			Files.delete(output);
		} catch (IOException e) {
		}
        l.add(output.toString());
        if (!comboBox.isDisable()) {
            l.add("--compress");
            l.add(comboBox.getValue());
        }
        if (cfn) {
			l.add("--class-for-name");
		}
        return l;
    }

    private Stage s;

    public void runprosses(List<String> list) throws IOException {
        s = new Stage();
        Label label = new Label();
        label.setText("arbejter...");
        VBox vBox = new VBox(label);
        Scene seen = new Scene(vBox, 100, 100);
        s.setScene(seen);
        s.show();
        Platform.runLater(() -> {
            try {
            	StringWriter writer = new StringWriter();
            	PrintWriter pw = new PrintWriter(writer);
            	jlink.run(pw, pw, list.toArray(new String[0]));
                s.close();
                outputprop.set(pw.toString());
            } catch (Exception e) {
                this.outputprop.set(e.getMessage()+ System.getProperty("line.separator") + collectStackTraces(e));
            }

        });
    }

    private String collectStackTraces(Throwable throwable) {
        Writer writer = new StringWriter(1024);
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        printWriter.write(System.getProperty("line.separator"));
        return writer.toString();
    }
}
