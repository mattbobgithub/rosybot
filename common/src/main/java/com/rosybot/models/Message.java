package com.rosybot.models;

public class Message {
	public Message(){
		
	}
	
	public Message(String content){
		setContentType("PlainText");
		setContent(content);
	}
	
	private String contentType;
	private String content;
	
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
}
