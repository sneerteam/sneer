package sneer.commons.tests;

import static org.junit.Assert.*;

import org.junit.*;

import sneer.commons.*;

public class SystemReportTests {

	@Test
	public void systemReport() {
		SystemReport.updateReport("Shields On", true);
		SystemReport.updateReport("Phasers", "Stun");
		String report = SystemReport.report().toBlockingObservable().first();
		assertEquals("Phasers: Stun\n\nShields On: true\n\n", report);
	}

}
