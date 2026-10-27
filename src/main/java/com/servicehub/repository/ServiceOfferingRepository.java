package com.servicehub.repository;

import com.servicehub.model.ServiceOffering;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long>, JpaSpecificationExecutor<ServiceOffering> {
    @Override
    @EntityGraph(attributePaths = "provider")
    Page<ServiceOffering> findAll(Specification<ServiceOffering> specification, Pageable pageable);

    @Query("select s from ServiceOffering s join fetch s.provider p where s.id = :id and s.active = true and p.active = true and p.userType = com.servicehub.model.UserType.PROVIDER")
    Optional<ServiceOffering> findVisibleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ServiceOffering s where s.id = :id")
    Optional<ServiceOffering> findByIdForUpdate(@Param("id") Long id);

    @Query("select s.provider.id from ServiceOffering s where s.id = :id")
    Optional<Long> findProviderId(@Param("id") Long id);

    // A janela limita cada prestador no banco, sem carregar todas as ofertas para depois descartar.
    // Projeção escalar contém o perfil público, evitando lazy loads e vazamento de contatos.
    @Query(value = """
        select ranked.id as id, ranked.title as title, ranked.description as description,
               ranked.price as price, ranked.category as category,
               ranked.provider_id as providerId, ranked.provider_name as providerName,
               ranked.provider_bio as providerBio, ranked.provider_avatar as providerAvatar,
               ranked.created_at as createdAt, ranked.updated_at as updatedAt
          from (
            select s.*, p.full_name as provider_name, p.bio as provider_bio,
                   p.avatar_url as provider_avatar,
                   row_number() over (partition by s.provider_id order by s.id) as position_in_provider
              from services s join users p on p.id = s.provider_id
             where s.provider_id in (:providerIds) and s.is_active and p.is_active and p.role = 'PROVIDER'
          ) ranked
         where ranked.position_in_provider <= 20
         order by ranked.provider_id, ranked.id
        """, nativeQuery = true)
    List<PublicCatalogRow> findActiveByProviderIds(@Param("providerIds") Collection<Long> providerIds);

    interface PublicCatalogRow {
        Long getId();
        String getTitle();
        String getDescription();
        BigDecimal getPrice();
        String getCategory();
        Long getProviderId();
        String getProviderName();
        String getProviderBio();
        String getProviderAvatar();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
    }

    @Modifying
    @Query("update ServiceOffering s set s.active = false, s.updatedAt = CURRENT_TIMESTAMP where s.provider.id = :providerId and s.active = true")
    int deactivateByProviderId(@Param("providerId") Long providerId);
}
