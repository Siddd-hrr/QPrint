package com.qprint.objects.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qprint.objects.dto.PrintObjectResponse;
import com.qprint.objects.dto.UploadPreferencesDto;
import com.qprint.objects.model.PrintObject;
import com.qprint.objects.repository.PrintObjectRepository;
import com.qprint.objects.util.PriceCalculator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectService {

    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "jpg", "jpeg", "png");

    private final PrintObjectRepository repository;
    private final DocumentAnalyzer analyzer;
    private final MinioStorageService storageService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @SuppressWarnings("null")
    public PrintObjectResponse upload(String userIdHeader, MultipartFile file, String preferencesJson) throws Exception {
        UUID userId = parseUserId(userIdHeader);
        validateFile(file);
        byte[] data = file.getBytes();

        UploadPreferencesDto prefs = parsePreferences(preferencesJson);
        validatePreferences(prefs);

        int pageCount = analyzer.detectPageCount(file.getOriginalFilename(), file.getContentType(), data);
        BigDecimal price = PriceCalculator.calculate(pageCount, prefs.copies(), prefs.sides(), prefs.colorMode(), prefs.binding());

        String objectName = buildObjectName(userId, file.getOriginalFilename());
        storageService.upload(data, file.getContentType(), objectName);

        PrintObject entity = PrintObject.builder()
                .userId(userId)
                .originalFilename(file.getOriginalFilename())
                .fileRef(objectName)
                .copies(prefs.copies())
                .colorMode(prefs.colorMode())
                .sides(prefs.sides())
                .pageRange(normalizePageRange(prefs.pageRange()))
                .paperSize(prefs.paperSize())
                .binding(prefs.binding())
                .pageCount(pageCount)
                .calculatedPrice(price)
                .specialInstructions(trimToNull(prefs.specialInstructions()))
                .build();

        PrintObject saved = repository.save(entity);
        return PrintObjectResponse.fromEntity(saved);
    }

    public PrintObjectResponse get(UUID id, String userIdHeader) {
        UUID userId = parseUserId(userIdHeader);
        PrintObject entity = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Print object not found"));
        return PrintObjectResponse.fromEntity(entity);
    }

    public void delete(UUID id, String userIdHeader) throws Exception {
        UUID userId = parseUserId(userIdHeader);
        PrintObject entity = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Print object not found"));
        storageService.delete(entity.getFileRef());
        repository.delete(entity);
    }

    private UploadPreferencesDto parsePreferences(String json) {
        try {
            byte[] payload = json == null ? "{}".getBytes(StandardCharsets.UTF_8) : json.getBytes(StandardCharsets.UTF_8);
            return objectMapper.readValue(payload, UploadPreferencesDto.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid preferences payload");
        }
    }

    private void validatePreferences(UploadPreferencesDto prefs) {
        Set<ConstraintViolation<UploadPreferencesDto>> violations = validator.validate(prefs);
        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getMessage();
            throw new IllegalArgumentException(message);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds 50MB limit");
        }
        String ext = extension(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file type. Use PDF, DOCX, JPG, or PNG.");
        }
    }

    private UUID parseUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        return UUID.fromString(header.trim());
    }

    private String buildObjectName(UUID userId, String originalFilename) {
        String safeName = originalFilename == null ? "file" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return userId + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return null;
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String normalizePageRange(String pageRange) {
        if (pageRange == null || pageRange.isBlank()) return "ALL";
        return pageRange.trim();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
