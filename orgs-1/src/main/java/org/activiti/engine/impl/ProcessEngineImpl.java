/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine.impl;

import java.util.Map;

import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionContextFactory;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class ProcessEngineImpl implements ProcessEngine {

  private static Logger log = LoggerFactory.getLogger(ProcessEngineImpl.class);

  protected String name;
  protected IdentityService identityService;
  protected ManagementService managementService;
  protected CommandExecutor commandExecutor;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  protected TransactionContextFactory transactionContextFactory;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  public ProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.processEngineConfiguration = processEngineConfiguration;
    this.name = processEngineConfiguration.getProcessEngineName();
    this.identityService = processEngineConfiguration.getIdentityService();
    this.managementService = processEngineConfiguration.getManagementService();
    this.commandExecutor = processEngineConfiguration.getCommandExecutor();
    this.sessionFactories = processEngineConfiguration.getSessionFactories();
    this.transactionContextFactory = processEngineConfiguration.getTransactionContextFactory();
    
    commandExecutor.execute(processEngineConfiguration.getSchemaCommandConfig(), new SchemaOperationsProcessEngineBuild());

    if (name == null) {
      log.info("default activiti ProcessEngine created");
    } else {
      log.info("ProcessEngine {} created", name);
    }
    
    ProcessEngines.registerProcessEngine(this);

    
    if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
      processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineBuilt(this);
    }
    
    processEngineConfiguration.getEventDispatcher().dispatchEvent(
    		ActivitiEventBuilder.createGlobalEvent(ActivitiEventType.ENGINE_CREATED));
  }
  
  public void close() {
    ProcessEngines.unregister(this);

    commandExecutor.execute(processEngineConfiguration.getSchemaCommandConfig(), new SchemaOperationProcessEngineClose());
    
    if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
      processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineClosed(this);
    }
    
    processEngineConfiguration.getEventDispatcher().dispatchEvent(
    		ActivitiEventBuilder.createGlobalEvent(ActivitiEventType.ENGINE_CLOSED));
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public IdentityService getIdentityService() {
    return identityService;
  }

  public ManagementService getManagementService() {
    return managementService;
  }


  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }
}
