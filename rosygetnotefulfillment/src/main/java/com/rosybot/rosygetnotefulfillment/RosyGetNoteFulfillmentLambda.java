package com.rosybot.rosygetnotefulfillment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.rosybot.models.CurrentIntent;
import com.rosybot.models.DialogActionElicitSlot;
import com.rosybot.models.GenericAttachment;
import com.rosybot.models.LexRequest;
import com.rosybot.models.LexResponse;
import com.rosybot.models.Message;
import com.rosybot.models.ResponseCard;

public class RosyGetNoteFulfillmentLambda implements RequestHandler<Object, Object> {
	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();

	private static Map<String, String> sessionAttributes = new HashMap<String, String>();
	private static Map<String, String> inputSlots = new HashMap<String, String>();
	private static CurrentIntent currentIntent;

	@Override
	public Object handleRequest(Object input, Context context) {
		ll = context.getLogger();
		ll.log("Input: " + input); 
		lexReq = LexRequest.fromLexObject(input);
		ll.log("CURRENT INTENT: " + lexReq.getCurrentIntent().getName() + "     request obj: " + lexReq.toString());

		CurrentIntent currentIntent = lexReq.getCurrentIntent();
inputSlots.clear();
		inputSlots.putAll(currentIntent.getSlots());
		ll.log("INPUT SLOTS:" + inputSlots.toString());

		if (lexReq.getSessionAttributes() != null) {
			// ll.log("SESSION ATTRIBS:" +
			// lexReq.getSessionAttributes().toString());
			sessionAttributes.clear();
			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}

		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		ll.log("input transcript:" + lexReq.getInputTranscript());
		
		
		String noteText = inputSlots.get("noteText");
		String noteConfirmed = inputSlots.get("noteConfirmed").toLowerCase();

		cleanTheLexBS();

		if (noteText.toLowerCase().equals("x")) {
			ll.log("noteText is x, first time through.");

			da = getNoteDA();

		} else {

			sessionAttributes.put("s_noteText", noteText);

			if (noteConfirmed.equals("x")) {
				ll.log("note confirmed is x, first time through for confirmation.");
				da = getNoteConfirmationDA();
			} else {
				ll.log("note confirmed is not x, should be y or n: " + noteConfirmed);
				sessionAttributes.put("s_noteConfirmed", noteConfirmed);
				if (!noteConfirmed.equals("y")) {
					ll.log("note note confirmed, re-prompt");

					// retry to get note
					da = getNoteDA();
				} else {
					// note confirmed move on
					ll.log("note confirmed move on to get recipient name");
					
					//if customer name already exists because exisiting customer, move on to get recipient name
					if (!StringUtils.isNullOrEmpty(sessionAttributes.get("s_customerFullName"))) {
						
						da = getRecipientNameDA();
						
						
						
					}else{
					da = getCustomerNameDA();
					}
				}

			}
		}

		lexRes.setDialogAction(da);
		lexRes.setSessionAttributes(sessionAttributes);

		ll.log("PRINTING ALL SESSION ATTRIBUTES:");
		for (Map.Entry<String, String> entry : sessionAttributes.entrySet()) {
			ll.log(entry.getKey() + " : " + entry.getValue());
		}

		return lexRes;
	}

	private DialogActionElicitSlot getCustomerNameDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotGetCustomerName");
		da.setMessage(new Message("OK, enter your full name:"));
		inputSlots.clear();

		
			inputSlots.put("customerFullName", "x");
			da.setSlotToElicit("customerFullName");
		

		da.setSlots(inputSlots);
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private DialogActionElicitSlot getNoteDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		// remove/reset session attribs for this intent
		sessionAttributes.remove("s_noteText");
		sessionAttributes.remove("s_noteConfirmed");

		da.setIntentName("rosyGetNote");
		da.setMessage(new Message("Enter a short message to be printed on a note with the flowers:"));
		inputSlots.clear();
		inputSlots.put("noteText", "x");
		inputSlots.put("noteConfirmed", "x");

		da.setSlots(inputSlots);
		da.setSlotToElicit("noteText");
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private DialogActionElicitSlot getNoteConfirmationDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		da.setIntentName("rosyGetNote");
		da.setMessage(new Message("Confirm note [y or n]:" + inputSlots.get("noteText")));

		da.setSlots(inputSlots);
		da.setSlotToElicit("noteConfirmed");
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private DialogActionElicitSlot getRecipientNameDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		// get by sharing contact
		da.setIntentName("GetRecipientName");
		inputSlots.clear();
		inputSlots.put("recipientFullName", null);
		inputSlots.put("saturdayDeliveryConfirm", "x");
		da.setSlots(inputSlots);
		da.setSlotToElicit("recipientFullName");
		String messagePrompt = "Enter the full name of the recipient, or type 1 to share the contact of the recpient.";
		da.setMessage(new Message(messagePrompt));

		return da;

	}

	
	
	
	
	private void cleanTheLexBS() {
		// for some reason need to remvoe some old slots here, they are
		// carrying over, WTF!!!!!!!!
		if (inputSlots.containsKey("customerFullName")) {
			inputSlots.remove("customerFullName");
		}
		if (inputSlots.containsKey("saturdayDeliveryConfirm")) {
			inputSlots.remove("saturdayDeliveryConfirm");
		}
	}
	
	

}
