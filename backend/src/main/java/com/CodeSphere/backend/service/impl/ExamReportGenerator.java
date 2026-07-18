package com.CodeSphere.backend.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class ExamReportGenerator {

    public byte[] generatePDFReport(String title, Map<String, Object> stats, List<Map<String, Object>> candidateDetails) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Document Title
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // Summary Stats
            Paragraph statsHeader = new Paragraph("Assessment Statistics", sectionFont);
            statsHeader.setSpacingAfter(10);
            document.add(statsHeader);

            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(20);

            addTableCell(statsTable, "Total Participated", normalFont);
            addTableCell(statsTable, String.valueOf(stats.get("participationCount")), normalFont);
            addTableCell(statsTable, "Completion Rate", normalFont);
            addTableCell(statsTable, String.format("%.2f%%", stats.get("completionRate")), normalFont);
            addTableCell(statsTable, "Average Score", normalFont);
            addTableCell(statsTable, String.format("%.2f", stats.get("averageScore")), normalFont);
            addTableCell(statsTable, "Passing Rate", normalFont);
            addTableCell(statsTable, String.format("%.2f%%", stats.get("passPercentage")), normalFont);

            document.add(statsTable);

            // Candidate Scores Table
            Paragraph candidatesHeader = new Paragraph("Candidate Performance Summary", sectionFont);
            candidatesHeader.setSpacingAfter(10);
            document.add(candidatesHeader);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingAfter(10);

            // Table Headers
            addHeaderCell(table, "User ID", headerFont);
            addHeaderCell(table, "Name", headerFont);
            addHeaderCell(table, "Score", headerFont);
            addHeaderCell(table, "Percentage", headerFont);
            addHeaderCell(table, "Status", headerFont);

            // Table Rows
            for (Map<String, Object> candidate : candidateDetails) {
                addTableCell(table, String.valueOf(candidate.get("userId")), normalFont);
                addTableCell(table, String.valueOf(candidate.get("name")), normalFont);
                addTableCell(table, String.format("%.2f", candidate.get("score")), normalFont);
                addTableCell(table, String.format("%.2f%%", candidate.get("percentage")), normalFont);
                addTableCell(table, String.valueOf(candidate.get("status")), normalFont);
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(42, 59, 92)); // Navy theme
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }
}
