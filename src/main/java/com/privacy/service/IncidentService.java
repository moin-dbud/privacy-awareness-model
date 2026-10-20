package com.privacy.service;

import com.privacy.dto.IncidentReportRequest;
import com.privacy.dto.IncidentReportResponse;
import com.privacy.entity.IncidentReport;
import com.privacy.entity.User;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.IncidentReportRepository;
import com.privacy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public IncidentReportResponse createReport(String username, IncidentReportRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        IncidentReport report = IncidentReport.builder()
                .user(user)
                .incidentType(request.getIncidentType())
                .title(request.getTitle())
                .description(request.getDescription())
                .platform(request.getPlatform())
                .status(IncidentReport.ReportStatus.OPEN)
                .build();
        return IncidentReportResponse.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<IncidentReportResponse> getUserReports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reportRepository.findByUserIdOrderByReportedAtDesc(user.getId()).stream()
                .map(IncidentReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentReportResponse getUserReport(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reportRepository.findByIdAndUserId(id, user.getId())
                .map(IncidentReportResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    // ─── Admin Methods ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IncidentReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByReportedAtDesc().stream()
                .map(IncidentReportResponse::from)
                .toList();
    }

    @Transactional
    public IncidentReportResponse updateStatus(Long id, IncidentReport.ReportStatus status, String adminNotes) {
        IncidentReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(status);
        if (adminNotes != null && !adminNotes.isBlank()) {
            report.setAdminNotes(adminNotes);
        }
        return IncidentReportResponse.from(reportRepository.save(report));
    }

    // ─── Export Methods (CSV & PDF) ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IncidentReport> getFilteredIncidents(String statusStr, String typeStr) {
        return reportRepository.findAllByOrderByReportedAtDesc().stream()
                .filter(report -> {
                    if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("ALL")) {
                        if (report.getStatus() == null || !report.getStatus().name().equalsIgnoreCase(statusStr.trim())) {
                            return false;
                        }
                    }
                    if (typeStr != null && !typeStr.isBlank() && !typeStr.equalsIgnoreCase("ALL")) {
                        if (report.getIncidentType() == null || !report.getIncidentType().name().equalsIgnoreCase(typeStr.trim())) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String statusStr, String typeStr) {
        List<IncidentReport> list = getFilteredIncidents(statusStr, typeStr);
        StringBuilder sb = new StringBuilder();

        // UTF-8 BOM for spreadsheet tools (Excel)
        sb.append("\uFEFF");

        // Headers
        sb.append(String.join(",",
                escapeCsv("Report ID"),
                escapeCsv("Report Date"),
                escapeCsv("Reporter Username"),
                escapeCsv("Reporter Email"),
                escapeCsv("Incident Type"),
                escapeCsv("Platform"),
                escapeCsv("Title"),
                escapeCsv("Description"),
                escapeCsv("Status"),
                escapeCsv("Admin Notes"),
                escapeCsv("Last Updated")
        )).append("\r\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (IncidentReport r : list) {
            String reporterUsername = r.getUser() != null ? r.getUser().getUsername() : "Unknown";
            String reporterEmail = r.getUser() != null ? r.getUser().getEmail() : "";
            String typeName = r.getIncidentType() != null ? r.getIncidentType().name() : "";
            String statusName = r.getStatus() != null ? r.getStatus().name() : "";
            String reportedAtStr = r.getReportedAt() != null ? r.getReportedAt().format(dtf) : "";
            String updatedAtStr = r.getUpdatedAt() != null ? r.getUpdatedAt().format(dtf) : "";

            sb.append(String.join(",",
                    escapeCsv(String.valueOf(r.getId())),
                    escapeCsv(reportedAtStr),
                    escapeCsv(reporterUsername),
                    escapeCsv(reporterEmail),
                    escapeCsv(typeName),
                    escapeCsv(r.getPlatform() != null ? r.getPlatform() : ""),
                    escapeCsv(r.getTitle()),
                    escapeCsv(r.getDescription()),
                    escapeCsv(statusName),
                    escapeCsv(r.getAdminNotes() != null ? r.getAdminNotes() : ""),
                    escapeCsv(updatedAtStr)
            )).append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String clean = value;

        // Protection against spreadsheet formula injection (CSV Injection)
        // If first character is =, +, -, @, tab, or carriage return, prefix with single quote
        if (!clean.isEmpty()) {
            char first = clean.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                clean = "'" + clean;
            }
        }

        return "\"" + clean.replace("\"", "\"\"") + "\"";
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(String statusStr, String typeStr) {
        List<IncidentReport> list = getFilteredIncidents(statusStr, typeStr);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 25, 25);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(15, 23, 42));
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139));
            Font statFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 41, 59));
            Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(30, 41, 59));
            Font tdBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(30, 41, 59));

            // Title
            Paragraph title = new Paragraph("PRIVACY GUARD Ã¢â‚¬â€  SECURITY INCIDENT REPORTS AUDIT", titleFont);
            title.setSpacingAfter(4);
            document.add(title);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String genDate = java.time.LocalDateTime.now().format(dtf);
            Paragraph meta = new Paragraph("Generated: " + genDate + "  |  Filter: [Status: " + 
                    (statusStr != null ? statusStr : "ALL") + ", Type: " + 
                    (typeStr != null ? typeStr : "ALL") + "]  |  Total Matching Records: " + list.size(), subFont);
            meta.setSpacingAfter(12);
            document.add(meta);

            // Summary Counts Table
            long openCount = list.stream().filter(r -> r.getStatus() == IncidentReport.ReportStatus.OPEN).count();
            long reviewCount = list.stream().filter(r -> r.getStatus() == IncidentReport.ReportStatus.UNDER_REVIEW).count();
            long resolvedCount = list.stream().filter(r -> r.getStatus() == IncidentReport.ReportStatus.RESOLVED).count();
            long dismissedCount = list.stream().filter(r -> r.getStatus() == IncidentReport.ReportStatus.DISMISSED).count();

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(14);

            addSummaryCell(summaryTable, "OPEN INCIDENTS", String.valueOf(openCount), new Color(239, 68, 68), statFont);
            addSummaryCell(summaryTable, "UNDER REVIEW", String.valueOf(reviewCount), new Color(245, 158, 11), statFont);
            addSummaryCell(summaryTable, "RESOLVED", String.valueOf(resolvedCount), new Color(16, 185, 129), statFont);
            addSummaryCell(summaryTable, "DISMISSED", String.valueOf(dismissedCount), new Color(107, 114, 128), statFont);
            document.add(summaryTable);

            // Main Table: 8 columns
            PdfPTable table = new PdfPTable(new float[]{4.5f, 10f, 11f, 12f, 8.5f, 18f, 23f, 13f});
            table.setWidthPercentage(100);

            // Header Row
            String[] headers = {"ID", "Date", "Reporter", "Type & Platform", "Status", "Title", "Description", "Resolution / Notes"};
            Color headerBg = new Color(30, 41, 59); // Slate-800
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, thFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            // Data Rows
            Color rowAlt = new Color(248, 250, 252); // Slate-50
            boolean alternate = false;

            if (list.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No incident reports found matching the specified criteria.", tdFont));
                emptyCell.setColspan(8);
                emptyCell.setPadding(14);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(emptyCell);
            } else {
                for (IncidentReport r : list) {
                    Color bg = alternate ? rowAlt : Color.WHITE;
                    alternate = !alternate;

                    table.addCell(createCell("#" + r.getId(), tdBold, bg));
                    table.addCell(createCell(r.getReportedAt() != null ? r.getReportedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Ã¢â‚¬â€ ", tdFont, bg));
                    table.addCell(createCell(r.getUser() != null ? r.getUser().getUsername() + "\n(" + r.getUser().getEmail() + ")" : "Unknown", tdFont, bg));
                    table.addCell(createCell((r.getIncidentType() != null ? r.getIncidentType().name().replace('_', ' ') : "") + 
                            (r.getPlatform() != null && !r.getPlatform().isBlank() ? "\n[" + r.getPlatform() + "]" : ""), tdFont, bg));
                    
                    // Status with color hint
                    PdfPCell statusCell = createCell(r.getStatus() != null ? r.getStatus().name() : "OPEN", tdBold, bg);
                    table.addCell(statusCell);

                    table.addCell(createCell(r.getTitle(), tdBold, bg));
                    table.addCell(createCell(r.getDescription(), tdFont, bg));
                    table.addCell(createCell(r.getAdminNotes() != null && !r.getAdminNotes().isBlank() ? r.getAdminNotes() : "Ã¢â‚¬â€ ", tdFont, bg));
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addSummaryCell(PdfPTable table, String label, String val, Color color, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBackgroundColor(new Color(241, 245, 249));
        Paragraph p1 = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, color));
        Paragraph p2 = new Paragraph(val, font);
        cell.addElement(p1);
        cell.addElement(p2);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setBackgroundColor(bgColor);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

}
