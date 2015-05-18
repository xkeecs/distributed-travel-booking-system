package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class deleteCarOP extends operations
{
	int id;
	int roomNum;
	int oldNum;
	int oldPrice;
	String location;

	public deleteCarOP(ResourceManager RM, int id, String location, int oldcount, int oldprice)
	{
		super(RM);
		this.location = location;
		this.oldNum = oldcount;
		this.oldPrice = oldprice;
		// TODO Auto-generated constructor stub
	}

	public void undo()
	{
		try
		{
			RM.addCars(id, location, oldNum, oldPrice);
		}
		catch (RemoteException | DeadlockException | RedundantLockRequestException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
