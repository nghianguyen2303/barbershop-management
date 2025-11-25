package com.barbershop.controller;

import com.barbershop.entity.*;
import com.barbershop.repository.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private NhanVienRepository nvRepo;

    @Autowired
    private LichHenRepository lichHenRepo;

    @Autowired
    private CaLamRepository caLamRepo;

    // =================== UTIL ===================

    // Lấy nhân viên từ account lưu trong session
    private NhanVien getNhanVienFromSession(HttpSession session) {
        Account acc = (Account) session.getAttribute("user");
        if (acc == null)
            return null;
        return nvRepo.findByAccount(acc);
    }

    // Nếu không phải nhân viên thì đá ra login
    private String checkStaffRole(HttpSession session) {
        Account acc = (Account) session.getAttribute("user");
        if (acc == null) {
            return "redirect:/login";
        }
        // Nếu bạn có field role trong Account thì có thể check:
        // if (!"STAFF".equalsIgnoreCase(acc.getRole())) return "redirect:/login";
        return null; // ok
    }

    // =================== TRANG HOME NHÂN VIÊN ===================
    // =================== TRANG HOME NHÂN VIÊN ===================

    @GetMapping("/home")
    public String homeStaff(Model model, HttpSession session) {

        // 1. Check quyền đăng nhập
        String redirect = checkStaffRole(session);
        if (redirect != null) {
            return redirect;
        }

        // 2. Lấy nhân viên từ session
        NhanVien nv = getNhanVienFromSession(session);
        LocalDate today = LocalDate.now();

        if (nv == null) {
            // Không gắn nhân viên -> vẫn trả về view nhưng không làm gì thêm
            model.addAttribute("today", today);
            model.addAttribute("errorMsg",
                    "Tài khoản này chưa được gắn với nhân viên trong hệ thống.");
            model.addAttribute("listKhachHang", Collections.emptyList());
            return "staff-home";
        }

        // 3. Lấy tất cả lịch hẹn của nhân viên này
        List<LichHen> all = lichHenRepo.findByNhanVien_Manv(nv.getManv());
        if (all == null) {
            all = Collections.emptyList();
        }

        // 4. Lọc ra danh sách khách hàng (duy nhất) đã từng hẹn với nhân viên
        Map<Integer, KhachHang> uniqueKhMap = new LinkedHashMap<>();
        for (LichHen lh : all) {
            if (lh == null)
                continue;
            KhachHang kh = lh.getKhachHang();
            if (kh == null || kh.getMakh() == null)
                continue;

            // dùng makh làm key để không trùng khách
            if (!uniqueKhMap.containsKey(kh.getMakh())) {
                uniqueKhMap.put(kh.getMakh(), kh);
            }
        }

        List<KhachHang> listKhachHang = new ArrayList<>(uniqueKhMap.values());

        // 5. Đẩy dữ liệu ra view
        model.addAttribute("nv", nv);
        model.addAttribute("today", today);
        model.addAttribute("listKhachHang", listKhachHang);

        return "staff-home";
    }

    // =================== CHỌN CA LÀM ===================

    // Trang chọn ca làm cho NV
    @GetMapping("/ca-lam")
    public String chonCaLamPage(Model model, HttpSession session) {

        String redirect = checkStaffRole(session);
        if (redirect != null) {
            return redirect;
        }

        NhanVien nv = getNhanVienFromSession(session);
        if (nv == null) {
            model.addAttribute("errorMsg",
                    "Tài khoản này chưa được gắn với nhân viên trong hệ thống.");
            return "staff-ca-lam";
        }

        List<CaLam> dsCa = caLamRepo.findAll();
        if (dsCa == null)
            dsCa = Collections.emptyList();

        model.addAttribute("nv", nv);
        model.addAttribute("dsCa", dsCa);

        return "staff-ca-lam";
    }

    // Nhân viên chọn một ca làm cụ thể
    @PostMapping("/ca-lam/chon")
    public String chonCaLam(@RequestParam("maCa") Integer maCa,
            HttpSession session,
            Model model) {

        String redirect = checkStaffRole(session);
        if (redirect != null) {
            return redirect;
        }

        NhanVien nv = getNhanVienFromSession(session);
        if (nv == null) {
            model.addAttribute("errorMsg",
                    "Tài khoản này chưa được gắn với nhân viên trong hệ thống.");
            return "staff-ca-lam";
        }

        if (maCa == null) {
            model.addAttribute("errorMsg", "Vui lòng chọn ca làm hợp lệ.");
            List<CaLam> dsCa = caLamRepo.findAll();
            model.addAttribute("nv", nv);
            model.addAttribute("dsCa", dsCa);
            return "staff-ca-lam";
        }

        CaLam ca = caLamRepo.findById(maCa).orElse(null);
        if (ca == null) {
            model.addAttribute("errorMsg", "Ca làm không tồn tại.");
            List<CaLam> dsCa = caLamRepo.findAll();
            model.addAttribute("nv", nv);
            model.addAttribute("dsCa", dsCa);
            return "staff-ca-lam";
        }

        nv.setCaLam(ca);
        nvRepo.save(nv);

        return "redirect:/staff/ca-lam";
    }
}
