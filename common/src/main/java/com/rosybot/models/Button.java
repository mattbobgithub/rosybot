package com.rosybot.models;

public class Button {
	public Button(){
		
	}
	
	public Button(String t, String v){
		setText(t);
		setValue(v);
	}
	
	private String text;
	private String value;
	
	
	
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
