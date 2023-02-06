package com.shelk;

import java.util.HashMap;

import org.bukkit.Location;

public class GolfArea {
	
	String areaId;
	HashMap<String,Location> balls = new HashMap<>();
	HashMap<String,Location> cauldrons = new HashMap<>();
	
	public GolfArea(String areaId) {
		this.areaId = areaId;
	}
	
	public GolfArea(String areaId, HashMap<String, Location> balls, HashMap<String,Location> cauldrons) {
		this.areaId = areaId;
		this.balls = balls;
		this.cauldrons = cauldrons;
	}

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}

	

	public HashMap<String, Location> getBalls() {
		return balls;
	}

	public void setBalls(HashMap<String, Location> balls) {
		this.balls = balls;
	}

	public HashMap<String,Location> getCauldrons() {
		return cauldrons;
	}

	public void setCauldron(HashMap<String,Location> cauldrons) {
		this.cauldrons = cauldrons;
	}
	
	
	

}
