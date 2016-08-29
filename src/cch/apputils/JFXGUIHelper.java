/*
 * The MIT License
 *
 * Copyright 2016 CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package cch.apputils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Collection of utility methods for JavaFX apps
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public class JFXGUIHelper {
	
	/** Static helper class, do not invoke */
	private JFXGUIHelper(){
		throw new UnsupportedOperationException("Static helper library "+this.getClass()+" cannot be instantiated.");
	}
	/**
	 * Loads an FXML document and gets its controller class instance. Requires that the controller class be in the same code source as the FXML document.
	 * @param <T> Template type for controller class
	 * @param controllerClass The expected class for the controller of the FXML document
	 * @param fxmlPath The resource path of the FXML document (e.g. <code>Paths.get("com","myapp","MainWindow.fxml")</code> )
	 * @param contentRootPane Atomic reference instance so that the root pane of the FXML GUI can be returned
	 * @return Returns the controller object instance
	 */
	public static <T> T loadFXML(Class<? extends T> controllerClass, Path fxmlPath, AtomicReference<Parent> contentRootPane){
		return loadFXML(controllerClass, AppHelper.delimitPath(fxmlPath,"/"), contentRootPane);
	}
	/**
	 * Loads an FXML document and gets its controller class instance. Requires that the controller class be in the same code source as the FXML document.
	 * @param <T> Template type for controller class
	 * @param controllerClass The expected class for the controller of the FXML document
	 * @param fxmlPath The resource path of the FXML document (e.g. "/com/myapp/MainWindow.fxml")
	 * @param contentRootPane Atomic reference instance so that the root pane of the FXML GUI can be returned
	 * @return Returns the controller object instance
	 */
	public static <T> T loadFXML(Class<? extends T> controllerClass, String fxmlPath, AtomicReference<Parent> contentRootPane){
		try{
			FXMLLoader loader = new FXMLLoader(controllerClass.getResource(fxmlPath));
			Parent root = loader.load();
			contentRootPane.set(root);
			T controller = loader.getController();
			return controller;
		}catch(IOException ex){
			throw new RuntimeException("Unchecked exception was thrown: "+ex.getClass().getSimpleName(), ex);
		}
	}
	/**
	 * Changes the contents of a JavaFX stage (a stage is basically a window) and then shows the stage.
	 * @param windowStage The stage instance to set
	 * @param contentRootPane The new GUI contents to put into the stage
	 */
	public static void setStageContents(Stage windowStage, Parent contentRootPane) {
		windowStage.setScene(new Scene(contentRootPane));
		windowStage.sizeToScene();
		windowStage.show();
	}
	
	/**
	 * Generates a modal error pop-up window
	 * @param message Error message
	 * @param error The error to show
	 */
	public static void showErrorMessage(String message, Throwable error){
		StringBuilder st = new StringBuilder();
		boolean first = true;
		while(error != null){
			if(!first){
				st.append("Caused by ").append(error.getClass().getName()).append(": ");
			}
			st.append(error.getLocalizedMessage()).append("\n");
			for(StackTraceElement e : error.getStackTrace()){
				st.append("\t").append(e.toString()).append("\n");
			}
			error = error.getCause();
			st.append("\n");
			first = false;
		}
		showMessageDialog("Error",message,st.toString(), null);
	}
	public static void showErrorMessage(String message, String details){
		showMessageDialog("Error",message,details, null);
	}
	/**
	 * Generates a modal error pop-up window
	 * @param title Pop-up title
	 * @param message The error message
	 * @param details Description (or stack-trace) of the error
	 * @param owningStage The owner of this created stage (can be null)
	 */
	public static void showMessageDialog(String title, String message, String details, Stage owningStage){ // owningStage can be null
		VBox root = new VBox();
		root.setPadding(new Insets(8));
		root.setSpacing(8);
		Label msg = new Label(message);
		msg.setWrapText(true);
		msg.setPrefWidth(400);
		root.getChildren().add(msg);
		TextArea dtl = new TextArea();
		dtl.setPrefSize(400, 250);
		dtl.setEditable(false);
		dtl.setStyle("-fx-font-family: monospace;");
		dtl.setText(details);
		dtl.setWrapText(true);
		VBox.setVgrow(dtl, Priority.ALWAYS);
		root.getChildren().add(dtl);
		Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(owningStage);
		dialog.setScene(new Scene(root));
		dialog.setTitle(title);
		dialog.showAndWait();
	}
	/**
	 * Creates a modal pop-up progress bar window with a background thread to 
	 * automatically update the progress bar from the values stored in the 
	 * AtomicAdder instances <code>progressTracker</code> and 
	 * <code>maxProgress</code>. The window closes and the background thread 
	 * shuts down when the AtomicBoolean instance <code>taskDone</code> is set 
	 * to <code>true</code>.
	 * @param title The title of the window
	 * @param message The message in the window
	 * @param progressTracker An instance of <code>DoubleAdder</code> whose value at any given moment represents progress towards the final goal.
	 * @param maxProgress An instance of <code>DoubleAdder</code> whose value at any given moment represents the final progress goal.
	 * @param taskDone 
	 * @param owner The owner of this created stage (can be null)
	 */
	public static void showProgressBar(final String title, final String message, final DoubleAdder progressTracker, final DoubleAdder maxProgress, final AtomicBoolean taskDone, final Stage owner){
		
		VBox root = new VBox();
		root.setPadding(new Insets(8));
		root.setSpacing(8);
		Label msg = new Label(message);
		msg.setWrapText(true);
		msg.setPrefWidth(400);
		root.getChildren().add(msg);
		final ProgressBar pbar = new ProgressBar();
		pbar.setPrefWidth(400);
		root.getChildren().add(pbar);
		Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(owner);
		dialog.initStyle(StageStyle.UNDECORATED);
		dialog.setScene(new Scene(root));
		dialog.setTitle(title);
		
		Thread updateThread = new Thread(()->{
			while (!taskDone.get() ) {
				javafx.application.Platform.runLater(() -> {
					pbar.setProgress(progressTracker.doubleValue() / maxProgress.doubleValue());
				});
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					break;
				}
			}
			javafx.application.Platform.runLater(() -> {dialog.close();});
		});
		updateThread.setDaemon(true);
		updateThread.start();
		dialog.show();
	}
	
	/**
	 * Creates a pop-up window asking the user to confirm something
	 * @param title Title of the window
	 * @param message The Yes/No question to confirm
	 * @param yesString The text on the button that means "yes"
	 * @param noString The text on the button that means "no"
	 * @param owner The owner of this created stage (can be null)
	 * @return true if the user clicked "yes", false otherwise
	 */
	public static boolean getConfirmationYesNo(String title, String message, String yesString, String noString, Stage owner){
		AtomicBoolean result = new AtomicBoolean(false);
		final Stage dialog = new Stage();
		VBox root = new VBox();
		root.setPadding(new Insets(8));
		root.setSpacing(8);
		Label msg = new Label(message);
		msg.setWrapText(true);
		msg.setPrefWidth(400);
		root.getChildren().add(msg);
		HBox buttonRow = new HBox();
		buttonRow.setPadding(new Insets(8));
		buttonRow.setSpacing(32);
		Button yesButton = new Button(yesString);
		Button noButton = new Button(noString);
		yesButton.setOnAction((ActionEvent ae)->{
			result.set(true);
			dialog.close();
		});
		noButton.setOnAction((ActionEvent ae)->{
			result.set(false);
			dialog.close();
		});
		buttonRow.getChildren().addAll(yesButton,noButton);
		root.getChildren().add(buttonRow);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(owner);
		dialog.setScene(new Scene(root));
		dialog.setTitle(title);
		dialog.showAndWait();
		return result.get();
	}
	
	/**
	 * Creates a pop-up window asking the user for a number
	 * @param title The window title
	 * @param message The message on the window
	 * @param defaultValue The default value
	 * @param okString The text on the "OK" button
	 * @param cancelString The text on the "Cancel" button
	 * @param owner The owner of this created stage (can be null)
	 * @return A Number instance representing the user input or the default, or null if the user input could not be parsed as a number.
	 */
	public static Number getNumberFromUser(String title, String message, Number defaultValue, String okString, String cancelString, Stage owner){
		final AtomicReference<Number> result = new AtomicReference<>(defaultValue);
		final Stage dialog = new Stage();
		VBox root = new VBox();
		root.setPadding(new Insets(8));
		root.setSpacing(8);
		Label msg = new Label(message);
		msg.setWrapText(true);
		msg.setPrefWidth(400);
		root.getChildren().add(msg);
		final TextField inputField = new TextField(String.valueOf(defaultValue));
		inputField.textProperty().addListener((ChangeListener<String>)(ObservableValue<? extends String> observable, String oldValue, String newValue)->{
				if(newValue.equals(oldValue)) return; // not really a change, prevents infinite recursion
				try{
					Integer.parseInt(newValue);
				}catch(NumberFormatException nfe){
					inputField.textProperty().set(oldValue);
				}
			});
		inputField.setOnAction((ActionEvent ae)->{
			String content = inputField.getText().trim();
			try{
				Long integer = Long.parseLong(content);
				result.set(integer);
			} catch(NumberFormatException nfe){
				// ignore exception, try againas double
				try {
					Double d = Double.parseDouble(content);
					result.set(d);
				} catch (NumberFormatException nfe2) {
					// Not a number
					result.set(null);
				}
			}
			dialog.close();
		});
		root.getChildren().add(inputField);
		HBox buttonRow = new HBox();
		buttonRow.setPadding(new Insets(8));
		buttonRow.setSpacing(32);
		Button yesButton = new Button(okString);
		Button noButton = new Button(cancelString);
		yesButton.setOnAction((ActionEvent ae)->{
			String content = inputField.getText().trim();
			try{
				Long integer = Long.parseLong(content);
				result.set(integer);
			} catch(NumberFormatException nfe){
				// ignore exception, try againas double
				Double d = Double.parseDouble(content);
				result.set(d);
			}
			dialog.close();
		});
		noButton.setOnAction((ActionEvent ae)->{
			result.set(defaultValue);
			dialog.close();
		});
		buttonRow.getChildren().addAll(yesButton,noButton);
		root.getChildren().add(buttonRow);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(owner);
		dialog.setScene(new Scene(root));
		dialog.setTitle(title);
		dialog.showAndWait();
		return result.get();
	}
}

