public class Solution
{
	public static class ListNode
	{

		int val;

		ListNode next;

		public ListNode(int value)
		{

			val = value;

			next = null;

		}

	}

	public static ListNode partition(ListNode head, int x)
	{

		ListNode headbkup = head;
		ListNode firsthead = new ListNode(-1);
		ListNode firstNode = firsthead;

		ListNode secondhead = new ListNode(-1);
		ListNode secondList = secondhead;

		if (head == null)
			return null;

		while (headbkup != null)
		{
			if (headbkup.val < x)
			{
				firstNode.next = headbkup;
				firstNode = firstNode.next;
			}
			else
			{
				secondList.next = headbkup;
				secondList = secondList.next;
			}

			headbkup = headbkup.next;
		}
		firstNode.next = secondhead.next;
		return firsthead.next;
	}

	public static void main(String args[])
	{
		ListNode l1 = new ListNode(2);
		ListNode l2 = new ListNode(1);
		l1.next = l2;
		System.out.println(partition(l1, 2).next.next.next.val);
	}

}
