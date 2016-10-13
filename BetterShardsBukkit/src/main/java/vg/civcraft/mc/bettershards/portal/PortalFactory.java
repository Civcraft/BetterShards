package vg.civcraft.mc.bettershards.portal;

import java.util.HashMap;
import java.util.Map;

public class PortalFactory {

	private Map<Integer, Class<? extends Portal>> portals = new HashMap<Integer, Class<? extends Portal>>();
	private Map<String, Integer> portalsNames = new HashMap<String, Integer>();
	
	public <E extends Portal> E buildPortal(Class<E> clazz) {
		try {
			E p = (E) clazz.newInstance();
			return p;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public <E extends Portal> void registerPortal(int real_id, Class<E> clazz, String name) {
		portals.put(real_id, clazz);
		portalsNames.put(name, real_id);
	}
	
	public String[] getAllPortalNames() {
		return (String[]) portalsNames.keySet().toArray();
	}
	
	public Class<? extends Portal> getPortal(int id) {
		return portals.get(id);
	}
	
	public Class<? extends Portal> getPortal(String name) {
		return portals.get(portalsNames.get(name));
	}
}
