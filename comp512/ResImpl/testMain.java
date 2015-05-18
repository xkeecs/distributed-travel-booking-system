package ResImpl;

public class testMain
{
	public static void main(String[] args)
	{

		/*
		 * String dir = args[0]; // "/home/kai/Kai/servercode/config/mw_config.xml"; XmlParser Parser = new
		 * XmlParser(dir);
		 */
		/*
		 * String flight_server = Parser.getTuple2("flight").x; int flight_port = Parser.getTuple2("flight").y;
		 * 
		 * String car_server = Parser.getTuple2("car").x; int car_port = Parser.getTuple2("car").y;
		 * 
		 * String room_server = Parser.getTuple2("room").x; int room_port = Parser.getTuple2("room").y;
		 * 
		 * String rmi_server = Parser.getTuple2("customer").x; int rmi_port = Parser.getTuple2("customer").y;
		 */
		/*
		 * String flight_server = "132.206.52.101"; String car_server = "132.206.52.101"; String room_server =
		 * "132.206.52.101";
		 */

		String room_server = "127.0.0.1";
		String flight_server = "127.0.0.1";
		String car_server = "127.0.0.1";

		int flight_port = 5001;
		int car_port = 5002;
		int room_port = 5003;
		int rmi_port = 5004;

		Middleware middlebox = new Middleware(flight_server, flight_port, car_server, car_port, room_server, room_port);

		middlebox.connection(flight_server, flight_port, car_server, car_port, room_server, room_port, rmi_port);
	}
}
