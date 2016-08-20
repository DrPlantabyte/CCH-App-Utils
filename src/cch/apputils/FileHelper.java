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

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public class FileHelper {
	/** Static helper class, do not invoke */
	private FileHelper(){
		throw new UnsupportedOperationException("Static helper library class "+this.getClass()+" cannot be instantiated.");
	}
	/**
	 * Recursively copied a folder from one location to another.
	 * @param src Source directory
	 * @param destDir Destination directory (including the final directory name)
	 * @param overwriteExisting If true, existing files will be overwritten
	 * @throws IOException 
	 */
	public static void copyDirectory(final Path src, final Path destDir, boolean overwriteExisting) throws IOException{
		ArrayList<CopyOption> copyOptions = new ArrayList<>();
		if(overwriteExisting){
			copyOptions.add(StandardCopyOption.REPLACE_EXISTING);
		}
		copyOptions.add(StandardCopyOption.COPY_ATTRIBUTES);
		
		Files.walkFileTree(src, new FileVisitor<Path>(){
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					final Path newFile = destDir.resolve(file.startsWith(src) ? src.relativize(file) : file);
					final Path dir = Files.isDirectory(file) ? file : file.getParent();
					final Path newDir = Files.isDirectory(file) ? newFile : newFile.getParent();
					if(Files.notExists(newDir)){
						Files.createDirectories(newDir);
					}
					Files.copy(file, newFile, copyOptions.toArray(new CopyOption[0]));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					Logger.getLogger(FileHelper.class.getName()).log(Level.WARNING, "Failed to copy file ".concat(file.toString()),exc);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if(exc != null){
						Logger.getLogger(FileHelper.class.getName()).log(Level.WARNING, "Failed to copy directory ".concat(dir.toString()),exc);
					}
					return FileVisitResult.CONTINUE;
				}
			});
	}
}
