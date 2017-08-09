package com.rosythebot.models;

import java.util.Set;

public class ResponseCard {
	public ResponseCard(){
		
	}
	
	private String version;
	private String contentType;
	private Set<GenericAttachment> genericAttachments;
	
	
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public Set<GenericAttachment> getGenericAttachments() {
		return genericAttachments;
	}
	public void setGenericAttachments(Set<GenericAttachment> genericAttachments) {
		this.genericAttachments = genericAttachments;
	}


}
