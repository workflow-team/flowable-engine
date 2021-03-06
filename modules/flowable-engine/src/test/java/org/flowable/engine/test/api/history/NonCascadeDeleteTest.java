package org.flowable.engine.test.api.history;

import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.task.Task;

public class NonCascadeDeleteTest extends PluggableFlowableTestCase {

    private static String PROCESS_DEFINITION_KEY = "oneTaskProcess";

    private String deploymentId;

    private String processInstanceId;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHistoricProcessInstanceQuery() {
        deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .deploy().getId();

        processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        taskService.complete(task.getId());

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertEquals(PROCESS_DEFINITION_KEY, processInstance.getProcessDefinitionKey());

            // Delete deployment and historic process instance remains.
            repositoryService.deleteDeployment(deploymentId, false);

            HistoricProcessInstance processInstanceAfterDelete = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertNull(processInstanceAfterDelete.getProcessDefinitionKey());
            assertNull(processInstanceAfterDelete.getProcessDefinitionName());
            assertNull(processInstanceAfterDelete.getProcessDefinitionVersion());

            // clean
            historyService.deleteHistoricProcessInstance(processInstanceId);
        }
    }
}
