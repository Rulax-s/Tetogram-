/* Tetogram, 2026. GNU GPL v2 or later. Reference counts for concurrent draft/export media users. */
package org.telegram.messenger.tetogram;
import java.util.HashMap;
public final class AppearanceLeases {
    private final HashMap<String,Integer> counts=new HashMap<>();
    public synchronized void acquire(String file){counts.put(file,counts.getOrDefault(file,0)+1);}
    public synchronized void release(String file){int count=counts.getOrDefault(file,0);if(count<=1)counts.remove(file);else counts.put(file,count-1);}
    public synchronized boolean pending(String file){return counts.containsKey(file);}
}
