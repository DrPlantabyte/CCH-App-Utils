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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Collection of utilities for Java applications
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public class AppHelper {

	/**
	 * Export a path to a String using a specific path separator. Useful for 
	 * converting a filepath from local machine's format to URL-compatible 
	 * format or another file system's format.
	 * @param filePath path to convert
	 * @param delimiter The new path separator to use
	 * @return Basically the equivalent of Path.toString(), but using your 
	 * specified path delimiter
	 */
	public static String delimitPath(Path filePath, String delimiter) {
		StringBuilder path = new StringBuilder();
		boolean notFirst = false;
		for(Path element : filePath){
			if(notFirst){
				path.append(delimiter);
			}
			path.append(element);
			notFirst = true;
		}
		return path.toString();
	}
	/** Static helper class, do not invoke */
	private AppHelper(){
		throw new UnsupportedOperationException("Static helper library class "+this.getClass()+" cannot be instantiated.");
	}
	/**
	 * Gets the filepath to a location used for storing app resource files.
	 * @param appName Name of the app
	 * @param relativeResourcePath Path within the app's data folder to the 
	 * desired resource file (e.g. <code>Paths.get("images","icon.png")</code>).
	 * @param portableExecution If true, then the resource location will be in 
	 * the same folder as the executable, where-ever that may be. If false, then 
	 * it will be in a folder in the user's standard application data folder 
	 * (OS dependant).
	 * @return Actual filepath to the requested file
	 */
	public static Path getResourceFilePath(String appName, Path relativeResourcePath, boolean portableExecution){
		return getAppDataFolder(appName,portableExecution).resolve(relativeResourcePath);
	}
	/**
	 * Gets an input stream for reading from a file internal to the program 
	 * (such as a file packaged within the executable .jar file).
	 * @param refClass A class from the same code source (i.e. jar file) as the 
	 * requested file resource.
	 * @param internalResourcePath The path to resource
	 * @return Effectively returns <code>refClass.getResourceAsStream(path)</code>, 
	 * performing appropriate checks and conversions to assure that this 
	 * operation goes smoothly
	 */
	public static InputStream getInternalResource(Class refClass, Path internalResourcePath){
		String path = "/"+delimitPath(internalResourcePath,"/");
		InputStream in = refClass.getResourceAsStream(path);
		return in;
	}
	
	/**
	 * Extracts all files (recursively) within a java package namespace into a 
	 * specified destination directory.
	 * @param refClass A class from the same code source (i.e. jar file) as the 
	 * executable code (typically the class whose <code>main(...)</code> method 
	 * is invoked).
	 * @param internalPackage The package to extract
	 * @param destinationDir The folder into which you want to extract the 
	 * package files
	 * @param overwriteFiles If true, overwrite existing files. If false, skip 
	 * existing files
	 * @throws IOException Thrown if there were any problems reading or writing 
	 * files and folders.
	 */
	public static void unpackPackage(Class refClass, Package internalPackage, Path destinationDir, boolean overwriteFiles) throws IOException{
		String[] pathComps = internalPackage.getName().split("\\.");
		if(pathComps.length == 1) {
			unpackPackage(refClass, Paths.get(pathComps[0]) ,destinationDir, overwriteFiles);
		} else {
			unpackPackage(refClass, Paths.get(pathComps[0],Arrays.copyOfRange(pathComps, 1, pathComps.length)) ,destinationDir, overwriteFiles);
		}
	}
	/**
	 * Extracts all files (recursively) within a java package namespace into a 
	 * specified destination directory.
	 * @param refClass A class from the same code source (i.e. jar file) as the 
	 * executable code (typically the class whose <code>main(...)</code> method 
	 * is invoked).
	 * @param internalPackagePath The package to extract, given as a Path 
	 * instance (e.g. <code>Paths.get("myapp","config","files")</code> to extract the 
	 * contents of the package <code>myapp.config.files</code>).
	 * @param destinationDir The folder into which you want to extract the 
	 * package files
	 * @param overwriteFiles If true, overwrite existing files. If false, skip 
	 * existing files
	 * @throws IOException Thrown if there were any problems reading or writing 
	 * files and folders.
	 */
	public static void unpackPackage(Class refClass, Path internalPackagePath, Path destinationDir, boolean overwriteFiles) throws IOException{
		if(!Files.isDirectory(destinationDir)){
			Files.createDirectories(destinationDir);
		}
		String pkg = delimitPath(internalPackagePath,"/");
		Path src = getAppExecutablePath(refClass);
		if(Files.isDirectory(src)){
			// running from folder, not a .jar
			Path prefix = src.resolve(internalPackagePath);
			FileHelper.copyDirectory(prefix, destinationDir, true);
		} else if(Files.isRegularFile(src)){
			// is running from a .jar (which is basically a .zip file)
			final int BUFFER_SIZE = 4096;
			File destDir = destinationDir.toFile();
			File zipFile = src.toFile();
			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry entry = zipIn.getNextEntry();
			// iterates over entries in the zip file
			while (entry != null) {
				String entryName = entry.getName();
				if(entryName.startsWith(pkg)) {
					String destName = entryName.replace(pkg, "");
					String filePath = destDir + File.separator + destName;
					if (!entry.isDirectory()) {
						// if the entry is a file, extracts it
						File nf = new File(filePath);
						if(!nf.exists() || overwriteFiles){
							BufferedOutputStream bos = new BufferedOutputStream(
									new FileOutputStream(nf)
							);
							byte[] bytesIn = new byte[BUFFER_SIZE];
							int read;
							while ((read = zipIn.read(bytesIn)) != -1) {
								bos.write(bytesIn, 0, read);
							}
							bos.close();
						}
					} else {
						// if the entry is a directory, make the directory
						File dir = new File(filePath);
						dir.mkdir();
					}
					zipIn.closeEntry();
				}
				entry = zipIn.getNextEntry();
			}
			zipIn.close();
		} else {
			// running from something exotic
			throw new IOException("Unrecognized code source format for code source '"+src.toString()+"'");
		}
	}
	
	/**
	 * Gets the file path to the java program being run (usually a .jar file).
	 * @param refClass A class from the same code source (i.e. jar file) as the 
	 * executable code (typically the class whose <code>main(...)</code> method 
	 * is invoked).
	 * @return The path to the .jar file, is running from a jar, or the path of the folder that is the root of the class path if running from class files
	 */
	public static Path getAppExecutablePath(Class refClass){
		try {
			Path p = Paths.get(refClass.getProtectionDomain().getCodeSource().getLocation().toURI());
			return p;
		} catch (URISyntaxException ex) {
			Logger.getLogger(refClass.getName()).log(Level.SEVERE, "URI conversion error", ex);
			throw new RuntimeException("Failed to resolve class file path",ex);
		}
	}
	
	/**
	 * Gets the local user's application data folder appropriate for your app
	 * @param appName Name (or domain) of your appgetAppDataFolder
	 * @param portableExecution If true, then the 
	 * @return Returns the 
	 */
	public static Path getAppDataFolder(String appName, boolean portableExecution){
		final Properties properties = System.getProperties();
		if(portableExecution) return Paths.get(properties.getProperty("user.dir", "."));
		Path appDir;
		
		switch(properties.getProperty("os.name")){
						case ("Windows Vista"):
						case ("Windows 7"):
						case ("Windows 8"):
						case ("Windows 10"):
						case ("Windows NT"):
							appDir = Paths.get(properties.getProperty("user.home"),"AppData","Roaming",appName);
							break;
						case ("Windows XP"):
						case ("Windows 95"):
						case ("Windows 98"):
						case ("Windows 2000"):
							appDir = Paths.get(properties.getProperty("user.home"),"Application Data",appName);
							break;
						case ("Mac OS X"):
							appDir = Paths.get(properties.getProperty("user.home"),"Library","Application Support",appName);
							break;
						case ("Linux"):
						case ("Unix"):
						case ("FreeBSD"):
						case ("Digital Unix"):
						case ("Solaris"):
							appDir = Paths.get(properties.getProperty("user.home"),"."+appName);
							break;
						default:
							// unknown/unsupported OS
							if(properties.getProperty("os.name").startsWith("Windows")){
								return Paths.get(properties.getProperty("user.home"),"AppData","Roaming",appName);
							}
							Logger.getLogger(AppHelper.class.getName()).log(Level.WARNING, 
								"OS type '"+properties.getProperty("os.name")
										+"' is not supported. Running as portable App instead.");
								return getAppDataFolder(appName,true);
					}
		return appDir;
	}
	
	// TODO: remove test code and files
	@Deprecated // for testing only
	public static void main(String[] args){
		Class refClass = AppHelper.class;
		try {
			boolean portable = false;
			System.out.println(Arrays.toString(AppHelper.class.getPackage().getName().split("\\.")));
			System.out.println(System.getProperties().getProperty("os.name"));
			System.out.println(getResourceFilePath("MyApp",Paths.get("images","face.png"),portable).toAbsolutePath());
			BufferedReader in = new BufferedReader(new InputStreamReader(getInternalResource(refClass, Paths.get("cch","testdata","test.txt"))));
			System.out.println(in.readLine());
			in.close();
			System.out.println(getAppExecutablePath(refClass));
			//unpackPackage(Paths.get("cch"),Paths.get("C:\\Users\\CCHall\\temp"),portable);
			unpackPackage(
					refClass,
					Paths.get("cch","testdata"),
					getResourceFilePath("MyApp",Paths.get("data"),portable),
					true);
		} catch (Exception ex) {
			Logger.getLogger(refClass.getName()).log(Level.SEVERE, "Error", ex);
		}
	}
}
