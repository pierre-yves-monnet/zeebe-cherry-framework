/* ******************************************************************** */
/*                                                                      */
/*  Abstract Connector                                                     */
/*                                                                      */
/*  To be manage by Cherry, a worker must extend this class            */
/*  It contains the basic information required by Cherry                */
/*   - define the type, and Input/Output/Errors                         */
/*   - be able to give description, logo, name                          */
/*  and the contract implementation on parameters                       */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

public abstract class AbstractConnector extends AbstractRunner implements OutboundConnectorFunction {
    Logger logger = LoggerFactory.getLogger(AbstractConnector.class.getName());

    private AbstractConnectorInput abstractConnectorInput;

    private AbstractConnectorOutput abstractConnectorOutput;
    protected AbstractConnector(String type,
                                Class<?> connectorInputClass,
                                Class<?> connectorOutputClass,
                                List<BpmnError> listBpmnErrors) {
        super(type, Collections.emptyList(),
                Collections.emptyList(),
                listBpmnErrors);
        // Create class ConnectorInput/ConnectorOutput to get parameters
        try {
            Object inputClass = connectorInputClass.getConstructors()[0].newInstance();
            if (inputClass instanceof AbstractConnectorInput abstractConnectorInput) {
                this.abstractConnectorInput = abstractConnectorInput;
                setListInput(abstractConnectorInput.getInputParameters());
            } else {
                logger.error("AbstractConnector: connectorInputClass must extends AbstractConnectorInput");
            }

        } catch (Exception e) {
            logger.error("AbstractConnector: can't create ConnectorInput to get listOfParameters " + e);
        }
        if (connectorOutputClass!=null) {
            try {

                for (Constructor constructor : connectorOutputClass.getConstructors()) {
                    if (constructor.getParameterCount() == 0) {
                        Object outputClass = constructor.newInstance();
                        if (outputClass instanceof AbstractConnectorOutput abstractConnectorOutput) {
                            this.abstractConnectorOutput = abstractConnectorOutput;
                            setListOutput(abstractConnectorOutput.getListOutput());
                        }
                        // it is acceptable that this class does not extend the AbstractConnectorOutput (pure connector pattern)
                    }
                }

            } catch (Exception e) {
                logger.error("AbstractConnector: can't create ConnectorOutput to get list OfParameters" + e);
            }
        }

    }


    public AbstractConnectorInput getAbstractConnectorInput() {
        return abstractConnectorInput;
    }

    public AbstractConnectorOutput getAbstractConnectorOutput() {
        return abstractConnectorOutput;
    }

}
