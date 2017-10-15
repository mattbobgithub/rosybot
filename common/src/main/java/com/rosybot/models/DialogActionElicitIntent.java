package com.rosybot.models;

import java.util.Map;

public class DialogActionElicitIntent extends DialogAction {

	public DialogActionElicitIntent(){
		setType("ElicitIntent");
	}
	
	private String type;
	private Message message;
	private ResponseCard responseCard;
	
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Message getMessage() {
		return message;
	}
	public void setMessage(Message message) {
		this.message = message;
	}
	public ResponseCard getResponseCard() {
		return responseCard;
	}
	public void setResponseCard(ResponseCard responseCard) {
		this.responseCard = responseCard;
	}
	
	
	
}
