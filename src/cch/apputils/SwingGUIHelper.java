/*
 * The MIT License
 *
 * Copyright 2016 CCHall 
 * <a href="mailto:explosivegnome@yahoo.com">explosivegnome@yahoo.com</a>.
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

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public class SwingGUIHelper {
	/** Static helper class, do not invoke */
	private SwingGUIHelper(){
		throw new UnsupportedOperationException("Static helper library "+this.getClass()+" cannot be instantiated.");
	}
	/**
	 * Asks the user if they're sure that they want to do something.
	 * @param message The message to display in a pop-up window
	 * @return <code>true</code> if the user clicked "yes", false otherwise.
	 */
	public static boolean confirmUserIntent(String message){
		String title = "Are you sure?";
		int action = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		return (action == JOptionPane.YES_OPTION);
	}
	/**
	 * Shows a message in a pop-up window
	 * @param string Message to show in a popup
	 */
	public static void showInfoPopup(String string) {
		JOptionPane.showMessageDialog(null, string);
	}
	
	/**
	 * Asks the user for a folder via a JFileChooser pop-up. 
	 * @param rootDir Location where the file chooser first looks
	 * @return Returns the selected folder, or null if the operation was canceled.
	 */
	public static Path askForFolder(Path rootDir){
		return askForFolder("Select a folder",rootDir);
	}
	
	/**
	 * Asks the user for a folder via a JFileChooser pop-up. 
	 * @param title Window title
	 * @param rootDir Location where the file chooser first looks
	 * @return Returns the selected folder, or null if the operation was canceled.
	 */
	public static Path askForFolder(String title, Path rootDir){
		JFileChooser jfc = new JFileChooser(rootDir.toFile());
		jfc.setDialogTitle(title);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int action = jfc.showOpenDialog(null);
		if(action == JFileChooser.CANCEL_OPTION) return null;
		if(jfc.getSelectedFile() == null) return null;
		return jfc.getSelectedFile().toPath();
	}
	
	
	/**
	 * Asks the user for a file via a JFileChooser pop-up. 
	 * @param rootDir Location where the file chooser first looks
	 * @param saveFile If true, then uses a save-file dialog instead of an 
	 * open-file dialog.
	 * @return Returns the selected file, or null if the operation was canceled.
	 */
	public static Path askForFile(Path rootDir, boolean saveFile){
		return askForFile("Select a file",rootDir, saveFile);
	}
	/**
	 * Asks the user for a file via a JFileChooser pop-up. 
	 * @param title Window title
	 * @param rootDir Location where the file chooser first looks
	 * @param saveFile If true, then uses a save-file dialog instead of an 
	 * open-file dialog.
	 * @return Returns the selected file, or null if the operation was canceled.
	 */
	public static Path askForFile(String title,Path rootDir, boolean saveFile){
		JFileChooser jfc = new JFileChooser(rootDir.toFile());
		jfc.setDialogTitle(title);
		int action;
		if(saveFile){
			action = jfc.showSaveDialog(null);
		} else {
			action = jfc.showOpenDialog(null);
		}
		if(action == JFileChooser.CANCEL_OPTION) return null;
		if(jfc.getSelectedFile() == null) return null;
		if(saveFile && jfc.getSelectedFile().exists()){
			// are you sure?
			if(!confirmUserIntent("Overwrite file '"+jfc.getSelectedFile()+"'?")){
				return null;
			}
		}
		return jfc.getSelectedFile().toPath();
	}
	
	/**
	 * Asks the user for multiple files via a JFileChooser pop-up. 
	 * @param rootDir Location where the file chooser first looks
	 * @return Returns the selected files, or null if the operation was canceled.
	 */
	public static Path[] askForFiles(Path rootDir){
		return askForFiles("Select files",rootDir);
	}
	
	/**
	 * Asks the user for multiple files via a JFileChooser pop-up. 
	 * @param title Window title
	 * @param rootDir Location where the file chooser first looks
	 * @return Returns the selected files, or null if the operation was canceled.
	 */
	public static Path[] askForFiles(String title, Path rootDir){
		JFileChooser jfc = new JFileChooser(rootDir.toFile());
		jfc.setDialogTitle(title);
		jfc.setMultiSelectionEnabled(true);
		int action = jfc.showOpenDialog(null);
		if(action == JFileChooser.CANCEL_OPTION) return null;
		return filesToPaths(jfc.getSelectedFiles());
	}
	
	/**
	 * Asks the user for a file via a JFileChooser pop-up. 
	 * @param title Window title
	 * @param rootDir Location where the file chooser first looks
	 * @param saveFile If true, then uses a save-file dialog instead of an 
	 * open-file dialog.
	 * @param allowedSuffixes Array of valid file extensions
	 * @return Returns the selected file, or null if the operation was canceled.
	 */
	public static Path askForFile(String title, Path rootDir, boolean saveFile, String... allowedSuffixes){
		final String[] suffixes = allowedSuffixes;
		final String description = Arrays.toString(suffixes);
		JFileChooser jfc = new JFileChooser(rootDir.toFile());
		jfc.setDialogTitle(title);
		jfc.setFileFilter(new FileFilter(){

			@Override
			public boolean accept(File f) {
				if(f.isDirectory())return true;
				if(f.getName().contains(".") == false) return false;
				String suffix = f.getName().substring(f.getName().lastIndexOf("."));
				for(String s : suffixes){
					if(suffix.equalsIgnoreCase(s)) return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return description;
			}
		});
		int action;
		if(saveFile){
			action = jfc.showSaveDialog(null);
		} else {
			action = jfc.showOpenDialog(null);
		}
		if(action == JFileChooser.CANCEL_OPTION) return null;
		if(jfc.getSelectedFile() == null) return null;
		if(jfc.getSelectedFile().isDirectory()) return null;
		if(saveFile && jfc.getSelectedFile().exists()){
			// are you sure?
			if(!confirmUserIntent("Overwrite file '"+jfc.getSelectedFile()+"'?")){
				return null;
			}
		}
		return jfc.getSelectedFile().toPath();
	}
	
	/**
	 * Asks the user for multiple files via a JFileChooser pop-up. 
	 * @param title Window title
	 * @param rootDir Location where the file chooser first looks
	 * @param allowedSuffixes Array of valid file extensions
	 * @return Returns the selected files, or null if the operation was canceled.
	 */
	public static Path[] askForFiles(String title, Path rootDir, String... allowedSuffixes){
		final String[] suffixes = allowedSuffixes;
		final String description = Arrays.toString(suffixes);
		JFileChooser jfc = new JFileChooser(rootDir.toFile());
		jfc.setDialogTitle(title);
		jfc.setFileFilter(new FileFilter(){

			@Override
			public boolean accept(File f) {
				if(f.isDirectory())return true;
				if(f.getName().contains(".") == false) return false;
				String suffix = f.getName().substring(f.getName().lastIndexOf(".")+1);
				for(String s : suffixes){
					if(suffix.equalsIgnoreCase(s)) return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return description;
			}
		});
		jfc.setMultiSelectionEnabled(true);
		int action = jfc.showOpenDialog(null);
		if(action == JFileChooser.CANCEL_OPTION) return null;
		return filesToPaths(jfc.getSelectedFiles());
	}
	
	/**
	 * Asks the user for multiple files via a JFileChooser pop-up. 
	 * @param rootDir Location where the file chooser first looks
	 * @param allowedSuffixes Array of valid file extensions
	 * @return Returns the selected files, or null if the operation was canceled.
	 */
	public static Path[] askForFiles(Path rootDir, String... allowedSuffixes){
		return askForFiles("Select files", rootDir, allowedSuffixes);
	}

	/**
	 * Converts an array of File objects into an array of Path objects
	 * @param files java.io.File instances
	 * @return An array of java.nio.file.Path instances, or null if the input 
	 * was null.
	 */
	public static Path[] filesToPaths(File... files){
		if(files == null) return null;
		Path[] paths = new Path[files.length];
		for(int i = 0; i < files.length; i++){
			if(files[i] == null) continue;
			paths[i] = files[i].toPath();
		}
		return paths;
	}
	
}
