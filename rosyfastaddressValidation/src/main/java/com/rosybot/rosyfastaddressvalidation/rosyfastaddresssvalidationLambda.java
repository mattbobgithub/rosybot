package com.rosybot.rosyfastaddressvalidation;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.rosybot.models.CurrentIntent;
import com.rosybot.models.DialogActionDelegate;
import com.rosybot.models.LexRequest;
import com.rosybot.models.LexResponse;

public class rosyfastaddresssvalidationLambda implements RequestHandler<Object, Object> {
	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();

	private static Map<String, String> sessionAttributes = new HashMap<String, String>();
	private static Map<String, String> inputSlots = new HashMap<String, String>();
	private static CurrentIntent currentIntent;

	@Override
	public Object handleRequest(Object input, Context context) {
		ll = context.getLogger();
		
		lexReq = LexRequest.fromLexObject(input);
				
		CurrentIntent currentIntent = lexReq.getCurrentIntent();
	
		  
		ll.log("RECEIVED LEX REQ SESSION ATTRIBUTES:");
		for (Map.Entry<String, String> entry : lexReq.getSessionAttributes().entrySet()) {
			ll.log(entry.getKey() + " : " + entry.getValue());
		}
		
		if (lexReq.getSessionAttributes() != null) { 
			// ll.log("SESSION ATTRIBS:" +
			// lexReq.getSessionAttributes().toString());
			sessionAttributes.clear();
			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}
		
		inputSlots.putAll(currentIntent.getSlots());
		ll.log("INPUT SLOTS BEFORE:" + inputSlots.toString());
		
		String inputTranscript = lexReq.getInputTranscript();
		
		if(inputTranscript.toLowerCase().equals("n") || inputTranscript.toLowerCase().equals("no")  ){
			inputSlots.put("aptNo", inputTranscript);
			
		}
		ll.log("INPUT SLOTS AFTER:" + inputSlots.toString());
		
		lexRes = new LexResponse();
		DialogActionDelegate da = new DialogActionDelegate();
		
		da.setSlots(inputSlots);
		
		lexRes.setDialogAction(da);

		lexRes.setSessionAttributes(sessionAttributes);


        return lexRes;
    }

}
