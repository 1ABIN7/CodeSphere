package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Certification;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.CertificationRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.CertificationService;
import com.CodeSphere.backend.service.FileStorageService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfContentByte;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificationServiceImpl implements CertificationService {

    private final CertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public Certification issueCertificate(Long userId, String certTitle, Long examSessionId, Double score) {
        // Prevent duplicate certification issuance
        List<Certification> existing = certificationRepository.findByUserId(userId).stream()
                .filter(c -> examSessionId.equals(c.getExamSessionId()))
                .toList();
        if (!existing.isEmpty()) {
            log.info("Certificate already issued for session #{}", examSessionId);
            return existing.get(0);
        }

        String verificationCode = "CS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Certification cert = Certification.builder()
                .userId(userId)
                .certTitle(certTitle)
                .issuedBy("CodeSphere Platform")
                .verificationCode(verificationCode)
                .examSessionId(examSessionId)
                .score(score)
                .isValid(true)
                .build();

        Certification saved = certificationRepository.save(cert);
        log.info("Issued certificate with code {} to user ID {}", verificationCode, userId);

        // Generate PDF and upload to storage provider
        try {
            byte[] pdfBytes = generateCertificatePDF(saved);
            MultipartFile file = new ByteArrayMultipartFile(
                    pdfBytes,
                    "file",
                    "certificate_" + verificationCode + ".pdf",
                    "application/pdf"
            );
            fileStorageService.upload(file, "PROFILE", saved.getId(), userId);
        } catch (Exception e) {
            log.error("Failed to generate and upload certificate PDF for user ID {}: {}", userId, e.getMessage());
        }

        return saved;
    }

    @Override
    public Certification verifyCertificate(String verificationCode) {
        return certificationRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code: " + verificationCode));
    }

    @Override
    public Resource downloadCertificatePDF(Long certificationId) {
        Certification cert = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certification not found: " + certificationId));
        
        // Query the file attachment details using FileStorageService list method
        var attachments = fileStorageService.listByEntity("PROFILE", certificationId);
        if (attachments.isEmpty()) {
            throw new IllegalStateException("Certificate PDF file not found for ID: " + certificationId);
        }
        return fileStorageService.download(attachments.get(0).getId());
    }

    private byte[] generateCertificatePDF(Certification cert) throws Exception {
        User user = userRepository.findById(cert.getUserId()).orElse(null);
        String name = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Developer";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate()); // Landscape orientation
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();

        // 1. Draw decorative borders
        PdfContentByte canvas = writer.getDirectContent();
        canvas.setColorStroke(new Color(42, 59, 92)); // Navy border
        canvas.setLineWidth(5);
        canvas.rectangle(20, 20, PageSize.A4.rotate().getWidth() - 40, PageSize.A4.rotate().getHeight() - 40);
        canvas.stroke();

        // 2. Write Certificate details
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, Color.DARK_GRAY);
        Font presentedToFont = FontFactory.getFont(FontFactory.HELVETICA, 16, Color.GRAY);
        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(42, 59, 92));
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
        Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 12, Color.DARK_GRAY);

        Paragraph title = new Paragraph("CERTIFICATE OF ACHIEVEMENT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(50);
        document.add(title);

        Paragraph presentedTo = new Paragraph("This is proudly presented to", presentedToFont);
        presentedTo.setAlignment(Element.ALIGN_CENTER);
        presentedTo.setSpacingBefore(20);
        document.add(presentedTo);

        Paragraph namePara = new Paragraph(name, nameFont);
        namePara.setAlignment(Element.ALIGN_CENTER);
        namePara.setSpacingBefore(15);
        document.add(namePara);

        String descriptionText = String.format("For successfully demonstrating proficiency in the assessment\n\"%s\"\nwith a final score of %.2f%%.",
                cert.getCertTitle(), cert.getScore());
        Paragraph descPara = new Paragraph(descriptionText, bodyFont);
        descPara.setAlignment(Element.ALIGN_CENTER);
        descPara.setSpacingBefore(20);
        document.add(descPara);

        // 3. Generate QR Code bytes using ZXing
        String verifyUrl = "https://codesphere.com/verify/" + cert.getVerificationCode();
        byte[] qrBytes = generateQRCodeBytes(verifyUrl, 120, 120);

        Image qrImage = Image.getInstance(qrBytes);
        qrImage.setAlignment(Element.ALIGN_CENTER);
        qrImage.setSpacingBefore(15);
        document.add(qrImage);

        // Verification metadata
        String dateStr = cert.getIssuedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Paragraph verification = new Paragraph(String.format("Verification Code: %s   |   Issued At: %s",
                cert.getVerificationCode(), dateStr), codeFont);
        verification.setAlignment(Element.ALIGN_CENTER);
        verification.setSpacingBefore(15);
        document.add(verification);

        document.close();
        return out.toByteArray();
    }

    private byte[] generateQRCodeBytes(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String originalFilename;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String name, String originalFilename, String contentType) {
            this.content = content;
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
