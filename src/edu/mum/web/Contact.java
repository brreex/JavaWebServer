package edu.mum.web;

public class Contact {

	public StringBuilder doGet() {
		StringBuilder response = new StringBuilder();
		response.append("<!DOCTYPE html>");
		response.append("<html>");
		response.append("<head>");
		response.append("<title>Almost an HTTP Server</title>");
		response.append("</head>");
		response.append("<body>");
		response.append("<h1>Contacts Page</h1>");
		response.append("<br />");
		response.append("</body>");
		response.append("</html>");
		return response;
	}
	
	public void service(){
		
	}
}
