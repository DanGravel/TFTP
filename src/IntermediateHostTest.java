import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IntermediateHostTest {
	IntermediateHost iH;;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testPrompts() {
		iH = new IntermediateHost(1);
		ByteArrayInputStream in = new ByteArrayInputStream("1 1 33".getBytes());
		iH.sendAndReceive(in);
		
	}
	
	public void testPrompts2() {
		iH = new IntermediateHost(2);
		ByteArrayInputStream in = new ByteArrayInputStream("1 3 33".getBytes());
		iH.sendAndReceive(in);
	}

}
