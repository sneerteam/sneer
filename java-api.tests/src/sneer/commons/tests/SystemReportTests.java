package sneer.commons.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import sneer.commons.Clock;
import sneer.commons.SystemReport;

public class SystemReportTests {

	@Test
	public void systemReport() {
		Clock.startMocking();
		SystemReport.updateReport("Shields On", true);
		SystemReport.updateReport("Phasers", "Stun");
		String report = SystemReport.report().toBlocking().first();
		Clock.stopMocking();
		assertEquals("Phasers: Stun\n\nShields On: true\n\n", report);
	}

}
