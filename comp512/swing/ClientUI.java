package swing;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import clientsrc.client;

public class ClientUI
{
	private JFrame frame = new JFrame("McGill Travel Booking System - Client Interface"); // create Frame

	// private JPanel UIpannel = new JPanel();

	private JButton startButton = new JButton("Start a transaction");
	private JButton abortButton = new JButton("Abort this transaction");
	private JButton quitButton = new JButton("Quit client");
	private JButton helpButton = new JButton("Show Command Menu");
	private JButton commitButton = new JButton("Commit this transaction");
	private JTextArea textArea;
	private PrintStream standardOut;

	static int TXID = 0;
	// private JTextField text = new JTextField("Console COnsole");
	// 1. create the pipes
	static PipedInputStream inPipe = new PipedInputStream();
	static PipedInputStream outPipe = new PipedInputStream();
	static PrintWriter inWriter;

	public static void main(String[] args) throws Exception
	{

		// 2. set the System.in and System.out streams
		System.setIn(inPipe);
		System.setOut(new PrintStream(new PipedOutputStream(outPipe), true));

		ClientUI gui = new ClientUI();
		gui.launchFrame();
		// client.main();

	}

	/*
	 * public static JTextArea console(final InputStream out, final PrintWriter in) { final JTextArea area = new
	 * JTextArea(800, 400); Font font = new Font("Courier", Font.BOLD, 16); // set font for JTextField
	 * area.setFont(font); // area.setBounds(50, 180, 500, 500); // handle "System.out" new SwingWorker<Void, String>()
	 * {
	 * 
	 * @Override protected Void doInBackground() throws Exception { Scanner s = new Scanner(out); while
	 * (s.hasNextLine()) publish(s.nextLine() + "\n"); return null; }
	 * 
	 * @Override protected void process(List<String> chunks) { for (String line : chunks) area.append(line); }
	 * }.execute();
	 * 
	 * // handle "System.in" area.addKeyListener(new KeyAdapter() { private StringBuffer line = new StringBuffer();
	 * 
	 * @Override public void keyTyped(KeyEvent e) { char c = e.getKeyChar(); if (c == KeyEvent.VK_ENTER) {
	 * in.println(line); line.setLength(0); } else if (c == KeyEvent.VK_BACK_SPACE) { line.setLength(line.length() - 1);
	 * } else if (!Character.isISOControl(c)) { line.append(e.getKeyChar()); } } }); return area; }
	 */

	public ClientUI() throws IOException
	{
		textArea = new JTextArea();

		textArea.setEditable(false);
		PrintStream printStream = new PrintStream(new CustomOutputStream(textArea));

		// keeps reference of standard output stream
		standardOut = System.out;

		// re-assigns standard output stream and error output stream
		System.setOut(printStream);
		System.setErr(printStream);

		inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		startButton.setMargin(new Insets(2, 2, 2, 2));
		abortButton.setMargin(new Insets(2, 2, 2, 2));
		quitButton.setMargin(new Insets(2, 2, 2, 2));
		helpButton.setMargin(new Insets(2, 2, 2, 2));

		// UIpannel.setPreferredSize(new Dimension(800, 500));
		frame.add(startButton);
		frame.add(abortButton);
		frame.add(quitButton);
		frame.add(helpButton);
		frame.add(commitButton);
		// JTextArea consoleoutput = console(outPipe, inWriter);
		// consoleoutput.setBounds(50, 180, 500, 500);
		JScrollPane pann = new JScrollPane(textArea);
		textArea.setBounds(100, 180, 400, 200);
		frame.add(pann);

		/*
		 * startButton.setBounds(10, 50, 200, 50); abortButton.setBounds(200, 50, 200, 50); commitButton.setBounds(400,
		 * 50, 200, 50); helpButton.setBounds(120, 100, 200, 50); quitButton.setBounds(330, 100, 200, 50);
		 */
		startButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pEvent)
			{
				TXID = client.start();
			}
		});
		abortButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pEvent)
			{
				client.abort(TXID);
			}
		});
		commitButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pEvent)
			{
				client.commit(TXID);
			}
		});
		helpButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pEvent)
			{
				client.listCommands();
			}
		});
		quitButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pEvent)
			{
				System.exit(1);
			}

		});
		// Container contentPane = frame.getContentPane();
		// UIpannel.setLayout(new BorderLayout());
	}

	public void launchFrame()
	{
		frame.setLayout(null);

		frame.setSize(1000, 800);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// frame.pack();
		frame.setVisible(true);
	}

	public class ListenCloseWdw extends WindowAdapter
	{
		public void windowClosing(WindowEvent e)
		{
			System.exit(0);
		}
	}
}
