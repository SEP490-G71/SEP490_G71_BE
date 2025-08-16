package vn.edu.fpt.medicaldiagnosis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemReportItem;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemStatisticResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.entity.InvoiceItem;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;
import vn.edu.fpt.medicaldiagnosis.repository.InvoiceItemRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalServiceRepository;
import vn.edu.fpt.medicaldiagnosis.service.impl.InvoiceItemServiceImpl;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class InvoiceItemServiceImplTest {

    @Mock
    InvoiceItemRepository invoiceItemRepository;

    @Mock
    MedicalServiceRepository medicalServiceRepository;

    @InjectMocks
    InvoiceItemServiceImpl service;

    Map<String, String> noFilter;

    @BeforeEach
    void setUp() {
        noFilter = new HashMap<>();
    }

    // ---------- Helpers ----------
    private MedicalService ms(String id, String code, String name, BigDecimal price) {
        MedicalService s = new MedicalService();
        s.setId(id);
        s.setServiceCode(code);
        s.setName(name);
        s.setPrice(price);
        return s;
    }

    private InvoiceItem item(String serviceId, String serviceCode, String name,
                             BigDecimal price, int qty,
                             BigDecimal discountPercent, BigDecimal vatPercent,
                             BigDecimal total) {
        MedicalService s = ms(serviceId, serviceCode, name, price);
        InvoiceItem it = new InvoiceItem();
        it.setService(s);
        it.setServiceCode(serviceCode);
        it.setName(name);
        it.setPrice(price);
        it.setQuantity(qty);
        it.setDiscount(discountPercent);
        it.setVat(vatPercent);
        it.setTotal(total);
        return it;
    }

    private List<MedicalService> services(MedicalService... arr) {
        return Arrays.asList(arr);
    }

    private List<InvoiceItem> items(InvoiceItem... arr) {
        return Arrays.asList(arr);
    }

    private Map<String, String> filters(String k, String v) {
        Map<String, String> m = new HashMap<>();
        m.put(k, v);
        return m;
    }

    // =========================================================
    // 1) Sort by serviceCode ASC
    // =========================================================
    @Test
    void sort_serviceCode_asc() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S2", "B02", "B", new BigDecimal("100"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100")),
                item("S1", "A01", "A", new BigDecimal("200"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("200")),
                ms("S2", "B02", "B", new BigDecimal("100"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        List<InvoiceItemReportItem> content = resp.getDetails().getContent();

        assertThat(content).extracting(InvoiceItemReportItem::getServiceCode)
                .containsExactly("A01", "B02");
    }

    // =========================================================
    // 2) Sort by name ASC (nulls last)
    // =========================================================
    @Test
    void sort_name_asc_nullsLast() {
        InvoiceItem i1 = item("S1", "C01", null, new BigDecimal("50"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50"));
        InvoiceItem i2 = item("S2", "B01", "Alpha", new BigDecimal("60"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("60"));
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(i1, i2));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "C01", null, new BigDecimal("50")),
                ms("S2", "B01", "Alpha", new BigDecimal("60"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "name", "asc");
        List<InvoiceItemReportItem> content = resp.getDetails().getContent();

        assertThat(content.get(0).getName()).isEqualTo("Alpha");
        assertThat(content.get(1).getName()).isNull(); // null đứng cuối
    }

    // =========================================================
    // 3) Sort by price DESC
    // =========================================================
    @Test
    void sort_price_desc() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "X01", "X", new BigDecimal("100"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100")),
                item("S2", "Y01", "Y", new BigDecimal("300"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("300"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "X01", "X", new BigDecimal("100")),
                ms("S2", "Y01", "Y", new BigDecimal("300"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "price", "desc");
        assertThat(resp.getDetails().getContent()).extracting(InvoiceItemReportItem::getPrice)
                .containsExactly(new BigDecimal("300"), new BigDecimal("100"));
    }

    // =========================================================
    // 4) Sort by total (alias of totalRevenue) ASC
    // =========================================================
    @Test
    void sort_total_asc() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("10"), 2, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20")),
                item("S2", "B01", "B", new BigDecimal("10"), 5, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10")),
                ms("S2", "B01", "B", new BigDecimal("10"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "total", "asc");
        assertThat(resp.getDetails().getContent()).extracting(InvoiceItemReportItem::getTotalRevenue)
                .containsExactly(new BigDecimal("20"), new BigDecimal("50"));
    }

    // =========================================================
    // 5) Sort by totalRevenue → (behavior hiện tại: thực chất đang sort theo totalUsage)
    // =========================================================
    @Test
    void sort_totalRevenue_currentlySortsByUsage_bugDocumented() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("10"), 2, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000")), // usage=2
                item("S2", "B01", "B", new BigDecimal("10"), 5, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1"))     // usage=5
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10")),
                ms("S2", "B01", "B", new BigDecimal("10"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "totalRevenue", "asc");
        // Nếu thật sự sort theo totalRevenue asc thì 1 < 1000 (S2 trước)
        // Nhưng code hiện tại map sang so sánh theo totalUsage → 2 < 5 (S1 trước)
        assertThat(resp.getDetails().getContent()).extracting(InvoiceItemReportItem::getServiceCode)
                .containsExactly("A01", "B01"); // chứng minh đang sort theo usage
    }

    // =========================================================
    // 6) Sort by totalUsage DESC
    // =========================================================
    @Test
    void sort_totalUsage_desc() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("10"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10")),
                item("S2", "B01", "B", new BigDecimal("10"), 10, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10")),
                ms("S2", "B01", "B", new BigDecimal("10"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "totalUsage", "desc");
        assertThat(resp.getDetails().getContent()).extracting(InvoiceItemReportItem::getTotalUsage)
                .containsExactly(10L, 1L);
    }

    // =========================================================
    // 7) Invalid sortBy → fallback serviceCode + cảnh báo (chỉ verify order)
    // =========================================================
    @Test
    void sort_invalid_fallback_serviceCode() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S2", "B", "B", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1")),
                item("S1", "A", "A", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A", "A", new BigDecimal("1")),
                ms("S2", "B", "B", new BigDecimal("1"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "___", "asc");
        assertThat(resp.getDetails().getContent()).extracting(InvoiceItemReportItem::getServiceCode)
                .containsExactly("A", "B");
    }

    // =========================================================
    // 8) Pagination: first page
    // =========================================================
    @Test
    void pagination_first_page() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A", "A", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1")),
                item("S2", "B", "B", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1")),
                item("S3", "C", "C", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A", "A", new BigDecimal("1")),
                ms("S2", "B", "B", new BigDecimal("1")),
                ms("S3", "C", "C", new BigDecimal("1"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 2, "serviceCode", "asc");
        PagedResponse<InvoiceItemReportItem> page = resp.getDetails();

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isLast()).isFalse();
    }

    // =========================================================
    // 9) Pagination: last page
    // =========================================================
    @Test
    void pagination_last_page() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A", "A", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1")),
                item("S2", "B", "B", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1")),
                item("S3", "C", "C", new BigDecimal("1"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A", "A", new BigDecimal("1")),
                ms("S2", "B", "B", new BigDecimal("1")),
                ms("S3", "C", "C", new BigDecimal("1"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 1, 2, "serviceCode", "asc");
        PagedResponse<InvoiceItemReportItem> page = resp.getDetails();

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.isLast()).isTrue();
    }

    // =========================================================
    // 10) departmentId → gọi findAllByDepartment_Id và ghép service 0-usage
    // =========================================================
    @Test
    void department_filter_calls_repo_and_includes_zero_usage_services() {
        Map<String, String> f = filters("departmentId", "DEP1");

        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("10"), 3, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30"))
        ));

        // chỉ trả service thuộc department
        when(medicalServiceRepository.findAllByDepartment_Id("DEP1")).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10")),
                ms("S2", "B01", "B", new BigDecimal("20")) // không có item → sẽ xuất hiện với usage=0
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(f, 0, 10, "serviceCode", "asc");
        assertThat(resp.getDetails().getContent()).extracting(InvoiceItemReportItem::getServiceCode)
                .containsExactly("A01", "B01");
        assertThat(resp.getDetails().getContent().get(1).getTotalUsage()).isZero();

        verify(medicalServiceRepository, times(1)).findAllByDepartment_Id("DEP1");
        verify(medicalServiceRepository, never()).findAll();
    }

    // =========================================================
    // 11) Không có departmentId → gọi findAll()
    // =========================================================
    @Test
    void no_department_calls_findAll() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        assertThat(resp.getDetails().getContent()).hasSize(1);
        verify(medicalServiceRepository, times(1)).findAll();
        verify(medicalServiceRepository, never()).findAllByDepartment_Id(anyString());
    }

    // =========================================================
    // 12) Gộp nhiều invoiceItem cùng serviceId & tính tổng
    // =========================================================
    @Test
    void grouping_accumulates_usage_and_amounts() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("50"), 2, new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("90")),
                item("S1", "A01", "A", new BigDecimal("50"), 3, new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("140"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(ms("S1", "A01", "A", new BigDecimal("50"))));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        InvoiceItemReportItem r = resp.getDetails().getContent().get(0);

        assertThat(r.getTotalUsage()).isEqualTo(5L);           // 2 + 3
        assertThat(r.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("230")); // 90 + 140 (theo item.total)
        // original = price*qty = 50*(2+3)=250
        assertThat(r.getTotalOriginal()).isEqualByComparingTo(new BigDecimal("250.00"));
        // discount = 10% * original = 25.00
        assertThat(r.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("25.00"));
        // vat = 8% * original = 20.00
        assertThat(r.getTotalVat()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    // =========================================================
    // 13) Null price/discount/vat → xử lý như 0
    // =========================================================
    @Test
    void nulls_treated_as_zero() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", null, 4, null, null, new BigDecimal("0"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(ms("S1", "A01", "A", null)));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        InvoiceItemReportItem r = resp.getDetails().getContent().get(0);

        assertThat(r.getTotalOriginal()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(r.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(r.getTotalVat()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    // =========================================================
    // 14) quantity = 0
    // =========================================================
    @Test
    void quantity_zero() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("100"), 0, new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("0"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(ms("S1", "A01", "A", new BigDecimal("100"))));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        InvoiceItemReportItem r = resp.getDetails().getContent().get(0);

        assertThat(r.getTotalUsage()).isZero();
        assertThat(r.getTotalOriginal()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(r.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(r.getTotalVat()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    // =========================================================
    // 15) mostUsedService behavior hiện tại: lấy phần tử đầu sau sort + filter usage>0
    //     → không đảm bảo là usage lớn nhất thật sự
    // =========================================================
    @Test
    void mostUsedService_current_behavior_not_true_max() {
        // A đứng trước theo serviceCode nhưng usage nhỏ hơn B
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("10"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10")), // usage=1
                item("S2", "B01", "B", new BigDecimal("10"), 100, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10")),
                ms("S2", "B01", "B", new BigDecimal("10"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        // Behavior hiện tại: mostUsedService là item đầu tiên có usage>0 (sau khi sort theo serviceCode)
        assertThat(resp.getMostUsedService().getServiceCode()).isEqualTo("A01");
        assertThat(resp.getMostUsedService().getTotalUsage()).isEqualTo(1L);

        // Tổng usage & revenue vẫn đúng
        assertThat(resp.getTotalUsage()).isEqualTo(101L);
        assertThat(resp.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("1010"));
    }

    // =========================================================
    // 16) Pagination: page vượt phạm vi -> content rỗng, isLast=true, totals vẫn đúng
    // =========================================================
    @Test
    void pagination_out_of_range_page_returns_empty_but_totals_correct() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A", "A", new BigDecimal("10"), 2, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20")),
                item("S2", "B", "B", new BigDecimal("15"), 3, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("45"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A", "A", new BigDecimal("10")),
                ms("S2", "B", "B", new BigDecimal("15"))
        ));

        // totalElements=2, size=2 -> totalPages=1, page=5 là vượt
        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 5, 2, "serviceCode", "asc");

        assertThat(resp.getDetails().getContent()).isEmpty();
        assertThat(resp.getDetails().isLast()).isTrue(); // (page+1)*size >= totalElements

        // Totals vẫn tính trên toàn bộ dataset, không phụ thuộc trang
        assertThat(resp.getTotalUsage()).isEqualTo(5L);
        assertThat(resp.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("65"));
    }

    // =========================================================
    // 17) departmentId = chuỗi trắng -> isBlank=true => dùng findAll(), không gọi theo department
    // =========================================================
    @Test
    void department_blank_string_calls_findAll_not_by_department() {
        Map<String, String> f = filters("departmentId", "   "); // blank
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10")),
                ms("S2", "B01", "B", new BigDecimal("20"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(f, 0, 10, "serviceCode", "asc");

        assertThat(resp.getDetails().getContent()).hasSize(2);
        verify(medicalServiceRepository, times(1)).findAll();
        verify(medicalServiceRepository, never()).findAllByDepartment_Id(anyString());
    }

    // =========================================================
    // 18) Rounding HALF_UP ở biên .005 cho discount% & vat%
    // =========================================================
    @Test
    void rounding_half_up_on_boundary_for_discount_and_vat() {
        // price=100, qty=1 => original=100
        // discount=12.345% of 100 => 12.345 -> round(2) HALF_UP => 12.35
        // vat=7.345% of 100 => 7.345 -> round(2) HALF_UP => 7.35
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A",
                        new BigDecimal("100"), 1,
                        new BigDecimal("12.345"),
                        new BigDecimal("7.345"),
                        new BigDecimal("100"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(ms("S1", "A01", "A", new BigDecimal("100"))));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        InvoiceItemReportItem r = resp.getDetails().getContent().get(0);

        assertThat(r.getTotalOriginal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(r.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("12.35"));
        assertThat(r.getTotalVat()).isEqualByComparingTo(new BigDecimal("7.35"));
    }

    // =========================================================
    // 19) Sort theo serviceCode ASC với serviceCode=null -> nullsLast
    // =========================================================
    @Test
    void sort_serviceCode_asc_nullsLast() {
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(
                item("S1", "A01", "A", new BigDecimal("10"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10")),
                // serviceCode=null
                item("S2", null, "B", new BigDecimal("20"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20"))
        ));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "A01", "A", new BigDecimal("10")),
                ms("S2", null, "B", new BigDecimal("20"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        List<InvoiceItemReportItem> content = resp.getDetails().getContent();

        assertThat(content.get(0).getServiceCode()).isEqualTo("A01");
        assertThat(content.get(1).getServiceCode()).isNull();
    }

    // =========================================================
    // 20) Nhiều item cùng serviceId nhưng meta khác nhau -> giữ meta của lần xuất hiện đầu (computeIfAbsent)
    // =========================================================
    @Test
    void grouping_keeps_first_item_metadata_for_same_serviceId() {
        // Hai item cùng serviceId="S1" nhưng serviceCode/name/price khác nhau
        InvoiceItem first = item("S1", "AA1", "NameFirst", new BigDecimal("50"), 1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50"));
        InvoiceItem second = item("S1", "BB2", "NameSecond", new BigDecimal("999"), 2, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1998"));
        when(invoiceItemRepository.findAll(any(Specification.class))).thenReturn(items(first, second));
        when(medicalServiceRepository.findAll()).thenReturn(services(
                ms("S1", "AA1", "NameFirst", new BigDecimal("50"))
        ));

        InvoiceItemStatisticResponse resp = service.getInvoiceItemStatistics(noFilter, 0, 10, "serviceCode", "asc");
        InvoiceItemReportItem r = resp.getDetails().getContent().get(0);

        // Giữ meta từ lần đầu computeIfAbsent (first)
        assertThat(r.getServiceCode()).isEqualTo("AA1");
        assertThat(r.getName()).isEqualTo("NameFirst");
        assertThat(r.getPrice()).isEqualByComparingTo(new BigDecimal("50"));

        // Tổng usage & revenue vẫn cộng dồn
        assertThat(r.getTotalUsage()).isEqualTo(3L);
        assertThat(r.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("2048")); // 50 + 1998
        // original = 50*1 + 999*2 = 2048.00
        assertThat(r.getTotalOriginal()).isEqualByComparingTo(new BigDecimal("2048.00"));
    }

}
