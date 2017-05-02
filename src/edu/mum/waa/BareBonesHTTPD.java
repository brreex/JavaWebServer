package edu.mum.waa;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BareBonesHTTPD extends Thread {

	private static final int PortNumber = 8080;
	private static final String currentDir = System.getProperty("user.dir") + "\\document\\";

	private static List<String> ServletClasses = Arrays.asList("Test", "Welcome", "Contact");

	Socket connectedClient = null;

	public BareBonesHTTPD(Socket client) {
		connectedClient = client;
	}

	public void run() {
		try {
			System.out.println(connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");
			BBHttpRequest httpRequest = getRequest(connectedClient.getInputStream());
			BBHttpResponse httpResponse = new BBHttpResponse();
			processRequest(httpRequest, httpResponse);
			sendResponse(httpResponse);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processRequest(BBHttpRequest httpRequest, BBHttpResponse httpResponse) {
		boolean found = false;
		String requestedResource = httpRequest.getUri().substring(1);
		StringBuilder response = new StringBuilder();

		System.out.println("Requested Resource : " + requestedResource.split("\\.")[0]);

		for (String Class : BareBonesHTTPD.ServletClasses) {
			if (requestedResource.split("\\.")[0].equals(Class)) {
				System.out.println("Found " + Class);
				found = true;
				break;
			}
		}

		if (found) { // servlet class exists
			try {
				String servletClassName = "edu.mum.web."+requestedResource.split("\\.")[0];
				Class<?> servletClass = Class.forName(servletClassName);
				Object servlet = servletClass.newInstance();
				
				String methodName = "doGet";
				Method doGet = servlet.getClass().getDeclaredMethod(methodName);
				response = (StringBuilder) doGet.invoke(servlet);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		else { // return the file
			System.out.println("Reading The File " + BareBonesHTTPD.currentDir + "" + requestedResource);

			List<String> line = new ArrayList<>();
			String statusMessage;

			try (BufferedReader br = Files
					.newBufferedReader(Paths.get(BareBonesHTTPD.currentDir + "" + httpRequest.getUri().substring(1)))) {
				line = br.lines().collect(Collectors.toList());
				statusMessage = "File Found";
				httpResponse.setStatusCode(200);
			} catch (IOException e) {
				statusMessage = "404 File Not Found";
				httpResponse.setStatusCode(404);
				e.printStackTrace();
			}
			response.append("<!DOCTYPE html>");
			response.append("<html>");
			response.append("<head>");
			response.append("<title>Almost an HTTP Server</title>");
			response.append("</head>");
			response.append("<body>");
			response.append("<h1>This is the HTTP Server Serving File Content</h1>");
			response.append("<br />");
			response.append("<h2>" + statusMessage + "</h2>");
			Iterator<String> it = line.iterator();

			while (it.hasNext()) {
				response.append("<p>" + it.next() + "</p>");
			}
			response.append("</body>");
			response.append("</html>");
		}
		httpResponse.setContentType("text/html");
		httpResponse.setMessage(response.toString());
	}

	private BBHttpRequest getRequest(InputStream inputStream) throws IOException {

		BBHttpRequest httpRequest = new BBHttpRequest();

		BufferedReader fromClient = new BufferedReader(new InputStreamReader(inputStream));

		String requestString = fromClient.readLine();
		String headerLine = requestString;

		System.out.println("The HTTP request is ....");
		System.out.println(requestString);

		// Header Line
		StringTokenizer tokenizer = new StringTokenizer(headerLine);

		httpRequest.setMethod(tokenizer.nextToken());
		httpRequest.setUri(tokenizer.nextToken());
		httpRequest.setHttpVersion(tokenizer.nextToken());

		// Header Fields and Body
		boolean readingBody = false;
		ArrayList<String> fields = new ArrayList<>();
		ArrayList<String> body = new ArrayList<>();

		while (fromClient.ready()) {
			requestString = fromClient.readLine();
			System.out.println("Reading Line : " + requestString);

			if (!requestString.isEmpty()) {
				if (readingBody) {
					body.add(requestString);
				} else {
					fields.add(requestString);
				}
			} else {
				readingBody = true;
			}
		}
		httpRequest.setFields(fields);
		httpRequest.setMessage(body);
		return httpRequest;
	}

	private void sendResponse(BBHttpResponse response) throws IOException {

		String statusLine = null;
		if (response.getStatusCode() == 200) {
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		} else if (response.getStatusCode() == 404) {
			statusLine = "HTTP/1.1 404 Requested File Not Found " + "\r\n";
		} else {
			statusLine = "HTTP/1.1 501 Not Implemented" + "\r\n";
		}

		String serverdetails = "Server: BareBones HTTPServer";
		String contentLengthLine = "Content-Length: " + response.getMessage().length() + "\r\n";
		String contentTypeLine = "Content-Type: " + response.getContentType() + "\r\n";

		DataOutputStream toClient = new DataOutputStream(connectedClient.getOutputStream());

		toClient.writeBytes(statusLine);
		toClient.writeBytes(serverdetails);
		toClient.writeBytes(contentTypeLine);
		toClient.writeBytes(contentLengthLine);
		toClient.writeBytes("Connection: close\r\n");
		toClient.writeBytes("\r\n");
		toClient.writeBytes(response.getMessage());

		toClient.close();
	}

	public static void main(String args[]) throws Exception {

		ServerSocket Server = new ServerSocket(PortNumber, 10, InetAddress.getByName("127.0.0.1"));
		System.out.println("Server Started on port " + PortNumber);

		try {
			while (true) {
				Socket connected = Server.accept();
				(new BareBonesHTTPD(connected)).start();
			}
		} catch (Exception e) {
			Server.close();
		}
	}
}
