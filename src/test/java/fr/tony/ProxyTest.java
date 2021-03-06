package fr.tony;

import static org.junit.Assert.assertEquals;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.stop;

import org.apache.http.HttpHost;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class ProxyTest {

	static final int PROXY_PORT = 8085;
	
	static Proxy proxy = new Proxy(PROXY_PORT);
	static Thread thread = null;
	
	@BeforeClass
	public static void initProxy() throws InterruptedException {

		// Start proxy in separate thread
		thread = new Thread(() -> {
			proxy.listen();
		});
		thread.start();
		
		// Start a Web resource
		get("/hello", (req, res) -> {
			//System.out.println(req.headers());
			if (!"my-value".equals(req.headers("My-Param"))) {
				throw new Exception();
			}
			return "Hello World";
		});
		
		post("/say", (req, res) -> {
			//System.out.println("BODY received: " + req.body());
			if (!"val1".equals(req.queryParams("field1"))) {
				throw new Exception();
			}
			return "ok";
		});
		
		// Use proxy for all unirest requests
		Unirest.setProxy(new HttpHost("127.0.0.1", PROXY_PORT));
		
		Thread.sleep(2000); // waiting web server boot
	}
	
	@AfterClass
	public static void stopProxy() {
		proxy.closeServer(); // stop proxy
		stop(); // stop web server
	}

	@Test
	public void get_request() throws UnirestException {
		
		HttpResponse<String> response = Unirest.get("http://localhost:4567/hello")
		.header("Content-Type", "application/json")
		.header("My-Param", "my-value")
		.asString();
		
		assertEquals("Hello World", response.getBody());
	}

	@Test
	public void post_request() throws UnirestException {
		
		HttpResponse<String> response = Unirest.post("http://localhost:4567/say")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.field("field1", "val1")
				.field("field2", "val2")
				.asString();
		
		assertEquals("ok", response.getBody());
	}
	
}
