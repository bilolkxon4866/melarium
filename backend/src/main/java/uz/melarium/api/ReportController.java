package uz.melarium.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import uz.melarium.api.dto.BeekeeperReportResponse;
import uz.melarium.api.dto.InvestmentResponse;
import uz.melarium.api.dto.InvestorReportResponse;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.InvestmentService;
import uz.melarium.service.ReportsService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportsService reportsService;
    private final InvestmentService investmentService;

    public ReportController(ReportsService reportsService, InvestmentService investmentService) {
        this.reportsService = reportsService;
        this.investmentService = investmentService;
    }

    @GetMapping("/investor")
    @PreAuthorize("hasRole('INVESTOR')")
    public InvestorReportResponse investor(@AuthenticationPrincipal AuthenticatedUser actor) {
        return reportsService.investor(actor);
    }

    @GetMapping("/investor/export.csv")
    @PreAuthorize("hasRole('INVESTOR')")
    public ResponseEntity<byte[]> exportInvestor(@AuthenticationPrincipal AuthenticatedUser actor) {
        StringBuilder csv = new StringBuilder("\uFEFFid,listing,location,units,amount,currency,status,created_at\r\n");
        for (InvestmentResponse item : investmentService.myOrders(actor)) {
            csv.append(escape(item.id().toString())).append(',').append(escape(item.listingTitle())).append(',')
                    .append(escape(item.location())).append(',').append(item.unitsQuantity()).append(',')
                    .append(item.amount()).append(',').append(item.currency()).append(',')
                    .append(item.status()).append(',').append(item.createdAt()).append("\r\n");
        }
        return csvResponse("melarium-investor-report.csv", csv.toString());
    }

    @GetMapping("/beekeeper")
    @PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
    public BeekeeperReportResponse beekeeper(@RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to,
                                              @AuthenticationPrincipal AuthenticatedUser actor) {
        return reportsService.beekeeper(actor, from, to);
    }

    @GetMapping("/beekeeper/export.csv")
    @PreAuthorize("hasAnyRole('BEEKEEPER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportBeekeeper(@RequestParam(required = false) LocalDate from,
                                                  @RequestParam(required = false) LocalDate to,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        BeekeeperReportResponse report = reportsService.beekeeper(actor, from, to);
        String csv = "\uFEFFmetric,value,from,to\r\n"
                + "apiary_count," + report.apiaryCount() + "," + report.from() + "," + report.to() + "\r\n"
                + "colony_count," + report.colonyCount() + "," + report.from() + "," + report.to() + "\r\n"
                + "active_listings," + report.activeListings() + "," + report.from() + "," + report.to() + "\r\n"
                + "pending_listings," + report.pendingListings() + "," + report.from() + "," + report.to() + "\r\n"
                + "harvest_kg," + report.harvestKg() + "," + report.from() + "," + report.to() + "\r\n"
                + "expenses_uzs," + report.expensesUzs() + "," + report.from() + "," + report.to() + "\r\n";
        return csvResponse("melarium-beekeeper-report.csv", csv);
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csv) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private String escape(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
