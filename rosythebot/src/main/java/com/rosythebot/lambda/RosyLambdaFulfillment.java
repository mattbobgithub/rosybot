package com.rosythebot.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosythebot.f1.F1Request;
import com.rosythebot.models.GenericAttachment;
import com.rosythebot.models.ResponseCard;
import com.rosythebot.models.RosyCustomer;
import com.rosythebot.models.RosyProduct;
import com.rosythebot.services.RecipientAddressService;
import com.rosythebot.services.RosyCustomerService;
import com.rosythebot.services.RosyOrderService;
import com.rosythebot.services.RosyProductService;
import com.rosythebot.models.CurrentIntent;
import com.rosythebot.models.DialogActionElicitSlot;
import com.rosythebot.models.LexRequest;
import com.rosythebot.models.LexResponse;
import com.rosythebot.models.Message;
import com.rosythebot.models.RecipientAddress;

public class RosyLambdaFulfillment implements RequestHandler<Object, Object> {
	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();

	private static Map<String, String> sessionAttributes = new HashMap<String, String>();
	private static Map<String, String> inputSlots = new HashMap<String, String>();
	private static CurrentIntent currentIntent;

	private static final int OFFER_COUNT_LIMIT = Integer.parseInt(System.getenv("OFFER_COUNT_LIMIT"));
	private static final int RETRY_GET_DELIVERY_DATE_ATTEMPTS = 3;
	private static final String F1_SHIPPING_FEE = System.getenv("F1_SHIPPING_FEE");

	@Override
	public Object handleRequest(Object input, Context context) {

		ll = context.getLogger();
	//	context.getLogger().log("Input: " + input);

	 
		///////////////////////////////////////////////////////
		// get lexRequest and populate intance variables ////
		///////////////////////////////////////////////////////
		lexReq = LexRequest.fromLexObject(input);
		ll.log(lexReq.toString());

		currentIntent = lexReq.getCurrentIntent();
		
	

		//always copy session variables over  !!! should never be empty because intro validlation function populates some
		sessionAttributes.putAll(lexReq.getSessionAttributes());
		
		ll.log("NEW REQUEST----------------------------------------- CURRENT INTENT: " + currentIntent.getName() + "    CONFIRMATIONSTATUS:" + currentIntent.getConfirmationStatus());
		ll.log("SESSION ATTRIBUTES BELOW:");
		for(Map.Entry<String,String> entry : sessionAttributes.entrySet()){
			ll.log(entry.getKey() + " : " + entry.getValue());
		}
		
		
		if (null == currentIntent.getSlots() || currentIntent.getSlots().isEmpty()) {
			// do nothing
			ll.log("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXcurrent intent slots is empty or null");
		} else {
			inputSlots.putAll(currentIntent.getSlots());
		}

//		if(!sessionAttributes.containsKey("s_incomingPhoneNumber")){
//			
//			String customerPhone = lexReq.getUserId();
//			customerPhone=customerPhone.replaceAll("[^\\d]", "");
//			sessionAttributes.put("s_incomingPhoneNumber", customerPhone);
//			//check if customer exists already
//			RosyCustomerService rosycustsvc = new RosyCustomerService();
//			RosyCustomer existingCustomer = rosycustsvc.getRosyCustomerByPhone(customerPhone);
//			if(existingCustomer != null){
//				ll.log("foundCustomer: " + existingCustomer.getFirstName() + " " + existingCustomer.getLastName());
//			sessionAttributes.put("existingCustomerFirstName", existingCustomer.getFirstName());
//			}
//		}
//		
		
		/// trim spaces from input and set y and n.
		for (Map.Entry<String, String> entry : inputSlots.entrySet()) { 
			//replace yeses and nos with y and n and then just trim spaces from everything else
			if(!StringUtils.isNullOrEmpty(entry.getValue())){
			if (entry.getValue().toLowerCase().trim().equals("yes")) {
				inputSlots.replace(entry.getKey(), "y");
			} else if (entry.getValue().toLowerCase().trim().equals("no")) {
				inputSlots.replace(entry.getKey(), "n");
			} else {
				// just trim everything else
				inputSlots.replace(entry.getKey(), entry.getValue().trim());
			}
			}
		}
		lexReq.setInputTranscript(lexReq.getInputTranscript().trim());

		///////////////////////////////////////////////////////
		///////////////////////////////////////////////////////

		///////////////////////////////////////////////////////
		////// determine next action - route request
		/////////////////////////////////////////////////////// //////////////////////////////////
		///////////////////////////////////////////////////////
		String nextAction = determineNextAction();
		///////////////////////////////////////////////////////
		///////////////////////////////////////////////////////

		addNextdialogAction(nextAction);

		ll.log("----------------------------------------------------------------SENDING RESPONSE FROM LAMBDA FULFILLMENT:");
		ll.log(lexRes.getDialogAction().toString());
		ll.log(lexRes.getSessionAttributes().toString());
		return lexRes;
	}

	private String determineNextAction() {
		String nextAction = "";

		// if first fulfillment, initial session
		if (sessionAttributes == null || sessionAttributes.isEmpty()) {

			sessionAttributes.put("currentFulfilledAction", "Intro");
		} else {
			// not first fulfillment, see where we are and initialize some
			// things
			String returnedIntentName = lexReq.getCurrentIntent().getName();
			if (returnedIntentName.equals("Intro")) {
				// clear session if not first call and Intro is the calling
				// method (must be a reset)
				inputSlots.clear();
				
				//only copy over some session attribs

				String ecfn = sessionAttributes.get("existingCustomerFirstName");
				String ecln = sessionAttributes.get("existingCustomerLastName");
				String eci = sessionAttributes.get("existingCustomerId");
				String ecf = sessionAttributes.get("existingCustomerFlag");
				String ipn = sessionAttributes.get("s_incomingPhoneNumber");
				
				sessionAttributes.clear();

				sessionAttributes.put("existingCustomerFirstName", ecfn);				
				sessionAttributes.put("existingCustomerLastName", ecln);				
				sessionAttributes.put("existingCustomerFlag", ecf);				
				sessionAttributes.put("existingCustomerId", eci);
				sessionAttributes.put("s_incomingPhoneNumber", ipn);
				
			}
			ll.log("setting fulfilled action from returned current intent of:" + returnedIntentName);
			sessionAttributes.put("currentFulfilledAction", returnedIntentName);
		}

		/// now determine next action
		String currentFulfilledAction = sessionAttributes.get("currentFulfilledAction");
		ll.log(" determine next action based on:" + currentFulfilledAction);

		switch (currentFulfilledAction) {
		case "Intro":
			// route to get occasion
			nextAction = "GetOccasion";
			break;
		case "GetOccasion":
			// route to get product selection
			nextAction = "GetProductSelection";
			break;
		case "GetProductSelection":
			// get product selection offer
			nextAction = "GetProductSelection";
			break;
		case "GetRecipientName":
			// get product selection offer
			nextAction = "GetNote";
			break;
		case "GetNote":
			// get confirmation of note
			nextAction = "GetNote";
			break;
		case "GetDeliveryAddress":
			// get confirmation of note
			nextAction = "GetDeliveryAddress";
			break;
		case "GetManualDeliveryAddress":
			// get confirmation of note
			nextAction = "GetManualDeliveryAddress";
			break;
		case "GetDeliveryPhone":
			// get receipientPHone
			nextAction = "GetDeliveryPhone";
			break;
		case "GetDeliveryDate":
			// get confirmation of note
			nextAction = "GetDeliveryDate";
			break;
		case "GetCustomerName":
			// get confirmation of note
			nextAction = "ShowOrderURL";
			break;
		case "ShowOrderURL":
			// get confirmation of note
			nextAction = "ShowOrderURL";
			break;

		}

		// first check if any session variables are set yet

		return nextAction;

	}

	private void addNextdialogAction(String nextAction) {

		DialogActionElicitSlot da = new DialogActionElicitSlot();
		switch (nextAction) {
		case "GetOccasion":
			// set a dummy sessionAttribute to make sure it gets initialized
			sessionAttributes.put("s_introPassed", "Y");
			da.setIntentName("GetOccasion");
			String occasionMessage = "What's the occasion (or flower type)? "
					+ "Say things like: 'Birthday', 'peach roses', 'just because', or 'get well'. "
					+ "Be patient while pics load and make sure data is on";
			
			da.setMessage(new Message(occasionMessage));
					setInputSlots(da.getIntentName());
			da.setSlots(inputSlots);
			da.setSlotToElicit("occasion");
			lexRes.setDialogAction(da);
			ll.log("prompting to get occasion");
			break;
		case "GetProductSelection":
			// set session occasion
			// check if product has been selected
			if (StringUtils.isNullOrEmpty(inputSlots.get("productSelected"))
					|| !inputSlots.get("productSelected").toLowerCase().equals("y")) {

				// initialize local offercount to 1, will be updated by session
				// next line
				int offerCount = 1;
				// initialize session occassion on first loop through
				if (StringUtils.isNullOrEmpty(inputSlots.get("productSelected"))) {
					if(inputSlots.containsKey("occasion") && StringUtils.isNullOrEmpty(inputSlots.get("occasion"))){
						//have to get occasion from input transcript cause lex is lame and slot type is not getting assigned
						inputSlots.put("occasion", lexReq.getInputTranscript());
						sessionAttributes.put("s_occasion", inputSlots.get("occasion"));
					}else{
						sessionAttributes.put("s_occasion", inputSlots.get("occasion"));
					}
					
				}
				// increment session offer count
				if (!StringUtils.isNullOrEmpty(sessionAttributes.get("offerCount"))) {
					offerCount = Integer.parseInt(sessionAttributes.get("offerCount")) + 1;
					// reset offer count at limit
					if (offerCount > OFFER_COUNT_LIMIT) {

						offerCount = 1;
					}
					if (!inputSlots.get("productSelected").toLowerCase().equals("y")
							&& !inputSlots.get("productSelected").toLowerCase().equals("n")) {

						offerCount = 1;
					}
				}

				// first determine offering from occasion
				RosyProductService rps = new RosyProductService();
				RosyProduct productOffering = rps.fromOccasion(sessionAttributes.get("s_occasion"), offerCount);

				sessionAttributes.put("rosyCategory", productOffering.getRosyCategory().name());
				sessionAttributes.put("offerCount", StringUtils.fromInteger(offerCount));

				da.setIntentName("GetProductSelection");
				da.setMessage(new Message("Offer #1 of " + StringUtils.fromInteger(OFFER_COUNT_LIMIT) + ", (y or n)"));
				setInputSlots(da.getIntentName());
				da.setSlots(inputSlots);
				da.setSlotToElicit("productSelected");

				//get total price for offer by adding shipping fee
				Double priceDouble = Double.parseDouble(productOffering.getPrice());
				Double shipFee = Double.parseDouble(F1_SHIPPING_FEE);
				Double offerTot = priceDouble + shipFee;
				Double truncatedDouble = BigDecimal.valueOf(offerTot)
				    .setScale(3, RoundingMode.HALF_UP)
				    .doubleValue();
				
				// create message
				String productMessageString = "Offer #" + StringUtils.fromInteger(offerCount) + " of "
						+ StringUtils.fromInteger(OFFER_COUNT_LIMIT) + " Price: $" + StringUtils.fromDouble(truncatedDouble)
						+ " Description:" + productOffering.getName() + "  (y or n)";
				if (offerCount > 1) {
					productMessageString = productMessageString + "...or any other key to start list over.";
				}
				da.setMessage(new Message(productMessageString));

				// create response card
				ResponseCard rc = new ResponseCard();
				rc.setContentType("application/vnd.amazonaws.card.generic");
				GenericAttachment ga = new GenericAttachment();
				ga.setTitle("Offer#" + offerCount);
				ga.setSubTitle(productOffering.getPrice());
				ga.setImageUrl(productOffering.getImageURL());
				Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
				gas.add(ga);
				rc.setGenericAttachments(gas);

				da.setResponseCard(rc);
				lexRes.setDialogAction(da);

				// set sessionvariable with currentOfferProductCode
				sessionAttributes.put("s_rosyProductId", StringUtils.fromInteger(productOffering.getId()));
				sessionAttributes.put("s_currentOfferProductCode", productOffering.getCode());
	
				
				ll.log("prompting to get product selection");
			} else {

				// product selected, move to next slot
				da = getGetRecipientNameDA();
				// da.setIntentName("GetNote");
				// da.setMessage(new Message("Write a note to be printed on a
				// card:"));
				// setInputSlots(da.getIntentName());
				// da.setSlots(inputSlots);
				// da.setSlotToElicit("noteText");
				// lexRes.setDialogAction(da);
				// ll.log("prompting to get noteText");

			}

			break;

		case "GetNote":

			if (!StringUtils.isNullOrEmpty(inputSlots.get("noteConfirmed"))
					&& inputSlots.get("noteConfirmed").toLowerCase().equals("y")) {

				// move to get delivery address for the first time
				da.setIntentName("GetDeliveryAddress");
				da.setMessage(new Message("Enter Delivery street number and street name for delivery (without apt or suite num.  like, '1522 N. Anystreet Ave')"));
				setInputSlots(da.getIntentName());
				da.setSlots(inputSlots);
				da.setSlotToElicit("streetAddress");
				lexRes.setDialogAction(da);
				ll.log("prompting to get streetAddress");
			} else {
				if (StringUtils.isNullOrEmpty(inputSlots.get("noteText"))) {
					// reset confirmed flag here
					inputSlots.put("noteConfirmed", null);
					// inputSlots.put("noteText", null);

					ll.log("note is not confirmed, re-prompt");
					da.setIntentName("GetNote");
					da.setMessage(new Message("Write a note to be printed on a card (200 char max).  I will re-ask if it's too long."));
					setInputSlots(da.getIntentName());
					da.setSlots(inputSlots);
					da.setSlotToElicit("noteText");
					lexRes.setDialogAction(da);
					ll.log("prompting to get noteText");

				} else {
					if (StringUtils.isNullOrEmpty(inputSlots.get("noteConfirmed"))) {
						// ll.log("NOTE TO BE CONFIRMED: inputSlots: " +
						// inputSlots.get("noteText") + " transcript: "
						// + lexReq.getInputTranscript());
						inputSlots.put("noteText", lexReq.getInputTranscript());
						ll.log("note is not confirmed");
						da = getNoteConfirmationDA();
						ll.log("prompting to get noteConfirmed");
						sessionAttributes.put("s_noteText", inputSlots.get("noteText"));
					} else {

						// it's not empty (basically, it's no)

						// reset confirmed flag here
						inputSlots.put("noteConfirmed", null);
						// inputSlots.put("noteText", null);

						ll.log("note is not confirmed, re-prompt");
						da.setIntentName("GetNote");
						da.setMessage(new Message("Write a note to be printed on a card (200 char max).  I will re-ask if it's too long."));
						setInputSlots(da.getIntentName());
						da.setSlots(inputSlots);
						da.setSlotToElicit("noteText");
						lexRes.setDialogAction(da);
						ll.log("prompting to get noteText");

					}
				}
			}
			break;
		case "GetDeliveryAddress":

			if (StringUtils.isNullOrEmpty(inputSlots.get("addressConfirmed"))) {
				String addyField = "";

				if (StringUtils.isNullOrEmpty(inputSlots.get("streetAddress"))
						&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_streetAddress"))) {
					addyField = "streetAddress";

					da = getDeliveryAddressDA(addyField);
				} else {
					// store street address in session so it doesn't get blanked
					// out
					if (!StringUtils.isNullOrEmpty(inputSlots.get("streetAddress"))) {
						sessionAttributes.put("s_streetAddress", inputSlots.get("streetAddress"));
					}
					
					
					//set to null if n entered
					if(lexReq.getInputTranscript().toLowerCase().equals("n")){
						inputSlots.put("aptNo", "n");
					};
				
				
					
					ll.log("STREET ADDY ENTERED, NOW CHECK IF APT NO IS ALREADY ENTERED");
					
					if (StringUtils.isNullOrEmpty(inputSlots.get("aptNo"))
							&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_aptNo"))) {			

						ll.log("APT NO IS NULL, PROMPT FOR IT NOW");
						
						
						addyField = "aptNo";
						da = getDeliveryAddressDA(addyField);
					} else {
						

						ll.log("APT NO IS NOW ENTERED, STORE IN SESSION AND PROMPT FOR POSTAL");
											
						
						if (!StringUtils.isNullOrEmpty(inputSlots.get("aptNo"))) {
							sessionAttributes.put("s_aptNo", inputSlots.get("aptNo"));
						}
						
						
						
						
						
						
						
						
						if (StringUtils.isNullOrEmpty(inputSlots.get("postalCode"))
							&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_postalCode"))) {								
								addyField = "postalCode";
									da = getDeliveryAddressDA(addyField);
									
						} else {

						// store postal code in session so it doesn't get
						// blanked out
						if (!StringUtils.isNullOrEmpty(inputSlots.get("postalCode"))) {
								sessionAttributes.put("s_postalCode", inputSlots.get("postalCode"));
						}
						String addy1addy2 = sessionAttributes.get("s_streetAddress");
						if(sessionAttributes.get("s_aptNo").toLowerCase().equals("n"))
						{
							 sessionAttributes.put("s_aptNo", "");
						}else{
							addy1addy2 = addy1addy2 + " " + sessionAttributes.get("s_aptNo");
						}
						// get full address from GOOGLE from postal and city and
						// get confirmation
						RecipientAddressService ras = new RecipientAddressService();
						RecipientAddress ra = ras.fromStreetAddressAndPostal(addy1addy2,
								sessionAttributes.get("s_postalCode"));
						String derivedAddressForConfirmation = ra.getStreetAddress1() + ", " + ra.getCity() + ", "
								+ ra.getState() + " " + ra.getPostalCode();

						sessionAttributes.put("s_streetAddress", sessionAttributes.get("s_streetAddress"));
						sessionAttributes.put("s_city", ra.getCity());
						sessionAttributes.put("s_state", ra.getState());
						sessionAttributes.put("s_postalCode", ra.getPostalCode());
						
						if(ra.getCountry().equals("USA")){
						sessionAttributes.put("s_country", "US");

						da = getDeliveryAddressConfirmationDA(derivedAddressForConfirmation);
						}
						else{
							//country is not USA, send back to begining with warning
							
						}
					}
				}
			}

			} else {
				// we know it's not null, so it's been prompted already, process
				// from here
				if (!inputSlots.get("addressConfirmed").toLowerCase().equals("y")) {
					// move to get address manually here //go back to manual
					// delivery step one by resetting all inputslots.
					inputSlots.clear();
					sessionAttributes.remove("s_streetAddress");
					sessionAttributes.remove("s_aptNo");
					sessionAttributes.remove("s_city");
					sessionAttributes.remove("s_state");
					sessionAttributes.remove("s_postalCode");
					// restart call to manual delivery address with street
					// address
					da = getManualDeliveryAddressDA("manualStreetAddress");
				} else {

					// no need to record address, it's already in session
					// variables

					// move to next step

					da = getDeliveryPhoneDA();
					
				

				}
			}
			break;
		case "GetManualDeliveryAddress":
			// get deliveryAddressManually
			ll.log("getting delivery address manually");
			if (StringUtils.isNullOrEmpty(inputSlots.get("manualAddressConfirmed"))) {
				String addyField = "";

				if (StringUtils.isNullOrEmpty(inputSlots.get("manualStreetAddress")) && StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualStreetAddress"))) {
					addyField = "manualStreetAddress";
					da = getManualDeliveryAddressDA(addyField);
				} else {
					//set session street address
					if(!StringUtils.isNullOrEmpty(inputSlots.get("manualStreetAddress"))){
						sessionAttributes.put("s_manualStreetAddress", inputSlots.get("manualStreetAddress"));
						
					}
					
					if (StringUtils.isNullOrEmpty(inputSlots.get("manualAptNo")) && StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualAptNo"))) {
						addyField = "manualAptNo";
						da = getManualDeliveryAddressDA(addyField);
					} else {
						//set session manualAptNo
						if(!StringUtils.isNullOrEmpty(inputSlots.get("manualAptNo"))){
							sessionAttributes.put("s_manualAptNo", inputSlots.get("manualAptNo"));
							
						}
			
						
					if (StringUtils.isNullOrEmpty(inputSlots.get("manualCity")) && StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualCity")) ) {
						addyField = "manualCity";
						da = getManualDeliveryAddressDA(addyField);
					} else {
					
						//set session manualCity
						if(!StringUtils.isNullOrEmpty(inputSlots.get("manualCity"))){
							sessionAttributes.put("s_manualCity", inputSlots.get("manualCity"));
							
						}
						
						if (StringUtils.isNullOrEmpty(inputSlots.get("manualState")) && StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualState"))) {
							addyField = "manualState";
							da = getManualDeliveryAddressDA(addyField);
						} else {						
							
							//set session manualState
							if(!StringUtils.isNullOrEmpty(inputSlots.get("manualState"))){
								sessionAttributes.put("s_manualState", inputSlots.get("manualState"));
								
							}
							
							if (StringUtils.isNullOrEmpty(inputSlots.get("manualPostalCode")) && StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualPostalCode"))) {
								addyField = "manualPostalCode";
								da = getManualDeliveryAddressDA(addyField);
							} else {
								
								//set session manualCity
								if(!StringUtils.isNullOrEmpty(inputSlots.get("manualPostalCode"))){
									sessionAttributes.put("s_manualPostalCode", inputSlots.get("manualPostalCode"));
									
								}
								
								ll.log("all manual address fields obtained");
								// address populated, get confirmation
								
								if (inputSlots.get("manualAptNo").toLowerCase().equals("n")){
									inputSlots.put("manualAptNo", " ");
								}
								
								if (sessionAttributes.get("s_manualAptNo").toLowerCase().equals("n")){
									sessionAttributes.put("s_manualAptNo", " ");
								}
								
//								String confirmAddyString = inputSlots.get("manualStreetAddress") + " " + inputSlots.get("manualAptNo") + ","
//										+ inputSlots.get("manualCity") + "," + inputSlots.get("manualState") + ","
//										+ inputSlots.get("manualPostalCode");
								
								String confirmAddyString = sessionAttributes.get("s_manualStreetAddress") + " " + sessionAttributes.get("s_manualAptNo") + " "
										+ sessionAttributes.get("s_manualCity ") + "," + sessionAttributes.get("s_manualState") + " "
										+ sessionAttributes.get("s_manualPostalCode");
								
								da = getManualDeliveryAddressConfirmationDA(confirmAddyString);
							}
						}
					}
					}
				}
			} else {
				// we know it's not null, so it's been prompted already, process
				// from here
				if (!inputSlots.get("manualAddressConfirmed").toLowerCase().equals("y")) {
					// go back to manual delivery step one by resetting all
					// inputslots.
					inputSlots.clear();
					// restart call to manual delivery address with street
					// address
					da = getManualDeliveryAddressDA("manualStreetAddress");
				}
				ll.log("MANUAL ADDRESS CONFIRMED!");
				// record address here
				sessionAttributes.put("s_streetAddress", sessionAttributes.get("s_manualStreetAddress"));
				if(!sessionAttributes.get("s_manualAptNo").toLowerCase().equals("n")){
				sessionAttributes.put("s_aptNo", sessionAttributes.get("s_manualAptNo"));
				}
				sessionAttributes.put("s_city", sessionAttributes.get("s_manualCity"));
				sessionAttributes.put("s_state", sessionAttributes.get("s_manualState"));
				sessionAttributes.put("s_postalCode", sessionAttributes.get("s_manualPostalCode"));
				sessionAttributes.put("s_country", "US");
				

				// move to next step
				da = getDeliveryPhoneDA();

			}
			break;

		case "GetDeliveryPhone":
			// if null, then get actual entered date from input transcript
			if (StringUtils.isNullOrEmpty(inputSlots.get("recipientPhone"))) {
				inputSlots.put("recipientPhone", lexReq.getInputTranscript());
			}
			

			if (StringUtils.isNullOrEmpty(inputSlots.get("recipientPhone"))) {
		
				da = getDeliveryPhoneDA();
				
			}else{			
			
				String recipPhone =  inputSlots.get("recipientPhone");			
				
				sessionAttributes.put("s_recipientPhone", recipPhone);
			
				da = getFirstDeliveryDateDA();
			}
		
		break;
		case "GetDeliveryDate":
			// if null, then get actual entered date from input transcript
			if (StringUtils.isNullOrEmpty(inputSlots.get("deliveryDateConfirmed"))) {

				inputSlots.put("deliveryDateConfirmed", lexReq.getInputTranscript());
			}

			if (inputSlots.get("deliveryDateConfirmed").toLowerCase().equals("y")) {
				String acceptedFirstDate = inputSlots.get("deliveryDateConfirmed");
				// check date and then let them know it's okay
				// acceptedFirstDate = normalizeInputDate(acceptedFirstDate);
				ll.log("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX-acceptedFirstDate:"
						+ sessionAttributes.get("s_soonestDeliveryDate"));
				sessionAttributes.put("s_deliveryDate", sessionAttributes.get("s_soonestDeliveryDate"));
				
				// move to next step
				if(sessionAttributes.get("existingCustomerFlag").equals("false")){
					da = getCustomerNameDA(null);
				}else
				{
					sessionAttributes.put("s_customerFullName", sessionAttributes.get("existingCustomerFirstName") + " " + sessionAttributes.get("existingCustomerLastName"));
					String orderUrl = getOrderUrl();
					da = getShowOrderURLDA(orderUrl);
					
				}
				

			} else {

				if (inputSlots.get("deliveryDateConfirmed").length() > 2) {
					String totalEnteredDate = inputSlots.get("deliveryDateConfirmed");
					// check date and then let them know it's okay
					totalEnteredDate = normalizeInputDate(totalEnteredDate);
					sessionAttributes.put("s_deliveryDate", totalEnteredDate);

					List<String> allDeliveryDates = getAllF1DeliveryDates(sessionAttributes.get("s_postalCode"));

					if (checkForAvailableF1DeliveryDate(totalEnteredDate, allDeliveryDates)) {

						// move to next step
						// move to next step
						if(sessionAttributes.get("existingCustomerFlag").equals("false")){
							da = getCustomerNameDA(null);
						}else
						{	
							sessionAttributes.put("s_customerFullName", sessionAttributes.get("existingCustomerFirstName") + " " + sessionAttributes.get("existingCustomerLastName"));
						
							String orderUrl = getOrderUrl();
							da = getShowOrderURLDA(orderUrl);
							
						}

					} else {
						// date is not in list.

						da = getRetryGetDateDA(totalEnteredDate);

					}

				} else {
					da = getRetryGetDateDA(null);
				}
			}

			break;
		case "ShowOrderURL":
			sessionAttributes.put("s_customerFullName", inputSlots.get("customerFullName"));
			String orderUrl = getOrderUrl();
			da = getShowOrderURLDA(orderUrl);
			break;
		default:
			ll.log("something went wrong, no NextAction set for:" + nextAction);

			// clear everything and start over

		}

		lexRes.setSessionAttributes(sessionAttributes);

	}

	private void setInputSlots(String intentName) {
		switch (intentName) {
		case "Intro":
			inputSlots.clear();
			break;
		case "GetOccasion":
			if (!inputSlots.containsKey("occasion")) {
				inputSlots.put("occasion", null);
			}
			break;
		case "GetProductSelection":
			// put slots into session variables
			if (!StringUtils.isNullOrEmpty(inputSlots.get("occasion"))) {
				sessionAttributes.put("s_occasion", inputSlots.get("occasion"));

			}

			// remove slots from previous intent
			inputSlots.remove("occasion");

			// add or keep slots for existing intent
			if (!inputSlots.containsKey("productSelected")) {
				inputSlots.put("productSelected", null);
			}
			break;
		case "GetRecipientName":

			// put slots into session variables
			sessionAttributes.put("s_productSelected", inputSlots.get("productSelected"));
			ll.log("removing product sleected.");
			// remove slots from previous intent
			inputSlots.remove("productSelected");

			// add or keep slots for existing intent
			if (!inputSlots.containsKey("recipientFullName")) {
				inputSlots.put("recipientFullName", null);
			}

			break;
		case "GetNote":

			// put slots into session variables
			if (!StringUtils.isNullOrEmpty(inputSlots.get("recipientFullName"))) {
				sessionAttributes.put("s_recipientFullName", inputSlots.get("recipientFullName"));
			}

			// remove slots from previous intent
			inputSlots.remove("recipientFullName");

			// add or keep slots for existing intent
			if (!inputSlots.containsKey("noteText")) {
				inputSlots.put("noteText", null);
			}
			if (!inputSlots.containsKey("noteConfirmed")) {
				inputSlots.put("noteConfirmed", null);
			}

			break;
		case "GetDeliveryAddress":

			// put slots into session variables

			// remove slots from previous intent
			inputSlots.remove("noteText");
			inputSlots.remove("noteConfirmed");

			// add or keep slots for existing intent
			if (!inputSlots.containsKey("streetAddress")) {
				inputSlots.put("streetAddress", null);
			}
			if (!inputSlots.containsKey("aptNo")) {
				inputSlots.put("aptNo", null);
			}
			if (!inputSlots.containsKey("postalCode")) {
				inputSlots.put("postalCode", null);
			}
			if (!inputSlots.containsKey("addressConfirmed")) {
				inputSlots.put("addressConfirmed", null);
			}

			break;

		case "GetManualDeliveryAddress":

			// put slots into session variables
			// sessionAttributes.put("s_noteText", inputSlots.get("noteText"));

			ll.log("GET MANUAL ADDRESS, INPUT SLOTS BEFORE RESET");

			ll.log(inputSlots.toString());
			// remove slots from previous intent
			inputSlots.remove("postalCode");
			inputSlots.remove("addressConfirmed");
			inputSlots.remove("streetAddress");
			inputSlots.remove("aptNo");
			
			// add or keep slots for existing intent
			if (!inputSlots.containsKey("manualStreetAddress")) {
				inputSlots.put("manualStreetAddress", null);
			}
			if (!inputSlots.containsKey("manualAptNo")) {
				inputSlots.put("manualAptNo", null);
			}
			if (!inputSlots.containsKey("manualCity")) {
				inputSlots.put("manualCity", null);
			}
			if (!inputSlots.containsKey("manualState")) {
				inputSlots.put("manualState", null);
			}
			if (!inputSlots.containsKey("manualPostalCode")) {
				inputSlots.put("manualPostalCode", null);
			}
			if (!inputSlots.containsKey("manualAddressConfirmed")) {
				inputSlots.put("manualAddressConfirmed", null);
			}


			ll.log("GET MANUAL ADDRESS, INPUT SLOTS AFTER RESET - sending this back to Lex");
			ll.log(inputSlots.toString());
			break;
		case "GetDeliveryPhone":

			inputSlots.remove("postalCode");
			inputSlots.remove("addressConfirmed");
			inputSlots.remove("streetAddress");
			inputSlots.remove("aptNo");

			inputSlots.remove("manualStreetAddress");
			inputSlots.remove("manualAptNo");
			inputSlots.remove("manualCity");
			inputSlots.remove("manualState");
			inputSlots.remove("manualPostalCode");
			inputSlots.remove("manualAddressConfirmed");

			// set to null always because get delivery date intent always has to
			// populate it.
			inputSlots.put("recipientPhone", null);
			// if (!inputSlots.containsKey("deliveryDateConfirmed")) {
			// inputSlots.put("deliveryDateConfirmed", null);
			// }
			break;
		case "GetDeliveryDate":

			inputSlots.remove("recipientPhone");

			// set to null always because get delivery date intent always has to
			// populate it.
			inputSlots.put("deliveryDateConfirmed", null);
			// if (!inputSlots.containsKey("deliveryDateConfirmed")) {
			// inputSlots.put("deliveryDateConfirmed", null);
			// }
			break;
		case "GetCustomerName":

			inputSlots.remove("deliveryDateConfirmed");

			if (!inputSlots.containsKey("customerFullName")) {
				inputSlots.put("customerFullName", null);
			}
			break;
		case "ShowOrderURL":
			// move to next step
		
			if(inputSlots.containsKey("customerFullName")){
				inputSlots.remove("customerFullName");
			}
		
				inputSlots.remove("deliveryDateConfirmed");			

			if (!inputSlots.containsKey("dummySlot")) {
				inputSlots.put("dummySlot", null);
			}
			break;

		}

	}

	private DialogActionElicitSlot getNoteConfirmationDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		ll.log("note is not confirmed");
		da.setIntentName("GetNote");
		da.setMessage(new Message("Confirm note with (y or n):" + inputSlots.get("noteText")));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("noteConfirmed");
		lexRes.setDialogAction(da);
		ll.log("prompting to get noteConfirmed");
		return da;
	}

	private DialogActionElicitSlot getDeliveryAddressDA(String addressField) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		String addyDescr = addressField;
		if (addressField.equals("postalCode")) {
			addyDescr = "postal code of delivery (US only)";
		}
		if (addressField.equals("streetAddress")) {
			addyDescr = "street address of delivery";
		}
		if (addressField.equals("aptNo")) {
			addyDescr = "apt/suite/unit # of delivery (or n if not applicable)";
		}

		da.setIntentName("GetDeliveryAddress");
		da.setMessage(new Message("Enter only " + addyDescr));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit(addressField);
		lexRes.setDialogAction(da);
		ll.log("prompting to get " + addressField);
		return da;
	}

	private DialogActionElicitSlot getDeliveryAddressConfirmationDA(String fullAddress) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetDeliveryAddress");
		da.setMessage(new Message("Does this look right (y or n):" + fullAddress));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("addressConfirmed");
		lexRes.setDialogAction(da);
		ll.log("prompting to get delivery address confirmation");
		return da;
	}

	// FOR MANUAL ADDRESS

	private DialogActionElicitSlot getManualDeliveryAddressDA(String addressField) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		String addyDescr = addressField;

		if (addressField.toLowerCase().contains("streetaddress")) {
			addyDescr = "street address of delivery address";
		}
		if (addressField.toLowerCase().contains("aptno")) {
			addyDescr = "suite/apt/unit # of delivery address (or n if not applicable)";
		}
		if (addressField.toLowerCase().contains("city")) {
			addyDescr = "city of delivery address";
		}
		if (addressField.toLowerCase().contains("state")) {
			addyDescr = "2-digit state code of delivery address";
		}
		if (addressField.toLowerCase().contains("postalcode")) {
			addyDescr = "postal code of delivery address";
		}

		
		da.setIntentName("GetManualDeliveryAddress");
		da.setMessage(new Message("Enter only " + addyDescr ));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit(addressField);
		lexRes.setDialogAction(da);
		ll.log("prompting to get " + addressField);
		return da;
	}

	private DialogActionElicitSlot getManualDeliveryAddressConfirmationDA(String fullAddress) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetManualDeliveryAddress");
		da.setMessage(new Message("Does this look right (y or n):" + fullAddress));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("manualAddressConfirmed");
		lexRes.setDialogAction(da);
		ll.log("prompting to get manual delivery address confirmation");
		return da;
	}

	private DialogActionElicitSlot getFirstDeliveryDateDA() {

		// now check with list
		List<String> allDeliveryDates = this.getAllF1DeliveryDates(sessionAttributes.get("s_postalCode"));
		String soonestDeliveryDate = allDeliveryDates.get(0);
		sessionAttributes.put("s_soonestDeliveryDate", soonestDeliveryDate.trim());
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetDeliveryDate");
		da.setMessage(new Message("Soonest delivery date is:" + soonestDeliveryDate.trim()
				+ ". Enter 'y' to accept or enter your own date (MM-DD) format:"));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("deliveryDateConfirmed");
		lexRes.setDialogAction(da);
		ll.log("prompting to get deliverydate");
		return da;
	}

	
	private DialogActionElicitSlot getDeliveryPhoneDA() {

		String messageText = "Enter the phone number of recipient (for delivery use only):";

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetDeliveryPhone");
		da.setMessage(new Message(messageText));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("recipientPhone");
		lexRes.setDialogAction(da);
		ll.log("prompting to get delivery Phone");
		return da;
	}
	private DialogActionElicitSlot getCustomerNameDA(String messagePrefix) {

		String messageText = "OK, last one. Please enter your full name:";
		if (!StringUtils.isNullOrEmpty(messagePrefix)) {
			messageText = messagePrefix + "  " + messageText;
		}

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetCustomerName");
		da.setMessage(new Message(messageText));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("customerFullName");
		lexRes.setDialogAction(da);
		ll.log("prompting to get customer name");
		return da;
	}

	private DialogActionElicitSlot getRetryGetDateDA(String dateNotInList) {

		String messageText = null;
		if (StringUtils.isNullOrEmpty(dateNotInList)) {
			messageText = "Ok, please enter desired delivery date with MM-DD format:";
		} else {
			messageText = "For some reason, we can't do that date(" + dateNotInList
					+ ") ...try again with DD-MM format:";
		}

		// update count
		int count = 0;
		if (StringUtils.isNullOrEmpty(sessionAttributes.get("s_getDateAttempts"))) {
			count = 1;
		} else {
			count = Integer.parseInt(sessionAttributes.get("s_getDateAttempts"));
			count++;
		}
		sessionAttributes.put("s_getDateAttempts", StringUtils.fromInteger(count));

		DialogActionElicitSlot da = new DialogActionElicitSlot();
		if (count <= RETRY_GET_DELIVERY_DATE_ATTEMPTS) {
			ll.log("Date is refused, re-try#" + StringUtils.fromInteger(count));
			da.setIntentName("GetDeliveryDate");
			da.setMessage(new Message(messageText));
			setInputSlots(da.getIntentName());
			da.setSlots(inputSlots);
			da.setSlotToElicit("deliveryDateConfirmed");
			lexRes.setDialogAction(da);
			ll.log("prompting to get delivery date confirmed");
		} else {
			
			
			if(sessionAttributes.get("existingCustomerFlag").equals("false")){
				da = getCustomerNameDA(null);
			}else
			{
				sessionAttributes.put("s_customerFullName", sessionAttributes.get("existingCustomerFirstName") + " " + sessionAttributes.get("existingCustomerLastName"));
				String orderUrl = getOrderUrl();
				
				da = getShowOrderURLDA(orderUrl);
				
				
			}

		}

		return da;
	}

	private DialogActionElicitSlot getShowOrderURLDA(String orderURL) {

		String messageText = "OK, done! Click this link to checkout:  " + orderURL;

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("ShowOrderURL");
		da.setMessage(new Message(messageText));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("dummySlot");
		lexRes.setDialogAction(da);
		ll.log("sending order URL back to client");
		return da;
	}

	private DialogActionElicitSlot getGetRecipientNameDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		da.setIntentName("GetRecipientName");
		da.setMessage(new Message("Enter the full name of the recipient."));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		da.setSlotToElicit("recipientFullName");
		lexRes.setDialogAction(da);
		ll.log("prompting to get recipientName");
		return da;
	}

	private DialogActionElicitSlot getResetDA() {

		String messageText = "Oops, Rosy's having a problem, please type \"reset\" to start over.";

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("Intro");
		da.setMessage(new Message(messageText));
		setInputSlots(da.getIntentName());
		da.setSlots(inputSlots);
		// da.setSlotToElicit("dummySlot");
		lexRes.setDialogAction(da);
		ll.log("ERROR - sending back to Intro");
		return da;
	}

	private List<String> getAllF1DeliveryDates(String postalCode) {
		F1Request f1_getDeliveryDates = new F1Request();
		String[] allDeliveryDates = {};
		List<String> allDatesList = new ArrayList<String>();
		try {
			String jsonAllDates = f1_getDeliveryDates.post("/checkdeliverydate?zipcode=" + postalCode);
			ll.log(jsonAllDates);
			ObjectMapper allDatesMapper = new ObjectMapper();
			JsonNode rootNode = allDatesMapper.readTree(jsonAllDates);
			JsonNode datesNode = rootNode.get("DATES");
			String datesNodeStr = datesNode.toString();
			datesNodeStr = datesNodeStr.replace('[', ' ');
			datesNodeStr = datesNodeStr.replace(']', ' ');
			datesNodeStr = datesNodeStr.replace('\"', ' ');

			datesNodeStr = datesNodeStr.replace('/', '-');

			allDeliveryDates = datesNodeStr.split(",");
			allDatesList = Arrays.asList(allDeliveryDates);

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ll.log(allDatesList.toString());
		return allDatesList;
	}

	private boolean checkForAvailableF1DeliveryDate(String date_DD_MM, List<String> allDeliveryDatesList) {

		// ll.log(allDeliveryDatesList.toString());
		boolean found = false;
		// List<String> allDatesList = Arrays.asList(allDeliveryDates);
		for (String date : allDeliveryDatesList) {
			if (date.contains(date_DD_MM)) {
				// ll.log("date of:" + date_DD_MM + " IS in:" + date);
				sessionAttributes.put("s_deliveryDate", date.trim());
				found = true;
				break;
			} else {
				// ll.log("date of:" + date_DD_MM + " not in:" + date);
			}
		}
		return found;
	}

	private String normalizeInputDate(String totalEnteredDate) {
		totalEnteredDate = totalEnteredDate.trim();
		String[] parsedDate = totalEnteredDate.split("-");

		// new date entered, parse to MM-DD
		if (!totalEnteredDate.contains("-")) {

			if (totalEnteredDate.contains("/")) {
				parsedDate = totalEnteredDate.split("/");
			} else {
				if (totalEnteredDate.contains(".")) {
					parsedDate = totalEnteredDate.split(".");
				} else {
					if (totalEnteredDate.contains("\\")) {
						parsedDate = totalEnteredDate.split("\\");
					} else {
						parsedDate[0] = totalEnteredDate.substring(0, 2);
						// ll.log("p1:" + parsedDate[0]);
						parsedDate[1] = totalEnteredDate.substring(2, 4);
						// ll.log("p2:" + parsedDate[1]);
					}
				}
			}

			totalEnteredDate = parsedDate[0] + "-" + parsedDate[1];

		}

		// add year to the entered date
		int monthEntered = Integer.parseInt(parsedDate[0]);
		int dayEntered = Integer.parseInt(parsedDate[1]);

		DateTime today = new DateTime();
		int todayMonth = today.getMonthOfYear();
		int derivedYear = today.getYear();
		if (monthEntered < todayMonth) {
			derivedYear = derivedYear + 1;
		}

		if (monthEntered < 10) {
			parsedDate[0] = "0" + StringUtils.fromInteger(monthEntered);
		}

		if (dayEntered < 10) {
			parsedDate[1] = "0" + StringUtils.fromInteger(dayEntered);
		}

		totalEnteredDate = parsedDate[0] + "-" + parsedDate[1] + "-" + Integer.toString(derivedYear);

		return totalEnteredDate;
	}

	private String getOrderUrl() {
		String OrderNo = "sampleOrderNumber";

		// build order from sessionAttributes and write to dynamoDB
		if (StringUtils.isNullOrEmpty(sessionAttributes.get("s_orderPlacedInd"))
				|| !sessionAttributes.get("s_orderPlacedInd").equals("Y")) {

			// create rosy order (very similar to F1 order. Then checkout will
			// create F1 order
			String selectedProductCode = sessionAttributes.get("s_currentOfferProductCode");
			RosyProductService rps = new RosyProductService();
			RosyProduct rp = rps.fromProductCode(selectedProductCode);
			sessionAttributes.put("s_rosyProductId", StringUtils.fromInteger(rp.getId()));
			sessionAttributes.put("s_price", rp.getPrice());
			sessionAttributes.put("s_productImageUrl", rp.getImageURL());
			sessionAttributes.put("s_productName", rp.getName());
			Double price = Double.parseDouble(rp.getPrice());
			Double shipFee = Double.parseDouble(F1_SHIPPING_FEE);
			Double ordTot = price + shipFee;
			sessionAttributes.put("s_orderTotal", StringUtils.fromDouble(ordTot));

			RosyOrderService ros = new RosyOrderService();
			OrderNo = ros.createRosyOrder(sessionAttributes);

			sessionAttributes.put("s_orderId", OrderNo);
			sessionAttributes.put("s_orderPlacedInd", "Y");
		} else {
			ll.log(" ...called get order again, but order already has been placed, just display existing order.");
		}

		String encodedOrderNumber = sessionAttributes.get("s_orderId");
		String orderUrl = "http://rosybot.com/?ro=" + encodedOrderNumber;
		// return order url
		return orderUrl;
	}

}
