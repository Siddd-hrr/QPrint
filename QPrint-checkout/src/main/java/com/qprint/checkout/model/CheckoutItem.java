package com.qprint.checkout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "checkout_items")
public class CheckoutItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_id")
    private Checkout checkout;

    @Column(name = "object_id", nullable = false)
    private UUID objectId;

    @Column(name = "filename", length = 500)
    private String filename;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_mode", length = 10)
    private ColorMode colorMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sides sides;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_size", length = 10)
    private PaperSize paperSize;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Binding binding;

    @Column(nullable = false)
    private Integer copies;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "page_range", length = 100)
    private String pageRange;
}
