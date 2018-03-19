package com.fincons.token.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

public class DateUtil {
//	private static final SimpleDateFormat DATEFORMATFORM= new SimpleDateFormat("MM/dd/yyyy");
//	private static final SimpleDateFormat DATEFORMATRADICAL= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//	private static final SimpleDateFormat DATEFORMATRADICALSENTIMENT= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	final static Logger logger = Logger.getLogger(DateUtil.class);
	
	public static Date GetUTCdatetimeAsDate()
	{		
		//note: doesn't check for null
		return StringDateToDate(GetUTCdatetimeAsString());
	}
	public static String DateToStringDate(Date data){
		final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		final String utcTime = sdf.format(data);		
		return utcTime;
	}
	public static String GetUTCdatetimeAsString()
	{	
		final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		final String utcTime = sdf.format(new Date());		
		return utcTime;
	}

	public static Date StringDateToDate(String StrDate)
	{		
		Date dateToReturn = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);

		try
		{
			dateToReturn = (Date)dateFormat.parse(StrDate);
		}
		catch (ParseException e)
		{
			logger.error("Error parsing the UTC Date!", e);
			e.printStackTrace();
		}

		return dateToReturn;
	}
	
	/**
	 * Convert string date in format MM/dd/yyyy in timestamp
	 * @param date
	 * @return timestamp of date 
	 * @throws ParseException 
	 */

	public static long firstSecondOfDate(Date date){
		date.setHours(0);
		date.setMinutes(0);
		date.setSeconds(0);
		return date.getTime();
	}
	public static long lastSecondOfDate(Date date){
		date.setHours(23);
		date.setMinutes(59);
		date.setSeconds(59);
		return date.getTime();
	}
}
