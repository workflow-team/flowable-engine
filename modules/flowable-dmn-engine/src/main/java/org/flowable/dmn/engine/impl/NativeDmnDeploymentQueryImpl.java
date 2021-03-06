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
package org.flowable.dmn.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.NativeDmnDeploymentQuery;
import org.flowable.dmn.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.impl.interceptor.CommandExecutor;

public class NativeDmnDeploymentQueryImpl extends AbstractNativeQuery<NativeDmnDeploymentQuery, DmnDeployment> implements NativeDmnDeploymentQuery {

    private static final long serialVersionUID = 1L;

    public NativeDmnDeploymentQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public NativeDmnDeploymentQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    // results ////////////////////////////////////////////////////////////////

    public List<DmnDeployment> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
        return commandContext.getDeploymentEntityManager().findDeploymentsByNativeQuery(parameterMap, firstResult, maxResults);
    }

    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return commandContext.getDeploymentEntityManager().findDeploymentCountByNativeQuery(parameterMap);
    }

}
