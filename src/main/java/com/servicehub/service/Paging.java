package com.servicehub.service;

import com.servicehub.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Set;

/** Limita consultas e impede ordenar por associações ou campos privados da entidade. */
public final class Paging {
    private Paging() {}

    public static Pageable safe(Pageable pageable, Set<String> allowedSorts) {
        Pageable candidate = pageable == null || pageable.isUnpaged()
            ? PageRequest.of(0, 20, pageable == null ? Sort.unsorted() : pageable.getSort()) : pageable;
        if (candidate.getPageNumber() < 0 || candidate.getPageSize() < 1 || candidate.getPageSize() > 100)
            throw new BadRequestException("Paginação inválida: página deve ser não negativa e tamanho deve estar entre 1 e 100");
        for (Sort.Order order : candidate.getSort()) {
            if (!allowedSorts.contains(order.getProperty()) || order.isIgnoreCase()
                    || order.getNullHandling() != Sort.NullHandling.NATIVE)
                throw new BadRequestException("Ordenação inválida. Campos permitidos: " + String.join(", ", allowedSorts.stream().sorted().toList()));
        }
        return candidate;
    }
}
