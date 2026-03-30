package com.qprint.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddItemRequest(@NotNull UUID objectId) {
}
