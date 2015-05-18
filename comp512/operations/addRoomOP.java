package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class addRoomOP extends operations
{
	int id;
	int roomNum;
	int oldNum;
	int oldPrice;
	String location;

	public addRoomOP(ResourceManager RM, int id, String location, int count, int price, int oldcount, int oldprice)
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
				RM.deleteRooms(id, location);
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
				RM.setRooms(id, location, oldNum, oldPrice);
			}
			catch (RemoteException | DeadlockException | RedundantLockRequestException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
