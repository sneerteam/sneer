package sneer.commons.tests;

import org.junit.Test;
import sneer.commons.Clock;
import sneer.commons.SystemReport;

import static org.junit.Assert.assertEquals;

public class SystemReportTests {

	@Test
	public void systemReport() {
		Clock.startMocking();
		SystemReport.updateReport("Shields On", true);
		SystemReport.updateReport("Phasers", "Stun");
		String report = SystemReport.report().toBlocking().first();
		Clock.stopMocking();
		assertEquals("Phasers: 5 decades ago Stun\n\nShields On: 5 decades ago true\n\n", report);
	}

}
