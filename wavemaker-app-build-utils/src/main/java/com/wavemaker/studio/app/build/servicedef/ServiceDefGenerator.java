package com.wavemaker.studio.app.build.servicedef;

import java.util.*;

import com.wavemaker.studio.common.OperationNotExistException;
import com.wavemaker.studio.common.servicedef.model.Parameter;
import com.wavemaker.studio.common.servicedef.model.ServiceDefinition;
import com.wavemaker.studio.common.servicedef.model.WMServiceOperationInfo;
import com.wavemaker.studio.common.swaggerdoc.handler.OperationHandler;
import com.wavemaker.studio.common.swaggerdoc.handler.PathHandler;
import com.wavemaker.studio.common.swaggerdoc.util.SwaggerDocUtil;
import com.wavemaker.tools.apidocs.tools.core.model.Operation;
import com.wavemaker.tools.apidocs.tools.core.model.Path;
import com.wavemaker.tools.apidocs.tools.core.model.Swagger;
import com.wavemaker.tools.apidocs.tools.core.model.auth.SecuritySchemeDefinition;

/**
 * @author <a href="mailto:sunil.pulugula@wavemaker.com">Sunil Kumar</a>
 * @since 29/4/16
 */
public class ServiceDefGenerator {

    private final Swagger swagger;

    public ServiceDefGenerator(final Swagger swagger) {
        this.swagger = swagger;
    }

    /**
     * Generates service definitions for all operation from swagger.
     */
    public Map<String, ServiceDefinition> generate() {
        Map<String, ServiceDefinition> serviceDefs = new HashMap<>();
        if(swagger.getPaths() != null) {
            for (Map.Entry entry : swagger.getPaths().entrySet()) {
                Path path = (Path) entry.getValue();
                for (Operation operation : path.getOperations())
                    if (operation != null) {

                        final String operationHttpType = new PathHandler(entry.getKey().toString(), path).getOperationType(operation.getOperationId());
                        final String operationType = new OperationHandler(operation, swagger.getDefinitions()).getFullyQualifiedReturnType();
                        final String relativePath = path.getBasePath() + path.getRelativePath();
                        final WMServiceOperationInfo operationInfo = buildWMServiceOperationInfo(swagger, operation, operationHttpType, relativePath);


                        serviceDefs.put(operation.getOperationId(), new ServiceDefinition().getNewInstance()
                                .addId(operation.getOperationId())
                                .addController(operation.getTags().get(0))
                                .addType(operationType)
                                .addOperationType(operationType)
                                .addService(swagger.getInfo().getServiceId())
                                .addWmServiceOperationInfo(operationInfo));
                    }
            }
        }
        return serviceDefs;
    }

    /**
     * Generates service definition for given operation Id.
     *
     * @param operationId
     * @return service definition for given operation Id.
     * @throws OperationNotExistException when operationId does not exist in the swagger object.
     */
    public ServiceDefinition generate(String operationId) throws OperationNotExistException {
        for (Map.Entry entry : swagger.getPaths().entrySet()) {
            Path path = (Path) entry.getValue();
            for (Operation operation : path.getOperations())
                if (operation != null) {
                    if (operation.getOperationId().equals(operationId)) {
                        final String operationHttpType = new PathHandler(entry.getKey().toString(), path).getOperationType(operation.getOperationId());
                        final String operationType = new OperationHandler(operation, swagger.getDefinitions()).getFullyQualifiedReturnType();
                        final String relativePath = path.getBasePath() + path.getRelativePath();
                        final WMServiceOperationInfo operationInfo = buildWMServiceOperationInfo(swagger, operation, operationHttpType, relativePath);


                        return new ServiceDefinition().getNewInstance()
                                .addId(operation.getOperationId())
                                .addController(operation.getTags().get(0))
                                .addType(operationType)
                                .addOperationType(operationType)
                                .addService(swagger.getInfo().getServiceId())
                                .addWmServiceOperationInfo(operationInfo);
                    }
                }
        }
        throw new OperationNotExistException("Operation Id " + operationId + " does not exist in service " + swagger.getInfo().getServiceId());
    }


    private WMServiceOperationInfo buildWMServiceOperationInfo(final Swagger swagger, final Operation operation, final String httpMethod, final String relativePath) {
        List<Parameter> parameters = buildParameters(swagger, operation);

        return WMServiceOperationInfo.getNewInstance()
                .addName(operation.getMethodName())
                .addHttpMethod(httpMethod)
                .addRelativePath(relativePath)
                .addConsumes(operation.getConsumes())
                .addProduces(operation.getProduces())
                .addMethodType(httpMethod)
                .addParameters(parameters);
    }

    private List<Parameter> buildParameters(final Swagger swagger, final Operation operation) {
        List<Parameter> parameters = new ArrayList<>();
        for (com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter parameter : operation.getParameters()) {
            Parameter defParameter = buildParameter(swagger, parameter);
            parameters.add(defParameter);
        }
        buildSecurityParameters(swagger, operation, parameters);
        return parameters;
    }

    private Parameter buildParameter(final Swagger swagger, final com.wavemaker.tools.apidocs.tools.core.model.parameters.Parameter parameter) {
        final String fullyQualifiedName = SwaggerDocUtil.getParameterType(parameter);
        final String name = (parameter.getName() == null) ? parameter.getIn().toLowerCase() : parameter.getName();
        return Parameter.getNewInstance()
                .addName(name)
                .addParameterType(parameter.getIn())
                .addType(fullyQualifiedName);
    }

    private void buildSecurityParameters(final Swagger swagger, final Operation operation, final List<Parameter> parameters) {
        if (swagger.getSecurityDefinitions() != null) {
            SecuritySchemeDefinition securitySchemeDefinition = swagger.getSecurityDefinitions().get("WM_Rest_Service_Authorization");
            final List<Map<String, List<String>>> operationSecurity = operation.getSecurity();
            if (securitySchemeDefinition != null && securitySchemeDefinition.getType().equals("basic")) {
                if (operationSecurity != null && operationSecurity.size() > 0) {
                    if (operationSecurity.get(0).get("WM_Rest_Service_Authorization") != null) {
                        final Parameter userNameParameter = new Parameter();
                        userNameParameter.setName("wm_auth_username");
                        userNameParameter.setParameterType("auth");
                        parameters.add(userNameParameter);

                        final Parameter passwordParameter = new Parameter();
                        passwordParameter.setName("wm_auth_password");
                        passwordParameter.setParameterType("auth");
                        parameters.add(passwordParameter);
                    }
                }

            }
        }
    }

}
