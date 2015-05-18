package ResInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.transaction.InvalidTransactionException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResImpl.TransactionAbortedException;

/**
 * Simplified version from CSE 593 Univ. of Washington
 * 
 * Distributed System in Java.
 * 
 * failure reporting is done using two pieces, exceptions and boolean return values. Exceptions are used for systemy
 * things. Return values are used for operations that would affect the consistency
 * 
 * If there is a boolean return value and you're not sure how it would be used in your implementation, ignore it. I used
 * boolean return values in the interface generously to allow flexibility in implementation. But don't forget to return
 * true when the operation has succeeded.
 */

public interface ResourceManager extends Remote
{
	/*
	 * Add seats to a flight. In general this will be used to create a new flight, but it should be possible to add
	 * seats to an existing flight. Adding to an existing flight should overwrite the current price of the available
	 * seats.
	 * 
	 * @return success.
	 */
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException,
			DeadlockException, RedundantLockRequestException;

	/*
	 * Add cars to a location. This should look a lot like addFlight, only keyed on a string location instead of a
	 * flight number.
	 */
	public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/*
	 * Add rooms to a location. This should look a lot like addFlight, only keyed on a string location instead of a
	 * flight number.
	 */
	public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException,
			DeadlockException, RedundantLockRequestException;

	/* new customer just returns a unique customer identifier */
	public int newCustomer(int id) throws RemoteException, DeadlockException, RedundantLockRequestException;

	/* new customer with providing id */
	public boolean newCustomer(int id, int cid) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	public int addNewCustomer(int cid) throws RemoteException, DeadlockException, RedundantLockRequestException;

	/**
	 * Delete the entire flight. deleteflight implies whole deletion of the flight. all seats, all reservations. If
	 * there is a reservation on the flight, then the flight cannot be deleted
	 * 
	 * @return success.
	 */
	public boolean deleteFlight(int id, int flightNum) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/*
	 * Delete all Cars from a location. It may not succeed if there are reservations for this location
	 * 
	 * @return success
	 */
	public boolean deleteCars(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/*
	 * Delete all Rooms from a location. It may not succeed if there are reservations for this location.
	 * 
	 * @return success
	 */
	public boolean deleteRooms(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* deleteCustomer removes the customer and associated reservations */
	public boolean deleteCustomer(int id, int customer) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* queryFlight returns the number of empty seats. */
	public int queryFlight(int id, int flightNumber) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* return the number of cars available at a location */
	public int queryCars(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* return the number of rooms available at a location */
	public int queryRooms(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* return a bill */
	public String queryCustomerInfo(int id, int customer) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* queryFlightPrice returns the price of a seat on this flight. */
	public int queryFlightPrice(int id, int flightNumber) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* return the price of a car at a location */
	public int queryCarsPrice(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* return the price of a room at a location */
	public int queryRoomsPrice(int id, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* Reserve a seat on this flight */
	public boolean reserveFlight(int id, int customer, int flightNumber) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* reserve a car at this location */
	public boolean reserveCar(int id, int customer, String location) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* reserve a room certain at this location */
	public boolean reserveRoom(int id, int customer, String locationd) throws RemoteException, DeadlockException,
			RedundantLockRequestException;

	/* reserve an itinerary */
	public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room)
			throws RemoteException, DeadlockException, RedundantLockRequestException;

	/* Start the transaction */
	public int start() throws RemoteException, TransactionAbortedException;

	/* commit the change */
	public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException,
			InvalidTransactionException;

	public boolean commit() throws RemoteException, TransactionAbortedException, InvalidTransactionException;

	/* abort transaction */
	public void abort(int transactionId) throws RemoteException, InvalidTransactionException;

	/* Shutdown the RM */

	public boolean shutdown() throws RemoteException;

	public void setFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException,
			DeadlockException, RedundantLockRequestException;

	public void setRooms(int id, String location, int count, int price) throws RemoteException, DeadlockException,
			RedundantLockRequestException;;

	public void setCars(int id, String location, int oldNum, int oldPrice) throws RemoteException, DeadlockException,
			RedundantLockRequestException;;

	public boolean unreserveFlight(int id, int customerID, int flightNum) throws RemoteException;

	public boolean unreserveCar(int id, int customerID, String location) throws RemoteException;

	public boolean unreserveRoom(int id, int customerID, String location) throws RemoteException;

	public boolean selfDestruct() throws RemoteException;

	public boolean prepare(int transactionId) throws RemoteException;

	public boolean vote2PC(int transactionID) throws RemoteException;

	public void uncommit(int id) throws RemoteException;

	public ArrayList<Integer> recovery(HashMap<Integer, Boolean> decisionlist) throws RemoteException;

	public void startTimer(int id) throws RemoteException;

	public boolean crash(String which) throws RemoteException;

	public void setDieAfterPrepare(String which) throws RemoteException;

	public void setNotKill() throws RemoteException;

	public void setCase3() throws RemoteException;

	public void setCase2() throws RemoteException;

	public void setCase1() throws RemoteException;

	public void setCase4() throws RemoteException;

	public void setcase1(String which) throws RemoteException;

	public void setcase2(String which) throws RemoteException;

	public void setcase3(String which) throws RemoteException;

	public void setcase4(String which) throws RemoteException;

}