package com.rosybot.rosyintroprod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.rosybot.models.CurrentIntent;
import com.rosybot.models.DialogActionClose;
import com.rosybot.models.DialogActionElicitSlot;
import com.rosybot.models.GenericAttachment;
import com.rosybot.models.LexRequest;
import com.rosybot.models.LexResponse;
import com.rosybot.models.Message;
import com.rosybot.models.ResponseCard;
import com.rosybot.models.RosyCustomer;
import com.rosybot.models.RosyOrder;
import com.rosybot.models.Enums.RosyOrderStatus;
import com.rosybot.services.RosyCustomerService;
import com.rosybot.services.RosyOrderService;

public class rosyintroprodLambda implements RequestHandler<Object, Object> {
	private static final String F1_SUPPORT_NUMBER = System.getenv("F1_SUPPORT_NUMBER");
	private static final String GR_SUPPORT_NUMBER = System.getenv("GR_SUPPORT_NUMBER");

	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();
	private Map<String, String> sessionAttributes = new HashMap<String, String>();

	private Map<String, String> inputSlots = new HashMap<String, String>();

	@Override
	public Object handleRequest(Object input, Context context) {

		ll = context.getLogger();

		lexReq = LexRequest.fromLexObject(input);

		ll.log("CURRENT INTENT: " + lexReq.getCurrentIntent().getName() + "     request obj: " + lexReq.toString());

		CurrentIntent currentIntent = lexReq.getCurrentIntent();
		if (null == currentIntent.getSlots() || currentIntent.getSlots().isEmpty()) {
			// do nothing
			// ll.log("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXcurrent
			// intent slots is empty or null");
		} else {
			inputSlots.putAll(currentIntent.getSlots());
		}
		
		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");

		ll.log("received LEXREQ sessionAttributes:");
		for (Map.Entry<String, String> entry : lexReq.getSessionAttributes().entrySet()) {
			ll.log(entry.getKey() + " : " + entry.getValue());
		}
				

//		if (lexReq.getInputTranscript().trim().toLowerCase().equals("reset")
//				|| lexReq.getInputTranscript().trim().toLowerCase().equals("roses")) {
//			inputSlots.clear();
//
//		}
		inputSlots.clear();
		inputSlots.put("recipientRoute", lexReq.getInputTranscript().trim());
		
		if(lexReq.getInputTranscript().trim().toLowerCase().equals("reset") || lexReq.getInputTranscript().trim().toLowerCase().equals("roses")){
			//reset everything and don't er-populate from request.
			sessionAttributes.clear();
			
		}else{
			sessionAttributes.clear();			
			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		String introMessage = "";
boolean addPictureToIntroResponse = false;


//if customer types reset or roses mid-order, need to mimic 

		if (!sessionAttributes.containsKey("s_incomingPhoneNumber")) {
			ll.log("incoming phone session attribute not set yet, must be first time through. ...setting it now.");
			if(customerPhone.startsWith("1")){
			customerPhone = customerPhone.substring(1);
			}
			sessionAttributes.put("s_incomingPhoneNumber", customerPhone);

			// check if customer exists already
			if (!sessionAttributes.containsKey("existingCustomerFlag")) {
				ll.log("existing customer flag not set yet.  ....setting it now");

				sessionAttributes.put("existingCustomerFlag", "false");
				RosyCustomerService rosycustsvc = new RosyCustomerService();
				RosyCustomer existingCustomer = rosycustsvc.getRosyCustomerByPhone(customerPhone);

				if (existingCustomer != null && !StringUtils.isNullOrEmpty(existingCustomer.getFirstName())) {
					sessionAttributes.put("existingCustomerFirstName", existingCustomer.getFirstName());
					sessionAttributes.put("existingCustomerLastName", existingCustomer.getLastName());
					sessionAttributes.put("s_customerFullName",
							existingCustomer.getFirstName() + " " + existingCustomer.getLastName());
				//	sessionAttributes.put("existingCustomerEmail", existingCustomer.getEmail()); 
					sessionAttributes.put("s_customerEmail", existingCustomer.getEmail());
					sessionAttributes.put("existingCustomerId", StringUtils.fromInteger(existingCustomer.getId()));

					sessionAttributes.put("existingCustomerFlag", "true");
					// blank intro message and re-populate
					introMessage = "";
					introMessage = "Hi " + WordUtils.capitalize(existingCustomer.getFirstName()) + ",";

					// check for existing order:
					RosyOrder foundRosyOrder = new RosyOrder();
					RosyOrderService ros = new RosyOrderService();
					foundRosyOrder = ros.getRosyOrder(StringUtils.fromInteger(existingCustomer.getId()), "CustomerId");
					if (foundRosyOrder != null) {

						sessionAttributes.put("existingRecipientFullName", foundRosyOrder.getRecipientFullName());

						introMessage = introMessage + " your order";
						if(!StringUtils.isNullOrEmpty(foundRosyOrder.getRosyOrderId())){
							introMessage = introMessage + "(#" + foundRosyOrder.getRosyOrderId() + ")";
						}
						
						introMessage = introMessage	+ " to "	+ foundRosyOrder.getRecipientFullName();

						DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy");
						DateTime deliveryDt = formatter.parseDateTime(foundRosyOrder.getDeliveryDate());
						LocalDate deliveryDateLocal = deliveryDt.toLocalDate();

						DateTime todayDt = DateTime.now();

						LocalDate todayDateLocal = todayDt.toLocalDate();

						if (foundRosyOrder.getRosyOrderStatus().equals(RosyOrderStatus.DRAFT)) {
							introMessage = introMessage + " has not been completed, " + "click link to checkout: "
									+ "http://rosybot.com/checkout.html?ro=" + foundRosyOrder.getId().toString();
							introMessage = introMessage
									+ ". For a new order, type 'y' for a dozen red premium roses, or type 'more' for color and amount options. ";
						} else {

							if (deliveryDateLocal.isBefore(todayDateLocal)) {
								introMessage = introMessage + " was delivered on " + foundRosyOrder.getDeliveryDate()
										+ ".";
							} else {
								introMessage = introMessage + " is scheduled for delivery on "
										+ foundRosyOrder.getDeliveryDate();
								if (foundRosyOrder.getRosyOrderId().substring(0, 2).equals("GR")) {
									introMessage = introMessage
											+ ".  If you have an issue you can call GlobalRose support at "
											+ GR_SUPPORT_NUMBER;
								} else {
									introMessage = introMessage
											+ ".  If you have an issue you can call FloristOne support at "
											+ F1_SUPPORT_NUMBER;
								}
							}
							introMessage = introMessage
									+ ". For a new order, type 'y' for a dozen red premium roses, or type 'more' for color and amount options. ";

						}
					} else {
						// repeat customer, no existing order --add picture of roses (no
						introMessage = introMessage
								+ ". For a new order, type 'y' for a dozen red premium, roses, or type 'more' for color and amount options. ";
						addPictureToIntroResponse = true;
					}
				} else {

					// new customer, record to database prior to anything.
					RosyCustomerService rcs = new RosyCustomerService();
					int createdCustId = rcs.createCustomerByPhone(customerPhone);
					sessionAttributes.put("existingCustomerId", StringUtils.fromInteger(createdCustId));
					
					
					introMessage = introMessage + "Hi, I'm ROSY! ";
					introMessage = introMessage + "Reply 'y' for a dozen red premium roses delivered tomorrow for $40, ..or type 'more' for different color and amount options. ";
					
					//add main offer da with picture of roses
					addPictureToIntroResponse = true;
					
					

				}
			}

			
			da = createIntroResponseDA(addPictureToIntroResponse, introMessage);
			lexRes.setDialogAction(da);
			
			
		}else{
			
			//second time through, route to appropriate intent
			
			String recipientRoute = inputSlots.get("recipientRoute").toLowerCase();
			
			if(recipientRoute.equals("y")){
				// accept main offer and move to get note
				//set product selection sessionAttributes
				sessionAttributes.put("s_productColor", "red");
				sessionAttributes.put("s_productAmount", "12");
				sessionAttributes.put("s_productConfirmed", "y");
				

				sessionAttributes.put("s_globalRoseProdId", "c12");
				da=getGetNoteDA();
				lexRes.setDialogAction(da);
				
			}else{
				
				
				if(recipientRoute.equals("more")){
					// go to product selection intent.
					da=getColorOptionsDA();
					lexRes.setDialogAction(da);
					
					
				}else{
					// goodbye with close dialog. 
					DialogActionClose daClose =getByeDA();
					lexRes.setDialogAction(daClose);
					
				}
				
				
				   
			}
			   
			
			
		}
		
	
		lexRes.setSessionAttributes(sessionAttributes);
		
		ll.log("SENDING THESE SESSION ATTRIBUTES:");
		for (Map.Entry<String, String> entry : lexRes.getSessionAttributes().entrySet()) {
			ll.log(entry.getKey() + " : " + entry.getValue());
		}

		return lexRes;
	}
	


	
	private DialogActionElicitSlot createIntroResponseDA(boolean addPic, String introMsg) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotIntro");
		da.setMessage(new Message(introMsg));
		// setInputSlots(da.getIntentName());
		inputSlots.clear();
		inputSlots.put("recipientRoute", "x");

		da.setSlots(inputSlots);
		da.setSlotToElicit("recipientRoute");

		if(addPic){
		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("dozen red roses offer");
		ga.setSubTitle("dozen red roses offer");
		ga.setImageUrl("https://www.rosybot.com/images/products/c12.jpg");
		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);

		da.setResponseCard(rc);
		}
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}
	
	
	private DialogActionElicitSlot getGetNoteDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyGetNote");
		da.setMessage(new Message("Enter a short message to be printed on a note with the flowers:"));
		// setInputSlots(da.getIntentName());
		inputSlots.clear();
		inputSlots.put("noteText", "x");
		inputSlots.put("noteConfirmed", "x");

		da.setSlots(inputSlots);
		da.setSlotToElicit("noteText");

		
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}
	
	private DialogActionElicitSlot getColorOptionsDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotGetProductSelection");
		da.setMessage(new Message("Enter letter of color [A-I]:"));
		// setInputSlots(da.getIntentName());
		inputSlots.clear();
		inputSlots.put("colorOption", "x");
		inputSlots.put("amountOption", "x");
		inputSlots.put("productRoute", "more");
		inputSlots.put("productConfirmed", "x");

		da.setSlots(inputSlots);
		da.setSlotToElicit("colorOption");


		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("dozen red roses offer");
		ga.setSubTitle("dozen red roses offer");
		ga.setImageUrl("https://www.rosybot.com/images/products/colorOptions.jpg");
		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);

		da.setResponseCard(rc);
		
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}
	
	
	private DialogActionClose getByeDA() {
		DialogActionClose da = new DialogActionClose();
		da.setFulfillmentState("Fulfilled");
		da.setMessage(new Message("Ok, bye. Just text me 'roses' anytime you need roses."));	
		
		ll.log("CLOSING DIALOG");

		return da;
	}
	
	
	
}
