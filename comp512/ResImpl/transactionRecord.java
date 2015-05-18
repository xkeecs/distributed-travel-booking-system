package ResImpl;

import java.util.Stack;

import operations.operations;

public class transactionRecord
{
	Stack<operations> opStack = new Stack();

	public void add(operations op)
	{
		opStack.push(op);
	}

	public Stack<operations> getTransactionList()
	{
		return opStack;
	}
}
