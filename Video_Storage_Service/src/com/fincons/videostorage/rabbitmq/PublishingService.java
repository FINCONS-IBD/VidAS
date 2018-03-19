package com.fincons.videostorage.rabbitmq;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.fincons.rabbitmq.event.Event;
import com.fincons.rabbitmq.publisher.BasicEventFactory;
import com.fincons.rabbitmq.publisher.Publisher;

public class PublishingService implements Runnable {
	private Publisher pubApp;
	private volatile boolean stop;
	private String pathFile;
	private String service;
	
	public PublishingService (Publisher pubApp, String pathFile, String service ) {
		this.pubApp = pubApp;
		this.stop = true;
		this.pathFile = pathFile;
		this.service=service;
	}

	@Override
	public void run() {
		Event ensE;
		try {
//			stop = false;
//			while (!stop && pubApp.isConnected()) {

				ensE = createEvent();
				pubApp.publish(ensE);
				System.out.println("Event published: " + ensE.toString());
//			}
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		}

	}
	/*
	 * per ora passiamo solo il path del file, in futuro verrà passata anche la policy per criptare il file ottenuto dall'analisi
	 */
	private Event createEvent () {	

		StringBuffer payload = new StringBuffer();
		Map<String,Object> headers = new HashMap<String,Object>();
		headers.put("Path", pathFile + "");
		headers.put("Service", service + "");
		payload.append(pathFile);
		payload.append(", ");
		payload.append(service);
		try {
			return new BasicEventFactory().create(headers,payload.toString().getBytes(Event.DEFAULT_CONTENT_ENCODING), false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}
	
	public void stop () {
		this.stop = true;
	}

}
