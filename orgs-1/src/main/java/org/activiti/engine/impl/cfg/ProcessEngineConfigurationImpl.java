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

package org.activiti.engine.impl.cfg;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.cfg.ProcessEngineConfigurator;
import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventDispatcherImpl;
import org.activiti.engine.impl.IdentityServiceImpl;
import org.activiti.engine.impl.ManagementServiceImpl;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.ServiceImpl;
import org.activiti.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.activiti.engine.impl.db.DbIdGenerator;
import org.activiti.engine.impl.db.DbSqlSessionFactory;
import org.activiti.engine.impl.db.IbatisVariableTypeHandler;
import org.activiti.engine.impl.delegate.DefaultDelegateInterceptor;
import org.activiti.engine.impl.event.logger.EventLogger;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.impl.interceptor.CommandContextFactory;
import org.activiti.engine.impl.interceptor.CommandContextInterceptor;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.activiti.engine.impl.interceptor.CommandInvoker;
import org.activiti.engine.impl.interceptor.DelegateInterceptor;
import org.activiti.engine.impl.interceptor.LogInterceptor;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.GenericManagerFactory;
import org.activiti.engine.impl.persistence.GroupEntityManagerFactory;
import org.activiti.engine.impl.persistence.MembershipEntityManagerFactory;
import org.activiti.engine.impl.persistence.UserEntityManagerFactory;
import org.activiti.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.activiti.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.engine.impl.persistence.entity.TableDataManager;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntityManager;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.impl.util.ReflectUtil;
import org.activiti.engine.impl.variable.BooleanType;
import org.activiti.engine.impl.variable.ByteArrayType;
import org.activiti.engine.impl.variable.CustomObjectType;
import org.activiti.engine.impl.variable.DateType;
import org.activiti.engine.impl.variable.DefaultVariableTypes;
import org.activiti.engine.impl.variable.DoubleType;
import org.activiti.engine.impl.variable.EntityManagerSession;
import org.activiti.engine.impl.variable.EntityManagerSessionFactory;
import org.activiti.engine.impl.variable.IntegerType;
import org.activiti.engine.impl.variable.JPAEntityVariableType;
import org.activiti.engine.impl.variable.LongStringType;
import org.activiti.engine.impl.variable.LongType;
import org.activiti.engine.impl.variable.NullType;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.engine.impl.variable.ShortType;
import org.activiti.engine.impl.variable.StringType;
import org.activiti.engine.impl.variable.UUIDType;
import org.activiti.engine.impl.variable.VariableType;
import org.activiti.engine.impl.variable.VariableTypes;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ProcessValidatorFactory;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class ProcessEngineConfigurationImpl extends ProcessEngineConfiguration {  

  private static Logger log = LoggerFactory.getLogger(ProcessEngineConfigurationImpl.class);
  
  public static final String DB_SCHEMA_UPDATE_CREATE = "create";
  public static final String DB_SCHEMA_UPDATE_DROP_CREATE = "drop-create";

  public static final String DEFAULT_WS_SYNC_FACTORY = "org.activiti.engine.impl.webservice.CxfWebServiceClientFactory";
  
  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/activiti/db/mapping/mappings.xml";

  // SERVICES /////////////////////////////////////////////////////////////////

  protected IdentityService identityService = new IdentityServiceImpl();
  protected ManagementService managementService = new ManagementServiceImpl();
  
  // COMMAND EXECUTORS ////////////////////////////////////////////////////////
  
  protected CommandConfig defaultCommandConfig;
  protected CommandConfig schemaCommandConfig;
  
  protected CommandInterceptor commandInvoker;
  
  /** the configurable list which will be {@link #initInterceptorChain(java.util.List) processed} to build the {@link #commandExecutor} */
  protected List<CommandInterceptor> customPreCommandInterceptors;
  protected List<CommandInterceptor> customPostCommandInterceptors;
  
  protected List<CommandInterceptor> commandInterceptors;

  /** this will be initialized during the configurationComplete() */
  protected CommandExecutor commandExecutor;
  
  // SESSION FACTORIES ////////////////////////////////////////////////////////

  protected List<SessionFactory> customSessionFactories;
  protected DbSqlSessionFactory dbSqlSessionFactory;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  
  // Configurators ////////////////////////////////////////////////////////////
  
  protected boolean enableConfiguratorServiceLoader = true; // Enabled by default. In certain environments this should be set to false (eg osgi)
  protected List<ProcessEngineConfigurator> configurators; // The injected configurators
  protected List<ProcessEngineConfigurator> allConfigurators; // Including auto-discovered configurators
  
  protected int processDefinitionCacheLimit = -1; // By default, no limit
  
  protected int knowledgeBaseCacheLimit = -1;

  

  // MYBATIS SQL SESSION FACTORY //////////////////////////////////////////////
  
  protected SqlSessionFactory sqlSessionFactory;
  protected TransactionFactory transactionFactory;
  
  protected Set<Class<?>> customMybatisMappers;

  // ID GENERATOR /////////////////////////////////////////////////////////////
  
  protected IdGenerator idGenerator;
  protected DataSource idGeneratorDataSource;
  protected String idGeneratorDataSourceJndiName;
  
  
  // PROCESS VALIDATION 
  
  protected ProcessValidator processValidator;

  // OTHER ////////////////////////////////////////////////////////////////////


  protected List<VariableType> customPreVariableTypes;
  protected List<VariableType> customPostVariableTypes;
  protected VariableTypes variableTypes;
  

  protected String wsSyncFactoryClassName = DEFAULT_WS_SYNC_FACTORY;

  protected CommandContextFactory commandContextFactory;
  protected TransactionContextFactory transactionContextFactory;
  
  protected Map<Object, Object> beans;
  
  protected DelegateInterceptor delegateInterceptor;

  

  
  /**
   * Set this to true if you want to have extra checks on the BPMN xml that is parsed.
   * See http://www.jorambarrez.be/blog/2013/02/19/uploading-a-funny-xml-can-bring-down-your-server/
   * 
   * Unfortunately, this feature is not available on some platforms (JDK 6, JBoss),
   * hence the reason why it is disabled by default. If your platform allows 
   * the use of StaxSource during XML parsing, do enable it.
   */
  protected boolean enableSafeBpmnXml = false;
  
  /**
   * The following settings will determine the amount of entities loaded at once when the engine 
   * needs to load multiple entities (eg. when suspending a process definition with all its process instances).
   * 
   * The default setting is quite low, as not to surprise anyone with sudden memory spikes.
   * Change it to something higher if the environment Activiti runs in allows it.
   */
  protected int batchSizeProcessInstances = 25;
  protected int batchSizeTasks = 25;
  
  protected boolean enableEventDispatcher = true;
  protected ActivitiEventDispatcher eventDispatcher;
  protected List<ActivitiEventListener> eventListeners;
  protected Map<String, List<ActivitiEventListener>> typedEventListeners;
  
  // Event logging to database
  protected boolean enableDatabaseEventLogging = false;
  
  
  // buildProcessEngine ///////////////////////////////////////////////////////
  
  public ProcessEngine buildProcessEngine() {
    init();
    return new ProcessEngineImpl(this);
  }
  
  // init /////////////////////////////////////////////////////////////////////
  
  protected void init() {
  	initConfigurators();
  	configuratorsBeforeInit();
    initProcessDiagramGenerator();
    initVariableTypes();
    initBeans();
    initCommandContextFactory();
    initTransactionContextFactory();
    initCommandExecutors();
    initServices();
    initIdGenerator();
    initDataSource();
    initTransactionFactory();
    initSqlSessionFactory();
    initSessionFactories();
    initJpa();
    initDelegateInterceptor();
    initEventDispatcher();
    initProcessValidator();
    initDatabaseEventLogging();
    configuratorsAfterInit();
  }


  // command executors ////////////////////////////////////////////////////////
  
  protected void initCommandExecutors() {
    initDefaultCommandConfig();
    initSchemaCommandConfig();
    initCommandInvoker();
    initCommandInterceptors();
    initCommandExecutor();
  }

  protected void initDefaultCommandConfig() {
    if (defaultCommandConfig==null) {
      defaultCommandConfig = new CommandConfig();
    }
  }

  private void initSchemaCommandConfig() {
    if (schemaCommandConfig==null) {
      schemaCommandConfig = new CommandConfig().transactionNotSupported();
    }
  }

  protected void initCommandInvoker() {
    if (commandInvoker==null) {
      commandInvoker = new CommandInvoker();
    }
  }
  
  protected void initCommandInterceptors() {
    if (commandInterceptors==null) {
      commandInterceptors = new ArrayList<CommandInterceptor>();
      if (customPreCommandInterceptors!=null) {
        commandInterceptors.addAll(customPreCommandInterceptors);
      }
      commandInterceptors.addAll(getDefaultCommandInterceptors());
      if (customPostCommandInterceptors!=null) {
        commandInterceptors.addAll(customPostCommandInterceptors);
      }
      commandInterceptors.add(commandInvoker);
    }
  }

  protected Collection< ? extends CommandInterceptor> getDefaultCommandInterceptors() {
    List<CommandInterceptor> interceptors = new ArrayList<CommandInterceptor>();
    interceptors.add(new LogInterceptor());
    
    CommandInterceptor transactionInterceptor = createTransactionInterceptor();
    if (transactionInterceptor != null) {
      interceptors.add(transactionInterceptor);
    }
    
    interceptors.add(new CommandContextInterceptor(commandContextFactory, this));
    return interceptors;
  }

  protected void initCommandExecutor() {
    if (commandExecutor==null) {
      CommandInterceptor first = initInterceptorChain(commandInterceptors);
      commandExecutor = new CommandExecutorImpl(getDefaultCommandConfig(), first);
    }
  }

  protected CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
    if (chain==null || chain.isEmpty()) {
      throw new ActivitiException("invalid command interceptor chain configuration: "+chain);
    }
    for (int i = 0; i < chain.size()-1; i++) {
      chain.get(i).setNext( chain.get(i+1) );
    }
    return chain.get(0);
  }
  
  protected abstract CommandInterceptor createTransactionInterceptor();
  
  // services /////////////////////////////////////////////////////////////////
  
  protected void initServices() {
    initService(identityService);
    initService(managementService);
  }

  protected void initService(Object service) {
    if (service instanceof ServiceImpl) {
      ((ServiceImpl)service).setCommandExecutor(commandExecutor);
    }
  }
  
  // DataSource ///////////////////////////////////////////////////////////////
  
  protected void initDataSource() {
    if (dataSource==null) {
      if (dataSourceJndiName!=null) {
        try {
          dataSource = (DataSource) new InitialContext().lookup(dataSourceJndiName);
        } catch (Exception e) {
          throw new ActivitiException("couldn't lookup datasource from "+dataSourceJndiName+": "+e.getMessage(), e);
        }
        
      } else if (jdbcUrl!=null) {
        if ( (jdbcDriver==null) || (jdbcUrl==null) || (jdbcUsername==null) ) {
          throw new ActivitiException("DataSource or JDBC properties have to be specified in a process engine configuration");
        }
        
        log.debug("initializing datasource to db: {}", jdbcUrl);
        
        PooledDataSource pooledDataSource = 
          new PooledDataSource(ReflectUtil.getClassLoader(), jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword );
        
        if (jdbcMaxActiveConnections > 0) {
          pooledDataSource.setPoolMaximumActiveConnections(jdbcMaxActiveConnections);
        }
        if (jdbcMaxIdleConnections > 0) {
          pooledDataSource.setPoolMaximumIdleConnections(jdbcMaxIdleConnections);
        }
        if (jdbcMaxCheckoutTime > 0) {
          pooledDataSource.setPoolMaximumCheckoutTime(jdbcMaxCheckoutTime);
        }
        if (jdbcMaxWaitTime > 0) {
          pooledDataSource.setPoolTimeToWait(jdbcMaxWaitTime);
        }
        if (jdbcPingEnabled == true) {
          pooledDataSource.setPoolPingEnabled(true);
          if (jdbcPingQuery != null) {
            pooledDataSource.setPoolPingQuery(jdbcPingQuery);
          }
          pooledDataSource.setPoolPingConnectionsNotUsedFor(jdbcPingConnectionNotUsedFor);
        }
        if (jdbcDefaultTransactionIsolationLevel > 0) {
          pooledDataSource.setDefaultTransactionIsolationLevel(jdbcDefaultTransactionIsolationLevel);
        }
        dataSource = pooledDataSource;
      }
      
      if (dataSource instanceof PooledDataSource) {
        // ACT-233: connection pool of Ibatis is not properely initialized if this is not called!
        ((PooledDataSource)dataSource).forceCloseAll();
      }
    }

    if (databaseType == null) {
      initDatabaseType();
    }
  }
  
  protected static Properties databaseTypeMappings = getDefaultDatabaseTypeMappings();

  protected static Properties getDefaultDatabaseTypeMappings() {
    Properties databaseTypeMappings = new Properties();
    databaseTypeMappings.setProperty("H2","h2");
    databaseTypeMappings.setProperty("MySQL","mysql");
    databaseTypeMappings.setProperty("Oracle","oracle");
    databaseTypeMappings.setProperty("PostgreSQL","postgres");
    databaseTypeMappings.setProperty("Microsoft SQL Server","mssql");
    databaseTypeMappings.setProperty("DB2","db2");
    databaseTypeMappings.setProperty("DB2","db2");
    databaseTypeMappings.setProperty("DB2/NT","db2");
    databaseTypeMappings.setProperty("DB2/NT64","db2");
    databaseTypeMappings.setProperty("DB2 UDP","db2");
    databaseTypeMappings.setProperty("DB2/LINUX","db2");
    databaseTypeMappings.setProperty("DB2/LINUX390","db2");
    databaseTypeMappings.setProperty("DB2/LINUXX8664","db2");
    databaseTypeMappings.setProperty("DB2/LINUXZ64","db2");
    databaseTypeMappings.setProperty("DB2/400 SQL","db2");
    databaseTypeMappings.setProperty("DB2/6000","db2");
    databaseTypeMappings.setProperty("DB2 UDB iSeries","db2");
    databaseTypeMappings.setProperty("DB2/AIX64","db2");
    databaseTypeMappings.setProperty("DB2/HPUX","db2");
    databaseTypeMappings.setProperty("DB2/HP64","db2");
    databaseTypeMappings.setProperty("DB2/SUN","db2");
    databaseTypeMappings.setProperty("DB2/SUN64","db2");
    databaseTypeMappings.setProperty("DB2/PTX","db2");
    databaseTypeMappings.setProperty("DB2/2","db2");
    databaseTypeMappings.setProperty("DB2 UDB AS400", "db2");
    return databaseTypeMappings;
  }

  public void initDatabaseType() {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      DatabaseMetaData databaseMetaData = connection.getMetaData();
      String databaseProductName = databaseMetaData.getDatabaseProductName();
      log.debug("database product name: '{}'", databaseProductName);
      databaseType = databaseTypeMappings.getProperty(databaseProductName);
      if (databaseType==null) {
        throw new ActivitiException("couldn't deduct database type from database product name '"+databaseProductName+"'");
      }
      log.debug("using database type: {}", databaseType);

    } catch (SQLException e) {
      log.error("Exception while initializing Database connection", e);
    } finally {
      try {
        if (connection!=null) {
          connection.close();
        }
      } catch (SQLException e) {
          log.error("Exception while closing the Database connection", e);
      }
    }
  }
  
  // myBatis SqlSessionFactory ////////////////////////////////////////////////
  
  protected void initTransactionFactory() {
    if (transactionFactory==null) {
      if (transactionsExternallyManaged) {
        transactionFactory = new ManagedTransactionFactory();
      } else {
        transactionFactory = new JdbcTransactionFactory();
      }
    }
  }

  protected void initSqlSessionFactory() {
    if (sqlSessionFactory==null) {
      InputStream inputStream = null;
      try {
        inputStream = getMyBatisXmlConfigurationSteam();

        // update the jdbc parameters to the configured ones...
        Environment environment = new Environment("default", transactionFactory, dataSource);
        Reader reader = new InputStreamReader(inputStream);
        Properties properties = new Properties();
        properties.put("prefix", databaseTablePrefix);
        if(databaseType != null) {
          properties.put("limitBefore" , DbSqlSessionFactory.databaseSpecificLimitBeforeStatements.get(databaseType));
          properties.put("limitAfter" , DbSqlSessionFactory.databaseSpecificLimitAfterStatements.get(databaseType));
          properties.put("limitBetween" , DbSqlSessionFactory.databaseSpecificLimitBetweenStatements.get(databaseType));
          properties.put("limitOuterJoinBetween" , DbSqlSessionFactory.databaseOuterJoinLimitBetweenStatements.get(databaseType));
          properties.put("orderBy" , DbSqlSessionFactory.databaseSpecificOrderByStatements.get(databaseType));
          properties.put("limitBeforeNativeQuery" , ObjectUtils.toString(DbSqlSessionFactory.databaseSpecificLimitBeforeNativeQueryStatements.get(databaseType)));
        }
        XMLConfigBuilder parser = new XMLConfigBuilder(reader,"", properties);
        Configuration configuration = parser.getConfiguration();
        configuration.setEnvironment(environment);
        configuration.getTypeHandlerRegistry().register(VariableType.class, JdbcType.VARCHAR, new IbatisVariableTypeHandler());
        
        if (getCustomMybatisMappers() != null) {
        	for (Class<?> clazz : getCustomMybatisMappers()) {
        		configuration.addMapper(clazz);
        	}
        }
        
        configuration = parser.parse();

        sqlSessionFactory = new DefaultSqlSessionFactory(configuration);

      } catch (Exception e) {
        throw new ActivitiException("Error while building ibatis SqlSessionFactory: " + e.getMessage(), e);
      } finally {
        IoUtil.closeSilently(inputStream);
      }
    }
  }
  
  protected InputStream getMyBatisXmlConfigurationSteam() {
    return ReflectUtil.getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }
  
  public Set<Class<?>> getCustomMybatisMappers() {
	return customMybatisMappers;
  }

  public void setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
	this.customMybatisMappers = customMybatisMappers;
  }
  
  // session factories ////////////////////////////////////////////////////////
  

  protected void initSessionFactories() {
    if (sessionFactories==null) {
      sessionFactories = new HashMap<Class<?>, SessionFactory>();

      dbSqlSessionFactory = new DbSqlSessionFactory();
      dbSqlSessionFactory.setDatabaseType(databaseType);
      dbSqlSessionFactory.setIdGenerator(idGenerator);
      dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
      dbSqlSessionFactory.setDbIdentityUsed(isDbIdentityUsed);
      dbSqlSessionFactory.setDbHistoryUsed(isDbHistoryUsed);
      dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
      dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
      dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
      dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
      addSessionFactory(dbSqlSessionFactory);
      
      addSessionFactory(new GenericManagerFactory(ResourceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ByteArrayEntityManager.class));
      addSessionFactory(new GenericManagerFactory(TableDataManager.class));
      addSessionFactory(new GenericManagerFactory(VariableInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(EventLogEntryEntityManager.class));
      
      
      addSessionFactory(new UserEntityManagerFactory());
      addSessionFactory(new GroupEntityManagerFactory());
      addSessionFactory(new MembershipEntityManagerFactory());
    }
    
    if (customSessionFactories!=null) {
      for (SessionFactory sessionFactory: customSessionFactories) {
        addSessionFactory(sessionFactory);
      }
    }
  }
  
  protected void addSessionFactory(SessionFactory sessionFactory) {
    sessionFactories.put(sessionFactory.getSessionType(), sessionFactory);
  }
  
  protected void initConfigurators() {
  	
  	allConfigurators = new ArrayList<ProcessEngineConfigurator>();
  	
  	// Configurators that are explicitely added to the config
    if (configurators != null) {
      for (ProcessEngineConfigurator configurator : configurators) {
        allConfigurators.add(configurator);
      }
    }
    
    // Auto discovery through ServiceLoader
    if (enableConfiguratorServiceLoader) {
    	ClassLoader classLoader = getClassLoader();
    	if (classLoader == null) {
    		classLoader = ReflectUtil.getClassLoader();
    	}
    	
    	ServiceLoader<ProcessEngineConfigurator> configuratorServiceLoader
    			= ServiceLoader.load(ProcessEngineConfigurator.class, classLoader);
    	int nrOfServiceLoadedConfigurators = 0;
    	for (ProcessEngineConfigurator configurator : configuratorServiceLoader) {
    		allConfigurators.add(configurator);
    		nrOfServiceLoadedConfigurators++;
    	}
    	
    	if (nrOfServiceLoadedConfigurators > 0) {
    		log.info("Found {} auto-discoverable Process Engine Configurator{}", nrOfServiceLoadedConfigurators++, nrOfServiceLoadedConfigurators > 1 ? "s" : "");
    	}
    	
    	if (allConfigurators.size() > 0) {
    		
    		// Order them according to the priorities (usefule for dependent configurator)
	    	Collections.sort(allConfigurators, new Comparator<ProcessEngineConfigurator>() {
	    		@Override
	    		public int compare(ProcessEngineConfigurator configurator1, ProcessEngineConfigurator configurator2) {
	    			int priority1 = configurator1.getPriority();
	    			int priority2 = configurator2.getPriority();
	    			
	    			if (priority1 < priority2) {
	    				return -1;
	    			} else if (priority1 > priority2) {
	    				return 1;
	    			} 
	    			return 0;
	    		}
				});
	    	
	    	// Execute the configurators
	    	log.info("Found {} Process Engine Configurators in total:", allConfigurators.size());
	    	for (ProcessEngineConfigurator configurator : allConfigurators) {
	    		log.info("{} (priority:{})", configurator.getClass(), configurator.getPriority());
	    	}
	    	
    	}
    	
    }
  }
  
  protected void configuratorsBeforeInit() {
  	for (ProcessEngineConfigurator configurator : allConfigurators) {
  		log.info("Executing configure() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
  		configurator.beforeInit(this);
  	}
  }
  
  protected void configuratorsAfterInit() {
  	for (ProcessEngineConfigurator configurator : allConfigurators) {
  		log.info("Executing configure() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
  		configurator.configure(this);
  	}
  }
  
  

  protected void initProcessDiagramGenerator() {
    if (processDiagramGenerator == null) {
      processDiagramGenerator = new DefaultProcessDiagramGenerator();
    }
  }
  
  // id generator /////////////////////////////////////////////////////////////
  
  protected void initIdGenerator() {
    if (idGenerator==null) {
      CommandExecutor idGeneratorCommandExecutor = null;
      if (idGeneratorDataSource!=null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(idGeneratorDataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutor();
      } else if (idGeneratorDataSourceJndiName!=null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSourceJndiName(idGeneratorDataSourceJndiName);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutor();
      } else {
        idGeneratorCommandExecutor = getCommandExecutor();
      }
      
      DbIdGenerator dbIdGenerator = new DbIdGenerator();
      dbIdGenerator.setIdBlockSize(idBlockSize);
      dbIdGenerator.setCommandExecutor(idGeneratorCommandExecutor);
      dbIdGenerator.setCommandConfig(getDefaultCommandConfig().transactionRequiresNew());
      idGenerator = dbIdGenerator;
    }
  }

  // OTHER ////////////////////////////////////////////////////////////////////
  
  protected void initCommandContextFactory() {
    if (commandContextFactory==null) {
      commandContextFactory = new CommandContextFactory();
      commandContextFactory.setProcessEngineConfiguration(this);
    }
  }

  protected void initTransactionContextFactory() {
    if (transactionContextFactory==null) {
      transactionContextFactory = new StandaloneMybatisTransactionContextFactory();
    }
  }

  protected void initVariableTypes() {
    if (variableTypes==null) {
      variableTypes = new DefaultVariableTypes();
      if (customPreVariableTypes!=null) {
        for (VariableType customVariableType: customPreVariableTypes) {
          variableTypes.addType(customVariableType);
        }
      }
      variableTypes.addType(new NullType());
      variableTypes.addType(new StringType(4000));
      variableTypes.addType(new LongStringType(4001));
      variableTypes.addType(new BooleanType());
      variableTypes.addType(new ShortType());
      variableTypes.addType(new IntegerType());
      variableTypes.addType(new LongType());
      variableTypes.addType(new DateType());
      variableTypes.addType(new DoubleType());
      variableTypes.addType(new UUIDType());
      variableTypes.addType(new ByteArrayType());
      variableTypes.addType(new SerializableType());
      if (customPostVariableTypes!=null) {
        for (VariableType customVariableType: customPostVariableTypes) {
          variableTypes.addType(customVariableType);
        }
      }
    }
  }
  protected void initDelegateInterceptor() {
    if(delegateInterceptor == null) {
      delegateInterceptor = new DefaultDelegateInterceptor();
    }
  }
  
  
  // JPA //////////////////////////////////////////////////////////////////////
  
  protected void initJpa() {
    if(jpaPersistenceUnitName!=null) {
      jpaEntityManagerFactory = JpaHelper.createEntityManagerFactory(jpaPersistenceUnitName);
    }
    if(jpaEntityManagerFactory!=null) {
      sessionFactories.put(EntityManagerSession.class, new EntityManagerSessionFactory(jpaEntityManagerFactory, jpaHandleTransaction, jpaCloseEntityManager));
      VariableType jpaType = variableTypes.getVariableType(JPAEntityVariableType.TYPE_NAME);
      // Add JPA-type
      if(jpaType == null) {
        // We try adding the variable right before SerializableType, if available
        int serializableIndex = variableTypes.getTypeIndex(SerializableType.TYPE_NAME);
        if(serializableIndex > -1) {
          variableTypes.addType(new JPAEntityVariableType(), serializableIndex);
        } else {
          variableTypes.addType(new JPAEntityVariableType());
        }        
      }
    }
  }
  
  protected void initBeans() {
    if (beans == null) {
      beans = new HashMap<Object, Object>();
    }
  }
  
  protected void initEventDispatcher() {
  	if(this.eventDispatcher == null) {
  		this.eventDispatcher = new ActivitiEventDispatcherImpl();
  	}
  	
  	this.eventDispatcher.setEnabled(enableEventDispatcher);
  	
  	if(eventListeners != null) {
  		for(ActivitiEventListener listenerToAdd : eventListeners) {
  			this.eventDispatcher.addEventListener(listenerToAdd);
  		}
  	}
  	
  	if(typedEventListeners != null) {
  		for(Entry<String, List<ActivitiEventListener>> listenersToAdd : typedEventListeners.entrySet()) {
  			// Extract types from the given string
  			ActivitiEventType[] types = ActivitiEventType.getTypesFromString(listenersToAdd.getKey());
  			
  			for(ActivitiEventListener listenerToAdd : listenersToAdd.getValue()) {
  				this.eventDispatcher.addEventListener(listenerToAdd, types);
  			}
  		}
  	}
  	
  }
  
  protected void initProcessValidator() {
  	if (this.processValidator == null) {
  		this.processValidator = new ProcessValidatorFactory().createDefaultProcessValidator();
  	}
  }
  
  protected void initDatabaseEventLogging() {
  	if (enableDatabaseEventLogging) {
  		// Database event logging uses the default logging mechanism and adds
  		// a specific event listener to the list of event listeners
//  		getEventDispatcher().addEventListener(new EventLogger(clock));
  	}
  }

  // getters and setters //////////////////////////////////////////////////////
  
  public CommandConfig getDefaultCommandConfig() {
    return defaultCommandConfig;
  }
  
  public void setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
    this.defaultCommandConfig = defaultCommandConfig;
  }
  
  public CommandConfig getSchemaCommandConfig() {
    return schemaCommandConfig;
  }
  
  public void setSchemaCommandConfig(CommandConfig schemaCommandConfig) {
    this.schemaCommandConfig = schemaCommandConfig;
  }

  public CommandInterceptor getCommandInvoker() {
    return commandInvoker;
  }
  
  public void setCommandInvoker(CommandInterceptor commandInvoker) {
    this.commandInvoker = commandInvoker;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptors() {
    return customPreCommandInterceptors;
  }
  
  public ProcessEngineConfigurationImpl setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
    this.customPreCommandInterceptors = customPreCommandInterceptors;
    return this;
  }
  
  public List<CommandInterceptor> getCustomPostCommandInterceptors() {
    return customPostCommandInterceptors;
  }
  
  public ProcessEngineConfigurationImpl setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
    this.customPostCommandInterceptors = customPostCommandInterceptors;
    return this;
  }
  
  public List<CommandInterceptor> getCommandInterceptors() {
    return commandInterceptors;
  }
  
  public ProcessEngineConfigurationImpl setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
    this.commandInterceptors = commandInterceptors;
    return this;
  }
  
  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }
  
  public ProcessEngineConfigurationImpl setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return this;
  }
  
  
  public IdentityService getIdentityService() {
    return identityService;
  }
  
  public ProcessEngineConfigurationImpl setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
    return this;
  }
  
  public ManagementService getManagementService() {
    return managementService;
  }
  
  public ProcessEngineConfigurationImpl setManagementService(ManagementService managementService) {
    this.managementService = managementService;
    return this;
  }
  
  public ProcessEngineConfiguration getProcessEngineConfiguration() {
    return this;
  }
  
  public Map<Class< ? >, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }
  
  public ProcessEngineConfigurationImpl setSessionFactories(Map<Class< ? >, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }
  
  public List<ProcessEngineConfigurator> getConfigurators() {
    return configurators;
  }

  public ProcessEngineConfigurationImpl addConfigurator(ProcessEngineConfigurator configurator) {
    if(this.configurators == null) {
      this.configurators = new ArrayList<ProcessEngineConfigurator>();
    }
    this.configurators.add(configurator);
    return this;
  }
  
  public ProcessEngineConfigurationImpl setConfigurators(List<ProcessEngineConfigurator> configurators) {
    this.configurators = configurators;
    return this;
  }

  public void setEnableConfiguratorServiceLoader(boolean enableConfiguratorServiceLoader) {
  	this.enableConfiguratorServiceLoader = enableConfiguratorServiceLoader;
  }

  public List<ProcessEngineConfigurator> getAllConfigurators() {
		return allConfigurators;
  }
  
  public IdGenerator getIdGenerator() {
    return idGenerator;
  }
  
  public ProcessEngineConfigurationImpl setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }
  
  public String getWsSyncFactoryClassName() {
    return wsSyncFactoryClassName;
  }
  
  public ProcessEngineConfigurationImpl setWsSyncFactoryClassName(String wsSyncFactoryClassName) {
    this.wsSyncFactoryClassName = wsSyncFactoryClassName;
    return this;
  }
  
  
  public VariableTypes getVariableTypes() {
    return variableTypes;
  }
  
  public ProcessEngineConfigurationImpl setVariableTypes(VariableTypes variableTypes) {
    this.variableTypes = variableTypes;
    return this;
  }
  
  
  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }
  
  public ProcessEngineConfigurationImpl setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }
  
  public TransactionContextFactory getTransactionContextFactory() {
    return transactionContextFactory;
  }
  
  public ProcessEngineConfigurationImpl setTransactionContextFactory(TransactionContextFactory transactionContextFactory) {
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }
  
  
  public SqlSessionFactory getSqlSessionFactory() {
    return sqlSessionFactory;
  }
  
  public ProcessEngineConfigurationImpl setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }
  
  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public ProcessEngineConfigurationImpl setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }
  
  public TransactionFactory getTransactionFactory() {
    return transactionFactory;
  }

  public ProcessEngineConfigurationImpl setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public List<SessionFactory> getCustomSessionFactories() {
    return customSessionFactories;
  }
  
  public ProcessEngineConfigurationImpl setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }


  public List<VariableType> getCustomPreVariableTypes() {
    return customPreVariableTypes;
  }

  public ProcessEngineConfigurationImpl setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
    this.customPreVariableTypes = customPreVariableTypes;
    return this;
  }
  
  public List<VariableType> getCustomPostVariableTypes() {
    return customPostVariableTypes;
  }

  public ProcessEngineConfigurationImpl setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
    this.customPostVariableTypes = customPostVariableTypes;
    return this;
  }


  public Map<Object, Object> getBeans() {
    return beans;
  }

  public ProcessEngineConfigurationImpl setBeans(Map<Object, Object> beans) {
    this.beans = beans;
    return this;
  }
    
  public ProcessEngineConfigurationImpl setDelegateInterceptor(DelegateInterceptor delegateInterceptor) {
    this.delegateInterceptor = delegateInterceptor;
    return this;
  }
    
  public DelegateInterceptor getDelegateInterceptor() {
    return delegateInterceptor;
  }

  public DataSource getIdGeneratorDataSource() {
    return idGeneratorDataSource;
  }
  
  public ProcessEngineConfigurationImpl setIdGeneratorDataSource(DataSource idGeneratorDataSource) {
    this.idGeneratorDataSource = idGeneratorDataSource;
    return this;
  }
  
  public String getIdGeneratorDataSourceJndiName() {
    return idGeneratorDataSourceJndiName;
  }

  public ProcessEngineConfigurationImpl setIdGeneratorDataSourceJndiName(String idGeneratorDataSourceJndiName) {
    this.idGeneratorDataSourceJndiName = idGeneratorDataSourceJndiName;
    return this;
  }

  public int getBatchSizeProcessInstances() {
    return batchSizeProcessInstances;
  }

  public ProcessEngineConfigurationImpl setBatchSizeProcessInstances(int batchSizeProcessInstances) {
    this.batchSizeProcessInstances = batchSizeProcessInstances;
    return this;
  }
  
  public int getBatchSizeTasks() {
    return batchSizeTasks;
  }
  
  public ProcessEngineConfigurationImpl setBatchSizeTasks(int batchSizeTasks) {
    this.batchSizeTasks = batchSizeTasks;
    return this;
  }
  
  public int getProcessDefinitionCacheLimit() {
    return processDefinitionCacheLimit;
  }

  public ProcessEngineConfigurationImpl setProcessDefinitionCacheLimit(int processDefinitionCacheLimit) {
    this.processDefinitionCacheLimit = processDefinitionCacheLimit;
    return this;
  }

  public int getKnowledgeBaseCacheLimit() {
    return knowledgeBaseCacheLimit;
  }

  public ProcessEngineConfigurationImpl setKnowledgeBaseCacheLimit(int knowledgeBaseCacheLimit) {
    this.knowledgeBaseCacheLimit = knowledgeBaseCacheLimit;
    return this;
  }

  public boolean isEnableSafeBpmnXml() {
    return enableSafeBpmnXml;
  }

  public ProcessEngineConfigurationImpl setEnableSafeBpmnXml(boolean enableSafeBpmnXml) {
    this.enableSafeBpmnXml = enableSafeBpmnXml;
    return this;
  }
  
  public ActivitiEventDispatcher getEventDispatcher() {
	  return eventDispatcher;
  }
  
  public void setEventDispatcher(ActivitiEventDispatcher eventDispatcher) {
	  this.eventDispatcher = eventDispatcher;
  }
  
  public void setEnableEventDispatcher(boolean enableEventDispatcher) {
	  this.enableEventDispatcher = enableEventDispatcher;
  }
  
  public void setTypedEventListeners(Map<String, List<ActivitiEventListener>> typedListeners) {
	  this.typedEventListeners = typedListeners;
  }
  
  public void setEventListeners(List<ActivitiEventListener> eventListeners) {
	  this.eventListeners = eventListeners;
  }

	public ProcessValidator getProcessValidator() {
		return processValidator;
	}

	public void setProcessValidator(ProcessValidator processValidator) {
		this.processValidator = processValidator;
	}

	public boolean isEnableEventDispatcher() {
		return enableEventDispatcher;
	}

	public boolean isEnableDatabaseEventLogging() {
		return enableDatabaseEventLogging;
	}

	public ProcessEngineConfigurationImpl setEnableDatabaseEventLogging(boolean enableDatabaseEventLogging) {
		this.enableDatabaseEventLogging = enableDatabaseEventLogging;
    return this;
	}
	
}
