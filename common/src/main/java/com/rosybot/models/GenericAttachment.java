package com.rosybot.models;

import java.util.Set;

public class GenericAttachment {
	public GenericAttachment(){
		
	}
	
	private String title;
	private String subTitle;
	private String imageUrl;
	private String attachmentLinkUrl;
	private Set<Button> buttons;
	
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getSubTitle() {
		return subTitle;
	}
	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public String getAttachmentLinkUrl() {
		return attachmentLinkUrl;
	}
	public void setAttachmentLinkUrl(String attachmentLinkUrl) {
		this.attachmentLinkUrl = attachmentLinkUrl;
	}
	public Set<Button> getButtons() {
		return buttons;
	}
	public void setButtons(Set<Button> buttons) {
		this.buttons = buttons;
	}


	
	
}
