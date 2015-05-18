package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class addCarOP extends operations
{

	int id;
	int roomNum;
	int oldNum;
	int oldPrice;
	String location;

	public addCarOP(ResourceManager RM, int id, String location, int count, int price, int oldcount, int oldprice)
	{
		super(RM);
		this.roomNum = count;
		this.location = location;
		this.oldNum = oldcount;
		this.oldPrice = oldprice;
		// TODO Auto-generated constructor stub
	}

	public void undo()
	{
		if (oldNum == 0 && oldPrice == 0)
		{
			try
			{
				RM.deleteCars(id, location);
			}
			catch (RemoteException | DeadlockException | RedundantLockRequestException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				RM.setCars(id, location, oldNum, oldPrice);
			}
			catch (RemoteException | DeadlockException | RedundantLockRequestException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
