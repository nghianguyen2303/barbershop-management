package com.barbershop.controller;

import com.barbershop.entity.LichHen;
import com.barbershop.entity.LichHenDichVu;
import com.barbershop.repository.LichHenRepository;
import com.barbershop.repository.LichHenDichVuRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/lichhen")
public class LichHenAdminController {

    @Autowired
    private LichHenRepository lichHenRepo;

    @Autowired
    private LichHenDichVuRepository lhDvRepo;

    // ========================= LIST + BỘ LỌC =========================
    @GetMapping
    public String list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model,
            HttpSession session) {

        if (session.getAttribute("user") == null)
            return "redirect:/login";

        // Load thông báo rồi xóa
        Object success = session.getAttribute("successMsg");
        Object error = session.getAttribute("errorMsg");

        if (success != null) {
            model.addAttribute("successMsg", success);
            session.removeAttribute("successMsg");
        }
        if (error != null) {
            model.addAttribute("errorMsg", error);
            session.removeAttribute("errorMsg");
        }

        // Chuẩn hóa keyword rỗng => null để query gọn
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        // Lấy danh sách theo bộ lọc
        List<LichHen> list = lichHenRepo.searchForAdmin(keyword, fromDate, toDate);

        // Map: maLh -> danh sách dịch vụ
        Map<Integer, List<LichHenDichVu>> mapDv = new HashMap<>();
        for (LichHen lh : list) {
            mapDv.put(
                    lh.getMaLh(),
                    lhDvRepo.findByLichHen_MaLh(lh.getMaLh()));
        }

        model.addAttribute("listLichHen", list);
        model.addAttribute("mapDichVu", mapDv);

        // Đẩy lại filter để hiển thị lại trên form
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "lichhen-admin-list";
    }

    // ========================= DELETE =========================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable int id, HttpSession session) {

        LichHen lh = lichHenRepo.findById(id).orElse(null);

        if (lh == null) {
            session.setAttribute("errorMsg", "❌ Lịch hẹn không tồn tại!");
            return "redirect:/admin/lichhen";
        }

        // Xóa dịch vụ trước
        lhDvRepo.deleteByLichHen_MaLh(id);

        // Xóa lịch hẹn
        lichHenRepo.deleteById(id);

        session.setAttribute("successMsg", "✔ Đã xóa lịch hẹn thành công!");

        return "redirect:/admin/lichhen";
    }
}
