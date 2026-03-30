package com.qprint.objects.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "print_objects")
public class PrintObject {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "file_ref", nullable = false, length = 1000)
    private String fileRef;

    @Column(nullable = false)
    private Integer copies;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_mode", nullable = false, length = 10)
    private ColorMode colorMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sides sides;

    @Column(name = "page_range", length = 100)
    private String pageRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_size", nullable = false, length = 10)
    private PaperSize paperSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Binding binding;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @Column(name = "calculated_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal calculatedPrice;

    @Column(name = "special_instructions", length = 1000)
    private String specialInstructions;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
