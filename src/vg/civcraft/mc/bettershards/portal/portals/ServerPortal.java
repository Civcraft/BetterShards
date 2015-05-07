package vg.civcraft.mc.bettershards.portal.portals;

import vg.civcraft.mc.bettershards.portal.Portal;

/**
 * The portal object of a portal on another server.
 */
public class ServerPortal extends Portal{

	private String serverName;
	public ServerPortal(String serverName, Portal portal) {
		super(null, portal);
		this.serverName = serverName;
	}

	public String getServerName(){
		return serverName;
	}
}
