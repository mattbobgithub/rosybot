package com.rosythebot.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosythebot.models.RecipientAddress;

public class RecipientAddressService {

	private static final String GOOGLEMAPS_AUTH_KEY = System.getenv("GOOGLEMAPS_AUTH_KEY");
	private static final String GOOGLEMAPS_URL = System.getenv("GOOGLEMAPS_URL");

	public RecipientAddress fromStreetAddressAndPostal(String streetAddress, String postalCode) {
		RecipientAddress ra = new RecipientAddress();

		Map<String, String> addy = getCityNameFromPostalCode(postalCode, streetAddress);

		ra.setStreetAddress1(addy.get("streetAddress").trim());
		ra.setCity(addy.get("city").trim());
		ra.setState(addy.get("state").trim());
		ra.setPostalCode(addy.get("postalCode").trim());
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


			ObjectMapper mapper = new ObjectMapper();
			JsonNode root;
			String foundStreetAddress = "";
			String foundCityName = "";
			String foundStateName = "";
			String foundFullCityStatePostal = "";

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
						System.out.println(foundAddressArray.toString());
						foundCityName = foundAddressArray[0];
						foundStateName = foundAddressArray[1];

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

}
