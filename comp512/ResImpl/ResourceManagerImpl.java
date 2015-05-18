// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package ResImpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.transaction.InvalidTransactionException;

import LockManager.DeadlockException;
import LockManager.LockManager;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class ResourceManagerImpl implements ResourceManager
{

	public static RMHashtable m_itemHT;
	LockManager lockManager = new LockManager();
	// transactionManager TM = new transactionManager(this);
	public static String flight = "flight";
	public static String car = "car";
	public static String hotel = "hotel";
	public static String customerkey = "customer";
	protected String configureFile = "";
	public static boolean isCrash = false;
	public static String dataFile1 = "datafile_1.dat";
	public static String dataFile2 = "datafile_2.dat";
	public static String masterRecord = "masterRecord.txt";
	public static String backupFileName = "backup.bat";
	public static Hashtable<Integer, Timer> timerTable = new Hashtable<Integer, Timer>();
	public static Hashtable<Integer, Boolean> iftimerTable = new Hashtable<Integer, Boolean>();
	public static ArrayList<Integer> txlist = new ArrayList<Integer>();
	public static boolean case1 = false;
	public static boolean case2 = false;
	public static boolean case2_2 = false;
	public static boolean case3 = false;

	public void setCase1() throws RemoteException
	{
		System.out.println("Crash after receive vote request but before sending answer");
		case1 = true;
	}

	public void unsetCase1() throws RemoteException
	{
		System.out.println("Crash after receive vote request but before sending answer");
		case1 = false;
	}

	public void setCase2() throws RemoteException
	{
		System.out.println("Crash after sending answer: abort");
		case2 = true;
	}

	public void unsetCase2() throws RemoteException
	{
		System.out.println("Crash after sending answer: abort");
		case2 = false;
	}

	public void setCase3() throws RemoteException
	{
		System.out.println("Crash after sending answer: commit");
		case2_2 = true;
	}

	public void unsetCase3() throws RemoteException
	{
		System.out.println("Crash after sending answer: commit");
		case2_2 = false;
	}

	public void setCase4() throws RemoteException
	{
		System.out.println("Crash after receiving decision but before committing/aborting");

		case3 = true;
	}

	public void unsetCase4() throws RemoteException
	{
		System.out.println("Crash after receiving decision but before committing/aborting");

		case3 = false;
	}

	public class TimeoutTask extends TimerTask
	{
		int transID;

		public TimeoutTask(int id)
		{
			transID = id;
		}

		@Override
		public void run()
		{
			// TODO Auto-generated method stub
			// Time out, abort transaction
			iftimerTable.put(transID, true);
			try
			{
				System.out.println("Timeout! abort all");
				uncommit(transID);
			}
			catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[])
	{
		// Figure out where server is running
		// String server = "132.206.52.100";
		// String server = "127.0.0.1";
		int port = Integer.parseInt(args[0]);

		if (args.length == 1)
		{
			// server = server + ":" + args[0];
			port = Integer.parseInt(args[0]);
		}
		else if (args.length != 2 && args.length != 1)
		{
			System.err.println("Wrong usage");
			System.out.println("Usage: java ResImpl.ResourceManagerImpl [port]");
			System.exit(1);
		}

		try
		{
			// create a new Server object
			ResourceManagerImpl obj = new ResourceManagerImpl();
			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry(port);
			// 1st parameter as the server's name
			registry.rebind(args[1], rm);

			System.err.println("Server ready");
		}
		catch (Exception e)
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}

		File f = new File("masterRecord.txt");
		if (f.exists())
		{
			/* do something */
			System.out.println("Master Record Exists, Loading Data From It");
			String dataFile = readRecord();
			System.out.println("The record is" + dataFile);
			readHT(dataFile);
		}
		else
		{
			m_itemHT = new RMHashtable();
		}

		// Create and install a security manager
		/*
		 * if (System.getSecurityManager() == null) { System.setSecurityManager(new RMISecurityManager()); }
		 */
	}

	public ResourceManagerImpl() throws RemoteException
	{
	}

	/*
	 * protected void joinCluster() { try { // channel // channel = new JChannel(configureFile); } catch (Exception e) {
	 * // TODO Auto-generated catch block e.printStackTrace(); }
	 * 
	 * // channel.send(msg);
	 * 
	 * }
	 */

	// Reads a data item
	private RMItem readData(int id, String key)
	{
		synchronized (m_itemHT)
		{
			return (RMItem) m_itemHT.get(key);
		}
	}

	// Writes a data item
	private void writeData(int id, String key, RMItem value)
	{
		synchronized (m_itemHT)
		{
			m_itemHT.put(key, value);
		}
	}

	// Remove the item out of storage
	protected RMItem removeData(int id, String key)
	{
		synchronized (m_itemHT)
		{
			return (RMItem) m_itemHT.remove(key);
		}
	}

	// deletes the entire item
	protected boolean deleteItem(int id, String key)
	{
		Trace.info("RM::deleteItem(" + id + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(id, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(id, curObj.getKey());
				Trace.info("RM::deleteItem(" + id + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + id + ", " + key
						+ ") item can't be deleted because some customers reserved it");
				return false;
			}
		} // if
	}

	// query the number of available seats/rooms/cars
	protected int queryNum(int id, String key)
	{
		Trace.info("RM::queryNum(" + id + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(id, key);
		int value = 0;
		if (curObj != null)
		{
			value = curObj.getCount();

		} // else
		Trace.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
		return value;
	}

	// query the price of an item
	protected int queryPrice(int id, String key)
	{
		Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(id, key);
		int value = 0;
		if (curObj != null)
		{
			value = curObj.getPrice();
		} // else
		Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$" + value);
		return value;
	}

	// reserve an item
	protected boolean reserveItem(int id, int customerID, String key, String location)
	{
		Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " + key + ", " + location + " ) called");
		// Read customer object if it exists (and read lock it)
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null)
		{
			Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", " + location
					+ ")  failed--customer doesn't exist");
			return false;
		}

		// check if the item is available
		ReservableItem item = (ReservableItem) readData(id, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location
					+ ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location
					+ ") failed--No more items");
			return false;
		}
		else
		{
			cust.reserve(key, location, item.getPrice());
			writeData(id, cust.getKey(), cust);

			// decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);

			Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	protected boolean unreserveItem(int id, int customerID, String key, String location)
	{
		System.out.println("Unreserving ITEM");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));

		if (cust == null)
		{
			Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", " + location
					+ ")  failed--customer doesn't exist");
			return false;
		}

		// check if the item is available
		ReservableItem item = (ReservableItem) readData(id, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location
					+ ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location
					+ ") failed--No more items");
			return false;
		}
		else
		{
			cust.unreserve(key, location, item.getPrice());
			writeData(id, cust.getKey(), cust);

			// increase the number of available items in the storage
			item.setCount(item.getCount() + 1);
			item.setReserved(item.getReserved() - 1);

			Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException,
			DeadlockException, RedundantLockRequestException
	{
		Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called");
		Flight curObj = (Flight) readData(id, Flight.getKey(flightNum));

		// construct a command object and add it to Transaction record
		/*
		 * addFlightOP addflightcmd = new addFlightOP(); lockManager.Lock(id, flight, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, addflightcmd);

		if (curObj == null)
		{
			// doesn't exist...add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(id, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + id + ") created new flight " + flightNum + ", seats=" + flightSeats
					+ ", price=$" + flightPrice);

		}
		else
		{
			// add seats to existing flight and update the price...
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			} // if
			writeData(id, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats="
					+ curObj.getCount() + ", price=$" + flightPrice);
		} // else

		return (true);
	}

	public void setFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException,
			DeadlockException, RedundantLockRequestException
	{
		// set flight to previous state
		Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called");
		Flight curObj = (Flight) readData(id, Flight.getKey(flightNum));

		// construct a command object and add it to Transaction record
		/*
		 * addFlightOP addflightcmd = new addFlightOP(); lockManager.Lock(id, flight, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, addflightcmd);

		// add seats to existing flight and update the price...
		curObj.setCount(flightSeats);
		if (flightPrice > 0)
		{
			curObj.setPrice(flightPrice);
		} // if
		writeData(id, curObj.getKey(), curObj);
		Trace.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount()
				+ ", price=$" + flightPrice);

	}

	public boolean deleteFlight(int id, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		/*
		 * deleteFlightOP deleteflightcmd = new deleteFlightOP(); lockManager.Lock(id, flight, DataObj.WRITE); //
		 * TM.addOPtoTransaction(id, deleteflightcmd);
		 */return deleteItem(id, Flight.getKey(flightNum));
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int id, String location, int count, int price) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		/*
		 * addRoomOP addroomcmd = new addRoomOP(); lockManager.Lock(id, hotel, DataObj.WRITE); //
		 * TM.addOPtoTransaction(id, addroomcmd);
		 */
		Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
		Hotel curObj = (Hotel) readData(id, Hotel.getKey(location));
		if (curObj == null)
		{
			// doesn't exist...add it
			Hotel newObj = new Hotel(location, count, price);
			writeData(id, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + id + ") created new room location " + location + ", count=" + count
					+ ", price=$" + price);
		}
		else
		{
			// add count to existing object and update price...
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			} // if
			writeData(id, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + id + ") modified existing location " + location + ", count="
					+ curObj.getCount() + ", price=$" + price);
		} // else
		return (true);
	}

	public void setRooms(int id, String location, int count, int price) throws RemoteException, DeadlockException,
			RedundantLockRequestException

	{
		Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
		Hotel curObj = (Hotel) readData(id, Hotel.getKey(location));

		// add count to existing object and update price...
		curObj.setCount(count);
		if (price > 0)
		{
			curObj.setPrice(price);
		} // if
		writeData(id, curObj.getKey(), curObj);
		Trace.info("RM::addRooms(" + id + ") modified existing location " + location + ", count=" + curObj.getCount()
				+ ", price=$" + price);
		// else
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		/*
		 * deleteHotelOP deleteroomcmd = new deleteHotelOP(); lockManager.Lock(id, hotel, DataObj.WRITE); //
		 * TM.addOPtoTransaction(id, deleteroomcmd);
		 */
		return deleteItem(id, Hotel.getKey(location));

	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int id, String location, int count, int price) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car) readData(id, Car.getKey(location));

		/*
		 * addCarOP addcarcmd = new addCarOP(); lockManager.Lock(id, Car.getKey(location), DataObj.WRITE); //
		 * TM.addOPtoTransaction(id, addcarcmd);
		 */
		if (curObj == null)
		{
			// car location doesn't exist...add it
			Car newObj = new Car(location, count, price);
			writeData(id, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + id + ") created new location " + location + ", count=" + count + ", price=$"
					+ price);
		}
		else
		{
			// add count to existing car location and update price...
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			} // if
			writeData(id, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + id + ") modified existing location " + location + ", count="
					+ curObj.getCount() + ", price=$" + price);
		} // else

		return (true);
	}

	public void setCars(int id, String location, int count, int price) throws RemoteException, DeadlockException,
			RedundantLockRequestException

	{
		Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car) readData(id, Car.getKey(location));
		// add count to existing car location and update price...
		curObj.setCount(count);
		if (price > 0)
		{
			curObj.setPrice(price);
		} // if
		writeData(id, curObj.getKey(), curObj);
		Trace.info("RM::addCars(" + id + ") modified existing location " + location + ", count=" + curObj.getCount()
				+ ", price=$" + price);

	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		/*
		 * deleteCarOP deletecarcmd = new deleteCarOP(); lockManager.Lock(id, car, DataObj.WRITE); //
		 * TM.addOPtoTransaction(id, deletecarcmd);
		 */
		return deleteItem(id, Car.getKey(location));
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		// lockManager.Lock(id, flight, DataObj.READ);

		return queryNum(id, Flight.getKey(flightNum));
	}

	// Returns the number of reservations for this flight.
	// public int queryFlightReservations(int id, int flightNum)
	// throws RemoteException
	// {
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
	// RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
	// if ( numReservations == null ) {
	// numReservations = new RMInteger(0);
	// } // if
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
	// return numReservations.getValue();
	// }

	// Returns price of this flight
	public int queryFlightPrice(int id, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		// lockManager.Lock(id, flight, DataObj.READ);

		return queryPrice(id, Flight.getKey(flightNum));
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{

		// lockManager.Lock(id, hotel, DataObj.READ);

		return queryNum(id, Hotel.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		// lockManager.Lock(id, hotel, DataObj.READ);

		return queryPrice(id, Hotel.getKey(location));
	}

	// Returns the number of cars available at a location
	public int queryCars(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		// lockManager.Lock(id, car, DataObj.READ);
		return queryNum(id, Car.getKey(location));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		// lockManager.Lock(id, car, DataObj.READ);

		return queryPrice(id, Car.getKey(location));
	}

	// Returns data structure containing customer reservation info. Returns null if the
	// customer doesn't exist. Returns empty RMHashtable if customer exists but has no
	// reservations.
	public RMHashtable getCustomerReservations(int id, int customerID) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{

		// lockManager.Lock(id, customerkey + customerID, DataObj.READ);

		Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null)
		{
			Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID
					+ ") failed--customer doesn't exist");
			return null;
		}
		else
		{
			return cust.getReservations();
		} // if
	}

	// return a bill
	public String queryCustomerInfo(int id, int customerID) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		// lockManager.Lock(id, customerkey + customerID, DataObj.READ);

		Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
			return ""; // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
		}
		else
		{
			String s = cust.printBill();
			Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows...");
			System.out.println(s);
			return s;
		} // if
	}

	// customer functions
	// new customer just returns a unique customer identifier

	public int newCustomer(int id) throws RemoteException, DeadlockException, RedundantLockRequestException
	{

		Trace.info("INFO: RM::newCustomer(" + id + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(id)
				+ String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
				+ String.valueOf(Math.round(Math.random() * 100 + 1)));
		// lockManager.Lock(id, customerkey + cid, DataObj.WRITE);

		Customer cust = new Customer(cid);
		writeData(id, cust.getKey(), cust);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public int addNewCustomer(int cid) throws RemoteException, DeadlockException, RedundantLockRequestException
	{
		Customer cust = new Customer(cid);

		writeData(0, cust.getKey(), cust);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	// I opted to pass in customerID instead. This makes testing easier
	public boolean newCustomer(int id, int customerID) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{

		Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
		// lockManager.Lock(id, customerkey + customerID, DataObj.READ);

		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null)
		{
			// lockManager.Lock(id, customerkey + customerID, DataObj.WRITE);

			cust = new Customer(customerID);
			writeData(id, cust.getKey(), cust);
			Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer");
			return true;
		}
		else
		{
			Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
			return false;
		} // else
	}

	// Deletes customer from the database.
	public boolean deleteCustomer(int id, int customerID) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
		// lockManager.Lock(id, customerkey + customerID, DataObj.WRITE);

		Customer cust = (Customer) readData(id, Customer.getKey(customerID));
		if (cust == null)
		{
			Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist");
			return false;
		}
		else
		{
			// Increase the reserved numbers of all reservable items which the customer reserved.
			RMHashtable reservationHT = cust.getReservations();
			for (Enumeration e = reservationHT.keys(); e.hasMoreElements();)
			{
				String reservedkey = (String) (e.nextElement());
				ReservedItem reserveditem = cust.getReservedItem(reservedkey);
				Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey()
						+ " " + reserveditem.getCount() + " times");
				ReservableItem item = (ReservableItem) readData(id, reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey()
						+ "which is reserved" + item.getReserved() + " times and is still available " + item.getCount()
						+ " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
			}

			// remove the customer from the storage
			removeData(id, cust.getKey());

			Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded");
			return true;
		} // if
	}

	/*
	 * // Frees flight reservation record. Flight reservation records help us make sure we // don't delete a flight if
	 * one or more customers are holding reservations public boolean freeFlightReservation(int id, int flightNum) throws
	 * RemoteException { Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" ); RMInteger
	 * numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) ); if ( numReservations !=
	 * null ) { numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) ); } // if writeData(id,
	 * Flight.getNumReservationsKey(flightNum), numReservations ); Trace.info("RM::freeFlightReservations(" + id + ", "
	 * + flightNum + ") succeeded, this flight now has " + numReservations + " reservations" ); return true; }
	 */

	// Adds car reservation to this customer.
	public boolean reserveCar(int id, int customerID, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		/*
		 * reserveCarOP reservecarcmd = new reserveCarOP(); lockManager.Lock(id, car, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, reservecarcmd);

		return reserveItem(id, customerID, Car.getKey(location), location);
	}

	public boolean unreserveCar(int id, int customerID, String location)
	{
		/*
		 * reserveCarOP reservecarcmd = new reserveCarOP(); lockManager.Lock(id, car, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, reservecarcmd);

		return unreserveItem(id, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer.
	public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		/*
		 * reserveHotelOP reservehotelcmd = new reserveHotelOP(); lockManager.Lock(id, hotel, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, reservehotelcmd);

		return reserveItem(id, customerID, Hotel.getKey(location), location);
	}

	public boolean unreserveRoom(int id, int customerID, String location)
	{

		return unreserveItem(id, customerID, Hotel.getKey(location), location);
	}

	// Adds flight reservation to this customer.
	public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		/*
		 * reserveFlightOP reserveflightcmd = new reserveFlightOP(); lockManager.Lock(id, hotel, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, reserveflightcmd);

		return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	public boolean unreserveFlight(int id, int customerID, int flightNum)
	{
		/*
		 * reserveFlightOP reserveflightcmd = new reserveFlightOP(); lockManager.Lock(id, hotel, DataObj.WRITE); //
		 * TM.addOPtoTransaction(id, reserveflightcmd);
		 */
		return unreserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Reserve an itinerary
	public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car1, boolean Room)
			throws RemoteException, DeadlockException, RedundantLockRequestException
	{
		Trace.info("RM::itinerary(" + id + ", " + customer + ") called");
		return false;
	}

	public boolean vote2PC(int transactionID) throws RemoteException
	{
		System.out.println("Voting to the coordinator");

		if (case1)
		{
			System.exit(1);
		}

		boolean some = iftimerTable.containsKey(transactionID);
		if (some)
		{
			System.out.println("Timer found");
			boolean iftimeout = iftimerTable.get(transactionID);
			if (isCrash || iftimeout)
			{
				Timer timer = timerTable.get(transactionID);
				timer.cancel();

				timerTable.remove(transactionID);
				iftimerTable.remove(transactionID);
				System.out.println("Timeout!!!!!!!!!!!!!!!! Return abort to Middleware");
				startTimer(transactionID);
				if (case2)
				{
					System.exit(1);
				}
				return false;
			}
			else
			{ // backup files
				System.out.println("Cancel Timer");
				Timer timer = timerTable.get(transactionID);
				timer.cancel();
				timerTable.remove(transactionID);
				iftimerTable.remove(transactionID);
				System.out.println("Writing log information");
				txlist.add(transactionID);
				writeList();
				backupHT(backupFileName);
				System.out.println("Sending YES, waiting for decision");
				return true;
			}
		}

		boolean ifkeythere = timerTable.containsKey(transactionID);
		if (ifkeythere)
		{
			System.out.println("cancel vote-request Timer");
			Timer timer = timerTable.get(transactionID);
			timer.cancel();
			timerTable.remove(transactionID);
			iftimerTable.remove(transactionID);
		}

		if (isCrash)
		{
			System.out.println("crashing?");
			startTimer(transactionID);
			if (case2)
			{
				System.exit(1);
			}
			return false;
		}
		else
		{ // backup files
			txlist.add(transactionID);
			writeList();
			backupHT(backupFileName);
			System.out.println("Sending YES, waiting for decision");
			return true;
		}
	}

	public int currentXID;
	public double TTL;

	/* commit the change */
	public boolean commit(int id) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		System.out.println("Decision is to commit, executing...");

		txlist.remove(id);
		String currentFile = readRecord();
		if (case3)
		{
			System.exit(1);
		}
		// update stable storage
		writeHT(getDataFileToWrite());

		// update master record
		writeRecord(getDataFileToWrite());

		File backup = new File(backupFileName);
		if (backup.exists())
		{
			backup.delete();
		}
		return true;
		// unlock all data
	}

	/* abort transaction */
	public void abort(int transactionId) throws RemoteException, InvalidTransactionException
	{
		// restore data

	}

	public boolean selfDestruct() throws RemoteException
	{
		System.out.println("Is Crashing......");
		// unlock all data
		// clean transaction
		System.exit(1);
		return true;
	}

	@Override
	public int start()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean shutdown() throws RemoteException
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void writeHT(String filename)
	{
		try
		{
			System.out.println("Creating File/Object output stream...");
			FileOutputStream fileOut = new FileOutputStream(filename);
			OutputStream buffer = new BufferedOutputStream(fileOut);
			ObjectOutputStream out = new ObjectOutputStream(buffer);

			System.out.println("Writing Data Hashtable...");
			out.writeObject(m_itemHT);
			System.out.println("Done!!!\n");
			out.close();
			fileOut.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public ArrayList<Integer> recovery(HashMap<Integer, Boolean> map) throws RemoteException
	{
		// read txlist from file, the variable is the list that transactions with decision
		txlist = readTxlist();
		ArrayList<Integer> mylist = new ArrayList<Integer>();
		for (int my : txlist)
		{
			if (map.containsKey(my))
			{
				mylist.add(my);
			}

		}
		System.out.println("Recoverying from log information");

		readHT(backupFileName);

		return mylist;
	}

	public void backupHT(String filename)
	{
		try
		{
			System.out.println("Creating File/Object output stream...");
			FileOutputStream fileOut = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			System.out.println("Writing Data Hashtable...");
			out.writeObject(m_itemHT);
			System.out.println("Done!!!\n");
			out.close();
			fileOut.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void writeList()
	{
		try
		{
			System.out.println("Creating File/Object output stream...");
			FileOutputStream fileOut = new FileOutputStream("TxList.dat");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			System.out.println("Writing Data Hashtable...");
			out.writeObject(txlist);
			System.out.println("Done!!!\n");
			out.close();
			fileOut.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void readHT(String filename)
	{
		try
		{
			// System.out.println("Creating File/Object input stream...");
			FileInputStream fileIn = new FileInputStream(filename);
			InputStream buffer = new BufferedInputStream(fileIn);
			ObjectInputStream in = new ObjectInputStream(buffer);
			// System.out.println("Loading Hashtable Object...");

			m_itemHT = (RMHashtable) in.readObject();
			// do sth with object

			// System.out.println("Closing all input streams...\n");
			in.close();
			fileIn.close();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static ArrayList<Integer> readTxlist()
	{
		try
		{
			System.out.println("Creating File/Object input stream...");
			FileInputStream fileIn = new FileInputStream("TxList.dat");
			InputStream buffer = new BufferedInputStream(fileIn);
			ObjectInputStream in = new ObjectInputStream(buffer);
			System.out.println("Loading Hashtable Object...");

			txlist = (ArrayList<Integer>) in.readObject();
			// do sth with object

			System.out.println("Closing all input streams...\n");
			in.close();
			fileIn.close();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return txlist;
	}

	public static String readRecord()
	{
		String dataName = "";
		/*
		 * try { System.out.println("Reading master record..."); FileInputStream fileIn = new FileInputStream(f);
		 * ObjectInputStream in = new ObjectInputStream(fileIn); System.out.println("Reading Record..."); dataName =
		 * (String) in.readObject(); System.out.println("Closing all input streams...\n"); in.close(); fileIn.close(); }
		 * catch (ClassNotFoundException e) { e.printStackTrace(); } catch (FileNotFoundException e) {
		 * e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
		 */
		try
		{
			Scanner fileData = new Scanner(new File(masterRecord));

			while (fileData.hasNextLine())
			{
				String line = fileData.nextLine();
				dataName = line.trim();

				if ("".equals(line))
				{
					continue;
				} // end if

			} // end while

			fileData.close(); // close file
		} // end try

		catch (FileNotFoundException e)
		{
			// Error message
		} // end catch
		return dataName;
	}

	public static void writeRecord(String dataFileName)
	{
		/*
		 * try { System.out.println("Writing master record"); FileOutputStream fileOut = new
		 * FileOutputStream("masterRecord.txt"); ObjectOutputStream out = new ObjectOutputStream(fileOut);
		 * out.writeObject(dataFileName); System.out.println("Done!!!\n"); out.close(); fileOut.close(); } catch
		 * (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
		 */
		try
		{
			PrintWriter toFile = new PrintWriter(masterRecord);
			toFile.println(dataFileName);
			toFile.close();
		} // end try

		catch (FileNotFoundException e)
		{
			// Error Message saying file could not be opened
		} // end catch

	}

	public void abortAll()
	{
		String currentone = readRecord();
		if (currentone.equalsIgnoreCase(dataFile1))
		{
			readHT(dataFile1);
			// writeRecord(dataFile2);
		}
		else
		{
			readHT(dataFile2);
			// writeRecord(dataFile1);
		}

	}

	public void startTimer(int id) throws RemoteException
	{
		System.out.println("Timer Started");
		Timer timeout = new Timer();
		timeout.schedule(new TimeoutTask(id), 100000);
		timerTable.put(id, timeout);
		iftimerTable.put(id, false);
		// txlist.add(id);
	}

	public static String getDataFileToWrite()
	{
		String currentFile = readRecord();
		if (currentFile.equalsIgnoreCase(dataFile1))
		{
			return dataFile2;
		}
		else
		{
			return dataFile1;
		}
	}

	public void uncommit(int id) throws RemoteException
	{
		System.out.println("decision is to abort all - executing at RM");
		try
		{
			// txlist.remove(new Integer(id));
		}
		catch (IndexOutOfBoundsException e)
		{
			System.out.println("Tx_id not there");
		}
		abortAll();// String currentFile = readRecord();
		/*
		 * if (currentFile.equalsIgnoreCase(dataFile1)) { writeRecord(dataFile2); } else { writeRecord(dataFile1); }
		 */
	}

	@Override
	public boolean commit() throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean crash(String which) throws RemoteException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDieAfterPrepare(String which) throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setNotKill() throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean prepare(int transactionId)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setcase1(String which) throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setcase2(String which) throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setcase3(String which) throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setcase4(String which) throws RemoteException
	{
		// TODO Auto-generated method stub

	}

}
