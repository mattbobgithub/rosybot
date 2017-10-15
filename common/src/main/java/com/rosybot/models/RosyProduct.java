package com.rosybot.models;

import com.rosybot.models.Enums.*;



public class RosyProduct {

	public RosyProduct() {

	}

	private int id;
	private String name;
	private String code;
	private String price;
	private String imageURL;
	private String category;
	private RosyCategory rosyCategory;
	private int rosyOfferOrder;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}


	public RosyCategory getRosyCategory() {
		return rosyCategory;
	}

	public void setRosyCategory(RosyCategory rosyCategory) {
		this.rosyCategory = rosyCategory;
	}


	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}


	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}


	public int getRosyOfferOrder() {
		return rosyOfferOrder;
	}

	public void setRosyOfferOrder(int rosyOfferOrder) {
		this.rosyOfferOrder = rosyOfferOrder;
	}


}
