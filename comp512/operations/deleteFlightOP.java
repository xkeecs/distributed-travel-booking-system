package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class deleteFlightOP extends operations
{

	int id;
	int flightNum;
	int flightSeats;
	int flightPrice;
	int oldSeat;
	int oldPrice;

	public deleteFlightOP(ResourceManager RM, int id, int flightNum, int oldSeat, int oldPrice)
	{
		super(RM);
		this.id = id;
		this.flightNum = flightNum;

		this.oldPrice = oldPrice;
		this.oldSeat = oldSeat;
		// TODO Auto-generated constructor stub
	}

	public void undo()
	{
		try
		{
			RM.addFlight(id, flightNum, oldSeat, oldPrice);
		}
		catch (RemoteException | DeadlockException | RedundantLockRequestException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
