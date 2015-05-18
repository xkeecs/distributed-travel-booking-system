package ResImpl;

import java.util.Stack;

import operations.operations;
import LockManager.LockManager;

public class Transaction
{
	transactionRecord record = new transactionRecord();
	private int id;
	private static long time;
	LockManager lockManager;

	public Transaction(int transactionID, LockManager lock)
	{
		this.id = transactionID;
		this.lockManager = lock;
		updateTime();
	}

	public void addOP(operations op)
	{
		record.add(op);
	}

	public void undoOP()
	{
		Stack<operations> opRecord = record.getTransactionList();
		while (opRecord.empty() == false)
		{
			operations tempOP = opRecord.pop();
			tempOP.undo();
		}
	}

	public void updateTime()
	{
		time = System.currentTimeMillis();
	}

	public long getTime()
	{
		return time;
	}

	public int getID()
	{
		return id;
	}

	public void commit()
	{
		unlockAll();
		clearUNDO();
	}

	public void clearUNDO()
	{
		Stack<operations> opRecord = record.getTransactionList();
		while (opRecord.empty() == false)
		{
			operations tempOP = opRecord.pop();
		}
	}

	public void unlockAll()
	{
		lockManager.UnlockAll(this.id);
	}

	public void abort()
	{
		System.out.println("Transaction aborted");
		unlockAll();
		undoOP();
		unlockAll();

	}

}
