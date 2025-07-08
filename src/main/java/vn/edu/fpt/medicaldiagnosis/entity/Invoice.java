package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@Table(name = "invoices")
public class Invoice extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "invoice_code", unique = true, nullable = false)
    private String invoiceCode;

//    @ManyToOne
//    @JoinColumn(name = "shift_id", nullable = false)
//    private CashierShift shift;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

//    @ManyToOne
//    @JoinColumn(name = "visit_id", nullable = false)
//    private PatientVisit visit;

    private BigDecimal total;

    @Column(name = "discount_total")
    private BigDecimal discountTotal;

    @Column(name = "vat_total")
    private BigDecimal vatTotal;

    @Column(name = "original_total")
    private BigDecimal originalTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @ManyToOne
    @JoinColumn(name = "confirmed_by")
    private Staff confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
