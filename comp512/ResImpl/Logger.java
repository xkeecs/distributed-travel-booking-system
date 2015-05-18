package ResImpl;

import java.io.Serializable;
import java.util.ArrayList;

public class Logger implements Serializable
{
	/*
	 * public static Hashtable<Integer, Transaction> flightLog; public static Hashtable<Integer, Transaction> carLog;
	 * public static Hashtable<Integer, Transaction> hotelLog;
	 */
	public static ArrayList<Transaction> flightTxLog = new ArrayList<Transaction>();
	public static ArrayList<Transaction> carTxLog = new ArrayList<Transaction>();
	public static ArrayList<Transaction> hotelTxLog = new ArrayList<Transaction>();

	public Logger()
	{
	}
}
