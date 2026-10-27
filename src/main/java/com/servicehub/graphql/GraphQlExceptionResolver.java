package com.servicehub.graphql;

import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.util.Map;

/** GraphQL reports application errors in errors[].extensions.code, normally with HTTP 200. */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        ErrorType type;
        String code;
        String message;
        if (exception instanceof AuthenticationException) {
            type = ErrorType.UNAUTHORIZED;
            code = "UNAUTHORIZED";
            message = "Autenticação necessária";
        } else if (exception instanceof AccessDeniedException) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean anonymous = authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken;
            type = anonymous ? ErrorType.UNAUTHORIZED : ErrorType.FORBIDDEN;
            code = anonymous ? "UNAUTHORIZED" : "FORBIDDEN";
            message = anonymous ? "Autenticação necessária" : "Sem permissão para esta operação";
        } else if (exception instanceof ResourceNotFoundException) {
            type = ErrorType.NOT_FOUND;
            code = "NOT_FOUND";
            message = exception.getMessage();
        } else if (exception instanceof DuplicateResourceException || exception instanceof BusinessRuleException) {
            type = ErrorType.BAD_REQUEST;
            code = "CONFLICT";
            message = exception.getMessage();
        } else if (exception instanceof IllegalArgumentException || exception instanceof ConstraintViolationException
                || exception instanceof BindException) {
            type = ErrorType.BAD_REQUEST;
            code = "BAD_REQUEST";
            message = exception instanceof IllegalArgumentException ? exception.getMessage() : "Dados inválidos";
        } else {
            return null; // Spring's default resolver hides unexpected internal exception details.
        }
        return GraphqlErrorBuilder.newError(environment)
                .errorType(type)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
