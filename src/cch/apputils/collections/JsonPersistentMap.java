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
package cch.apputils.collections;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonWriter;
import com.grack.nanojson.JsonWriterException;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of java.util.Map that syncs to a JSON file whenever it 
 * changes. Thread-safe!
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public class JsonPersistentMap extends PersistentMap {
	
	private static final Charset utf8 = Charset.forName("UTF-8");

	private final Path filePath;

	private JsonObject memory = new JsonObject();
/**
 * Constructs a new PersistentMap that uses JSON format to store data.
 * @param jsonFile The file to store the persistent data
 */
	public JsonPersistentMap(Path jsonFile) {
		this.filePath = jsonFile;
	}
/**
 * Writes to the JSON file
 * @throws IOException Thrown if there was a problem making the new file
 */
	@Override
	protected void syncToFile() throws IOException {
		guarenteeFile(this.getFilepath());

		Writer out = new OutputStreamWriter(new FileOutputStream(filePath.toFile()), utf8);
		try{
			JsonWriter.indent("   ").on(out).object(memory).done();
		} catch(JsonWriterException ex){
			throw new UnsupportedOperationException(ex.getLocalizedMessage(), ex);
		}
		out.close();
	}
/**
 * Reads the JSON file
 * @throws IOException Thrown if there was a problem making the new file
 */
	@Override
	protected void syncFromFile() throws IOException {
		guarenteeFile(this.getFilepath());
		// file access
		Reader in = new InputStreamReader(new FileInputStream(filePath.toFile()), utf8);
		try {
			memory = JsonParser.object().from(in);
		} catch (JsonParserException ex) {
			throw new IOException("JSON syntax error", ex);
		}
		in.close();
	}

	private void guarenteeFile(Path f) throws IOException {

		if (Files.notExists(f)) {
			Files.createDirectories(f.getParent());
			Files.createFile(f);
		}
	}
/**
 * Gets the in-memory JSON map
 * @return A map
 */
	@Override
	protected Map<String, Object> getMapCache() {
		return memory;
	}
/**
 * Returns the filepath of the JSON file
 * @return A file path
 */
	@Override
	protected Path getFilepath() {
		return filePath;
	}
/**
 * Initializes a new empty JSON file
 * @throws IOException Thrown if there was a problem making the new file
 */
	@Override
	protected void initializeNewFile() throws IOException{
		memory = new JsonObject();
		syncToFile();
		
	}
/**
	 * Checks if a given object is allowed to be persistently stored in this 
	 * collection. An object is valid if the implementation of 
	 * {@link #syncFromFile() } and {@link #syncToFile() } know how to store 
	 * and retrieve this kind of object from its persistent storage file. 
	 * Typical implementations only know how to store Strings, numbers, and 
	 * maps (of Strings and numbers).
	 * @param instance An object instance to test
	 * @return <code>true</code> if this object can be stored and retrieved 
	 * from a persistent storage file.
	 */
	@Override
	protected boolean isValidEntry(Object o) {
		if(o instanceof String) return true;
		if(o instanceof Number) return true;
		if(o instanceof Boolean) return true;
		if(o instanceof Map) return true;
		if(o instanceof Collection) return true;
		if(o.getClass().isArray()) {
			if(Array.getLength(o) > 0) return isValidEntry(Array.get(o, 0));
			return true;
		}
		return false;
	}

}
