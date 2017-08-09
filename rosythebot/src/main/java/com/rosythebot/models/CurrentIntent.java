package com.rosythebot.models;

import java.util.Map;

public class CurrentIntent {
	
	public CurrentIntent(){
		
	}
	
	private String name;
	private Map<String, String> slots;
	private String confirmationStatus;
	
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<String, String> getSlots() {
		return slots;
	}
	public void setSlots(Map<String, String> slots) {
		this.slots = slots;
	}
	public String getConfirmationStatus() {
		return confirmationStatus;
	}
	public void setConfirmationStatus(String confirmationStatus) {
		this.confirmationStatus = confirmationStatus;
	}
	
	

}


