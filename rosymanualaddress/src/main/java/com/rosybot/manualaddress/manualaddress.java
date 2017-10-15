package com.rosybot.manualaddress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class manualaddress implements RequestHandler<Object, Object> {
	
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

		inputSlots.putAll(currentIntent.getSlots());
		ll.log("INPUT SLOTS:" + inputSlots.toString());

		if (lexReq.getSessionAttributes() != null) {
			ll.log("SESSION ATTRIBS:" + lexReq.getSessionAttributes().toString());
			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}

		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");

		ll.log("input transcript:" + lexReq.getInputTranscript());


 DialogActionElicitSlot da = new DialogActionElicitSlot();
		
		
		// get deliveryAddressManually instead
		if (StringUtils.isNullOrEmpty(inputSlots.get("manualAddressConfirmed"))) {
			String addyField = "";

			if (StringUtils.isNullOrEmpty(inputSlots.get("manualStreetAddress"))
					&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualStreetAddress"))) {
				addyField = "manualStreetAddress";
				da = getManualDeliveryAddressDA(addyField);
			} else {
				// set session street address
				if (!StringUtils.isNullOrEmpty(inputSlots.get("manualStreetAddress"))) {
					sessionAttributes.put("s_manualStreetAddress", inputSlots.get("manualStreetAddress"));

				}

				if (StringUtils.isNullOrEmpty(inputSlots.get("manualAptNo"))
						&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualAptNo"))) {
					addyField = "manualAptNo";
					da = getManualDeliveryAddressDA(addyField);
				} else {
					// set session manualAptNo
					if (!StringUtils.isNullOrEmpty(inputSlots.get("manualAptNo"))) {
						sessionAttributes.put("s_manualAptNo", inputSlots.get("manualAptNo"));

					}

					if (StringUtils.isNullOrEmpty(inputSlots.get("manualCity"))
							&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualCity"))) {
						addyField = "manualCity";
						da = getManualDeliveryAddressDA(addyField);
					} else {

						// set session manualCity
						if (!StringUtils.isNullOrEmpty(inputSlots.get("manualCity"))) {
							sessionAttributes.put("s_manualCity", inputSlots.get("manualCity"));

						}

						if (StringUtils.isNullOrEmpty(inputSlots.get("manualState"))
								&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualState"))) {
							addyField = "manualState";
							da = getManualDeliveryAddressDA(addyField);
						} else {

							// set session manualState
							if (!StringUtils.isNullOrEmpty(inputSlots.get("manualState"))) {
								sessionAttributes.put("s_manualState", inputSlots.get("manualState"));

							}

							if (StringUtils.isNullOrEmpty(inputSlots.get("manualPostalCode"))
									&& StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualPostalCode"))) {
								addyField = "manualPostalCode";
								da = getManualDeliveryAddressDA(addyField);
							} else {

								// set session manualCity
								if (!StringUtils.isNullOrEmpty(inputSlots.get("manualPostalCode"))) {
									sessionAttributes.put("s_manualPostalCode", inputSlots.get("manualPostalCode"));

								}

								// address populated, get confirmation

								if (inputSlots.get("manualAptNo").toLowerCase().equals("n")) {
									inputSlots.put("manualAptNo", " ");
								}

								if (sessionAttributes.get("s_manualAptNo").toLowerCase().equals("n")) {
									sessionAttributes.put("s_manualAptNo", " ");
								}

								// String confirmAddyString =
								// inputSlots.get("manualStreetAddress") + "
								// " + inputSlots.get("manualAptNo") + ","
								// + inputSlots.get("manualCity") + "," +
								// inputSlots.get("manualState") + ","
								// + inputSlots.get("manualPostalCode");

								String confirmAddyString = sessionAttributes.get("s_manualStreetAddress") + " ";
								if(!StringUtils.isNullOrEmpty(sessionAttributes.get("s_manualAptNo"))){
									confirmAddyString = confirmAddyString + sessionAttributes.get("s_manualAptNo") + " ";									
								}
								confirmAddyString = confirmAddyString + sessionAttributes.get("s_manualCity ") + ",";
								confirmAddyString = confirmAddyString + sessionAttributes.get("s_manualState") + " ";
								confirmAddyString = confirmAddyString + sessionAttributes.get("s_manualPostalCode");
								
								
								ll.log(confirmAddyString);
								
								
								
								

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
			// record address here
			sessionAttributes.put("s_streetAddress1", sessionAttributes.get("s_manualStreetAddress"));
			if (!sessionAttributes.get("s_manualAptNo").toLowerCase().equals("n")) {
				sessionAttributes.put("s_streetAddress2", sessionAttributes.get("s_manualAptNo"));
			}
			sessionAttributes.put("s_city", sessionAttributes.get("s_manualCity"));
			sessionAttributes.put("s_state", sessionAttributes.get("s_manualState"));
			sessionAttributes.put("s_postalCode", sessionAttributes.get("s_manualPostalCode"));
			sessionAttributes.put("s_country", "US");

			// set da for next step (rp
		da = getMainOfferDA();

		}
		
		
		
		
		
		
		
		lexRes.setDialogAction(da);
		
		
		lexRes.setSessionAttributes(sessionAttributes);

		return lexRes;
    }
    
    
    
    
	private DialogActionElicitSlot getManualDeliveryAddressDA(String addressField) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		String addyDescr = addressField;

		if (addressField.toLowerCase().contains("streetaddress")) {
			addyDescr = "street address of delivery address";
		}
		if (addressField.toLowerCase().contains("aptno")) {
			addyDescr = "suite/apt/unit # of delivery address [or n if not applicable]";
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
		da.setMessage(new Message("Enter only " + addyDescr));
		setInputSlots();
		da.setSlots(inputSlots);
		da.setSlotToElicit(addressField);
		lexRes.setDialogAction(da);
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private DialogActionElicitSlot getManualDeliveryAddressConfirmationDA(String fullAddress) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetManualDeliveryAddress");
		da.setMessage(new Message("Does this look right [y or n]:" + fullAddress));
		setInputSlots();
		da.setSlots(inputSlots);
		da.setSlotToElicit("manualAddressConfirmed");
		lexRes.setDialogAction(da);
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}
	
	private DialogActionElicitSlot getMainOfferDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotGetProductSelection");
		da.setMessage(new Message("Our best offer is a dozen of the freshest premium red roses delivered next day for $40 ['y' to accept, or 'more' for more color and amount options]:"));
	//	setInputSlots(da.getIntentName());
		inputSlots.clear();
		inputSlots.put("productRoute", "x");
		inputSlots.put("amountOption", "x");
		inputSlots.put("colorOption", "x");
		inputSlots.put("productConfirmed", "x");
		da.setSlots(inputSlots);
		
		//add response card with main offer
		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("dozen red roses offer");
		ga.setSubTitle("dozen red roses offer");
		ga.setImageUrl("https://s3.amazonaws.com/rosybot.com/images/products/mainOffer.jpg");
		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);

		da.setResponseCard(rc);		
		
		
		
		
		
		lexRes.setDialogAction(da);
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}
	
	private void setInputSlots(){
		// put slots into session variables
		// remove slots from previous intent ??


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
	}

}
