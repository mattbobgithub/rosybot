package com.rosybot.rosyvcardfulfillment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder; 

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosybot.models.CurrentIntent;
import com.rosybot.models.DialogActionElicitSlot;
import com.rosybot.models.GenericAttachment;
import com.rosybot.models.LexRequest;
import com.rosybot.models.LexResponse;
import com.rosybot.models.Message;
import com.rosybot.models.RecipientAddress;
import com.rosybot.models.ResponseCard;
import com.rosybot.models.RosyOrder;
import com.rosybot.models.RosyProduct;
import com.rosybot.services.RecipientAddressService;
import com.rosybot.services.RosyOrderService;
import com.rosybot.services.RosyProductService;
 

public class vcardfulfillment implements RequestHandler<Object, Object> {

	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();

	private static final String SAT_SHIPPING_FEE = System.getenv("SAT_SHIPPING_FEE");

	private Map<String, String> sessionAttributes = new HashMap<String, String>();
	private Map<String, String> inputSlots = new HashMap<String, String>();
	private CurrentIntent currentIntent;
  
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
		inputSlots.clear();
		inputSlots.putAll(currentIntent.getSlots());
		ll.log("INPUT SLOTS:" + inputSlots.toString());

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		ll.log("input transcript:" + lexReq.getInputTranscript());
		String inputTranscript = lexReq.getInputTranscript();
		boolean incompleteAddress = false;
		boolean bypassOrderUrl = false;

		if (StringUtils.beginsWithIgnoreCase(inputTranscript, "https://api.twilio.com")) {

			String derivedAddressForConfirmation = "";
			// CHECK IF VCARD INPUT HERE:

			sessionAttributes.put("s_vCardInd", "true");
			// add vCard address to db, add vCard address id to
			// sessionVariables.
			RecipientAddressService ras = new RecipientAddressService();
			RecipientAddress vCardAddress = ras.getAddressFromVCardUrl(inputTranscript);

			if (!StringUtils.isNullOrEmpty(vCardAddress.getStreetAddress1())) {
				sessionAttributes.put("s_streetAddress1", vCardAddress.getStreetAddress1());
			} else {
				incompleteAddress = true;
			}
			if (!StringUtils.isNullOrEmpty(vCardAddress.getStreetAddress2())) {
				sessionAttributes.put("s_streetAddress2", vCardAddress.getStreetAddress2());
			}
			if (!StringUtils.isNullOrEmpty(vCardAddress.getCity())) {
				sessionAttributes.put("s_city", vCardAddress.getCity());
				incompleteAddress = false;
			} else {
				incompleteAddress = true;
			}
			if (!StringUtils.isNullOrEmpty(vCardAddress.getState())) {
				sessionAttributes.put("s_state", vCardAddress.getState());
			} else {
				incompleteAddress = true;
			}
			if (!StringUtils.isNullOrEmpty(vCardAddress.getPostalCode())) {
				sessionAttributes.put("s_postalCode", vCardAddress.getPostalCode());
			} else {
				incompleteAddress = true;
			}

			if (StringUtils.isNullOrEmpty(vCardAddress.getCountry()) || vCardAddress.getCountry().equals("USA")) {
				sessionAttributes.put("s_country", "US");
			}

			if (!StringUtils.isNullOrEmpty(vCardAddress.getPhone())) {
				sessionAttributes.put("s_recipientPhone", vCardAddress.getPhone());
			}

			if (!StringUtils.isNullOrEmpty(vCardAddress.getFullName())) {
				ll.log("vcardFullName: " + vCardAddress.getFullName());
				sessionAttributes.put("s_recipientFullName", vCardAddress.getFullName());
			}

			derivedAddressForConfirmation = " NAME:" + vCardAddress.getFullName().trim() + ",";
			derivedAddressForConfirmation = derivedAddressForConfirmation + " ADDRESS:";

			if (!StringUtils.isNullOrEmpty(vCardAddress.getStreetAddress1())) {
				derivedAddressForConfirmation = derivedAddressForConfirmation + vCardAddress.getStreetAddress1() + ", ";
			}

			if (!StringUtils.isNullOrEmpty(vCardAddress.getStreetAddress2())) {
				derivedAddressForConfirmation = derivedAddressForConfirmation + vCardAddress.getStreetAddress2() + ", ";
			}

			if (!StringUtils.isNullOrEmpty(vCardAddress.getCity())) {
				derivedAddressForConfirmation = derivedAddressForConfirmation + vCardAddress.getCity() + ", ";
			}

			if (!StringUtils.isNullOrEmpty(vCardAddress.getState())) {
				derivedAddressForConfirmation = derivedAddressForConfirmation + vCardAddress.getState() + " ";
			}

			if (!StringUtils.isNullOrEmpty(vCardAddress.getPostalCode())) {
				derivedAddressForConfirmation = derivedAddressForConfirmation + vCardAddress.getPostalCode() + ", ";
			}
			if (!StringUtils.isNullOrEmpty(vCardAddress.getPhone())) {
				derivedAddressForConfirmation = derivedAddressForConfirmation + " PHONE:" + vCardAddress.getPhone();
			}

			ll.log(derivedAddressForConfirmation);

		}else{
ll.log("not vcard input, assuming name, but could be no for sat delivery ");
if(!inputTranscript.toLowerCase().equals("n") && !inputTranscript.toLowerCase().equals("y")){
				//not a vcard input, must be just a name, verify and move on to show url. 
			incompleteAddress = true;
			String[] inputArray = inputTranscript.split(" ");
			if(inputArray.length > 1){
				sessionAttributes.put("s_recipientFullName", inputTranscript);
			}else{
				//send back to get recipient full name. 
				ll.log("sending back to get recipient name.");
				da = getRecipientNameDA();
				bypassOrderUrl = true;
			}
			
		}
else{
	ll.log("input transcript n or y");
}
		}
		 
		
		
		
		
		
		if(!bypassOrderUrl){
			// Calculate delivery date and show saturday delivery confirmation
			// if requried
			DateTime todayEasternDateTime = new DateTime(
					DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
			int todayHour = todayEasternDateTime.getHourOfDay();
			int deliveryDaysFromToday = 1;

			if (todayHour > 12) {
				deliveryDaysFromToday = 2;
			}

			DateTime deliveryDateEasternDateTime = todayEasternDateTime.plusDays(deliveryDaysFromToday);

			DateTimeFormatter dtfOut = DateTimeFormat.forPattern("MM-dd-yyyy");

			sessionAttributes.put("s_proposedDeliveryDate", dtfOut.print(deliveryDateEasternDateTime));

			String deliveryConfirmationMessage = "";

			ll.log("incomplete address:" + incompleteAddress);

			if (deliveryDaysFromToday == 2) {
				deliveryConfirmationMessage = deliveryConfirmationMessage + "After 1pm Eastern adds extra day. ";
			}

			// set default sat delivery option

			sessionAttributes.put("s_satDeliveryInd", "N");
			// verify calculated delivery date
			// ll.log("deliveryDate day of week: " +
			// deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + ",
			// in int:" + deliveryDateEasternDateTime.dayOfWeek().get());
			switch (deliveryDateEasternDateTime.dayOfWeek().get()) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
				// do nothing if delivery day is M-F --just inform user of 2 day
				// instead of 1 day
				deliveryConfirmationMessage = deliveryConfirmationMessage + "Delivery is set for "
						+ deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + " " + getDeliveryDateDisplay(deliveryDateEasternDateTime)
						+ ". ";
				da = showOrderUrlDA(deliveryConfirmationMessage, incompleteAddress);

				break;
			case 6:
				// Saturday
				if (inputSlots.get("saturdayDeliveryConfirm").toLowerCase().equals("y")) {

					sessionAttributes.put("s_satDeliveryInd", "Y");

					deliveryConfirmationMessage = "Delivery is set for "
							+ deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + " " + getDeliveryDateDisplay(deliveryDateEasternDateTime)
							+ ". ";
					da = showOrderUrlDA(deliveryConfirmationMessage, incompleteAddress);

				} else {

					if (inputSlots.get("saturdayDeliveryConfirm").toLowerCase().equals("n")) {

						deliveryDateEasternDateTime = deliveryDateEasternDateTime.plusDays(2);
					
						deliveryConfirmationMessage = "Delivery is set for "
								+ deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + " "
								+ getDeliveryDateDisplay(deliveryDateEasternDateTime) + ". ";
						da = showOrderUrlDA(deliveryConfirmationMessage, incompleteAddress);

					} else {

						deliveryConfirmationMessage = deliveryConfirmationMessage
								+ "Saturday Delivery is $15 extra [y to accept, n to move delivery to Monday]:";
						da = showSaturdayDeliveryOptionDA(deliveryConfirmationMessage);
					}
				}

				break;
			case 7:
				// Sundays
				// add another day if falls on sunday
				deliveryDateEasternDateTime = deliveryDateEasternDateTime.plusDays(1);

				deliveryConfirmationMessage = deliveryConfirmationMessage
						+ "We cannot deliver on Sunday. Delivery Date is set for "
						+ deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + " " + getDeliveryDateDisplay(deliveryDateEasternDateTime)
						+ ". ";

				da = showOrderUrlDA(deliveryConfirmationMessage, incompleteAddress);
				break;
			}

			// must be recipient full name

			// da.setIntentName("rosyBotgetVCard");
			// da.setMessage(new Message(
			// "look good? [y or n]:" + derivedAddressForConfirmation));
			//
			// inputSlots.clear();
			// inputSlots.put("deliveryAddressConfirmation", null);
			//
			// da.setSlots(inputSlots);
			// da.setSlotToElicit("deliveryAddressConfirmation");

			ll.log("INTENT TO BE CALLED:" + da.getIntentName());
			ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		}


		lexRes.setDialogAction(da);
		lexRes.setSessionAttributes(sessionAttributes);

//		ll.log("SENDING THESE SESSION ATTRIBUTES:");
//		for (Map.Entry<String, String> entry : lexRes.getSessionAttributes().entrySet()) {
//			ll.log(entry.getKey() + " : " + entry.getValue());
//		}
ll.log("sending response elicit intent of:" + da.getIntentName());
		return lexRes;
	}

	private DialogActionElicitSlot getFastDeliveryAddressDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetDeliveryAddress");
		da.setMessage(new Message("Enter only street number and name of delivery address [without unit/apt num]:"));
		setFastAddressInputSlots();
		da.setSlots(inputSlots);
		da.setSlotToElicit("streetAddress");
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private void setFastAddressInputSlots() {
		// put slots into session variables
		// remove slots from previous intent ??
		inputSlots.clear();

		if (sessionAttributes.containsKey("s_recipientFullName")) {
			inputSlots.put("recipientFullName", sessionAttributes.get("s_recipientFullName"));
		} else {
			inputSlots.put("recipientFullName", "x");
		}

		if (sessionAttributes.containsKey("s_recipientPhone")) {
			inputSlots.put("recipientPhone", sessionAttributes.get("s_recipientPhone"));
		} else {
			inputSlots.put("recipientPhone", "x");
		}

		inputSlots.put("streetAddress", "x");
		inputSlots.put("aptNo", "x");
		inputSlots.put("postalCode", "x");
		inputSlots.put("addressConfirmed", "x");

	}

	private DialogActionElicitSlot showOrderUrlDA(String deliveryMessage, boolean incompleteAddress) {

		// create order

		String orderUrl = createOrderUrl();

		// create display message
		String messageText = "";
   
		if (sessionAttributes.containsKey("s_orderJustDisplayOrderFlag")
				&& sessionAttributes.get("s_orderJustDisplayOrderFlag").equals("Y")) {
			messageText = "Thanks for your order, you can review the info here:  ";
		} else {
			if (incompleteAddress) {
				deliveryMessage = deliveryMessage + "I couldn't get all delivery info from the contact. ";
			}
			messageText = deliveryMessage + "Verify and checkout here:  ";

		}

		messageText = messageText + orderUrl;
		;

		DialogActionElicitSlot da = new DialogActionElicitSlot();
		da.setIntentName("rosyBotShowOrderUrl");

		da.setMessage(new Message(messageText));

		inputSlots.clear();
		inputSlots.put("followUpSlot", "x");

		da.setSlots(inputSlots);
		da.setSlotToElicit("followUpSlot");
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private String createOrderUrl() {
		String OrderNo = "sampleOrderNumber";

		// build order from sessionAttributes and write to mysql
		if (StringUtils.isNullOrEmpty(sessionAttributes.get("s_orderPlacedInd"))
				|| !sessionAttributes.get("s_orderPlacedInd").equals("Y")) {

			// create rosy order (very similar to F1 order. Then checkout will
			// create F1 order
			String selectedProductCode = sessionAttributes.get("s_globalRoseProdId");
			RosyProductService rps = new RosyProductService();
			RosyProduct rp = rps.fromProductCode(selectedProductCode);
			sessionAttributes.put("s_rosyProductId", StringUtils.fromInteger(rp.getId()));
			sessionAttributes.put("s_price", rp.getPrice());
			sessionAttributes.put("s_productImageUrl", rp.getImageURL());
			sessionAttributes.put("s_productName", rp.getName());
			
			if (sessionAttributes.get("s_satDeliveryInd").equals("Y")){
			Double price = Double.parseDouble(rp.getPrice());
			Double satShipFee = Double.parseDouble(SAT_SHIPPING_FEE);
			Double ordTot = price + satShipFee;
			sessionAttributes.put("s_orderTotal", StringUtils.fromDouble(ordTot));
			}else{
				sessionAttributes.put("s_orderTotal",  rp.getPrice());
			}
			    

			sessionAttributes.put("s_deliveryDate", sessionAttributes.get("s_proposedDeliveryDate"));
			
			RosyOrderService ros = new RosyOrderService();
			
			OrderNo = ros.createRosyGlobalRoseOrder(sessionAttributes);
 
			sessionAttributes.put("s_orderId", OrderNo);
			sessionAttributes.put("s_orderPlacedInd", "Y");
		} else {
			// ll.log(" ...called get order again, but order already has been
			// placed, just display existing order.");
			sessionAttributes.put("s_orderJustDisplayOrderFlag", "Y");
		}

		String encodedOrderNumber = sessionAttributes.get("s_orderId");
		String orderUrl = "http://rosybot.com/checkout.html?ro=" + encodedOrderNumber;
		// return order url
		return orderUrl;
	}

	private DialogActionElicitSlot showSaturdayDeliveryOptionDA(String orderConfirmationMessage) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		da.setIntentName("rosyBotgetVCard");
		da.setMessage(new Message(orderConfirmationMessage));

		da.setSlots(inputSlots);
		da.setSlotToElicit("saturdayDeliveryConfirm");
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
		da.setSlots(inputSlots);
		da.setSlotToElicit("recipientFullName");
		String messagePrompt = "Enter the full name of the recipient, or type 1 to share the contact of the recpient.";
		da.setMessage(new Message(messagePrompt));

		return da;

	}

	private String getDeliveryDateDisplay(DateTime dt){
		return StringUtils.fromInteger(dt.getMonthOfYear()) + "/" + StringUtils.fromInteger(dt.getDayOfMonth());

	}
	

}
