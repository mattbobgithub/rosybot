package com.rosybot.models;


public class RosyCustomer {
	
	public RosyCustomer(){
		
	}
	
	public RosyCustomer(String lexbotJSON){
		
	}
	
	private int id;
	private String phone;
	private String email;
	private String firstName;
	private String lastName;
	

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	

}
