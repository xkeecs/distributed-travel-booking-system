package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class addFlightOP extends operations
{
	int id;
	int flightNum;
	int flightSeats;
	int flightPrice;
	int oldSeat;
	int oldPrice;

	public addFlightOP(ResourceManager RM, int id, int flightNum, int flightSeats, int flightPrice, int oldSeat,
			int oldprice)
	{
		super(RM);
		this.id = id;
		this.flightNum = flightNum;
		this.flightSeats = flightSeats;
		this.flightPrice = flightPrice;
		this.oldPrice = oldprice;
		this.oldSeat = oldSeat;
	}

	public void undo()
	{
		// delete the flight
		if (oldSeat == 0 && oldPrice == 0)
		{
			try
			{
				System.out.println("deleting flight");
				RM.deleteFlight(id, flightNum);
			}
			catch (RemoteException | DeadlockException | RedundantLockRequestException e)
			{
				// TODO Auto-generated catch block
				System.out.println("Can not contact to server");
			}
		}
		else
		{
			try
			{
				System.out.println("reseting flight");
				RM.setFlight(id, flightNum, oldSeat, oldPrice);
			}
			catch (RemoteException | DeadlockException | RedundantLockRequestException e)
			{
				// TODO Auto-generated catch block
				System.out.println("Can not contact to server");
			}
		}

	}
}
