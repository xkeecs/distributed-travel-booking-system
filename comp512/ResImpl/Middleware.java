package ResImpl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.transaction.InvalidTransactionException;

import operations.DeleteCustomerOP;
import operations.NewCustomerOP;
import operations.addCarOP;
import operations.addCustomerOP;
import operations.addFlightOP;
import operations.addRoomOP;
import operations.deleteCarOP;
import operations.deleteFlightOP;
import operations.deleteHotelOP;
import operations.reserveCarOP;
import operations.reserveFlightOP;
import operations.reserveHotelOP;
import LockManager.DataObj;
import LockManager.DeadlockException;
import LockManager.LockManager;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class Middleware implements ResourceManager
{
	static final int flightRM = 0;
	static final int carRM = 1;
	static final int hotelRM = 2;
	static final int RMNum = 3;
	public static final String flight = "flight";
	public static final String car = "car";
	public static final String hotel = "hotel";
	public static final String customerkey = "customer";
	public static boolean iskill = false;
	public static String item;
	public static boolean beforesending = false;
	public static boolean case3 = false;

	// ArrayList<ResourceManager> ArrayRM = new ArrayList<ResourceManager>();
	// static int port_server = 1990;
	transactionManager TM;
	LockManager lockManager;
	protected static ArrayList<ResourceManager> RMlist = new ArrayList<ResourceManager>();
	public static HashMap<Integer, Boolean> decisionlist = new HashMap<Integer, Boolean>();

	public Middleware(String flightRM1, int flightPort, String carRM1, int carPort, String roomRM1, int roomPort)
	{

		// Figure out where server is running
		// String server = "132.206.52.125";

		// bound transaction manager to this middleware
		try
		{
			TM = new transactionManager(this);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		lockManager = new LockManager();

		/*
		 * if (RM.length != 3) { System.err.println("Wrong usage"); System.out.println("Usage: Need at least 3 RMs");
		 * System.exit(1); }
		 */

		/*
		 * Initialization; Perform as RMs, wait for clients
		 */

		// Create and install a security manager
		/*
		 * if (System.getSecurityManager() == null) { System.setSecurityManager(new RMISecurityManager()); }
		 */

		/*
		 * Initialization; Perform as clients Connect to RMs
		 */

	}

	protected RMHashtable m_itemHT = new RMHashtable();

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

	public void connection(String flightRM1, int flightPort, String carRM1, int carPort, String roomRM1, int roomPort,
			int rmi_port)
	{
		TM.start();
		System.out.println("Transaction Manager Starts!");

		Middleware middlebox2 = new Middleware(flightRM1, flightPort, carRM1, carPort, roomRM1, roomPort);
		String[] RM = { flightRM1, carRM1, roomRM1 };
		int[] port = { flightPort, carPort, roomPort };

		try
		{
			// create a new Server object
			// dynamically generate the stub (client proxy)
			ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(middlebox2, 0);
			// Bind the remote object's stub in the registry
			// System.out.println(rmi_port);
			Registry registry = LocateRegistry.getRegistry(rmi_port);
			registry.rebind("30Middleware", rm);

			System.err.println("Middleware ready");
		}
		catch (Exception e)
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}

		for (int i = 0; i < RM.length; i++)
		{
			try
			{
				// get a reference to the rmiregistry
				// System.out.println("RM addr " + RM[i]);

				Registry registry = LocateRegistry.getRegistry(RM[i], port[i]);
				// get the proxy and the remote reference by rmiregistry lookup
				// ResourceManager rm = (ResourceManager) registry.lookup(args[i]);
				// add a RM to the list
				RMlist.add((ResourceManager) registry.lookup("RM" + port[i]));

				if (RMlist.get(i) != null)
				{
					// System.out.println("Successful");
					System.out.println("Middleware Has Been Connected to RMs");
				}
				else
				{
					System.out.println("Unsuccessful");
				}
				// make call on remote method
			}
			catch (Exception e)
			{
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String userName = null;

		// read the username from the command-line; need to use try/catch with the
		// readLine() method
		while (true)
		{
			System.out.println("Please enter your command:");

			try
			{
				userName = br.readLine();
				userName = userName.trim();
			}
			catch (IOException ioe)
			{
				System.out.println("IO error !");
				System.exit(1);
			}
			if (userName.equalsIgnoreCase("flightServer"))
			{
				try
				{
					Registry registry = LocateRegistry.getRegistry(RM[flightRM], port[flightRM]);
					// get the proxy and the remote reference by rmiregistry lookup
					// ResourceManager rm = (ResourceManager) registry.lookup(args[i]);
					// add a RM to the list
					// RMlist.remove(flightRM);
					RMlist.set(flightRM, (ResourceManager) registry.lookup("RM" + port[flightRM]));
					System.out.println("Flight RM connected");
					System.out.println("Recovering uncommited transaction");

					if (Logger.flightTxLog.isEmpty() == false)
					{// redo all uncommited transaction
						ArrayList<Integer> tolist = RMlist.get(flightRM).recovery(decisionlist);
						for (int tid : tolist)
						{
							Boolean decision = decisionlist.get(tid);
							if (decision == true)
							{
								// commitcommit
								System.out.println("Commiting to server");
								RMlist.get(flightRM).commit(tid);
							}
							else
							{
								// abort
								abort(tid);
								try
								{
									RMlist.get(flightRM).uncommit(tid);

								}
								catch (Exception e)
								{
									// TODO Auto-generated catch block
									// e.printStackTrace();
									System.out.println("flight server remote exception".toUpperCase());
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					System.err.println("Client exception: " + e.toString());
					e.printStackTrace();
				}
			}
			else if (userName.equalsIgnoreCase("carServer"))
			{

				try
				{
					Registry registry = LocateRegistry.getRegistry(RM[carRM], port[carRM]);
					// get the proxy and the remote reference by rmiregistry lookup
					// ResourceManager rm = (ResourceManager) registry.lookup(args[i]);
					// add a RM to the list
					// RMlist.remove(carRM);
					RMlist.set(carRM, (ResourceManager) registry.lookup("RM" + port[carRM]));
					System.out.println("Car RM connected");

					if (Logger.carTxLog.isEmpty() == false)
					{// redo all uncommited transaction
						ArrayList<Integer> tolist = RMlist.get(carRM).recovery(decisionlist);
						for (int tid : tolist)
						{
							Boolean decision = decisionlist.get(tid);
							if (decision == true)
							{
								// commitcommit
								RMlist.get(carRM).commit(tid);
							}
							else
							{
								// abort
								abort(tid);
								try
								{
									RMlist.get(carRM).uncommit(tid);

								}
								catch (Exception e)
								{
									// TODO Auto-generated catch block
									// e.printStackTrace();
									System.out.println("flight server remote exception".toUpperCase());
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					System.err.println("Client exception: " + e.toString());
					e.printStackTrace();
				}

			}
			else if (userName.equalsIgnoreCase("hotelServer"))
			{
				try
				{
					Registry registry = LocateRegistry.getRegistry(RM[hotelRM], port[hotelRM]);
					// get the proxy and the remote reference by rmiregistry lookup
					// ResourceManager rm = (ResourceManager) registry.lookup(args[i]);
					// add a RM to the list
					// RMlist.remove(hotelRM);
					RMlist.set(hotelRM, (ResourceManager) registry.lookup("RM" + port[hotelRM]));
					System.out.println("Hotel RM connected");
					if (Logger.hotelTxLog.isEmpty() == false)
					{// redo all uncommited transaction
						ArrayList<Integer> tolist = RMlist.get(hotelRM).recovery(decisionlist);

						for (int tid : tolist)
						{
							Boolean decision = decisionlist.get(tid);
							if (decision == true)
							{
								// commitcommit
								RMlist.get(hotelRM).commit(tid);
							}
							else
							{
								// abort
								abort(tid);
								try
								{
									RMlist.get(hotelRM).uncommit(tid);

								}
								catch (Exception e)
								{
									// TODO Auto-generated catch block
									// e.printStackTrace();
									System.out.println("flight server remote exception".toUpperCase());
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					System.err.println("Client exception: " + e.toString());
					e.printStackTrace();
				}
			}
			else if (userName.equalsIgnoreCase("TM"))
			{
				try
				{
					TM = new transactionManager(this);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				TM.start();
			}
			else
			{
				System.out
						.println("Incorrect command, please choose one of the servers: flightServer, carServer, hotelServer");
			}

			System.out.println("Your command is:" + userName);
		}
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		// Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called");
		System.out.println("flight-comp512");
		synchronized (RMlist.get(flightRM))
		{
			try
			{
				String key = flight + flightNum;
				lockManager.Lock(id, key, DataObj.WRITE);

				int oldseat = RMlist.get(flightRM).queryFlight(id, flightNum);
				int oldprice = RMlist.get(flightRM).queryFlightPrice(id, flightNum);

				if (RMlist.get(flightRM).addFlight(id, flightNum, flightSeats, flightPrice))
				{
					addFlightOP addflightcmd = new addFlightOP(RMlist.get(flightRM), id, flightNum, flightSeats,
							flightPrice, oldseat, oldprice);
					lockManager.Lock(id, key, DataObj.WRITE);
					TM.addOPtoTransaction(id, addflightcmd);
					System.out.println("Flight added");
				}
				else
					System.out.println("Flight could not be added");
			}
			catch (Exception e)
			{
				System.out.println("EXCEPTION:");
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		return (true);
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int id, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called");
		synchronized (RMlist.get(hotelRM))
		{

			try
			{
				String key = hotel + location;
				lockManager.Lock(id, key, DataObj.WRITE);
				int oldcount = RMlist.get(hotelRM).queryRooms(id, location);
				int oldprice = RMlist.get(hotelRM).queryRoomsPrice(id, location);

				if (RMlist.get(hotelRM).addRooms(id, location, count, price))
				{

					addRoomOP addroomcmd = new addRoomOP(RMlist.get(hotelRM), id, location, count, price, oldcount,
							oldprice);

					TM.addOPtoTransaction(id, addroomcmd);
					System.out.println("Rooms added");
				}
				else
					System.out.println("Rooms could not be added");
			}
			catch (Exception e)
			{
				System.out.println("EXCEPTION:");
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		return (true);
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int id, String location, int count, int price) throws RemoteException
	{
		Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called");
		synchronized (RMlist.get(carRM))
		{
			try
			{
				String key = car + location;

				lockManager.Lock(id, key, DataObj.WRITE);
				int oldseat = RMlist.get(carRM).queryCars(id, location);
				int oldprice = RMlist.get(carRM).queryCarsPrice(id, location);
				if (RMlist.get(carRM).addCars(id, location, count, price))
				{
					addCarOP addcarcmd = new addCarOP(RMlist.get(carRM), id, location, count, price, oldseat, oldprice);
					TM.addOPtoTransaction(id, addcarcmd);
					System.out.println("Cars added");
				}
				else
					System.out.println("Cars could not be added");
			}
			catch (Exception e)
			{
				System.out.println("EXCEPTION:");
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		return (true);
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		synchronized (RMlist.get(flightRM))
		{
			lockManager.Lock(id, flight + flightNum, DataObj.READ);

			return RMlist.get(flightRM).queryFlight(id, flightNum);
		}
	}

	// Returns the number of reservations for this flight.
	// public int queryFlightReservations(int id, int flightNum)
	// throws RemoteException
	// {
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
	// RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
	// if( numReservations == null ) {
	// numReservations = new RMInteger(0);
	// } // if
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
	// return numReservations.getValue();
	// }

	// Returns price of this flight
	public int queryFlightPrice(int id, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		synchronized (RMlist.get(flightRM))
		{
			lockManager.Lock(id, flight + flightNum, DataObj.READ);

			return RMlist.get(flightRM).queryFlightPrice(id, flightNum);
		}
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		synchronized (RMlist.get(hotelRM))
		{
			lockManager.Lock(id, hotel + location, DataObj.READ);

			return RMlist.get(hotelRM).queryRooms(id, location);
		}
	}

	// Returns room price at this location
	public int queryRoomsPrice(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		synchronized (RMlist.get(hotelRM))
		{
			lockManager.Lock(id, hotel + location, DataObj.READ);

			return RMlist.get(hotelRM).queryRoomsPrice(id, location);
		}
	}

	// Returns the number of cars available at a location
	public int queryCars(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		synchronized (RMlist.get(carRM))
		{
			lockManager.Lock(id, car + location, DataObj.READ);

			return RMlist.get(carRM).queryCars(id, location);
		}
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		synchronized (RMlist.get(carRM))
		{
			lockManager.Lock(id, car + location, DataObj.READ);

			return RMlist.get(carRM).queryCarsPrice(id, location);
		}
	}

	// Returns data structure containing customer reservation info. Returns null if the
	// customer doesn't exist. Returns empty RMHashtable if customer exists but has no
	// reservations.

	// undo
	/*
	 * public RMHashtable getCustomerReservations(int id, int customerID) throws RemoteException {
	 * Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called"); Customer cust = (Customer)
	 * readData(id, Customer.getKey(customerID)); if (cust == null) { Trace.warn("RM::getCustomerReservations failed(" +
	 * id + ", " + customerID + ") failed--customer doesn't exist"); return null; } else { return
	 * cust.getReservations(); } // if }
	 */

	// return a bill
	public String queryCustomerInfo(int id, int customerID) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called");
		lockManager.Lock(id, customerkey + customerID, DataObj.READ);

		String a1 = RMlist.get(carRM).queryCustomerInfo(id, customerID);
		String a2 = RMlist.get(hotelRM).queryCustomerInfo(id, customerID);
		String a3 = RMlist.get(flightRM).queryCustomerInfo(id, customerID);
		return a1 + a2 + a3;

	}

	// customer functions
	// new customer just returns a unique customer identifier

	public int newCustomer(int id) throws RemoteException, DeadlockException, RedundantLockRequestException
	{

		Trace.info("INFO: RM::newCustomer(" + id + ") called");
		System.out.println("cpslab");
		int cid = RMlist.get(carRM).newCustomer(id);

		NewCustomerOP addcustomerCarcmd = new NewCustomerOP(RMlist.get(carRM), id, cid);
		lockManager.Lock(id, customerkey + cid, DataObj.WRITE);
		TM.addOPtoTransaction(id, addcustomerCarcmd);
		// Generate a globally unique ID for the new customer

		RMlist.get(flightRM).addNewCustomer(cid);

		addCustomerOP addcustomerFlightop = new addCustomerOP(RMlist.get(flightRM), id, cid);
		// lockManager.Lock(id, customerkey + cid, DataObj.WRITE);
		TM.addOPtoTransaction(id, addcustomerFlightop);

		RMlist.get(hotelRM).addNewCustomer(cid);

		addCustomerOP addcustomerHotelop = new addCustomerOP(RMlist.get(hotelRM), id, cid);
		// lockManager.Lock(id, customerkey + cid, DataObj.WRITE);
		TM.addOPtoTransaction(id, addcustomerFlightop);

		return cid;
	}

	// I opted to pass in customerID instead. This makes testing easier
	public boolean newCustomer(int id, int customerID) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called");
		lockManager.Lock(id, customerkey + customerID, DataObj.WRITE);

		boolean a1 = RMlist.get(carRM).newCustomer(id, customerID);
		addCustomerOP addcustomerCarcmd = new addCustomerOP(RMlist.get(carRM), id, customerID);
		// lockManager.Lock(id, customerkey + customerID, DataObj.WRITE);
		TM.addOPtoTransaction(id, addcustomerCarcmd);
		// Generate a globally unique ID for the new customer
		boolean a2 = RMlist.get(flightRM).newCustomer(id, customerID);

		addCustomerOP addcustomerFlightop = new addCustomerOP(RMlist.get(flightRM), id, customerID);
		// lockManager.Lock(id, customerkey + customerID, DataObj.WRITE);
		TM.addOPtoTransaction(id, addcustomerFlightop);

		boolean a3 = RMlist.get(hotelRM).newCustomer(id, customerID);

		addCustomerOP addcustomerHotelop = new addCustomerOP(RMlist.get(hotelRM), id, customerID);
		TM.addOPtoTransaction(id, addcustomerFlightop);
		return a1 && a2 && a3;

	}

	public int addNewCustomer(int cid) throws RemoteException
	{
		return 0;
	}

	public boolean deleteFlight(int id, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		int oldseat = RMlist.get(flightRM).queryFlight(id, flightNum);
		int oldprice = RMlist.get(flightRM).queryFlightPrice(id, flightNum);

		deleteFlightOP deleteflightcmd = new deleteFlightOP(RMlist.get(flightRM), id, flightNum, oldseat, oldprice);
		lockManager.Lock(id, flight + flightNum, DataObj.WRITE);
		TM.addOPtoTransaction(id, deleteflightcmd);
		return RMlist.get(flightRM).deleteFlight(id, flightNum);
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		int oldseat = RMlist.get(hotelRM).queryRooms(id, location);
		int oldprice = RMlist.get(hotelRM).queryRoomsPrice(id, location);
		deleteHotelOP deleteroomcmd = new deleteHotelOP(RMlist.get(hotelRM), id, location, oldseat, oldprice);
		lockManager.Lock(id, hotel + location, DataObj.WRITE);
		TM.addOPtoTransaction(id, deleteroomcmd);
		return RMlist.get(hotelRM).deleteRooms(id, location);

	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		int oldseat = RMlist.get(carRM).queryCars(id, location);
		int oldprice = RMlist.get(carRM).queryCarsPrice(id, location);
		deleteCarOP deletecarcmd = new deleteCarOP(RMlist.get(carRM), id, location, oldseat, oldprice);
		lockManager.Lock(id, hotel + location, DataObj.WRITE);
		TM.addOPtoTransaction(id, deletecarcmd);
		return RMlist.get(carRM).deleteCars(id, location);

	}

	// Deletes customer from the database.
	public boolean deleteCustomer(int id, int customerID) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		lockManager.Lock(id, customerkey + customerID, DataObj.WRITE);

		Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called");
		RMlist.get(hotelRM).deleteCustomer(id, customerID);

		DeleteCustomerOP deleteHotelcmd = new DeleteCustomerOP(RMlist.get(hotelRM), id, customerID);
		TM.addOPtoTransaction(id, deleteHotelcmd);

		RMlist.get(carRM).deleteCustomer(id, customerID);

		DeleteCustomerOP deleteCarcmd = new DeleteCustomerOP(RMlist.get(carRM), id, customerID);
		TM.addOPtoTransaction(id, deleteCarcmd);

		RMlist.get(flightRM).deleteCustomer(id, customerID);

		DeleteCustomerOP deleteFlightcmd = new DeleteCustomerOP(RMlist.get(flightRM), id, customerID);
		TM.addOPtoTransaction(id, deleteFlightcmd);
		return true;

	}

	// Frees flight reservation record. Flight reservation records help us make sure we
	// don't delete a flight if one or more customers are holding reservations
	// public boolean freeFlightReservation(int id, int flightNum)
	// throws RemoteException
	// {
	// Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" );
	// RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
	// if( numReservations != null ) {
	// numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) );
	// } // if
	// writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
	// Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") succeeded, this flight now has "
	// + numReservations + " reservations" );
	// return true;
	// }
	//
	// Adds car reservation to this customer.
	public boolean reserveCar(int id, int customerID, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{

		reserveCarOP reservecarop = new reserveCarOP(RMlist.get(carRM), id, customerID, location);
		lockManager.Lock(id, hotel + location, DataObj.WRITE);
		TM.addOPtoTransaction(id, reservecarop);

		return RMlist.get(carRM).reserveCar(id, customerID, location);
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

	public boolean unreserveCar(int id, int customerID, String location) throws RemoteException
	{
		/*
		 * reserveCarOP reservecarcmd = new reserveCarOP(); lockManager.Lock(id, car, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, reservecarcmd);

		return RMlist.get(carRM).unreserveCar(id, customerID, location);
	}

	// Adds room reservation to this customer.
	public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		reserveHotelOP reserveHotelop = new reserveHotelOP(RMlist.get(hotelRM), id, customerID, location);
		lockManager.Lock(id, hotel + location, DataObj.WRITE);
		TM.addOPtoTransaction(id, reserveHotelop);

		return RMlist.get(hotelRM).reserveRoom(id, customerID, location);
	}

	public boolean unreserveRoom(int id, int customerID, String location) throws RemoteException
	{
		/*
		 * reserveHotelOP reservehotelcmd = new reserveHotelOP(RM, id, customerID, location); lockManager.Lock(id,
		 * hotel, DataObj.WRITE);
		 */
		// TM.addOPtoTransaction(id, reservehotelcmd);
		// Adds flight reservation to this customer.

		return RMlist.get(hotelRM).unreserveRoom(id, customerID, location);
	}

	public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		reserveFlightOP reserveHotelop = new reserveFlightOP(RMlist.get(flightRM), id, customerID, flightNum);
		lockManager.Lock(id, flight + flightNum, DataObj.WRITE);
		TM.addOPtoTransaction(id, reserveHotelop);

		return RMlist.get(flightRM).reserveFlight(id, customerID, flightNum);
	}

	public boolean unreserveFlight(int id, int customerID, int flightNum) throws RemoteException
	{
		/*
		 * reserveFlightOP reserveflightcmd = new reserveFlightOP(RMlist.get(flightRM), flightNum, flightNum,
		 * flightNum); try { lockManager.Lock(id, flight, DataObj.WRITE); } catch (DeadlockException |
		 * RedundantLockRequestException e) { // TODO Auto-generated catch block e.printStackTrace(); } //
		 * TM.addOPtoTransaction(id, reserveflightcmd);
		 */
		return RMlist.get(flightRM).unreserveFlight(id, customerID, flightNum);
	}

	/* reserve an itinerary */
	public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room)
			throws RemoteException, NumberFormatException, DeadlockException, RedundantLockRequestException
	{
		Trace.info("RM::itinerary(" + id + ", " + customer + ") called");

		// Integer[] flightNumArr = (Integer[]) flightNumbers.toArray(new Integer[flightNumbers.size()]);
		// for (int flightNum : (Integer[]) flightNumbers.toArray())
		// for (int flightNum : flightNumArr)
		boolean flight = false, car = false, hotel = false;

		for (int i = 0; i < flightNumbers.size(); i++)
		{
			int num = Integer.parseInt(((String) flightNumbers.get(i)));
			reserveFlightOP reserveHotelop = new reserveFlightOP(RMlist.get(flightRM), id, customer, num);
			lockManager.Lock(id, "flight" + num, DataObj.WRITE);
			TM.addOPtoTransaction(id, reserveHotelop);

			flight = RMlist.get(flightRM)
					.reserveFlight(id, customer, Integer.parseInt(((String) flightNumbers.get(i))));
		}

		if (Car)
		{
			reserveCarOP reservecarop = new reserveCarOP(RMlist.get(carRM), id, customer, location);
			lockManager.Lock(id, "car" + location, DataObj.WRITE);
			TM.addOPtoTransaction(id, reservecarop);
			car = RMlist.get(carRM).reserveCar(id, customer, location);
		}
		if (Room)
		{
			reserveHotelOP reserveHotelop = new reserveHotelOP(RMlist.get(hotelRM), id, customer, location);
			lockManager.Lock(id, "hotel" + location, DataObj.WRITE);
			TM.addOPtoTransaction(id, reserveHotelop);

			hotel = RMlist.get(hotelRM).reserveRoom(id, customer, location);
		}
		if (Car)
		{
			if (Room)
			{
				return flight && car && hotel;
			}
			else
			{
				return flight && car;
			}
		}
		else
		{
			if (Room)
			{
				return flight && hotel;
			}
			else
			{
				return flight;
			}
		}
	}

	public int start() throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		// TODO Auto-generated method stub
		int id;
		id = TM.StartTx();
		try
		{
			RMlist.get(hotelRM).startTimer(id);
		}
		catch (IndexOutOfBoundsException e)
		{

		}

		try
		{
			RMlist.get(flightRM).startTimer(id);
		}
		catch (IndexOutOfBoundsException e)
		{

		}
		try
		{
			RMlist.get(carRM).startTimer(id);
		}
		catch (IndexOutOfBoundsException e)
		{

		}
		return id;
	}

	public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException,
			InvalidTransactionException
	{
		boolean success = TM.commit(transactionId);
		if (success)
			System.out.println("TM Commit successfully!");

		int counter = 0;
		for (int i = 0; i < 3; i++)
		{
			try
			{
				RMlist.get(i).commit(transactionId);
				// if succeed, remove it from table
				if (i == 0)
				{
					Logger.flightTxLog.remove(TM.getTx(transactionId));
				}
				else if (i == 1)
				{
					Logger.carTxLog.remove(TM.getTx(transactionId));
				}
				else if (i == 2)
				{
					Logger.hotelTxLog.remove(TM.getTx(transactionId));
				}
			}
			catch (Exception e)
			{
				// write to log
				counter++;
			}
		}

		return success;
	}

	public void abort(int transactionId) throws RemoteException, InvalidTransactionException
	{
		// abort transaction
		TM.abort(transactionId);
	}

	public boolean shutdown() throws RemoteException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		// only used in server
		// TODO Auto-generated method stub
	}

	@Override
	public void setRooms(int id, String location, int count, int price) throws RemoteException
	{
		// only used in server

		// TODO Auto-generated method stub

	}

	@Override
	public void setCars(int id, String location, int oldNum, int oldPrice) throws RemoteException, DeadlockException,
			RedundantLockRequestException
	{
		// only used in server
		// TODO Auto-generated method stub

	}

	// Nofication from Transaction Manager that transaction with id is aborted
	public boolean crash(String which) throws RemoteException
	{
		// crash servers
		int whichRM = 10;
		System.out.println("Crashing" + which + "server");

		if (which.equalsIgnoreCase("car"))
			whichRM = carRM;
		else if (which.equalsIgnoreCase("flight"))
			whichRM = flightRM;
		else if (which.equalsIgnoreCase("hotel"))
			whichRM = hotelRM;
		else if (which.equalsIgnoreCase("TM"))
			whichRM = 4;

		if (whichRM >= 0 && whichRM <= 3)
		{
			try
			{
				RMlist.get(whichRM).selfDestruct();
			}
			catch (RemoteException e)
			{
				System.out.println("Server has crashed");
				// RMlist.remove(whichRM);
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			return true;
		}
		else if (whichRM == 4)
		{
			TM.stop();
			TM.seflDestroy();
		}

		return false;
	}

	public void abortAll(int id)
	{
		try
		{
			RMlist.get(flightRM).uncommit(id);

		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("flight server remote exception".toUpperCase());
		}

		try
		{
			RMlist.get(carRM).uncommit(id);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("car server remote exception".toUpperCase());

		}
		try
		{
			RMlist.get(hotelRM).uncommit(id);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			System.out.println("hotel server remote exception".toUpperCase());
		}
		try
		{
			abort(id);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			System.out.println("abort  exception".toUpperCase());
		}
	}

	public void writeListDecision()
	{
		try
		{
			System.out.println("Creating File/Object output stream...");
			FileOutputStream fileOut = new FileOutputStream("DecisionList.dat");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			System.out.println("Writing Data Hashtable...");
			out.writeObject(decisionlist);
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

	public void writeListTxTable()
	{
		try
		{
			System.out.println("Creating File/Object output stream...");
			FileOutputStream fileOut = new FileOutputStream("TxTableList.dat");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			System.out.println("Writing Data Hashtable...");
			out.writeObject(TM.transactionTable);
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

	public boolean prepare(int transactionId) throws RemoteException
	{
		boolean resultFlight = false, resultCar = false, resultRoom = false;
		System.out.println("1st phase in 2PC - call for voting");

		try
		{
			resultFlight = RMlist.get(flightRM).vote2PC(transactionId);
		}
		catch (IndexOutOfBoundsException e)
		{
			System.out.println("Index out of bound, flight");

			resultFlight = false;
		}
		catch (RemoteException e)
		{
			System.out.println("Remote Exception, flight");

			resultFlight = false;
		}
		if (resultFlight)
		{
			Transaction flight = TM.getTx(transactionId);

			Logger.flightTxLog.add(flight);
		}
		try
		{
			resultCar = RMlist.get(carRM).vote2PC(transactionId);
		}
		catch (IndexOutOfBoundsException e)
		{
			System.out.println("Index out of bound, car");

			resultCar = false;
		}
		catch (RemoteException e)
		{
			System.out.println("Remote Exception, car");

			resultCar = false;
		}

		if (resultCar)
		{
			Transaction car = TM.getTx(transactionId);

			Logger.carTxLog.add(car);
		}
		try
		{
			resultRoom = RMlist.get(hotelRM).vote2PC(transactionId);
		}
		catch (IndexOutOfBoundsException e)
		{
			System.out.println("Index out of bound, hotel");
			resultRoom = false;

		}
		catch (RemoteException e)
		{
			System.out.println("Remote Exception, hotel");

			resultRoom = false;
		}

		if (resultRoom)
		{
			Transaction room = TM.getTx(transactionId);
			Logger.hotelTxLog.add(room);
		}

		if (iskill)
		{
			try
			{
				crash(item);
			}
			catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// check all informations
		if (resultFlight && resultCar && resultRoom)
		{
			if (beforesending)
			{
				crash("TM");
			}
			decisionlist.put(transactionId, true);
			try
			{
				commit(transactionId);
			}
			catch (RemoteException | TransactionAbortedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("All vote to commit, commit!");
			System.out.println("decision is to commit");

			return true;
		}
		else
		{
			System.out.println("decision is to abort all");

			System.out.println("some servers vote to abort, Abort all!");
			decisionlist.put(transactionId, false);
			abortAll(transactionId);
			return false;
		}

	}

	@Override
	public boolean selfDestruct() throws RemoteException
	{
		// only used in server
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean vote2PC(int id) throws RemoteException
	{
		// TODO Auto-generated method stub
		// only used in server
		return false;
	}

	@Override
	public boolean commit() throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startTimer(int id) throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	public void setDieAfterPrepare(String which) throws RemoteException
	{
		iskill = true;
		item = which;
	}

	public void setcase1(String which) throws RemoteException
	{
		if (which.equalsIgnoreCase("flight"))
		{
			RMlist.get(flightRM).setCase1();
		}
		else if (which.equalsIgnoreCase("car"))
		{
			RMlist.get(carRM).setCase1();
		}
		else if (which.equalsIgnoreCase("hotel"))
		{
			RMlist.get(flightRM).setCase1();
		}
	}

	public void setcase2(String which) throws RemoteException
	{
		if (which.equalsIgnoreCase("flight"))
		{
			RMlist.get(flightRM).setCase2();
		}
		else if (which.equalsIgnoreCase("car"))
		{
			RMlist.get(carRM).setCase2();
		}
		else if (which.equalsIgnoreCase("hotel"))
		{
			RMlist.get(flightRM).setCase2();
		}
	}

	public void setcase3(String which) throws RemoteException
	{// directly crash after receiving answer commit
		if (which.equalsIgnoreCase("flight"))
		{
			RMlist.get(flightRM).selfDestruct();
		}
		else if (which.equalsIgnoreCase("car"))
		{
			RMlist.get(carRM).selfDestruct();
		}
		else if (which.equalsIgnoreCase("hotel"))
		{
			RMlist.get(flightRM).selfDestruct();
		}
	}

	public void setcase4(String which) throws RemoteException
	{
		if (which.equalsIgnoreCase("flight"))
		{
			RMlist.get(flightRM).setCase4();
		}
		else if (which.equalsIgnoreCase("car"))
		{
			RMlist.get(carRM).setCase4();
		}
		else if (which.equalsIgnoreCase("hotel"))
		{
			RMlist.get(hotelRM).setCase4();
		}
	}

	public void setDiebeforeSending() throws RemoteException
	{
		beforesending = true;
	}

	public void setNotKill() throws RemoteException
	{
		iskill = false;
		item = null;
	}

	@Override
	public void uncommit(int id) throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<Integer> recovery(HashMap<Integer, Boolean> decisionlist) throws RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCase3() throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setCase2() throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setCase1() throws RemoteException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setCase4() throws RemoteException
	{
		// TODO Auto-generated method stub

	}

}
