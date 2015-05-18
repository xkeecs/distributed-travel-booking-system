package operations;

import java.rmi.RemoteException;

import LockManager.DeadlockException;
import LockManager.RedundantLockRequestException;
import ResInterface.ResourceManager;

public class NewCustomerOP extends operations
{
	int id;
	int customerID;

	public NewCustomerOP(ResourceManager RM, int id, int customerID)
	{
		super(RM);
		this.id = id;
		this.customerID = customerID;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void undo()
	{
		// delete customer
		try
		{
			RM.deleteCustomer(id, customerID);
		}
		catch (RemoteException | DeadlockException | RedundantLockRequestException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
