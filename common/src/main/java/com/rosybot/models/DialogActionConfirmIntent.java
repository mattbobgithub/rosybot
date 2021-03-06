package com.rosybot.models;

import java.util.Map;

public class DialogActionConfirmIntent extends DialogAction{
	
	public DialogActionConfirmIntent(){
		setType("ConfirmIntent");
		
	}	
	
	private String type;
	private String fulfillmentState;
	private Message message;
	private String intentName;
	private Map<String, String> slots;
	private String slotToElicit;
	private ResponseCard responseCard;
	
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFulfillmentState() {
		return fulfillmentState;
	}
	public void setFulfillmentState(String fulfillmentState) {
		this.fulfillmentState = fulfillmentState;
	}
	public Message getMessage() {
		return message;
	}
	public void setMessage(Message message) {
		this.message = message;
	}
	public String getIntentName() {
		return intentName;
	}
	public void setIntentName(String intentName) {
		this.intentName = intentName;
	}
	public Map<String, String> getSlots() {
		return slots;
	}
	public void setSlots(Map<String, String> slots) {
		this.slots = slots;
	}
	public String getSlotToElicit() {
		return slotToElicit;
	}
	public void setSlotToElicit(String slotToElicit) {
		this.slotToElicit = slotToElicit;
	}
	public ResponseCard getResponseCard() {
		return responseCard;
	}
	public void setResponseCard(ResponseCard responseCard) {
		this.responseCard = responseCard;
	}
	
	

}
