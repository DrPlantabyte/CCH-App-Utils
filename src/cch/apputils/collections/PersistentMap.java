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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract class that implements of java.util.Map for syncing to a file whenever it changes. Thread-safe!
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public abstract class PersistentMap implements Map<String, Object> {
	
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private FileTime timestamp = null;

	/**
	 * Implementation must take the <code>Map</code> instance returned by 
	 * {@link #getMapCache() } and write it to the persistent storage file.
	 * @throws IOException Thrown if there is an error accessing the file.
	 * @throws UnsupportedOperationException Thrown if the map contains object 
	 * types that are not supported for serialization (see {@link #isValidEntry(java.lang.Object) }) .
	 */
	protected abstract void syncToFile() throws IOException, UnsupportedOperationException; // UOE thrown for objects that it does not know how to serialize
	/**
	 * Implementation must parse the persistent storage file and update the 
	 * instance returned by {@link #getMapCache() } to match what is stored in 
	 * the file.
	 * @throws IOException Thrown if there is an error accessing the file.
	 * @throws UnsupportedOperationException Thrown if the file contains object 
	 * types that are not supported for serialization (see {@link #isValidEntry(java.lang.Object) }) .
	 */
	protected abstract void syncFromFile() throws IOException, UnsupportedOperationException;
	/**
	 * <code>Map</code> instance that represents the current state of this map. 
	 * This class will read from and write to this <code>Map</code>, calling 
	 * {@link #syncFromFile() } and {@link #syncToFile() } as necessary to 
	 * insure that the <code>Map</code> and its persistent storage file remain 
	 * synchronized.
	 * @return A  <code>Map</code> instance that represents the current state
	 */
	protected abstract Map<String, Object> getMapCache();
	
	/**
	 * Returns the path to a file that is written to every time {@link #syncToFile() } is invoked.
	 * @return A file path
	 */
	protected abstract Path getFilepath();
	
	/**
	 * Implementation must create a new file and initiate as a new empty collection (called if the file returned by {@link #getFilepath() } does not exist).
	 * @throws IOException 
	 */
	protected abstract void initializeNewFile() throws IOException;
	
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
	protected abstract boolean isValidEntry(Object instance);
	
	private void toFile() throws IOException{
		lock.writeLock().lock();
		try {
			if(Files.notExists(getFilepath())){
				initializeNewFile();
			}
			// check if sync is needed
			if(timestamp == null 
					|| !Files.getLastModifiedTime(getFilepath()).equals(timestamp)){
			
				// file access
				if (getMapCache() != null) {
					syncToFile();
					timestamp = Files.getLastModifiedTime(getFilepath());
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void fromFile() throws IOException{
		lock.writeLock().lock();
		try {
			
			if(Files.notExists(getFilepath())){
				initializeNewFile();
			}
			
			// check if sync is needed
			if(timestamp == null 
					|| !Files.getLastModifiedTime(getFilepath()).equals(timestamp)){
			
				// file access
				syncFromFile();
				timestamp = Files.getLastModifiedTime(getFilepath());
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void invalidate(){timestamp = null;}
	
	/**
	 * see {@link java.util.Map#size() }
	 * @return see {@link java.util.Map#size() }
	 */
	@Override
	public int size() {
		lock.readLock().lock();
		try{
			return getMapCache().size();
		} finally {
			lock.readLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#isEmpty() }
	 * @return see {@link java.util.Map#isEmpty() }
	 */
	@Override
	public boolean isEmpty() {
		lock.readLock().lock();
		try{
			return getMapCache().isEmpty();
		} finally {
			lock.readLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#containsKey(java.lang.Object) }
	 * @param key see {@link java.util.Map#containsKey(java.lang.Object) }
	 * @return see {@link java.util.Map#containsKey(java.lang.Object) }
	 */
	@Override
	public boolean containsKey(Object key) {
		lock.readLock().lock();
		try{
			return getMapCache().containsKey(key);
		} finally {
			lock.readLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#containsValue(java.lang.Object) }
	 * @param value see {@link java.util.Map#containsValue(java.lang.Object) }
	 * @return see {@link java.util.Map#containsValue(java.lang.Object)  }
	 */
	@Override
	public boolean containsValue(Object value) {
		lock.readLock().lock();
		try{
			return getMapCache().containsValue(value);
		} finally {
			lock.readLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#get(java.lang.Object) }
	 * @param key see {@link java.util.Map#get(java.lang.Object) }
	 * @return see {@link java.util.Map#get(java.lang.Object) }
	 */
	@Override
	public Object get(Object key) {
		lock.writeLock().lock();
		try {
			try {
				fromFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			return getMapCache().get(key);
		} finally {
			lock.writeLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#put(java.lang.Object, java.lang.Object) }
	 * @param key see {@link java.util.Map#put(java.lang.Object, java.lang.Object) }
	 * @param value see {@link java.util.Map#put(java.lang.Object, java.lang.Object) }
	 * @return see {@link java.util.Map#put(java.lang.Object, java.lang.Object) }
	 */
	@Override
	public Object put(String key, Object value) {
		if(value != null && !isValidEntry(value)) throw new UnsupportedOperationException("Cannot store object of type "+value.getClass().getName()+" in this "+this.getClass().getName());
		lock.writeLock().lock();
		try {
			try {
				fromFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			Object retVal = getMapCache().put(key,value);
			invalidate();
			
			try {
				toFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			return retVal;
		} finally {
			lock.writeLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#remove(java.lang.Object)  }
	 * @param key see {@link java.util.Map#remove(java.lang.Object)  }
	 * @return see {@link java.util.Map#remove(java.lang.Object)  }
	 */
	@Override
	public Object remove(Object key) {
		
		lock.writeLock().lock();
		try {
			try {
				fromFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			Object retVal = getMapCache().remove(key);
			invalidate();
			
			try {
				toFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			return retVal;
		} finally {
			lock.writeLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#putAll(java.util.Map) }
	 * @param m see {@link java.util.Map#putAll(java.util.Map)  }
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		lock.writeLock().lock();
		try {
			try {
				fromFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			getMapCache().putAll(m);
			invalidate();
			
			try {
				toFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#clear() }
	 */
	@Override
	public void clear() {
		lock.writeLock().lock();
		try {
			
			getMapCache().clear();
			invalidate();
			
			try {
				toFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	/** see {@link java.util.Map#keySet() }
	 * @return see {@link java.util.Map#keySet() }
	 */
	@Override
	public Set<String> keySet() {
		lock.writeLock().lock();
		try {
			try {
				fromFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			Set<String> retVal = Collections.unmodifiableSet(getMapCache().keySet());
			
			return retVal;
		} finally {
			lock.writeLock().unlock();
		}
	}
	/**
	 * see {@link java.util.Map#values() }
	 * @return see {@link java.util.Map#values() }
	 */
	@Override
	public Collection<Object> values() {
		lock.writeLock().lock();
		try {
			try {
				fromFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			Collection<Object> retVal = Collections.unmodifiableCollection(getMapCache().values());
			
			return retVal;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * see {@link java.util.Map#entrySet() }
	 * @return see {@link java.util.Map#entrySet() }
	 */
	@Override
	public Set<Entry<String, Object>> entrySet() {
		lock.writeLock().lock();
		try {
			try {
				fromFile();
			} catch (IOException ex) {
				throw new RuntimeException("Unchecked exception was thrown: "
						+ ex.getClass().getSimpleName(), ex);
			}
			
			Set<Entry<String, Object>> retVal = Collections.unmodifiableSet(getMapCache().entrySet());
			
			return retVal;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
}
