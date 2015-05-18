package operations;

import java.rmi.RemoteException;

import ResInterface.ResourceManager;

public class reserveHotelOP extends operations
{
	int id;
	int customerID;
	String location;

	public reserveHotelOP(ResourceManager RM, int id, int customerID, String location)
	{
		super(RM);
		this.id = id;
		this.customerID = customerID;
		this.location = location;
		// TODO Auto-generated constructor stub
	}

	public void undo()
	{
		try
		{
			RM.unreserveRoom(id, customerID, location);
		}
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
