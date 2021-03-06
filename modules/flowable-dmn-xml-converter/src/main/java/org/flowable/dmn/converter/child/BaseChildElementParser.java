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
package org.flowable.dmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.xml.constants.DmnXMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public abstract class BaseChildElementParser implements DmnXMLConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseChildElementParser.class);

    public abstract String getElementName();

    public abstract void parseChildElement(XMLStreamReader xtr, DmnElement parentElement, DecisionTable decisionTable) throws Exception;

    protected void parseChildElements(XMLStreamReader xtr, DmnElement parentElement, DecisionTable decisionTable, BaseChildElementParser parser) throws Exception {
        boolean readyWithChildElements = false;
        while (!readyWithChildElements && xtr.hasNext()) {
            xtr.next();
            if (xtr.isStartElement()) {
                if (parser.getElementName().equals(xtr.getLocalName())) {
                    parser.parseChildElement(xtr, parentElement, decisionTable);
                }

            } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                readyWithChildElements = true;
            }
        }
    }

    public boolean accepts(DmnElement element) {
        return element != null;
    }
}
