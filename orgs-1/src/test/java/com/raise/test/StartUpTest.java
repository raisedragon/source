package com.raise.test;

import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.test.TestHelper;
import org.junit.Test;

import static org.junit.Assert.*;

public class StartUpTest {
	@Test
	public void testStartUp(){
		String configurationResource = "activiti.cfg.xml";
		ProcessEngine processEngine = TestHelper.getProcessEngine(configurationResource);
		assertNotNull(processEngine);
		IdentityService identityService = processEngine.getIdentityService();
		assertNotNull(identityService);
	}
}
