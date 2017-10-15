package com.rosybot.customernamefulfillment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
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
import com.rosybot.models.RosyProduct;
import com.rosybot.services.RosyOrderService;
import com.rosybot.services.RosyProductService;

public class CustomernamefulfillmentLambda implements RequestHandler<Object, Object> {
	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();

	private static Map<String, String> sessionAttributes = new HashMap<String, String>();
	private static Map<String, String> inputSlots = new HashMap<String, String>();
	private static CurrentIntent currentIntent;
	
	private static final String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";

	private static final String F1_SHIPPING_FEE = System.getenv("F1_SHIPPING_FEE");
	private static final String SAT_SHIPPING_FEE = System.getenv("SAT_SHIPPING_FEE");

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
		
		ll.log("RECEIVING - ALL SESSION ATTRIBUTES:");
		for (Map.Entry<String, String> entry : sessionAttributes.entrySet()) {
			ll.log(entry.getKey() + " : " + entry.getValue());
		}
		

		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		ll.log("input transcript:" + lexReq.getInputTranscript());

		String custFullName = inputSlots.get("customerFullName");
		sessionAttributes.put("s_customerFullName", custFullName);
		
		da = getRecipientNameDA();
		


		lexRes.setDialogAction(da);
		lexRes.setSessionAttributes(sessionAttributes);
		
//		ll.log("SENDING -  ALL SESSION ATTRIBUTES:");
//		for (Map.Entry<String, String> entry : sessionAttributes.entrySet()) {
//			ll.log(entry.getKey() + " : " + entry.getValue());
//		}

		return lexRes;
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


	
	
	
	

}
