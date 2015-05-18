package operations;

import java.util.Vector;

import ResInterface.ResourceManager;

public class itineraryOP extends operations
{
	int id;
	int flightNum;
	int flightSeats;
	int flightPrice;
	int oldSeat;
	int oldPrice;

	public itineraryOP(ResourceManager RM, int id, int customer, Vector flightNumbers, String location, boolean Car,
			boolean Room)
	{
		super(RM);
		// TODO Auto-generated constructor stub
	}

	public void undo()
	{
		// Use undo from add * system
	}
}
