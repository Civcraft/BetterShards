package vg.civcraft.mc.bettershards.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerChangeServerEvent extends Event implements Cancellable{

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCancelled(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HandlerList getHandlers() {
		// TODO Auto-generated method stub
		return null;
	}

}
