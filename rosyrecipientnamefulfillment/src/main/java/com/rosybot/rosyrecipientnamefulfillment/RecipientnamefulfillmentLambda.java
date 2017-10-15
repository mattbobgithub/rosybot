package com.rosybot.rosyrecipientnamefulfillment;

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
import com.rosybot.models.RosyProduct;
import com.rosybot.services.RosyOrderService;
import com.rosybot.services.RosyProductService;

public class RecipientnamefulfillmentLambda implements RequestHandler<Object, Object> {

	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse(); 
	

	private static final String SAT_SHIPPING_FEE = System.getenv("SAT_SHIPPING_FEE");
  
	private static Map<String, String> sessionAttributes = new HashMap<String, String>();

	private static Map<String, String> inputSlots = new HashMap<String, String>();
	private static CurrentIntent currentIntent;  

	@Override
	public Object handleRequest(Object input, Context context) {
		ll = context.getLogger();
		lexReq = LexRequest.fromLexObject(input);

		ll.log("XXXXXXXXXXXXXXXXXXXXX------------INPUT TRANSCRIPT:" + lexReq.getInputTranscript());

		ll.log("CURRENT INTENT: " + lexReq.getCurrentIntent().getName() + "     request obj: " + lexReq.toString());

		CurrentIntent currentIntent = lexReq.getCurrentIntent();
		inputSlots.clear();
		inputSlots.putAll(currentIntent.getSlots());
		ll.log("INPUT SLOTS:" + inputSlots.toString());

		if (lexReq.getSessionAttributes() != null) {
			sessionAttributes.clear();
			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}
 
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		String inputTranscript = lexReq.getInputTranscript();
		
		if(inputTranscript.equals("1")){
			da = getVCardDA();
		}else{

			if(!inputTranscript.toLowerCase().equals("n") && !inputTranscript.toLowerCase().equals("y")){
			String recipFullName = inputSlots.get("recipientFullName");
			sessionAttributes.put("s_recipientFullName", recipFullName);
			}
	
			// Calculate delivery date and show saturday delivery confirmation

			DateTime todayEasternDateTime = new DateTime(
					DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
			int todayHour = todayEasternDateTime.getHourOfDay();
			int deliveryDaysFromToday = 1;

			if (todayHour > 12) {
				deliveryDaysFromToday = 2;
			}

			DateTime deliveryDateEasternDateTime = todayEasternDateTime.plusDays(deliveryDaysFromToday);

			DateTimeFormatter dtfOut = DateTimeFormat.forPattern("MM-dd-yyyy");

		//String proposedDeliveryDate = dtfOut.print(deliveryDateEasternDateTime);

			sessionAttributes.put("s_proposedDeliveryDate", dtfOut.print(deliveryDateEasternDateTime));

			String deliveryConfirmationMessage = "";



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
				da = showOrderUrlDA(deliveryConfirmationMessage);

				break;
			case 6:
				// Saturday
				if (inputSlots.get("saturdayDeliveryConfirm").toLowerCase().equals("y")) {

					sessionAttributes.put("s_satDeliveryInd", "Y");

					deliveryConfirmationMessage = "Delivery is set for "
							+ deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + " " + getDeliveryDateDisplay(deliveryDateEasternDateTime)
							+ ". ";
					da = showOrderUrlDA(deliveryConfirmationMessage);

				} else {

					if (inputSlots.get("saturdayDeliveryConfirm").toLowerCase().equals("n")) {

						deliveryDateEasternDateTime = deliveryDateEasternDateTime.plusDays(2);
						//proposedDeliveryDate = dtfOut.print(deliveryDateEasternDateTime);
						
						deliveryConfirmationMessage = "Delivery is set for "
								+ deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + " "
								+ getDeliveryDateDisplay(deliveryDateEasternDateTime) + ". ";
						da = showOrderUrlDA(deliveryConfirmationMessage);

					} else {

						ll.log("show sat delivery option da.");
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
				//proposedDeliveryDate = dtfOut.print(deliveryDateEasternDateTime);

				deliveryConfirmationMessage = deliveryConfirmationMessage
						+ "We cannot deliver on Sunday. Delivery Date is set for "
						+ deliveryDateEasternDateTime.dayOfWeek().getAsText(Locale.US) + " " + getDeliveryDateDisplay(deliveryDateEasternDateTime)
						+ ". ";

				da = showOrderUrlDA(deliveryConfirmationMessage);
				break;
			}
			
			
			

		}
		
		
		
		

	
		ll.log("sending elicit intent:" + da.getIntentName());


		lexRes.setDialogAction(da);
		lexRes.setSessionAttributes(sessionAttributes);
		
		return lexRes;
		
		
	}

	private DialogActionElicitSlot getVCardDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		// get by sharing contact
		da.setIntentName("rosyBotgetVCard");
		inputSlots.clear();
		inputSlots.put("deliveryAddressConfirmation", null);
		inputSlots.put("saturdayDeliveryConfirm", "x");
		da.setSlots(inputSlots);
		da.setSlotToElicit("deliveryAddressConfirmation");
		String vCardPrompt = "Save me as a contact, then share the contact of the recipient with me in a text message (For delivery only, we won't contact the recipient). ..or if you'd rather, just enter the full name of the recipient.";
		da.setMessage(new Message(vCardPrompt));

		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("get contact");
		ga.setSubTitle("get contact");
		ga.setImageUrl("https://www.rosybot.com/ROSYBOT.vcf");

		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);

		da.setResponseCard(rc);
		return da;

	}
	
	
	private DialogActionElicitSlot showOrderUrlDA(String deliveryMessage) {

		// create order

		String orderUrl = createOrderUrl();

		// create display message
		String messageText = "";

		if (sessionAttributes.containsKey("s_orderJustDisplayOrderFlag")
				&& sessionAttributes.get("s_orderJustDisplayOrderFlag").equals("Y")) {
			messageText = "Thanks for your order, you can review the info here:  ";
		} else {
			
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
			
			//set delivyer date
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
		da.setIntentName("GetRecipientName");
		da.setMessage(new Message(orderConfirmationMessage));

		da.setSlots(inputSlots);
		da.setSlotToElicit("saturdayDeliveryConfirm");
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	
	private String getDeliveryDateDisplay(DateTime dt){
		return StringUtils.fromInteger(dt.getMonthOfYear()) + "/" + StringUtils.fromInteger(dt.getDayOfMonth());

	}

}
