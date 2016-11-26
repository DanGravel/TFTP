import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IntermediateHostTest {
	IntermediateHost iH = new IntermediateHost();

	@Before
	public void setUp() throws Exception {
	}

	
	private void testOnePrompt(String s) {
		ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
		iH.sendAndReceive(in);
		
	}
	
	@Test
	public void testAll() {
		testPrompts(1);
		testPrompts(2);
		testPrompts(3);
		testPrompts(4);
		testPrompts(5);
		testPrompts(6);
		testPrompts(7);
		testPrompts(8);
		
		
	}
	
	
	private void testPrompts(int s) {
		testOnePrompt(s + " 1 1");
		testOnePrompt(s + " 2 1");
		testOnePrompt(s+ " 3 1");
		testOnePrompt(s+ " 4 1");
		testOnePrompt(s+ " 5 1");
		testOnePrompt(s+ " 6 1");
	}

}
