package com.swizima.clientapi.entity;

import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import com.swizima.clientapi.entity.enums.ServiceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A service offered by a client (e.g. car wash, parking, repair) with its pricing.
 */
@Entity
@Table(name = "service_catalogs",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_catalog_client_code",
                columnNames = {"client_id", "service_code"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ServiceCatalog extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "service_code", nullable = false, length = 50)
    private String serviceCode;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ServiceCategory category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "UGX";

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceCatalogStatus status = ServiceCatalogStatus.ACTIVE;
}
