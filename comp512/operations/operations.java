package operations;

import ResInterface.ResourceManager;

public abstract class operations
{
	ResourceManager RM;

	public operations(ResourceManager RM)
	{
		this.RM = RM;
	}

	public abstract void undo();
}
