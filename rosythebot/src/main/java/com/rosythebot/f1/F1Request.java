package com.rosythebot.f1;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class F1Request {

    private static final String APACHE_SIMPLE_LOGS = System.getenv("APACHE_LOGS");
    private static final String F1_AUTH_ENCODED = System.getenv("F1_AUTH");
    private static final String F1_URL = System.getenv("F1_URL");
    
	public F1Request(){
		
	}
	
	public String post(String url){
		
		
		//try to call florist one api here
		//set apache logger
		if (APACHE_SIMPLE_LOGS == "true"){
		java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
		java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);

		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
		}
							
		String f1_jsonOutput = "";
				HttpClient client = HttpClientBuilder.create().build();
				HttpGet request = new HttpGet(F1_URL + url );					
				Header headerAuth = new BasicHeader("Authorization","Basic  " + F1_AUTH_ENCODED);
				request.addHeader(headerAuth);		
				HttpResponse response;
				try {
					response = client.execute(request);
					HttpEntity he = response.getEntity();

					f1_jsonOutput = EntityUtils.toString(he);
					
					
					//HttpEntity he = response.getEntity();

//					inputStream = he.getContent();
//					StringWriter writer = new StringWriter();
//					String encoding = StandardCharsets.UTF_8.name();
//					IOUtils.copy(inputStream, writer, encoding);
//					googleMaps_jsonOutputString = writer.toString();
					
					
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

	
		return f1_jsonOutput;
	}
	
	
	
	
}
