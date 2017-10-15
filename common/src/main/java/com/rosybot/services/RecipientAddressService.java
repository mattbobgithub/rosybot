package com.rosybot.services;

 
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;


import org.apache.http.util.EntityUtils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosybot.models.RecipientAddress;
import com.rosybot.models.RosyOrder;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Telephone;

public class RecipientAddressService {

	private static final String GOOGLEMAPS_AUTH_KEY = System.getenv("GOOGLEMAPS_AUTH_KEY");
	private static final String GOOGLEMAPS_URL = System.getenv("GOOGLEMAPS_URL");
	private static final String APACHE_SIMPLE_LOGS = System.getenv("APACHE_SIMPLE_LOGS");
	

	public RecipientAddress fromStreetAddressAndPostal(String streetAddress, String postalCode) {
		RecipientAddress ra = new RecipientAddress();

		Map<String, String> addy = getCityNameFromPostalCode(postalCode, streetAddress);

		ra.setStreetAddress1(addy.get("streetAddress").trim());
		ra.setCity(addy.get("city").trim());
		ra.setState(addy.get("state").trim());
		ra.setPostalCode(addy.get("postalCode").trim());
		ra.setCountry(addy.get("country").trim());
		// System.out.println(addy.toString());
		// System.out.println("FOUNDaddress" + ra.toString());

		return ra;
	}

	private static Map<String, String> getCityNameFromPostalCode(String postalCode, String inputStreetAddress) {

		Map<String, String> address = new HashMap<String, String>();

		InputStream inputStream = null;
		String googleMaps_jsonOutputString = "";

		try {
			// add key
			String parm1 = URLEncoder.encode(inputStreetAddress, "UTF-8");
			String parm2 = URLEncoder.encode("country=US|postal_code:" + postalCode, "UTF-8");

			String googleMapsRequest = GOOGLEMAPS_URL + "/json?address=" + postalCode + "&component=" + parm2;

			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(googleMapsRequest);
			// request.addHeader(headerAuth);
			HttpResponse response;
			response = client.execute(request);
			HttpEntity he = response.getEntity();

			inputStream = he.getContent();
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(inputStream, writer, encoding);
			googleMaps_jsonOutputString = writer.toString();
			// System.out.println(googleMaps_jsonOutputString);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root;
			String foundStreetAddress = "";
			String foundCityName = "";
			String foundStateName = "";
			String foundFullCityStatePostal = "";
			String foundCountry = "";

			root = mapper.readTree(googleMaps_jsonOutputString);

			String status = root.get("status").asText();

			if (status.equals("OK")) {
				JsonNode resultsArray = root.path("results");
				int resultNodeIndex = 1;
				for (JsonNode resultNode : resultsArray) {

					// only get first resultnode
					if (resultNodeIndex == 1) {
						foundFullCityStatePostal = resultNode.get("formatted_address").asText();
						String[] foundAddressArray = foundFullCityStatePostal.split(",");

						foundCityName = foundAddressArray[0];
						foundStateName = foundAddressArray[1];
						foundCountry = foundAddressArray[foundAddressArray.length - 1];
						resultNodeIndex++;
					}
				}

			} else {
				System.out.println("GOOGLEMAPS STATUS NOT OKAY!!!!!");
			}

			int idx = foundStateName.lastIndexOf(" ");
			foundStateName = foundStateName.substring(0, idx);

			address.put("streetAddress", inputStreetAddress);
			address.put("city", foundCityName);
			address.put("state", foundStateName);
			address.put("postalCode", postalCode);
			address.put("country", foundCountry);

		} catch (JsonProcessingException e) {
			System.out.println("GOOGLEMAPS JsonProcessing ERROR!!!!!!!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			System.out.println("GOOGLEMAPS ClientProtocolException ERROR!!!!!!!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("GOOGLEMAPS IO ERROR!!!!!!!");
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		return address;

	}
	


	
	public RecipientAddress getAddressFromVCardUrl(String vCardUrl){
		
		if (APACHE_SIMPLE_LOGS == "true"){
			java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);

			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
			System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
			}


		RecipientAddress ra = new RecipientAddress();
		
					HttpClient client = HttpClientBuilder.create().build();
					HttpGet request = new HttpGet(vCardUrl);					
					//Header headerAuth = new BasicHeader("Authorization","Basic  " + F1_AUTH_ENCODED);
					//request.addHeader(headerAuth);		
					HttpResponse response;
					String vCardOutput = "";
					try {
						response = client.execute(request);
						HttpEntity he = response.getEntity();

						vCardOutput = EntityUtils.toString(he);
						
						//System.out.println(vCardOutput);
						
						VCard vcard = Ezvcard.parse(vCardOutput).first();
						
						//get address from vcard
						String st1 = "";
						String st2 = "";
						String city = "";
						String state = "";
						String zip = "";
						String country = "";
						
						
						if(vcard.getAddresses() != null && vcard.getAddresses().size() > 0){
							
						Address a =vcard.getAddresses().get(0);
					//	System.out.println(a.toString());
						
						List<String> vCardStreetAddresses = a.getStreetAddresses();	
					//	System.out.println("size of vcardstraddys:" + vCardStreetAddresses.size());
						if (vCardStreetAddresses!=null && vCardStreetAddresses.size() > 0){
							
							 st1 = vCardStreetAddresses.get(0);
							 if(vCardStreetAddresses.size() > 1){
								 st2 = vCardStreetAddresses.get(1);
							 }
							 
						}
							 city = a.getLocality();
							 state = a.getRegion();
							 zip = a.getPostalCode();
						
						//	System.out.println("parsed vcard:");
						//	System.out.println("st1:" + st1);
						//	System.out.println("st2:" + st2);
						//	System.out.println("city:" + city);
						//	System.out.println("state:" + state);
						//	System.out.println("zip:" + zip);
							
							ra.setStreetAddress1(st1);
							ra.setStreetAddress2(st2);
							ra.setCity(city);
							ra.setState(state);
							ra.setPostalCode(zip);
						    
						}else{

							System.out.println(" addresses  null or empty in vCard:");
						}
						
						
						
						//now get phone and name  ...just get the first phone number
						ra.setFullName(vcard.getFormattedName().getValue());
						Telephone t = vcard.getTelephoneNumbers().get(0);
						String phoneNum = t.getText().replaceAll("[^\\d]", "");
						System.out.println("RAS - phoneNum before:" + phoneNum);
						if(phoneNum.startsWith("1")){
							phoneNum=phoneNum.substring(1);
						}
						System.out.println("RAS - phoneNum after:" + phoneNum);
						ra.setPhone(phoneNum);
						
						
						
						
						
						//HttpEntity he = response.getEntity();

//						inputStream = he.getContent();
//						StringWriter writer = new StringWriter();
//						String encoding = StandardCharsets.UTF_8.name();
//						IOUtils.copy(inputStream, writer, encoding);
//						googleMaps_jsonOutputString = writer.toString();
						
						
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

		
			return ra;
	}

}
