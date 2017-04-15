import java.util.*;

/**
 * Utility class which provide common methods for the system
 * 
 * @author Cuong Truong (101265224)
 * @version 0.1
 */

public class Utility {
	// scanner object to read user input from keyboard
	private Scanner scanner;
	
    /**
     * Default constructor
     */
    public Utility() {
    	// read from user keyboard
    	scanner = new Scanner(System.in);
    }
    
    /**
     * "destructor" for this class, we call this to clean up objects
     */
    public void destructor() {
    	scanner.close();
    }
    
    /**
     * simply "clear" console screen by print many lines
     */
    public static void clearConsoleScreen() {
        for (int i = 0; i < 50; i++)
    		System.out.println();
    }
    

    /**
     * Check if a String is null or empty
     * @param text String to be checked
     * @return true if String is null or empty, false otherwise
     */
    public static boolean stringIsNullOrEmpty(String text) {
        if (text == null)
            return true;
            
        if (text.isEmpty())
            return true;
            
        return false;
    }
            
    /**
     * Read next integer number from keyboard
     * @return int number, -1 if unable to read
     */
    public int nextInt() {
    	int number = -1;
    	
    	try {
    		// try to parse input line from scanner
    		number = Integer.parseInt(scanner.nextLine());
    	} catch (Exception e) {
    		// if anything wrong happens, just give it -1
    		number = -1;
    	}
    	
    	return number;
    }
    
    /**
     * Read the whole line from user input
     * @return String line text received from user input
     */
    public String nextLine() {
    	String line ="";
    	
    	try {
    		line = scanner.nextLine();
    	} catch (Exception e) {
    		// if anything wrong happens, just give it an empty line
    		line = "";
    	}
    	
    	return line;
    }
    
    /**
     * Log next line
     * @param message Message to print out before waiting for next line
     * @return String input from user
     */
    public String logNextLine(String message) {
    	System.out.print(message);
    	String line = scanner.nextLine().trim();
    	return line;
    }


    /**
     * Log message 
     * @param message Message
     */
    public static void logMsg(String message) {
    	System.out.print(message);
    }

    /**
     * Log error message
     * @param err Error message
     */
    public static void logErr(String err) {
    	System.out.print("(x) " + err);
    }
    
    /**
     * Log a message
     * @param message Message
     */
    public static void log(String message) {
    	System.out.print(message);
    }

    /**
     * Log line message
     * @param message Message
     */
    public static void loglnMsg(String message) {
    	System.out.println("(i) " + message);
    }
    
    /**
     * Log error message then new line
     * @param err Error message
     */
    public static void loglnErr(String err) {
    	System.out.println("(x) " + err);
    }
    
    /**
     * Log a message then new line
     * @param message Message
     */
    public static void logln(String message) {
    	System.out.println(message);
    }
    
    /**
     * Wait for enter
     * @param message Message
     */
    public void waitForEnter(String message) {
    	System.out.print(message);
    	this.nextLine();
    }
}
