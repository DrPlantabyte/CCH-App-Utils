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

import cch.apputils.collections.JsonPersistentMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * WARNING: Only use one implementation per program!
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public abstract class CCHDesktopApp extends Application {

	private static final Lock initLock = new ReentrantLock();
	
	private static CCHDesktopApp instance = null;
	/**
	 * The name of this application
	 * @return App name
	 */
	public abstract String getAppName();
	/**
	 * The version number of this program (typically stored in version.properties)
	 * @return A period-delimited version string
	 */
	public abstract String getAppVersion();
	/**
	 * Gets the list of authors for this app
	 * @return Authors list
	 */
	public abstract String[] getAppAuthors();
	/**
	 * License description
	 * @return text
	 */
	public abstract String getAppLicense();
	/**
	 * Gets the URL path of the main window's FXML document
	 * @return FXML document path, such as "MainView.fxml" or 
	 * "/com/myapp/MyWindow.fxml"
	 */
	public abstract String getMainFXMLPath();
	
	/**
	 * Compares two version number strings
	 * @param versionA A version number, such as "1.21.1035b"
	 * @param versionB A version number, such as "1.22.1070"
	 * @return Returns a positive if <code>versionA</code> is later than 
	 * <code>versionB</code>, a negative number if <code>versionA</code> is 
	 * earlier than <code>versionB</code>, or zero if the versions are equal.
	 */
	public static int versionCompare(String versionA, String versionB){
		String[] a = versionA.split("\\.");
		String[] b = versionB.split("\\.");
		for(int i = 0; i < a.length && i < b.length; i++){
			int anum = Integer.parseInt(a[i].trim().toLowerCase(Locale.US), 36);
			int bnum = Integer.parseInt(b[i].trim().toLowerCase(Locale.US), 36);
			if(anum != bnum){
				return anum - bnum;
			}
		}
		return 0;
	}
	/** Instance of the UTF8 charset */
	public static final Charset UTF8 = Charset.forName("UTF-8");
	
	
	private Stage mainWindow = null;
	private Object mainController = null;
	
	private Path appDir = null;
	
	private Path scriptLibDir = null;
	
	
	
	private Map<String, Object> config;
	
	
    private static java.util.logging.ConsoleHandler loggerConsole = null;
	
    private static java.util.logging.FileHandler mainLogHandler = null;
	
	@Override
	public void init() throws Exception{
		initLock.lock();
		try{
			logger(this).fine("Initializing...");
			if(instance != null) {
				// FAILURE! ABORT!
				throw new IllegalStateException("ERROR! Only one implementation of "+CCHDesktopApp.class.getName()+" can be instantiated per JVM!");
			}
			instance = this;
			configureLoggers();
			appDir = AppHelper.getAppDataFolder(getAppName(), false);
			config = new JsonPersistentMap(AppHelper.getResourceFilePath(getAppName(), 
					Paths.get("config.json"), false));


			super.init();
			CCHDesktopApp.logger(this).fine("...Initialized");
		}finally{
			initLock.unlock();
		}
	}
	
	
	@Override
	public void start(Stage stage) throws Exception {
		initLock.lock();
		try{
			mainWindow = stage;

			FXMLLoader loader = new FXMLLoader(this.getClass().getResource(getMainFXMLPath()));
			Parent root = loader.load();
			mainController = loader.getController();

			Scene scene = new Scene(root);

			stage.setScene(scene);

			stage.show();

			logger(this).fine("GUI started.");
		} finally {
			initLock.unlock();
		}
	}
	
	
	public static java.util.logging.Logger logger(Object src){
		if(src == null) return logger(CCHDesktopApp.class);
		return logger(src.getClass());
	}
	
	public static java.util.logging.Logger logger(Class srcClass){
		String logName = srcClass.getCanonicalName();
		if(logName == null) return  Logger.getLogger(String.valueOf(CCHDesktopApp.class.getCanonicalName()));
		return Logger.getLogger(String.valueOf(logName));
	}
	public void configureLoggers() {
		Path logDir = AppHelper.getAppDataFolder(getAppName(), false).resolve("log");
		if(Files.isDirectory(logDir) == false){
			try {
				logger(this.getClass()).log(Level.INFO, "Creating logging folder "+logDir.toAbsolutePath().toString());
				Files.createDirectories(logDir);
			} catch (IOException ex) {
				logger(this.getClass()).log(Level.SEVERE, "Failed to create logging folder", ex);
			}
		}
        // configuring the root logger applies to all other loggers
        java.util.logging.LogManager.getLogManager().reset();
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.ALL);
        loggerConsole = new java.util.logging.ConsoleHandler();
        loggerConsole.setLevel(Level.INFO);
        loggerConsole.setFormatter(new java.util.logging.SimpleFormatter());
        rootLogger.addHandler(loggerConsole);

        int logFileSizeLimit = 0x400000; // 4 MB
        int logFileNumberLimit = 9; // start deleting old log files when there's more than this many sitting around
        boolean appendToOldFiles = true;
        // configure main catch-all log
        try {
            mainLogHandler = new java.util.logging.FileHandler(
                    "log"+File.separator+"console-log%g.txt", logFileSizeLimit, logFileNumberLimit, appendToOldFiles);
            mainLogHandler.setFormatter(new java.util.logging.SimpleFormatter());
            mainLogHandler.setEncoding("UTF-8");
            mainLogHandler.setLevel(Level.FINE);
			mainLogHandler.setFilter(( LogRecord lr ) -> {
                String sourceName = lr.getSourceClassName();
                return lr.getLevel().intValue() >= Level.INFO.intValue() 
						|| sourceName.startsWith(this.getClass().getPackage().getName());
            });
            rootLogger.addHandler(mainLogHandler);
        } catch ( IOException | SecurityException ex ) {
            logger(this.getClass()).log(Level.SEVERE, "Failed to create file output handler for logger", ex);
        }
    }
}
