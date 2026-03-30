package com.qprint.objects.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

@Component
public class DocumentAnalyzer {

    public int detectPageCount(String filename, String contentType, byte[] data) throws IOException {
        String ext = getExtension(filename);
        if (isPdf(ext, contentType)) {
            try (PDDocument doc = PDDocument.load(data)) {
                return doc.getNumberOfPages();
            }
        }
        if (isDocx(ext, contentType)) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
                int pages = doc.getProperties().getExtendedProperties().getUnderlyingProperties().getPages();
                return pages > 0 ? pages : 1;
            }
        }
        if (isImage(ext, contentType)) {
            return 1;
        }
        throw new IllegalArgumentException("Unsupported file type. Use PDF, DOCX, JPG, or PNG.");
    }

    private boolean isPdf(String ext, String contentType) {
        return "pdf".equals(ext) || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf"));
    }

    private boolean isDocx(String ext, String contentType) {
        return "docx".equals(ext) || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("word"));
    }

    private boolean isImage(String ext, String contentType) {
        return ext != null && (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) ||
                (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/"));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return null;
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
