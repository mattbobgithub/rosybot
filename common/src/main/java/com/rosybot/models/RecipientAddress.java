package com.rosybot.models;

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

public class RecipientAddress {
	

	
	public RecipientAddress(){
		
	}
	
	private int id;
	private int customerId;
	private String fullName;
	private String StreetAddress1;
	private String StreetAddress2;
	private String City;
	private String State;
	private String PostalCode;
	private String County;
	private String Country;
	private String phone;
	
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getCustomerId() {
		return customerId;
	}
	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getStreetAddress1() {
		return StreetAddress1;
	}
	public void setStreetAddress1(String streetAddress1) {
		StreetAddress1 = streetAddress1;
	}
	public String getStreetAddress2() {
		return StreetAddress2;
	}
	public void setStreetAddress2(String streetAddress2) {
		StreetAddress2 = streetAddress2;
	}
	public String getCity() {
		return City;
	}
	public void setCity(String city) {
		City = city;
	}
	
	public String getState() {
		return State;
	}
	public void setState(String state) {
		State = state;
	}
	public String getPostalCode() {
		return PostalCode;
	}
	public void setPostalCode(String postalCode) {
		PostalCode = postalCode;
	}
	public String getCounty() {
		return County;
	}
	public void setCounty(String county) {
		County = county;
	}
	public String getCountry() {
		return Country;
	}
	public void setCountry(String country) {
		Country = country;
	}
	
	
	
	
	
	
	
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	@Override
	public String toString() {
		return "RecipientAddress [StreetAddress1=" + StreetAddress1 + ", StreetAddress2=" + StreetAddress2 + ", City="
				+ City + ", State=" + State + ", PostalCode=" + PostalCode + ", County=" + County + ", Country="
				+ Country + "]";
	}
	


}
