package sneer.commons.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import sneer.commons.SystemReport;

public class SystemReportTests {

	@Test
	public void systemReport() {
		SystemReport.updateReport("Shields On", true);
		SystemReport.updateReport("Phasers", "Stun");
		String report = SystemReport.report().toBlocking().first();
		assertEquals("Phasers: Stun\n\nShields On: true\n\n", report);
	}

}
