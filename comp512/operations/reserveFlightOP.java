package operations;

import java.rmi.RemoteException;

import ResInterface.ResourceManager;

public class reserveFlightOP extends operations
{
	int id;
	int flightNum;
	int flightSeats;
	int flightPrice;
	int oldSeat;
	int oldPrice;
	int customerID;

	public reserveFlightOP(ResourceManager RM, int id, int customerID, int flightNum)
	{
		super(RM);

		this.id = id;
		this.flightNum = flightNum;
		this.customerID = customerID;
		// TODO Auto-generated constructor stub
	}

	public void undo()
	{
		try
		{
			RM.unreserveFlight(id, customerID, flightNum);
		}
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
