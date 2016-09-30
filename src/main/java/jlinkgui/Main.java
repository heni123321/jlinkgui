package jlinkgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Main extends Application {
    private static final String exeformat;
    private String jlink;
    private Path output;
    private TreeItem<String> jmods;
    private Set<Path> mlibs = new HashSet<>();
    private DirectoryChooser outputChooser;
    private DirectoryChooser mlibChooser;
    private ComboBox<String> comboBox;
    private StringProperty outputprop = new SimpleStringProperty(this, "output");
    private TreeView<String> tv;
	private boolean cfn;
    private static final String DOTSPLIT = Pattern.quote(".");

    static {
        String osname = System.getProperty("os.name");
        if (osname.contains("Windows")) {
            exeformat = ".exe";
        } else if (osname.contains("OSX")) {
            exeformat = ".dmg";
        } else {
            exeformat = "";
        }
    }

    @Override
    public void start(Stage mainStage) {
    	Controler.stage = mainStage;
    	Parent root = null;
		try {
			URL url = getClass().getResource("/seen.fxml");
			root = FXMLLoader.load(url);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
        
    	mainStage.setTitle("FXML Welcome");
    	mainStage.setScene(new Scene(root, 300, 275));
    	mainStage.show();
    	if (false) {
        int hitht = 100;
        int with = 100;
        jlink = System.getProperty("java.home") + File.separator + "bin" + File.separator + "jlink" + exeformat;
        Button mlib = new Button("mlib");
        mlib.setMinHeight(hitht);
        mlib.setMinWidth(with);
        mlib.setOnAction(e -> {
            File showDialog = mlibChooser.showDialog(mainStage);
            if (showDialog == null) {
				return;
			}
			Path path = showDialog.toPath();
            calculateModules(path);
            mlibs.add(path);
        });
        Button output = new Button("output");
        output.setMinHeight(hitht);
        output.setMinWidth(with);
        output.setOnAction(e -> this.output = outputChooser.showDialog(mainStage).toPath());
        comboBox = new ComboBox<>();
        comboBox.setDisable(true);
        comboBox.getItems().addAll("0", "1", "2");
        CheckBox compresion = new CheckBox("compression");
        compresion.setOnAction(e -> {
            if (compresion.isSelected()) {
                comboBox.setDisable(false);
            } else {
                comboBox.setDisable(true);
            }
        });
        Label l = new Label();
        l.textProperty().bind(outputprop);
        Button link = new Button("link");
        link.setMinHeight(hitht);
        link.setMinWidth(with);
        link.setOnAction(e -> {
            try {
                runprosses(getargs());
            } catch (Exception e1) {
                e1.printStackTrace();
                String collect = Stream.of(e1.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n"));
                l.setText(e1.toString() + "\n" + collect);
            }
        });
        jmods = new TreeItem<>();
        calculateModules(Paths.get(System.getProperty("java.home"), "jmods"));
        mlibs.add(Paths.get(System.getProperty("java.home"), "jmods"));
        jmods.setExpanded(true);
        tv = new TreeView<String>(jmods);
        tv.setShowRoot(false);
        tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox bottens = new VBox();
        bottens.getChildren().addAll(mlib, output, link);
        HBox hb = new HBox();
        VBox vb = new VBox();
        VBox options = new VBox(new HBox(compresion, comboBox));
        CheckBox cfn = new CheckBox("class-for-name");
        cfn.setOnAction(e -> this.cfn = cfn.isSelected());
        options.getChildren().add(cfn);
        hb.getChildren().addAll(bottens, tv, options);
        vb.getChildren().addAll(hb, l);
        Scene scene = new Scene(vb);
        mainStage.setScene(scene);
        mainStage.show();
    	}
    }

    private void calculateModules(Path path) {
        if (mlibs.contains(path)) {
            return;
        }
        TreeItem<String> item = new TreeItem<>(path.toString().substring(path.toString().lastIndexOf(File.separator) + 1));
        ModuleFinder mf = ModuleFinder.of(path);
        try {
            Field field = mf.getClass().getDeclaredField("isLinkPhase");
            field.setAccessible(true);
            field.set(mf, true);
            List<Item> items = parcestrings(mf.findAll().stream().map(mr -> mr.descriptor().name()).sorted().collect(Collectors.toList()));
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
                String[] split = name.split(DOTSPLIT);
                Item parent = null;
                for (int i = 0; i < split.length; i++) {
                    String itemName = split[i];
                    if (i == 0) {
                        currentItem = items.stream().filter(i2 -> i2.getName().equals(itemName)).findFirst().orElse(null);
                        if (currentItem == null) {
                            currentItem = new Item(itemName, i, i + 1 == split.length);
                            items.add(currentItem);
                        }
                    } else {
                        parent = currentItem;
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
        return items;
    }

    private List<String> getargs() {
        List<String> l = new ArrayList<>();
        l.add(jlink);
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

    public static void main(String[] args) {
        launch(args);
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
                ProcessBuilder pb = new ProcessBuilder(list);
                pb.directory(new File(System.getProperty("user.dir")));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("utf-8")));
                StringBuilder builder = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }
                s.close();
                outputprop.set(builder.toString());
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
