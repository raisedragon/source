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
package org.activiti.engine;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.activiti.engine.event.EventLogEntry;
import org.activiti.engine.impl.cmd.CustomSqlExecution;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.management.TableMetaData;
import org.activiti.engine.management.TablePage;
import org.activiti.engine.management.TablePageQuery;



/**
 * Service for admin and maintenance operations on the process engine.
 * 
 * These operations will typically not be used in a workflow driven application,
 * but are used in for example the operational console.
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 */
public interface ManagementService {

  /**
   * Get the mapping containing {table name, row count} entries of the
   * Activiti database schema.
   */
  Map<String, Long> getTableCount();
  
  /**
   * Gets the table name (including any configured prefix) for an Activiti entity like Task, Execution or the like.
   */
  String getTableName(Class<?> activitiEntityClass);
  
  /**
   * Gets the metadata (column names, column types, etc.) of a certain table. 
   * Returns null when no table exists with the given name.
   */
  TableMetaData getTableMetaData(String tableName);
 
  /**
   * Creates a {@link TablePageQuery} that can be used to fetch {@link TablePage}
   * containing specific sections of table row data.
   */
  TablePageQuery createTablePageQuery();
  


  /** get the list of properties. */
  Map<String, String> getProperties();
  
  /** programmatic schema update on a given connection returning feedback about what happened */
  String databaseSchemaUpgrade(Connection connection, String catalog, String schema);
  
  /**
   * Executes a given command with the default {@link CommandConfig}.
   * @param command the command, cannot be null.
   * @return the result of command execution
   */
  <T> T executeCommand(Command<T> command);

  /**
   * Executes a given command with the specified {@link CommandConfig}.
   * @param config the command execution configuration, cannot be null.
   * @param command the command, cannot be null.
   * @return the result of command execution
   */
  <T> T executeCommand(CommandConfig config, Command<T> command);
  
  /**
   * [EXPERIMENTAL]
   * 
   * Executes the sql contained in the {@link CustomSqlExecution} parameter.
   */
  <MapperType, ResultType> ResultType executeCustomSql(CustomSqlExecution<MapperType, ResultType> customSqlExecution);
  
  /**
   * [EXPERIMENTAL]
   * 
   * Returns a list of event log entries, describing everything the engine has processed.
   * Note that the event logging must specifically must be enabled in the process engine configuration.
   * 
   * Passing null as arguments will effectively fetch ALL event log entries. 
   * Be careful, as this list might be huge!
   */
  List<EventLogEntry> getEventLogEntries(Long startLogNr, Long pageSize);
  
  /**
   * [EXPERIMENTAL]
   * 
   * Returns a list of event log entries for a specific process instance id.
   * Note that the event logging must specifically must be enabled in the process engine configuration.
   * 
   * Passing null as arguments will effectively fetch ALL event log entries. 
   * Be careful, as this list might be huge!
   */
  List<EventLogEntry> getEventLogEntriesByProcessInstanceId(String processInstanceId);
  
  /**
   * Delete a EventLogEntry.
   * Typically only used in testing, as deleting log entries defeats the whole purpose of keeping a log.
   */
  void deleteEventLogEntry(long logNr);
  
}
