package com.DigitalBank.backend;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        // Si el error es una de nuestras validaciones (IllegalArgumentException)...
        if (ex instanceof IllegalArgumentException) {
            // ... creamos un error limpio de GraphQL con nuestro mensaje personalizado
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage()) // 👈 ¡Aquí exponemos tu texto!
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        // Si es un error de sistema u otro tipo, dejamos que GraphQL lo oculte
        return super.resolveToSingleError(ex, env);
    }
}