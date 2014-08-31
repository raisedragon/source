package com.raise.spring.annotations;

import org.springframework.core.io.Resource;

import com.raise.spring.SpringProcessEngineConfiguration;

import javax.sql.DataSource;

import java.util.List;

public interface ActivitiConfigurer {
	
	void processDefinitionResources(List<Resource> resourceList);

	void postProcessSpringProcessEngineConfiguration(SpringProcessEngineConfiguration springProcessEngineConfiguration);

	DataSource dataSource();
}