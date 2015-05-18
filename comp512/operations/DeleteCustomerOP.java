package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class DeleteCustomerOP extends operations
{
	int id;
	int cid;

	public DeleteCustomerOP(ResourceManager RM, int id, int cid)
	{
		super(RM);
		this.id = id;
		this.cid = cid;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void undo()
	{
		try
		{
			RM.addNewCustomer(cid);
		}
		catch (RemoteException | DeadlockException | RedundantLockRequestException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub

	}

}
