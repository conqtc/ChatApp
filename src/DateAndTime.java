import java.time.*;
import java.time.format.*;
import java.time.temporal.*;

/**
 * This class encapsulate a date and time object
 * 
 * @author Cuong (Alex) Truong (101265224)
 * @version 0.1
 */
public class DateAndTime implements Comparable<DateAndTime> {
	// The core data of this class is the object Instant
	private Instant instant;
	
	/**
	 * Constructor
	 * @param instant Instant object that this object associated with
	 */
	public DateAndTime(Instant instant) {
		this.instant = instant;
	}
	
	/**
	 * Get Instant object
	 * @return Instant object associated with this object
	 */
	public Instant getInstant() {
		return instant;
	}
	
	/**
	 * Return a DateAndTime object represents current time
	 * @return DateAndTime object
	 */
	public static DateAndTime now() {
		return new DateAndTime(Instant.now());
	}

	/**
	 * Return a string that represents the date and time
	 * @param pattern pattern to format the output
	 * @return String represents date and time in specific pattern format
	 */
    public String toString(String pattern)
    {
    	try {
    		// use system default zone otherwise pattern will not be recognized
    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
    		return formatter.format(this.instant);
    	} catch (Exception e) {
    		// something goes wrong
    		return "Invalid format.";
    	}
    }
    
    /**
     * Convert a number of minutes to date time format: "x day, y hour, z min."
     * @param startTime start time
     * @param endTime end time
     * @return String represents date and time in special format
     */
    public static String betweenAsString(DateAndTime startTime, DateAndTime endTime) {
    	long total = betweenInMinutes(startTime, endTime);	// total number of minutes difference
    	long days = total / 1440;			// number of days (1440 = 24 * 60)
    	long minutesLeft = total % 1440; 	// number of minutes left to calculate hour and minute
    	long hours = minutesLeft / 60;		// number of hours
    	long minutes = minutesLeft % 60;	// number of minutes
    	
    	String dateTimeString = ((days > 0) ? days + " d, " : "") +
    							((hours > 0) ? hours + " h, " : "") +
    							minutes + " min.";
    	
    	return dateTimeString;
    }

    /**
     * Get the minutes duration between two DateAndTime objects
     * @param startTime Start DateAndTime object 
     * @param endTime End DateAndTime object
     * @return long number of minutes between two DateAndTime objects
     */
    public static long betweenInMinutes(DateAndTime startTime, DateAndTime endTime) {
    	try {
    		// get duration in seconds between 2 instants then divide by 60
    		Duration duration = Duration.between(startTime.getInstant(), endTime.getInstant());
    		return duration.getSeconds() / 60;
    	} catch (Exception e) {
    		return 0;
    	}
    }
    
    /**
     * This method is VERY platform-dependent using the TemporalUnit as parameter
     * @param startTime start time
     * @param endTime end time
     * @param unit TemporalUnit (either HOURS, MINUTES etc.)
     * @return long number represent duration of "unit" between two DateAndTime objects
     */
    public static long betweenInTemporalUnit(DateAndTime startTime, DateAndTime endTime, TemporalUnit unit) {
    	try {
    		return unit.between(startTime.getInstant(), endTime.getInstant());
    	} catch (Exception e) {
    		return 0;
    	}
    }

    /**
     * Return a DateAndTime object with added number of minutes to current DateAndTime object
     * @param minutes long number of minutes to be added
     * @return DateAndTime object with new date and time
     */
	public DateAndTime plusMinutes(long minutes) {
		return new DateAndTime(instant.plusMillis(minutes * 60000));
	}
	
	/**
	 * Implement Comparable interface
	 * @param anotherDateAndTime Another DateAndTime to compare to
	 * @return 1 if bigger, 0 if equal, -1 otherwise
	 */
	public int compareTo(DateAndTime anotherDateAndTime) {
		// another date and time is null: come first
		if (anotherDateAndTime == null) {
			return -1;
		}
		
		return this.instant.compareTo(anotherDateAndTime.getInstant());
	}

	/**
	 * Construct DateAndTime object from a string following a pattern
	 * @param dateTimeString Date time string
	 * @param pattern Pattern of the string
	 * @throws DateTimeParseException If unable to parse
	 * @return DateAndTime or null
	 */
	public static DateAndTime fromString(String dateTimeString, String pattern) throws DateTimeParseException {
		DateTimeFormatter formatter;
		try {
			formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
		} catch (IllegalArgumentException iae) {
			throw new DateTimeParseException("DateAndTime.fromString pattern is illegal", dateTimeString, 0);
		}
		
		LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
		// Convert to ZoneDateTime first before Instant
		return new DateAndTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}
}
