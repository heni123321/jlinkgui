package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
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
	private File jlink;
	private File output;
	private TreeItem<String> jmods;
	private Set<Path> mlibs = new HashSet<>();
	private FileChooser jlinkChooser;
	private DirectoryChooser outputChooser;
	private DirectoryChooser mlibChooser;
	private String compression;
	private ComboBox<String> comboBox;
	private StringProperty outputprop = new SimpleStringProperty(this, "output");
	private TreeView<String> tv;
	private static final String DOTSPLIT = Pattern.quote(".");
	
	static {
		String osname = System.getProperty("os.name");
		if (osname.contains("Windows")) {
			exeformat = ".exe";
		} else {
			exeformat = "";
		}
	}

	@Override
	public void start(Stage mainStage) {
		int hitht=100;
		int with=100;
		setup();
		/*Button jlink = new Button("jlink");
		jlink.setMinHeight(hitht);
		jlink.setMinWidth(with);
		jlink.setOnAction(e -> this.jlink = jlinkChooser.showOpenDialog(mainStage));*/
		jlink = new File(System.getProperty("java.home"), "bin\\jlink" + exeformat);
		Button mlib = new Button("mlib");
		mlib.setMinHeight(hitht);
		mlib.setMinWidth(with);
		mlib.setOnAction(e -> {
			Path path = mlibChooser.showDialog(mainStage).toPath();
			calculateModules(path);
			mlibs.add(path);
		});
		Button output = new Button("output");
		output.setMinHeight(hitht);
		output.setMinWidth(with);
		output.setOnAction(e ->	this.output = outputChooser.showDialog(mainStage));
		comboBox = new ComboBox<>();
		comboBox.setDisable(true);
		comboBox.getItems().addAll("0", "1", "2");
		CheckBox box = new CheckBox("compression");
		box.setOnAction(e -> {
			if (box.isSelected()) {
				compression = "on";
				comboBox.setDisable(false);
			} else {
				compression = "off";
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
				//l.setText(runprosses(getargs()));
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
		bottens.getChildren().addAll(mlib, /*jlink,*/ output, link);
		HBox hb = new HBox();
		VBox vb = new VBox();
		hb.getChildren().addAll(bottens, tv, box, comboBox);
		vb.getChildren().addAll(hb, l);
		Scene scene = new Scene(vb);
		mainStage.setScene(scene);
		mainStage.show();
	}
	
	private void calculateModules(Path path) {
		if (mlibs.contains(path)) {
			return;
		}
		TreeItem<String> item = new TreeItem<>(path.toString()
		.substring(path.toString().lastIndexOf(File.separator)+1));
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
			titem.getChildren().add(new TreeItem<String>(module.getFullName()));
		}
		Collections.sort(items);
		for (Item item : items) {
			if (item.getChildens().size() == 0) {
				titem.getChildren().add(new TreeItem<String>(item.getFullName()));
			} else {
				TreeItem<String> ti = new TreeItem<String>(item.getName());
				titem.getChildren().add(ti);
				if (item.isModule()) {
					fillsubtree(item.getChildens(), ti, item);					
				} else {
					fillsubtree(item.getChildens(), ti, null);
				}
			}
		}
	}

	private List<Item> parcestrings(List<String> moduleNames) {
		List<Item> items = new ArrayList<>();
		for (String name : moduleNames) {
			Item currentItem = null;
			if (!name.contains(".")) {
				currentItem = new Item(name, 0, true);
				if(items.contains(currentItem))
					continue;
				else
					items.add(currentItem);
			} else {
				String[] split = name.split(DOTSPLIT);
				List<Item> itemColl = items;
				Item parent = null;
				
				for (int i = 0; i < split.length; i++) {
					String itemName = split[i];

					if(i == 0){
						currentItem = items.stream().filter(i2 -> i2.getName().equals(itemName)).findFirst().orElse(null);
						if(currentItem == null) {
							currentItem = new Item(itemName,i, i+1 == split.length);
							items.add(currentItem);
						}
						
					} else {
						parent = currentItem;
						currentItem = parent.getChildens().stream().filter(i2 -> i2.getName().equals(itemName)).findFirst().orElse(null);
						if(currentItem == null) {
							currentItem = new Item(itemName,i, i+1 == split.length);
							currentItem.setParent(parent);
							parent.getChildens().add(currentItem);
						}
					}

					
				}
			}
		}
		return items;
	}

	private List<String> getargs() {
		List<String> l = new ArrayList<>();
		l.add(jlink.toString());
		l.add("--modulepath");
		l.add(mlibs.stream().map(Path::toString).collect(Collectors.joining(";")));
		l.add("--addmods");
		if (tv.getSelectionModel().getSelectedItems().size() == 1) {
			l.add(tv.getSelectionModel().getSelectedItems().get(0).getValue());
		} else {
			l.add(tv.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.joining(",")));
		}
		l.add("--output");
		l.add(output.toString());
		l.add("--compress-resources");
		l.add(compression);
		if (!comboBox.isDisable()) {
			l.add("--compress-resources-level");
			l.add(comboBox.getValue());
		}
		return l;
	}

	private void setup() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getenv("JAVA_HOME")));
		fileChooser.setTitle("Jlink");
		fileChooser.getExtensionFilters().addAll(
		        new ExtensionFilter("exe Files", "*.exe"),
		        new ExtensionFilter("All Files", "*.*"));
		jlinkChooser = fileChooser;
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Output");
		dc.setInitialDirectory(new File(System.getProperty("user.dir")));
		outputChooser = dc;
		DirectoryChooser dc1 = new DirectoryChooser();
		dc1.setTitle("Output");
		dc1.setInitialDirectory(new File(System.getProperty("java.home")));
		mlibChooser = dc1;
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
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ( (line = reader.readLine()) != null) {
				   builder.append(line);
				   builder.append(System.getProperty("line.separator"));
				}
				s.close();
				outputprop.set(builder.toString());
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				e.printStackTrace();
				this.outputprop.set(e.getMessage());
			}
			
		});
	}
	
}
