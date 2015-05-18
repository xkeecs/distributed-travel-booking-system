package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class deleteHotelOP extends operations
{

	int id;
	int roomNum;
	int oldNum;
	int oldPrice;
	String location;

	public deleteHotelOP(ResourceManager RM, int id, String location, int oldcount, int oldprice)
	{
		super(RM);
		this.id = id;
		this.location = location;
		this.oldNum = oldcount;
		this.oldPrice = oldprice;
		// TODO Auto-generated constructor stub
	}

	public void undo()
	{
		try
		{
			RM.addRooms(id, location, oldNum, oldPrice);
		}
		catch (RemoteException | DeadlockException | RedundantLockRequestException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
