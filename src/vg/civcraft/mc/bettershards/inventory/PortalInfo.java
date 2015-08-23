package vg.civcraft.mc.bettershards.inventory;
/**
 * Just a class used to store the info about a portal when a player joins from another server.
 * @author Rourke750
 *
 */
public class PortalInfo implements Info{

	private String name;
	
	public PortalInfo(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
}
