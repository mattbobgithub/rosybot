package com.rosybot.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CurrentIntent {
	
	public CurrentIntent(){
		
	}
	
	private String name;
	private Map<String, String> slots;
	private Map<String, Object> slotDetails;
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
	@JsonIgnore
	public Map<String, Object> getSlotDetails() {
		return slotDetails;
	}
	@JsonIgnore
	public void setSlotDetails(Map<String, Object> slotDetails) {
		this.slotDetails = slotDetails;
	}
	
	public String getConfirmationStatus() {
		return confirmationStatus;
	}
	public void setConfirmationStatus(String confirmationStatus) {
		this.confirmationStatus = confirmationStatus;
	}
	
	

}


