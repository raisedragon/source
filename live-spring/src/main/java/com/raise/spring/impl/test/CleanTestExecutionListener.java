package com.raise.spring.impl.test;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Removes all deployments at the end of a complete test class.
 * 
 * Use this as follows in a Spring test:
 * 
 * @RunWith(SpringJUnit4ClassRunner.class)
 * @TestExecutionListeners(CleanTestExecutionListener.class)
 * @ContextConfiguration("...")
 * 
 * @author jbarrez
 */
public class CleanTestExecutionListener extends AbstractTestExecutionListener {
	
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
	}

}
