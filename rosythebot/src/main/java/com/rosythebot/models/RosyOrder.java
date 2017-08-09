package com.rosythebot.models;

import java.util.UUID;

import org.joda.time.DateTime;

import com.amazonaws.regions.Regions;
import com.rosythebot.models.Enums.RosyOrderStatus;

public class RosyOrder {
	
	public RosyOrder(){
		
	}
	
	private UUID id;
	private String rosyOrderId;
	private RosyOrderStatus rosyOrderStatus;	
	private int rosyCustomerId;
	private String customerFullName;
	private String customerPhone;
	private String customerEmail;	
	private int rosyProductId;
	private String productCode;
	private String recipientFullName;
	private String recipientInstitution;
	private String recipientStreetAddress1;
	private String recipientStreetAddress2;
	private String recipientCity;
	private String recipientState;
	private String recipientPostalCode;
	private String recipientCountry;
	private String recipientPhone;
	private Double orderTotal;
	private Double price;
	private String deliveryDate;
	private String cardMessage;
	private String specialInstructions;
	private String paymentId;
	private String productImageUrl;
	private String productName;
	private DateTime createDate;
	private DateTime modifiedDate;
	
	
	
	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
		
	public String getRosyOrderId() {
		return rosyOrderId;
	}
	public void setRosyOrderId(String rosyOrderId) {
		this.rosyOrderId = rosyOrderId;
	}
	public RosyOrderStatus getRosyOrderStatus() {
		return rosyOrderStatus;
	}
	public void setRosyOrderStatus(RosyOrderStatus rosyOrderStatus) {
		this.rosyOrderStatus = rosyOrderStatus;
	}
	public int getRosyCustomerId() {
		return rosyCustomerId;
	}
	public void setRosyCustomerId(int rosyCustomerId) {
		this.rosyCustomerId = rosyCustomerId;
	}
	public String getCustomerFullName() {
		return customerFullName;
	}
	public void setCustomerFullName(String customerFullName) {
		this.customerFullName = customerFullName;
	}
	public String getCustomerPhone() {
		return customerPhone;
	}
	public void setCustomerPhone(String customerPhone) {
		this.customerPhone = customerPhone;
	}
	public String getCustomerEmail() {
		return customerEmail;
	}
	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}
	public int getRosyProductId() {
		return rosyProductId;
	}
	public void setRosyProductId(int rosyProductId) {
		this.rosyProductId = rosyProductId;
	}
	public String getProductCode() {
		return productCode;
	}
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}
	public String getRecipientFullName() {
		return recipientFullName;
	}
	public void setRecipientFullName(String recipientFullName) {
		this.recipientFullName = recipientFullName;
	}
	public String getRecipientInstitution() {
		return recipientInstitution;
	}
	public void setRecipientInstitution(String recipientInstitution) {
		this.recipientInstitution = recipientInstitution;
	}
	public String getRecipientStreetAddress1() {
		return recipientStreetAddress1;
	}
	public void setRecipientStreetAddress1(String recipientStreetAddress1) {
		this.recipientStreetAddress1 = recipientStreetAddress1;
	}
	public String getRecipientStreetAddress2() {
		return recipientStreetAddress2;
	}
	public void setRecipientStreetAddress2(String recipientStreetAddress2) {
		this.recipientStreetAddress2 = recipientStreetAddress2;
	}
	public String getRecipientCity() {
		return recipientCity;
	}
	public void setRecipientCity(String recipientCity) {
		this.recipientCity = recipientCity;
	}
	public String getRecipientState() {
		return recipientState;
	}
	public void setRecipientState(String recipientState) {
		this.recipientState = recipientState;
	}
	public String getRecipientPostalCode() {
		return recipientPostalCode;
	}
	public void setRecipientPostalCode(String recipientPostalCode) {
		this.recipientPostalCode = recipientPostalCode;
	}
	public String getRecipientCountry() {
		return recipientCountry;
	}
	public void setRecipientCountry(String recipientCountry) {
		this.recipientCountry = recipientCountry;
	}
	public String getRecipientPhone() {
		return recipientPhone;
	}
	public void setRecipientPhone(String recipientPhone) {
		this.recipientPhone = recipientPhone;
	}
	
	public Double getPrice() {
		return price;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
	public Double getOrderTotal() {
		return orderTotal;
	}
	public void setOrderTotal(Double orderTotal) {
		this.orderTotal = orderTotal;
	}
	public String getDeliveryDate() {
		return deliveryDate;
	}
	public void setDeliveryDate(String deliveryDate) {
		this.deliveryDate = deliveryDate;
	}
	public String getCardMessage() {
		return cardMessage;
	}
	public void setCardMessage(String cardMessage) {
		this.cardMessage = cardMessage;
	}
	public String getSpecialInstructions() {
		return specialInstructions;
	}
	public void setSpecialInstructions(String specialInstructions) {
		this.specialInstructions = specialInstructions;
	}
	
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	
	public String getProductImageUrl() {
		return productImageUrl;
	}
	public void setProductImageUrl(String productImageUrl) {
		this.productImageUrl = productImageUrl;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public DateTime getCreateDate() {
		return createDate;
	}
	public void setCreateDate(DateTime createDate) {
		this.createDate = createDate;
	}
	public DateTime getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(DateTime modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
	
	
	
}
	
	
	
	