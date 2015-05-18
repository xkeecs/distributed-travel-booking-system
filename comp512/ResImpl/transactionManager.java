package ResImpl;

import java.rmi.RemoteException;
import java.util.Hashtable;

import javax.transaction.InvalidTransactionException;

import operations.operations;

/* Transaction Manager that controls activities by all transactions. It resides with the Middleware.
 * @author: kai xiong
 * McGIll University
 * */

public class transactionManager extends Thread
{
	public boolean keepAlive = true;
	private int Txid = 0;
	public final int timeout = 100000;
	public static Hashtable<Integer, Transaction> transactionTable = new Hashtable<Integer, Transaction>();

	public Middleware middleware;
	protected String configFile = "";

	public transactionManager(Middleware MW) throws Exception
	{
		middleware = MW;
		// channel = new JChannel(configFile);
	}

	public Transaction getTx(int id)
	{
		return transactionTable.get(id);
	}

	public void run()
	{
		final int checkfrequency = 1000;
		while (keepAlive)
		{
			// check if the transaction is still there
			synchronized (this)
			{
				for (Transaction temp : transactionTable.values())
				{
					long ActimeStamp = temp.getTime();
					if (System.currentTimeMillis() - ActimeStamp > timeout)
					{
						// transaction timeout. abort it.
						try
						{
							System.out.println("Transaction " + temp.getID() + "Timeout");

							abort(temp.getID());
						}
						catch (InvalidTransactionException e)
						{
							e.printStackTrace();
						}
					}
				}
				try
				{
					Thread.sleep(checkfrequency);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void addOPtoTransaction(int id, operations op)
	{
		Transaction temp = transactionTable.get(id);
		if (temp != null)
		{
			temp.addOP(op);
		}
		else
		{
			System.out.println("Transaction does not exist");
		}
	}

	public synchronized int StartTx()
	{
		Transaction tx = new Transaction(Txid, middleware.lockManager);
		transactionTable.put(Txid, tx);
		return Txid++;
	}

	public boolean commit(int id) throws InvalidTransactionException
	{

		Transaction currentTx = transactionTable.get(id);
		if (currentTx == null)
		{
			throw new InvalidTransactionException();
		}
		else
		{
			currentTx.commit();
			// remove committed transaction
			// transactionTable.remove(id);
			return true;
		}
	}

	public void abort(int id) throws InvalidTransactionException
	{

		System.out.println("Transaction " + id + " aborted and removed from Transaction manager");
		Transaction abortTx = transactionTable.get(id);

		if (abortTx != null)
		{
			// abort transaction
			abortTx.abort();
			transactionTable.remove(id);
			// notify middleware and cancel this transaction
			// middleware.notifyabort(id);
		}
		else
		{
			System.out.println("Transaction is not valid");
			throw new InvalidTransactionException(null);
		}
	}

	// Perform the 1st phase of 2PL
	public boolean prepare(int transactionId) throws RemoteException, TransactionAbortedException,
			InvalidTransactionException
	{
		return true;
	}

	public void seflDestroy()
	{
		System.exit(1);
	}

}
