package webserver;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.HttpRequestUtils;
import model.User;
import util.IOUtils;
import db.DataBase;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	
	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		//log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			
			DataOutputStream dos = new DataOutputStream(out);
			readRequest(br, dos);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void readRequest(BufferedReader br, DataOutputStream dos) throws IOException
	{
		String request = br.readLine();
		System.out.println(request);
		String[] html = request.split(" ");
		int con_len=0;
		String createInfo = null;
		
		if(html[1].contains("/create"))
		{
			String temp = br.readLine();
			while(!"".equals(temp))
			{		
				if(temp.contains("Content-Length"))
				{
					String num[] = temp.split(" ");
					con_len = Integer.parseInt(num[1]);
					
					System.out.println(temp);
				}
				temp = br.readLine();
			}
			createInfo = IOUtils.readData(br, con_len);
			//System.out.println(createInfo);
			readCreate(createInfo, dos);
		}
		else if(html[1].contains("/login?"))
		{
			readLogin(html[1], dos);
		}
		else if(html[1].contains(".css"))
		{
			readCSS(html[1], dos);
		}
		else
		{		
			readHTML(html[1], dos);
		}
	}
	
	private byte[] readCreate(String HTML, DataOutputStream dos) throws IOException
	{
		//System.out.println(HTML);
		//String parsed = HTML.substring(7);
		//System.out.println(parsed);
		Map<String, String> reqMap = HttpRequestUtils.parseQueryString(HTML);
		User user = new User(reqMap.get("userId"), reqMap.get("password"), reqMap.get("name"), reqMap.get("email"));
		DataBase.addUser(user);
		byte[] body = Files.readAllBytes(new File("./webapp/index.html").toPath());
		response302Header(dos, body.length);
		return body;
	}
	
	private byte[] readHTML(String HTML, DataOutputStream dos) throws IOException
	{
		byte[] body = Files.readAllBytes(new File("./webapp" + HTML).toPath());
		
		response200Header(dos, body.length, "text/html;charset=utf-8\r\n");
		responseBody(dos, body);
		return body;
	}
	
	private byte[] readCSS(String HTML, DataOutputStream dos) throws IOException
	{
		byte[] body = Files.readAllBytes(new File("./webapp" + HTML).toPath());
		
		response200Header(dos, body.length, "text/css;charset=utf-8\r\n");
		responseBody(dos, body);
		return body;
	}
	
	private byte[] readLogin(String HTML, DataOutputStream dos) throws IOException
	{
		boolean login = false;
		int deli1 = HTML.indexOf('?');
		int deli2 = HTML.indexOf('&');
		String id = HTML.substring(deli1, deli2);
		int deli3 = id.indexOf('=');
		id = id.substring(deli3+1);
		String pw = HTML.substring(deli2);
		deli3 = pw.indexOf('=');
		pw = pw.substring(deli3+1);
		
		if(pw.equals(DataBase.findUserById(id).getPassword()))
		{
			login = true;
			System.out.println("Login success");
		}
		else
			System.out.println("Login fail");
			
		byte[] body = Files.readAllBytes(new File("./webapp/index.html").toPath());
		
		response200Header(dos, body.length, login);
		responseBody(dos, body);
		return body;
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String css) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: " + css);
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	private void response200Header(DataOutputStream dos, int lengthOfBodyContent, boolean login) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			if(login==true)
				dos.writeBytes("Set-Cookie: logined=true \r\n");
			else
				dos.writeBytes("Set-Cookie: logined=false \r\n");
			
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	private void response302Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 302 Found \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Location: http://localhost:8080/index.html \r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.writeBytes("\r\n");
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
