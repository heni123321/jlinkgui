package jlinkgui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

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

    private static final String JAVA_HOME = System.getProperty("java.home");
	private DirectoryChooser outputChooser;
    private DirectoryChooser mlibChooser;
    private TreeItem<String> jmods;
    static Stage stage;
    private final Set<Path> mlibs = new HashSet<>();
    private final ToolProvider jlink = ToolProvider.findFirst("jlink").orElseThrow(() -> new RuntimeException("jlink not found"));
    private final StringProperty outputprop = new SimpleStringProperty(this, "output");
    private boolean cfn;
    private Path output;
    static final Map<TreeItem<String>, Path> MAP = new HashMap<>();
    static final List<TreeItem<String>> MOD = new ArrayList<>();
    @FXML
    private Label l;
    @FXML
    private ChoiceBox<String> compression;
    @FXML
    private CheckBox c;
    @FXML
    private CheckBox cl;
    @FXML
    private CheckBox vmb;
    @FXML
    private ChoiceBox<String> vm;
    @FXML
    private TreeView<String> tv;

    @FXML
    void initialize() {
        this.jmods = new TreeItem<>();
        setup();
        tv.setShowRoot(false);
        tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tv.setRoot(jmods);
        l.textProperty().bind(outputprop);
        calculateModules(Paths.get(JAVA_HOME, "jmods"));
        mlibs.add(Paths.get(JAVA_HOME, "jmods"));
        tv.setCellFactory(e -> new Handler());
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
        File file = outputChooser.showDialog(stage);
        if (file == null) {
            return;
        }
        this.output = file.toPath();
    }

    public void link() {
        if (output == null || tv.getSelectionModel().getSelectedItems().isEmpty()) {
            return;
        }
        runprosses(getargs());
    }

    @FXML
    void compresion(ActionEvent event) {
        compression.setDisable(c.isPressed());
    }

    @FXML
    void cfn(ActionEvent event) {
        cfn = cl.isSelected();
    }

    @FXML
    void vm(ActionEvent event) {
        vm.setDisable(vmb.isPressed());
    }

    private void calculateModules(Path path) {
        if (mlibs.contains(path)) {
            return;
        }
        TreeItem<String> item = new TreeItem<>(path.toString().substring(path.toString().lastIndexOf(File.separator) + 1));
        MAP.put(item, path);
        ModuleFinder mf = ModuleFinder.of(path);
        try {
            Field field = mf.getClass().getDeclaredField("isLinkPhase");
            field.setAccessible(true);
            field.set(mf, true);
            List<Item> items = parcestrings(mf.findAll().stream().map(mr -> mr.descriptor().name()).sorted().collect(Collectors.toList()));
            fillsubtree(items, item, null);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            outputprop.set(e.getMessage());
        }
        MOD.add(item);
        jmods.getChildren().add(item);
    }

    private void fillsubtree(List<Item> items, TreeItem<String> titem, Item module) {
        if (module != null) {
            TreeItem<String> treeItem = new TreeItem<>(module.toString());
            MOD.add(treeItem);
            titem.getChildren().add(treeItem);
        }
        Collections.sort(items);
        items.forEach(item -> {
            if (item.getChildrens().isEmpty()) {
                TreeItem<String> treeItem = new TreeItem<>(item.toString());
                MOD.add(treeItem);
                titem.getChildren().add(treeItem);
            } else {
                TreeItem<String> ti = new TreeItem<>(item.getName());
                titem.getChildren().add(ti);
                if (item.isModule()) {
                    fillsubtree(item.getChildrens(), ti, item);
                } else {
                    fillsubtree(item.getChildrens(), ti, null);
                }
            }
        });
    }

    private List<Item> parcestrings(List<String> moduleNames) {
        List<Item> items = new ArrayList<>();
        moduleNames.sort(null);
        moduleNames.forEach(name -> {
            Item currentItem = null;
            if (!name.contains(".")) {
                currentItem = new Item(name, 0, true);
                if (!items.contains(currentItem))
                    items.add(currentItem);
            } else {
                String[] split = name.split("\\.");
                for (int i = 0; i < split.length; i++) {
                    String itemName = split[i];
                    if (i == 0) {
                        currentItem = items.stream().filter(i2 -> i2.getName().equals(itemName)).findFirst().orElse(null);
                        if (currentItem == null) {
                            currentItem = new Item(itemName, i, i + 1 == split.length);
                            items.add(currentItem);
                        }
                    } else {
                        Item parent = currentItem;
                        if (parent != null) {
                            currentItem = parent.getChildrens().stream().filter(i2 -> i2.getName().equals(itemName)).findFirst().orElse(null);
                            if (currentItem == null) {
                                currentItem = new Item(itemName, i, i + 1 == split.length);
                                currentItem.setParent(parent);
                                parent.getChildrens().add(currentItem);
                            }
                        }
                    }
                }
            }
        });
        return items;
    }

    private void setup() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Output");
        dc.setInitialDirectory(new File(System.getProperty("user.dir")));
        outputChooser = dc;
        DirectoryChooser dc1 = new DirectoryChooser();
        dc1.setTitle("Output");
        dc1.setInitialDirectory(new File(JAVA_HOME));
        mlibChooser = dc1;
    }

    private List<String> getargs() {
        List<String> list = new ArrayList<>();
        list.add("--module-path");
        list.add(mlibs.stream().map(Path::toString).collect(Collectors.joining(";")));
        list.add("--add-modules");
        if (tv.getSelectionModel().getSelectedItems().size() == 1) {
            list.add(tv.getSelectionModel().getSelectedItems().get(0).getValue());
        } else {
            list.add(tv.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.joining(",")));
        }
        list.add("--output");
        try {
            Files.delete(output);
        } catch (IOException e) {
        	outputprop.set(e.getMessage());
        }
        list.add(output.toString());
        if (!compression.isDisable()) {
            list.add("--compress");
            list.add(compression.getValue());
        }
        if (cfn) {
            list.add("--class-for-name");
        }
        if (!vm.isDisable()) {
            list.add("--vm");
            list.add(vm.getValue());
        }
        return list;
    }

    public void runprosses(List<String> list) {
    	Stage s = new Stage();
        Label label = new Label();
        label.setText("working...");
        VBox vBox = new VBox(label);
        Scene seen = new Scene(vBox, 100, 100);
        s.setScene(seen);
        s.show();
        Platform.runLater(() -> {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            jlink.run(pw, pw, list.toArray(new String[0]));
            s.close();
            outputprop.set(writer.toString());

        });
    }

}
