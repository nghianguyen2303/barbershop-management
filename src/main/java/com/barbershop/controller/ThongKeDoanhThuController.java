package com.barbershop.controller;

import com.barbershop.entity.HoaDon;
import com.barbershop.entity.LichHen;
import com.barbershop.entity.LichHenDichVu;
import com.barbershop.repository.HoaDonRepository;
import com.barbershop.repository.LichHenDichVuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/thongke")
public class ThongKeDoanhThuController {

    @Autowired
    private LichHenDichVuRepository lhDvRepo;

    @Autowired
    private HoaDonRepository hoaDonRepo;

    // ============= TRANG THỐNG KÊ =============
    @GetMapping
    public String viewThongKe(
            @RequestParam(name = "nam", required = false) Integer nam,
            Model model) {

        if (nam == null)
            nam = Calendar.getInstance().get(Calendar.YEAR);

        List<Object[]> raw = hoaDonRepo.getDoanhThuTrongNam(nam);

        double[] doanhThu = new double[12];
        Arrays.fill(doanhThu, 0);

        for (Object[] row : raw) {
            int thang = (int) row[0];
            double tien = (double) row[1];
            doanhThu[thang - 1] = tien;
        }

        model.addAttribute("nam", nam);
        model.addAttribute("doanhThu", doanhThu);

        return "thongke-doanhthu";
    }

    // ============= API JSON (vẽ biểu đồ) =============
    @GetMapping("/chart-data")
    @ResponseBody
    public Map<String, Object> getChartData(@RequestParam("nam") int nam) {

        List<Object[]> raw = hoaDonRepo.getDoanhThuTrongNam(nam);

        double[] data = new double[12];
        Arrays.fill(data, 0);

        for (Object[] row : raw) {
            int month = (int) row[0];
            double amount = (double) row[1];
            data[month - 1] = amount;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("thang", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        map.put("doanhThu", data);

        return map;
    }

    // DTO đơn giản cho báo cáo
    public static class StatRow {
        private String ten;
        private long soLan;
        private double doanhThu;

        public StatRow(String ten) {
            this.ten = ten;
        }

        public String getTen() {
            return ten;
        }

        public void setTen(String ten) {
            this.ten = ten;
        }

        public long getSoLan() {
            return soLan;
        }

        public void setSoLan(long soLan) {
            this.soLan = soLan;
        }

        public double getDoanhThu() {
            return doanhThu;
        }

        public void setDoanhThu(double doanhThu) {
            this.doanhThu = doanhThu;
        }
    }

    // =============== BÁO CÁO DOANH THU THEO THÁNG (CHI TIẾT) ===============
    @GetMapping("/month")
    public String thongKeTheoThang(
            @RequestParam(name = "nam", required = false) Integer nam,
            @RequestParam(name = "thang", required = false) Integer thang,
            Model model) {

        LocalDate now = LocalDate.now();
        if (nam == null)
            nam = now.getYear();
        if (thang == null)
            thang = now.getMonthValue();

        // 1. Lấy tất cả hóa đơn trong tháng
        List<HoaDon> dsHoaDon = hoaDonRepo.findByMonthAndYear(nam, thang);

        double tongDoanhThu = 0d;
        int soHoaDon = dsHoaDon.size();

        // 2. Doanh thu theo ngày
        Map<Integer, Double> doanhThuTheoNgay = new TreeMap<>();

        // 3. Map thống kê chi tiết
        // key = tên dịch vụ / nhân viên / khách / phương thức
        Map<String, StatRow> serviceStatMap = new HashMap<>();
        Map<String, StatRow> staffStatMap = new HashMap<>();
        Map<String, StatRow> customerStatMap = new HashMap<>();
        Map<String, StatRow> paymentStatMap = new HashMap<>();

        for (HoaDon hd : dsHoaDon) {
            if (hd.getNgayThanhToan() == null)
                continue;

            double tienHd = hd.getTongTien() != null ? hd.getTongTien() : 0d;
            tongDoanhThu += tienHd;

            // --- Doanh thu theo ngày ---
            int day = hd.getNgayThanhToan().getDayOfMonth();
            doanhThuTheoNgay.put(day,
                    doanhThuTheoNgay.getOrDefault(day, 0d) + tienHd);

            // --- Theo phương thức thanh toán ---
            String pt = hd.getPhuongThucTt() != null ? hd.getPhuongThucTt() : "Khác";
            StatRow ptRow = paymentStatMap.get(pt);
            if (ptRow == null) {
                ptRow = new StatRow(pt);
                paymentStatMap.put(pt, ptRow);
            }
            ptRow.setSoLan(ptRow.getSoLan() + 1);
            ptRow.setDoanhThu(ptRow.getDoanhThu() + tienHd);

            // Lấy lịch hẹn gắn với hóa đơn
            LichHen lh = hd.getLichHen();

            if (lh != null) {
                // --- Theo nhân viên ---
                if (lh.getNhanVien() != null) {
                    String tenNv = lh.getNhanVien().getHoTen();
                    if (tenNv == null)
                        tenNv = "Không rõ";

                    StatRow nvRow = staffStatMap.get(tenNv);
                    if (nvRow == null) {
                        nvRow = new StatRow(tenNv);
                        staffStatMap.put(tenNv, nvRow);
                    }
                    nvRow.setSoLan(nvRow.getSoLan() + 1); // 1 lịch hẹn = 1 lần phục vụ
                    nvRow.setDoanhThu(nvRow.getDoanhThu() + tienHd);
                }

                // --- Theo khách hàng ---
                if (lh.getKhachHang() != null) {
                    String tenKh = lh.getKhachHang().getHoTen();
                    if (tenKh == null)
                        tenKh = "Khách lẻ";

                    StatRow khRow = customerStatMap.get(tenKh);
                    if (khRow == null) {
                        khRow = new StatRow(tenKh);
                        customerStatMap.put(tenKh, khRow);
                    }
                    khRow.setSoLan(khRow.getSoLan() + 1); // số lần đến trong tháng
                    khRow.setDoanhThu(khRow.getDoanhThu() + tienHd);
                }

                // --- Theo dịch vụ ---
                if (lh.getMaLh() != null) {
                    List<LichHenDichVu> dsDv = lhDvRepo.findByLichHen_MaLh(lh.getMaLh());
                    for (LichHenDichVu item : dsDv) {
                        if (item.getDichVu() == null)
                            continue;

                        String tenDv = item.getDichVu().getTenDv();
                        if (tenDv == null)
                            tenDv = "Dịch vụ khác";

                        double giaDv = item.getDichVu().getGia() != null ? item.getDichVu().getGia() : 0d;

                        StatRow dvRow = serviceStatMap.get(tenDv);
                        if (dvRow == null) {
                            dvRow = new StatRow(tenDv);
                            serviceStatMap.put(tenDv, dvRow);
                        }
                        dvRow.setSoLan(dvRow.getSoLan() + 1); // 1 dịch vụ / lần
                        dvRow.setDoanhThu(dvRow.getDoanhThu() + giaDv);
                    }
                }
            }
        }

        double doanhThuTrungBinh = soHoaDon > 0 ? tongDoanhThu / soHoaDon : 0d;

        // Chuyển map -> list và sắp xếp giảm dần theo doanh thu
        List<StatRow> topDichVu = new ArrayList<>(serviceStatMap.values());
        topDichVu.sort((a, b) -> Double.compare(b.getDoanhThu(), a.getDoanhThu()));

        List<StatRow> nhanVienStats = new ArrayList<>(staffStatMap.values());
        nhanVienStats.sort((a, b) -> Double.compare(b.getDoanhThu(), a.getDoanhThu()));

        List<StatRow> topKhachHang = new ArrayList<>(customerStatMap.values());
        topKhachHang.sort((a, b) -> Double.compare(b.getDoanhThu(), a.getDoanhThu()));
        // Nếu muốn chỉ lấy top 5 khách:
        if (topKhachHang.size() > 5) {
            topKhachHang = topKhachHang.subList(0, 5);
        }

        List<StatRow> paymentStats = new ArrayList<>(paymentStatMap.values());
        paymentStats.sort((a, b) -> Double.compare(b.getDoanhThu(), a.getDoanhThu()));

        // Lấy danh sách năm có dữ liệu để fill combobox
        List<Integer> dsNam = new ArrayList<>();
        for (Object[] row : hoaDonRepo.getDoanhThuTheoNam()) {
            dsNam.add(((Number) row[0]).intValue());
        }

        model.addAttribute("nam", nam);
        model.addAttribute("thang", thang);
        model.addAttribute("dsNam", dsNam);

        model.addAttribute("tongDoanhThu", tongDoanhThu);
        model.addAttribute("soHoaDon", soHoaDon);
        model.addAttribute("doanhThuTrungBinh", doanhThuTrungBinh);
        model.addAttribute("doanhThuTheoNgay", doanhThuTheoNgay);
        model.addAttribute("dsHoaDon", dsHoaDon);

        // Thống kê chi tiết
        model.addAttribute("topDichVu", topDichVu);
        model.addAttribute("nhanVienStats", nhanVienStats);
        model.addAttribute("topKhachHang", topKhachHang);
        model.addAttribute("paymentStats", paymentStats);

        return "thongke-doanhthu-thang";
    }
}
