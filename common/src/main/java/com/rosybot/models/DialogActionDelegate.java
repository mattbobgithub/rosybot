package com.rosybot.models;

import java.util.Map;

public class DialogActionDelegate extends DialogAction {
	
	public DialogActionDelegate(){
		setType("Delegate");
	}	
		
	private String type;
	private Map<String, String> slots;
	
	
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Map<String, String> getSlots() {
		return slots;
	}
	public void setSlots(Map<String, String> slots) {
		this.slots = slots;
	}
	
	
	
	
	
}
