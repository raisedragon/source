package com.raise.jdbc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/conf/spring/app-jdbc.xml"})
public class GetCode
{
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Test
	public void test1(){
		assertNotNull(jdbcTemplate);
	}
}
