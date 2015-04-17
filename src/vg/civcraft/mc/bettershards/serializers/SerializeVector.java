package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;

import org.bukkit.util.Vector;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class SerializeVector implements Serializable{

	private static final long serialVersionUID = -2757209269940152754L;

	private double x, y, z;
	
	public SerializeVector(Vector vec){
		x = vec.getX();
		y = vec.getY();
		z = vec.getZ();
	}
	
	public Vector unpackVector(){
		return new Vector(x,y,z);
	}
}

