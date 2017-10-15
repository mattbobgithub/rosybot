package com.rosybot.models;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LexRequest implements Serializable {
	   
	private static final long serialVersionUID = 1L;
	
	public LexRequest() {

	}

	// static constructor from lex input object
	public static LexRequest fromLexObject(Object lexObject) {
		LexRequest lr = new LexRequest();

		try {
			// map json input to pojo
			ObjectMapper mapper = new ObjectMapper();
			String inputJsonString = mapper.writeValueAsString(lexObject);
			System.out.println(inputJsonString);
			lr = mapper.readValue(inputJsonString, LexRequest.class);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return lr;

	}

	private CurrentIntent currentIntent;
	private Bot bot;
	private String userId;
	private String inputTranscript;
	private String invocationSource;
	private String outputDialogMode;
	private String messageVersion;
	private Map<String,String> sessionAttributes;

	public CurrentIntent getCurrentIntent() {
		return currentIntent;
	}

	public void setCurrentIntent(CurrentIntent currentIntent) {
		this.currentIntent = currentIntent;
	}

	public Bot getBot() {
		return bot;
	}

	public void setBot(Bot bot) {
		this.bot = bot;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getInputTranscript() {
		return inputTranscript;
	}

	public void setInputTranscript(String inputTranscript) {
		this.inputTranscript = inputTranscript;
	}

	public String getInvocationSource() {
		return invocationSource;
	}

	public void setInvocationSource(String invocationSource) {
		this.invocationSource = invocationSource;
	}

	public String getOutputDialogMode() {
		return outputDialogMode;
	}

	public void setOutputDialogMode(String outputDialogMode) {
		this.outputDialogMode = outputDialogMode;
	}

	public String getMessageVersion() {
		return messageVersion;
	}

	public void setMessageVersion(String messageVersion) {
		this.messageVersion = messageVersion;
	}

	public Map<String, String> getSessionAttributes() {
		return sessionAttributes;
	}

	public void setSessionAttributes(Map<String,String> sessionAttributes) {
		this.sessionAttributes=sessionAttributes;
	}

	@Override
	public String toString() {
		String outputStr = "LexRequest [currentIntent=" + currentIntent + ", slots=" + currentIntent.getSlots().toString() + ", bot=" + bot + ", userId=" + userId
				+ ", inputTranscript=" + inputTranscript + ", invocationSource=" + invocationSource
				+ ", outputDialogMode=" + outputDialogMode + ", messageVersion=" + messageVersion + ", sessionAttributes=";
		
		for (Map.Entry<String, String> entry : sessionAttributes.entrySet()) {
			outputStr = outputStr + entry.getKey() + " : " + entry.getValue();
		}
		return outputStr;
	}

	
	
	
	
	
	
	
	
}
