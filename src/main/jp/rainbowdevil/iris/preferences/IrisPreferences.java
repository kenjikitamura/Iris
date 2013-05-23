package jp.rainbowdevil.iris.preferences;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import jp.rainbowdevil.iris.IrisWindow;

public class IrisPreferences {
	private Preferences prefs;
	
	public static final String PROXY_ADDRESS = "proxy.address";
	public static final String PROXY_PORT = "proxy.port";
	public static final String WINDOW_HEIGHT = "window.height";
	public static final String WINDOW_WIDTH = "window.width";
	public static final String WINDOW_DIVIDER_POSITION1 = "window.dividerposition1";
	public static final String WINDOW_DIVIDER_POSITION2 = "window.dividerposition2";
	
	public IrisPreferences(){
		prefs = Preferences.userNodeForPackage(IrisWindow.class);
	}
	
	public void setValue(String key, int value){
		prefs.putInt(key, value);
	}
	
	public void setValue(String key, String value){
		prefs.put(key, value);
	}
	
	public void setValue(String key, double value){
		prefs.putDouble(key, value);
	}
	
	public int getInt(String key, int defaultValue){
		return prefs.getInt(key, defaultValue);
	}
	
	public double getDouble(String key, double defaultValue){
		return prefs.getDouble(key, defaultValue);
	}
	
	public String get(String key){
		return prefs.get(key, null);
	}
	
	public String get(String key, String defaultValue){
		return prefs.get(key, defaultValue);
	}
	
	public void save() throws BackingStoreException{
		prefs.flush();
	}
}
