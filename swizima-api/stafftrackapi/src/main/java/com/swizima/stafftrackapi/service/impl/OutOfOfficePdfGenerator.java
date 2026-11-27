package com.swizima.stafftrackapi.service.impl;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.swizima.stafftrackapi.entity.OutOfOfficeAttendingOfficer;
import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;
import com.swizima.stafftrackapi.entity.ReportBackAttachment;
import com.swizima.stafftrackapi.entity.ReportBackManagementAction;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import com.swizima.stafftrackapi.entity.enums.UnitHeadApprovalStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Generates the official A4 PDF document matching the SOOTR View Activity Report
 * ({@code /out-of-office/:id/view-report}) with Uganda flag ribbon, OPM Department of Refugees header,
 * QR code &amp; Code-128 style verification barcode, Section A (Planned Activity &amp; Deliverables),
 * Section B (Post-Engagement Report Back &amp; Management Actions), and Official Sign-Off block.
 */
@Slf4j
@Component
public class OutOfOfficePdfGenerator {

    private static final Color OPM_BLUE = new Color(0, 94, 184);
    private static final Color SLATE_900 = new Color(15, 23, 42);
    private static final Color SLATE_600 = new Color(71, 85, 105);
    private static final Color SLATE_300 = new Color(203, 213, 225);
    private static final Color SLATE_100 = new Color(241, 245, 249);
    private static final Color SUB_HEAD_BG = new Color(238, 242, 249);

    private static final Font FONT_GOVT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, SLATE_600);
    private static final Font FONT_DEPT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, OPM_BLUE);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13.5f, SLATE_900);
    private static final Font FONT_META = FontFactory.getFont(FontFactory.HELVETICA, 7.2f, SLATE_600);
    private static final Font FONT_META_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, SLATE_900);
    private static final Font FONT_BARCODE_TEXT = FontFactory.getFont(FontFactory.COURIER_BOLD, 6.3f, SLATE_900);

    private static final Font FONT_SEC_HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.8f, Color.WHITE);
    private static final Font FONT_SUB_HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, SLATE_900);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.2f, SLATE_900);
    private static final Font FONT_TD = FontFactory.getFont(FontFactory.HELVETICA, 8.2f, SLATE_900);
    private static final Font FONT_TD_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.2f, SLATE_900);
    private static final Font FONT_TD_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 7.6f, SLATE_600);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 7.2f, SLATE_600);
    private static final Font FONT_FOOTER_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, SLATE_900);

    public byte[] generateReportPdf(OutOfOfficeNotification item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 30f, 30f, 26f, 26f);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // 1. Uganda Flag Ribbon (Black / Yellow / Red)
            doc.add(buildUgandaFlagRibbon());

            // 2. Official OPM Department of Refugees Banner + QR Code & Barcode Cluster
            doc.add(buildHeaderBanner(item));

            // 3. SECTION A: PLANNED ACTIVITY & OUT-OF-OFFICE DETAILS
            doc.add(buildSectionHeading("SECTION A: PLANNED ACTIVITY & OUT-OF-OFFICE DETAILS", 10f));
            doc.add(buildSectionADetailsTable(item));

            // A1. Official Purpose & Planned Deliverables
            doc.add(buildSubSectionHeading("A1. OFFICIAL PURPOSE & PLANNED DELIVERABLES"));
            doc.add(buildPurposeTable(item));

            // A2. Other OPM Staff Attending Activity (if applicable)
            if (item.isAttendingWithOthers()
                    && item.getAttendingOfficers() != null
                    && !item.getAttendingOfficers().isEmpty()) {
                doc.add(buildSubSectionHeading("A2. OTHER OPM STAFF ATTENDING ACTIVITY"));
                doc.add(buildAttendingOfficersTable(item.getAttendingOfficers()));
            }

            // 4. SECTION B: POST-ENGAGEMENT REPORT BACK & MANAGEMENT ACTIONS
            doc.add(buildSectionHeading("SECTION B: POST-ENGAGEMENT REPORT BACK & MANAGEMENT ACTIONS", 12f));
            doc.add(buildSectionBSummaryTable(item));

            // B1. Key Issues Raised / Matters Discussed
            doc.add(buildSubSectionHeading("B1. KEY ISSUES RAISED / MATTERS DISCUSSED"));
            doc.add(buildNumberedItemsTable("Key Issue Raised / Matter Discussed",
                    extractKeyIssues(item),
                    "No key issues raised or matters discussed recorded yet."));

            // B2. What Was Achieved (Deliverables)
            doc.add(buildSubSectionHeading("B2. WHAT WAS ACHIEVED (DELIVERABLES)"));
            doc.add(buildNumberedItemsTable("Deliverable Achieved",
                    extractAchievements(item),
                    "No deliverables recorded yet."));

            // B3. Management Action
            doc.add(buildSubSectionHeading("B3. MANAGEMENT ACTION"));
            doc.add(buildManagementActionsTable(item));

            // B4. Field Photos or PDF report
            doc.add(buildSubSectionHeading("B4. FIELD PHOTOS OR PDF REPORT"));
            doc.add(buildAttachmentsTable(item));

            // 5. Official Sign-Off Table
            doc.add(buildSignOffTable(item));

            // 6. Footer Verification Strip
            doc.add(buildFooterStrip(item));

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate PDF report for Out-of-Office {}: {}",
                    item != null ? item.getReferenceNumber() : "unknown", ex.getMessage(), ex);
            return new byte[0];
        }
    }

    private PdfPTable buildUgandaFlagRibbon() {
        PdfPTable ribbon = new PdfPTable(3);
        ribbon.setWidthPercentage(100f);
        ribbon.setSpacingAfter(8f);

        ribbon.addCell(colorStripCell(new Color(17, 24, 39)));
        ribbon.addCell(colorStripCell(new Color(250, 204, 21)));
        ribbon.addCell(colorStripCell(new Color(220, 38, 38)));
        return ribbon;
    }

    private PdfPCell colorStripCell(Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBackgroundColor(color);
        cell.setFixedHeight(5f);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPTable buildHeaderBanner(OutOfOfficeNotification item) throws Exception {
        PdfPTable banner = new PdfPTable(new float[]{58f, 42f});
        banner.setWidthPercentage(100f);
        banner.setSpacingAfter(6f);

        // Left: OPM Department of Refugees Title Block
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOTTOM);
        left.setBorderWidthBottom(1.6f);
        left.setBorderColorBottom(OPM_BLUE);
        left.setPaddingBottom(8f);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph govt = new Paragraph("REPUBLIC OF UGANDA · OFFICE OF THE PRIME MINISTER", FONT_GOVT);
        Paragraph dept = new Paragraph("DEPARTMENT OF REFUGEES", FONT_DEPT);
        dept.setSpacingBefore(2f);
        Paragraph title = new Paragraph("Staff Out-of-Office Report", FONT_TITLE);
        title.setSpacingBefore(2f);

        left.addElement(govt);
        left.addElement(dept);
        left.addElement(title);
        banner.addCell(left);

        // Right: QR Code + Barcode + Reference Metadata
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOTTOM);
        right.setBorderWidthBottom(1.6f);
        right.setBorderColorBottom(OPM_BLUE);
        right.setPaddingBottom(8f);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPTable cluster = new PdfPTable(new float[]{28f, 72f});
        cluster.setWidthPercentage(100f);

        PdfPCell qrCell = new PdfPCell(new Phrase(""));
        qrCell.setFixedHeight(56f);
        qrCell.setBorderColor(SLATE_300);
        qrCell.setBackgroundColor(Color.WHITE);
        qrCell.setCellEvent(new QrCodeCellRenderer(item));
        cluster.addCell(qrCell);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorderColor(SLATE_300);
        metaCell.setBackgroundColor(new Color(248, 250, 252));
        metaCell.setPadding(4f);

        String refNo = item.getReferenceNumber() != null
                ? item.getReferenceNumber()
                : (item.getId() != null ? item.getId().toString().substring(0, 8).toUpperCase() : "OOO-101");
        String startClean = item.getStartDate() != null
                ? item.getStartDate().toString().replace("-", "")
                : LocalDate.now().toString().replace("-", "");
        String barcodeCode = "*" + refNo + "-" + startClean + "*";

        PdfPTable barcodeTable = new PdfPTable(1);
        barcodeTable.setWidthPercentage(100f);
        PdfPCell barsBox = new PdfPCell(new Phrase(""));
        barsBox.setFixedHeight(18f);
        barsBox.setBorder(Rectangle.NO_BORDER);
        barsBox.setBackgroundColor(Color.WHITE);
        barsBox.setCellEvent(new BarcodeCellRenderer(barcodeCode));
        barcodeTable.addCell(barsBox);

        PdfPCell codeLabelCell = new PdfPCell(new Phrase(barcodeCode, FONT_BARCODE_TEXT));
        codeLabelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        codeLabelCell.setBorder(Rectangle.NO_BORDER);
        codeLabelCell.setBackgroundColor(Color.WHITE);
        codeLabelCell.setPaddingBottom(2f);
        barcodeTable.addCell(codeLabelCell);

        metaCell.addElement(barcodeTable);

        Paragraph metaLine1 = new Paragraph();
        metaLine1.setSpacingBefore(2f);
        metaLine1.add(new Chunk("Ref No: ", FONT_META_BOLD));
        metaLine1.add(new Chunk(refNo, FONT_META));

        Paragraph metaLine2 = new Paragraph();
        metaLine2.add(new Chunk("Station: ", FONT_META_BOLD));
        metaLine2.add(new Chunk(safe(item.getDutyStation()) + " · ", FONT_META));
        metaLine2.add(new Chunk("Issued: ", FONT_META_BOLD));
        metaLine2.add(new Chunk(LocalDate.now().toString(), FONT_META));

        metaCell.addElement(metaLine1);
        metaCell.addElement(metaLine2);
        cluster.addCell(metaCell);

        right.addElement(cluster);
        banner.addCell(right);

        return banner;
    }

    private PdfPTable buildSectionHeading(String title, float spacingBefore) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(spacingBefore);

        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SEC_HEAD));
        cell.setBackgroundColor(OPM_BLUE);
        cell.setBorderColor(OPM_BLUE);
        cell.setPadding(6f);
        table.addCell(cell);
        return table;
    }

    private PdfPTable buildSubSectionHeading(String title) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(6f);

        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SUB_HEAD));
        cell.setBackgroundColor(SUB_HEAD_BG);
        cell.setBorderColor(SLATE_300);
        cell.setPadding(5f);
        table.addCell(cell);
        return table;
    }

    private PdfPTable buildSectionADetailsTable(OutOfOfficeNotification item) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{22f, 28f, 22f, 28f});
        table.setWidthPercentage(100f);

        addKeyValRow(table,
                "Staff Name", safe(item.getOfficerName()), true,
                "Designation", safe(item.getOfficerRole()), false);

        addKeyValRow(table,
                "Department / Unit", safe(item.getDepartment()), false,
                "Duty Station", safe(item.getDutyStation()), false);

        addKeyValRow(table,
                "Engagement Type", formatEngagementType(item.getEngagementType()), false,
                "Field Contact Phone", safe(item.getContactPhone()), false);

        addKeyValRow(table,
                "Destination / Venue", safe(item.getDestination()), true,
                "Host / Organizer", safe(item.getHostOrganization()), false);

        long days = computeDurationDays(item.getStartDate(), item.getEndDate());
        String returnText = (item.getEndDate() != null ? item.getEndDate().toString() : "—")
                + " (" + days + " day(s))";

        addKeyValRow(table,
                "Planned Start Date", item.getStartDate() != null ? item.getStartDate().toString() : "—", false,
                "Planned Return Date", returnText, false);

        String loggedDate = item.getCreatedAt() != null
                ? item.getCreatedAt().toLocalDate().toString()
                : LocalDate.now().toString();

        addKeyValRow(table,
                "Acting / Cover Officer", safe(item.getActingOfficer()), false,
                "Date Logged", loggedDate, false);

        if ((item.getUnitHeadName() != null && !item.getUnitHeadName().isBlank())
                || (item.getUnitHeadApprovalStatus() != null
                && item.getUnitHeadApprovalStatus() != UnitHeadApprovalStatus.NOT_REQUIRED)) {
            addKeyValRow(table,
                    "Unit Head", safe(item.getUnitHeadName()), true,
                    "Unit Head Decision", formatUnitHeadDecision(item), false);
        }

        return table;
    }

    private PdfPTable buildPurposeTable(OutOfOfficeNotification item) throws Exception {
        List<String> purposes = extractPurposeList(item);
        return buildNumberedItemsTable("Purpose / Planned Deliverable",
                purposes,
                "No planned deliverables recorded.");
    }

    private PdfPTable buildAttendingOfficersTable(List<OutOfOfficeAttendingOfficer> officers) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{8f, 32f, 28f, 32f});
        table.setWidthPercentage(100f);

        table.addCell(thCell("NO", Element.ALIGN_CENTER));
        table.addCell(thCell("STAFF NAME", Element.ALIGN_LEFT));
        table.addCell(thCell("DESIGNATION", Element.ALIGN_LEFT));
        table.addCell(thCell("DEPARTMENT / DUTY STATION", Element.ALIGN_LEFT));

        int idx = 1;
        for (OutOfOfficeAttendingOfficer ao : officers) {
            table.addCell(tdCell(String.valueOf(idx++), FONT_TD_BOLD, Element.ALIGN_CENTER));
            table.addCell(tdCell(safe(ao.getName()), FONT_TD_BOLD, Element.ALIGN_LEFT));
            table.addCell(tdCell(safe(ao.getDesignation()), FONT_TD, Element.ALIGN_LEFT));
            table.addCell(tdCell(safe(ao.getDutyStation()) + " · " + safe(ao.getDepartment()), FONT_TD, Element.ALIGN_LEFT));
        }
        return table;
    }

    private PdfPTable buildSectionBSummaryTable(OutOfOfficeNotification item) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{22f, 28f, 22f, 28f});
        table.setWidthPercentage(100f);

        OutOfOfficeReportBack rb = item.getReportBack();
        String actualReturn = rb != null && rb.getActualReturnDate() != null
                ? rb.getActualReturnDate().toString()
                : (item.getEndDate() != null ? item.getEndDate().toString() : "—");
        String submittedOn = rb != null && rb.getSubmittedAt() != null
                ? rb.getSubmittedAt().toString()
                : "Pending Submission";

        addKeyValRow(table,
                "Actual Return Date", actualReturn, false,
                "Report Submitted On", submittedOn, false);

        String stakeholders = rb != null && rb.getStakeholdersMet() != null && !rb.getStakeholdersMet().isBlank()
                ? rb.getStakeholdersMet()
                : safe(item.getHostOrganization());

        table.addCell(thCell("Stakeholders / Participants Met", Element.ALIGN_LEFT));
        PdfPCell spanVal = tdCell(stakeholders, FONT_TD, Element.ALIGN_LEFT);
        spanVal.setColspan(3);
        table.addCell(spanVal);

        return table;
    }

    private PdfPTable buildNumberedItemsTable(String columnHeader,
                                              List<String> items,
                                              String emptyText) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{8f, 92f});
        table.setWidthPercentage(100f);

        table.addCell(thCell("NO", Element.ALIGN_CENTER));
        table.addCell(thCell(columnHeader.toUpperCase(), Element.ALIGN_LEFT));

        if (items == null || items.isEmpty()) {
            PdfPCell empty = tdCell(emptyText, FONT_TD_MUTED, Element.ALIGN_CENTER);
            empty.setColspan(2);
            table.addCell(empty);
            return table;
        }

        int i = 1;
        for (String line : items) {
            table.addCell(tdCell(String.valueOf(i++), FONT_TD_BOLD, Element.ALIGN_CENTER));
            table.addCell(tdCell(safe(line), FONT_TD, Element.ALIGN_LEFT));
        }
        return table;
    }

    private PdfPTable buildManagementActionsTable(OutOfOfficeNotification item) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{8f, 48f, 26f, 18f});
        table.setWidthPercentage(100f);

        table.addCell(thCell("NO", Element.ALIGN_CENTER));
        table.addCell(thCell("FOLLOW UP ACTION", Element.ALIGN_LEFT));
        table.addCell(thCell("RESPONSIBLE OFFICER", Element.ALIGN_LEFT));
        table.addCell(thCell("TARGET DATE", Element.ALIGN_LEFT));

        OutOfOfficeReportBack rb = item.getReportBack();
        List<ReportBackManagementAction> actions = rb != null && rb.getManagementActions() != null
                ? rb.getManagementActions()
                : List.of();

        if (actions.isEmpty()) {
            if (item.getManagementActionRequired() != null && !item.getManagementActionRequired().isBlank()) {
                table.addCell(tdCell("1", FONT_TD_BOLD, Element.ALIGN_CENTER));
                table.addCell(tdCell(item.getManagementActionRequired(), FONT_TD, Element.ALIGN_LEFT));
                table.addCell(tdCell(safe(item.getOfficerName()), FONT_TD, Element.ALIGN_LEFT));
                table.addCell(tdCell(item.getEndDate() != null ? item.getEndDate().toString() : "—", FONT_TD, Element.ALIGN_LEFT));
            } else {
                PdfPCell empty = tdCell("No management follow-up actions recorded.", FONT_TD_MUTED, Element.ALIGN_CENTER);
                empty.setColspan(4);
                table.addCell(empty);
            }
            return table;
        }

        int i = 1;
        for (ReportBackManagementAction act : actions) {
            table.addCell(tdCell(String.valueOf(i++), FONT_TD_BOLD, Element.ALIGN_CENTER));
            table.addCell(tdCell(safe(act.getFollowUpAction()), FONT_TD, Element.ALIGN_LEFT));
            table.addCell(tdCell(safe(act.getResponsibleOfficer()), FONT_TD, Element.ALIGN_LEFT));
            table.addCell(tdCell(act.getTargetDate() != null ? act.getTargetDate().toString() : "—", FONT_TD, Element.ALIGN_LEFT));
        }
        return table;
    }

    private PdfPTable buildAttachmentsTable(OutOfOfficeNotification item) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{8f, 52f, 20f, 20f});
        table.setWidthPercentage(100f);

        table.addCell(thCell("NO", Element.ALIGN_CENTER));
        table.addCell(thCell("FILE NAME", Element.ALIGN_LEFT));
        table.addCell(thCell("DOCUMENT TYPE", Element.ALIGN_LEFT));
        table.addCell(thCell("UPLOADED DATE", Element.ALIGN_LEFT));

        OutOfOfficeReportBack rb = item.getReportBack();
        List<ReportBackAttachment> attachments = rb != null && rb.getAttachments() != null
                ? rb.getAttachments()
                : List.of();

        if (attachments.isEmpty()) {
            PdfPCell empty = tdCell("No attachments uploaded.", FONT_TD_MUTED, Element.ALIGN_CENTER);
            empty.setColspan(4);
            table.addCell(empty);
            return table;
        }

        int i = 1;
        for (ReportBackAttachment att : attachments) {
            table.addCell(tdCell(String.valueOf(i++), FONT_TD_BOLD, Element.ALIGN_CENTER));
            table.addCell(tdCell(safe(att.getName()), FONT_TD, Element.ALIGN_LEFT));
            table.addCell(tdCell(att.getKind() != null ? att.getKind().name() : "DOCUMENT", FONT_TD, Element.ALIGN_LEFT));
            table.addCell(tdCell(att.getUploadedAt() != null ? att.getUploadedAt().toString() : LocalDate.now().toString(), FONT_TD, Element.ALIGN_LEFT));
        }
        return table;
    }

    private PdfPTable buildSignOffTable(OutOfOfficeNotification item) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{22f, 78f});
        table.setWidthPercentage(100f);
        table.setSpacingBefore(12f);

        table.addCell(thCell("Submitted By (Officer)", Element.ALIGN_LEFT));

        PdfPCell officerCell = new PdfPCell();
        officerCell.setBorderColor(SLATE_300);
        officerCell.setPadding(6f);
        officerCell.addElement(new Paragraph(safe(item.getOfficerName()), FONT_TD_BOLD));
        officerCell.addElement(new Paragraph(safe(item.getOfficerRole()) + " · " + safe(item.getDutyStation()), FONT_TD_MUTED));
        table.addCell(officerCell);

        if ((item.getUnitHeadName() != null && !item.getUnitHeadName().isBlank())
                || (item.getUnitHeadApprovalStatus() != null
                && item.getUnitHeadApprovalStatus() != UnitHeadApprovalStatus.NOT_REQUIRED)) {
            table.addCell(thCell("Reviewed By (Unit Head)", Element.ALIGN_LEFT));
            PdfPCell uhCell = new PdfPCell();
            uhCell.setBorderColor(SLATE_300);
            uhCell.setPadding(6f);
            String reviewerName = item.getUnitHeadReviewedBy() != null && !item.getUnitHeadReviewedBy().isBlank()
                    ? item.getUnitHeadReviewedBy().trim()
                    : safe(item.getUnitHeadName());
            uhCell.addElement(new Paragraph(reviewerName, FONT_TD_BOLD));
            uhCell.addElement(new Paragraph(formatUnitHeadDecision(item), FONT_TD_MUTED));
            table.addCell(uhCell);
        }

        return table;
    }

    private PdfPTable buildFooterStrip(OutOfOfficeNotification item) throws Exception {
        PdfPTable footer = new PdfPTable(new float[]{55f, 45f});
        footer.setWidthPercentage(100f);
        footer.setSpacingBefore(10f);

        Phrase leftPhrase = new Phrase();
        leftPhrase.add(new Chunk("OPM · Department of Refugees", FONT_FOOTER_BOLD));
        leftPhrase.add(new Chunk(" · Staff Out-of-Office Tracker Verification Document", FONT_FOOTER));

        PdfPCell left = new PdfPCell(leftPhrase);
        left.setBorder(Rectangle.TOP);
        left.setBorderColorTop(SLATE_300);
        left.setPaddingTop(6f);
        footer.addCell(left);

        String stationClean = item.getDutyStation() != null
                ? item.getDutyStation().toUpperCase().replace(" ", "")
                : "OPM";
        String verifyId = "VERIFY-ID: " + (item.getId() != null ? item.getId() : item.getReferenceNumber()) + "-" + stationClean;

        PdfPCell right = new PdfPCell(new Phrase(verifyId, FONT_BARCODE_TEXT));
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setBorder(Rectangle.TOP);
        right.setBorderColorTop(SLATE_300);
        right.setPaddingTop(6f);
        footer.addCell(right);

        return footer;
    }

    private void addKeyValRow(PdfPTable table,
                              String k1, String v1, boolean bold1,
                              String k2, String v2, boolean bold2) {
        table.addCell(thCell(k1, Element.ALIGN_LEFT));
        table.addCell(tdCell(v1, bold1 ? FONT_TD_BOLD : FONT_TD, Element.ALIGN_LEFT));
        table.addCell(thCell(k2, Element.ALIGN_LEFT));
        table.addCell(tdCell(v2, bold2 ? FONT_TD_BOLD : FONT_TD, Element.ALIGN_LEFT));
    }

    private PdfPCell thCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBackgroundColor(SLATE_100);
        cell.setBorderColor(SLATE_300);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(5.5f);
        return cell;
    }

    private PdfPCell tdCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(SLATE_300);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(5.5f);
        return cell;
    }

    private List<String> extractPurposeList(OutOfOfficeNotification item) {
        if (item.getPurposeList() != null && !item.getPurposeList().isEmpty()) {
            return item.getPurposeList();
        }
        if (item.getPurpose() != null && !item.getPurpose().isBlank()) {
            return Arrays.stream(item.getPurpose().split("\\s*;\\s*|\\n+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return List.of();
    }

    private List<String> extractKeyIssues(OutOfOfficeNotification item) {
        OutOfOfficeReportBack rb = item.getReportBack();
        if (rb == null) return List.of();
        if (rb.getKeyIssuesList() != null && !rb.getKeyIssuesList().isEmpty()) {
            return rb.getKeyIssuesList();
        }
        if (rb.getOutstandingIssues() != null && !rb.getOutstandingIssues().isBlank()) {
            return Arrays.stream(rb.getOutstandingIssues().split("\\s*;\\s*|\\n+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return List.of();
    }

    private List<String> extractAchievements(OutOfOfficeNotification item) {
        OutOfOfficeReportBack rb = item.getReportBack();
        if (rb == null) return List.of();
        if (rb.getAchievementsList() != null && !rb.getAchievementsList().isEmpty()) {
            return rb.getAchievementsList();
        }
        if (rb.getWhatWasAchieved() != null && !rb.getWhatWasAchieved().isBlank()) {
            return Arrays.stream(rb.getWhatWasAchieved().split("\\s*;\\s*|\\n+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return List.of();
    }

    private static long computeDurationDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 1L;
        long diff = ChronoUnit.DAYS.between(start, end) + 1;
        return Math.max(1L, diff);
    }

    private static String formatEngagementType(EngagementType type) {
        if (type == null) return "Field Assignment";
        return switch (type) {
            case TRAVEL -> "Official Travel";
            case MEETING -> "External Meeting";
            case WORKSHOP -> "Workshop / Training";
            case FIELD_ASSIGNMENT -> "Field Assignment";
            case OFFICIAL_ENGAGEMENT -> "Official Engagement";
        };
    }

    private static String formatStatus(OutOfOfficeStatus status) {
        if (status == null) return "Currently Away";
        return switch (status) {
            case PENDING_APPROVAL -> "Pending Approval";
            case APPROVED -> "Approved (Upcoming)";
            case ACTIVE_AWAY -> "Currently Away";
            case RETURNED -> "Returned";
            case CANCELLED -> "Cancelled";
        };
    }

    private static String formatUnitHeadDecision(OutOfOfficeNotification item) {
        UnitHeadApprovalStatus st = item.getUnitHeadApprovalStatus();
        if (st == null || st == UnitHeadApprovalStatus.PENDING_UNIT_HEAD) {
            return "Pending Unit Head Acceptance";
        }
        String when = item.getUnitHeadReviewedAt() != null
                ? " on " + item.getUnitHeadReviewedAt().toLocalDate()
                : "";
        String note = item.getUnitHeadComment() != null && !item.getUnitHeadComment().isBlank()
                ? " (" + item.getUnitHeadComment().trim() + ")"
                : "";
        return switch (st) {
            case ACCEPTED -> "Accepted" + when + note + " · Forwarded to Overall Head";
            case REJECTED -> "Rejected" + when + note;
            case NOT_REQUIRED -> "Not Required";
            case PENDING_UNIT_HEAD -> "Pending Unit Head Acceptance";
        };
    }

    private static String safe(String val) {
        return val != null && !val.isBlank() ? val.trim() : "—";
    }

    /**
     * Renders the 21x21 verification QR Code directly onto the PDF canvas (matching view-report.ts).
     */
    private static class QrCodeCellRenderer implements PdfPCellEvent {
        private final OutOfOfficeNotification item;

        QrCodeCellRenderer(OutOfOfficeNotification item) {
            this.item = item;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
            cb.saveState();
            cb.setColorFill(SLATE_900);

            int size = 21;
            boolean[][] grid = new boolean[size][size];
            boolean[][] reserved = new boolean[size][size];

            drawFinder(grid, reserved, size, 0, 0);
            drawFinder(grid, reserved, size, size - 7, 0);
            drawFinder(grid, reserved, size, 0, size - 7);

            for (int i = 8; i < size - 8; i++) {
                reserved[6][i] = true;
                reserved[i][6] = true;
                grid[6][i] = (i % 2 == 0);
                grid[i][6] = (i % 2 == 0);
            }

            String payload = "SOOTR|" + item.getId() + "|" + item.getOfficerName() + "|"
                    + item.getDutyStation() + "|" + item.getStartDate() + "|" + item.getEndDate();
            int hash = (int) 2166136261L;
            for (int i = 0; i < payload.length(); i++) {
                hash ^= payload.charAt(i);
                hash *= 16777619;
            }

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (reserved[y][x]) continue;
                    hash ^= (x * 37 + y * 73 + payload.charAt((x + y) % payload.length())) & 0xff;
                    hash *= 16777619;
                    grid[y][x] = (Math.abs(hash) & 3) != 0 && (((x + y + (hash >>> 3)) & 1) == 1);
                }
            }

            float boxSize = Math.min(pos.getWidth(), pos.getHeight()) - 8f;
            float cellUnit = boxSize / size;
            float startX = pos.getLeft() + (pos.getWidth() - boxSize) / 2f;
            float startY = pos.getTop() - (pos.getHeight() - boxSize) / 2f;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (grid[y][x]) {
                        float rx = startX + x * cellUnit;
                        float ry = startY - (y + 1) * cellUnit;
                        cb.rectangle(rx, ry, cellUnit, cellUnit);
                    }
                }
            }
            cb.fill();
            cb.restoreState();
        }

        private void drawFinder(boolean[][] grid, boolean[][] reserved, int size, int ox, int oy) {
            for (int dy = -1; dy <= 7; dy++) {
                for (int dx = -1; dx <= 7; dx++) {
                    int x = ox + dx;
                    int y = oy + dy;
                    if (x < 0 || x >= size || y < 0 || y >= size) continue;
                    reserved[y][x] = true;
                    if (dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6) {
                        boolean onOuter = dx == 0 || dx == 6 || dy == 0 || dy == 6;
                        boolean onInner = dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4;
                        grid[y][x] = onOuter || onInner;
                    }
                }
            }
        }
    }

    /**
     * Renders Code-128 style verification barcode bars onto the PDF canvas (matching view-report.ts).
     */
    private static class BarcodeCellRenderer implements PdfPCellEvent {
        private final String code;

        BarcodeCellRenderer(String code) {
            this.code = code;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
            cb.saveState();
            cb.setColorFill(SLATE_900);

            float scaleX = (pos.getWidth() - 8f) / 148f;
            float baseLeft = pos.getLeft() + 4f;
            float barBottom = pos.getBottom() + 2f;
            float barHeight = pos.getHeight() - 4f;

            int cursor = 4;
            drawBar(cb, baseLeft, scaleX, barBottom, barHeight, cursor, 2);
            cursor += 3;
            drawBar(cb, baseLeft, scaleX, barBottom, barHeight, cursor, 1);
            cursor += 3;

            for (int i = 0; i < code.length(); i++) {
                int ch = code.charAt(i);
                int w1 = (ch % 3) + 1;
                int s1 = ((ch >> 2) % 2) + 1;
                int w2 = ((ch >> 3) % 2) + 1;
                int s2 = ((ch >> 4) % 2) + 1;
                if (cursor + w1 + s1 + w2 + s2 > 140) break;
                drawBar(cb, baseLeft, scaleX, barBottom, barHeight, cursor, w1);
                cursor += w1 + s1;
                drawBar(cb, baseLeft, scaleX, barBottom, barHeight, cursor, w2);
                cursor += w2 + s2;
            }

            if (cursor <= 142) {
                drawBar(cb, baseLeft, scaleX, barBottom, barHeight, cursor, 2);
                cursor += 3;
                drawBar(cb, baseLeft, scaleX, barBottom, barHeight, cursor, 1);
            }

            cb.fill();
            cb.restoreState();
        }

        private void drawBar(PdfContentByte cb, float baseLeft, float scaleX, float bottom, float height, int x, int w) {
            cb.rectangle(baseLeft + x * scaleX, bottom, w * scaleX, height);
        }
    }
}
