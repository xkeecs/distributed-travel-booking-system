package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class addCustomerOP extends operations
{

	int cid;
	int id;

	public addCustomerOP(ResourceManager RM, int id, int cid)
	{
		super(RM);
		this.cid = cid;
		this.id = id;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void undo()
	{
		// TODO Auto-generated method stub
		try
		{
			RM.deleteCustomer(id, cid);
		}
		catch (RemoteException | DeadlockException | RedundantLockRequestException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
