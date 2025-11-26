package com.barbershop.controller;

import com.barbershop.entity.Account;
import com.barbershop.entity.NhanVien;
import com.barbershop.repository.AccountRepository;
import com.barbershop.repository.CaLamRepository;
import com.barbershop.repository.NhanVienRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/nhanvien")
public class NhanVienController {

    @Autowired
    private NhanVienRepository nhanVienRepo;

    @Autowired
    private CaLamRepository caLamRepo;

    @Autowired
    private AccountRepository accountRepo;

    // =================== DANH SÁCH ====================
    @GetMapping
    public String list(Model model,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session) {

        if (session.getAttribute("user") == null)
            return "redirect:/login";

        List<NhanVien> list = (keyword != null && !keyword.trim().isEmpty())
                ? nhanVienRepo.search(keyword.trim())
                : nhanVienRepo.findAll();

        model.addAttribute("keyword", keyword);
        model.addAttribute("listNhanVien", list);
        return "nhanvien-list";
    }

    // =================== FORM THÊM ====================
    @GetMapping("/add")
    public String addForm(Model model, HttpSession session) {
        if (session.getAttribute("user") == null)
            return "redirect:/login";

        model.addAttribute("nhanVien", new NhanVien());
        model.addAttribute("listCaLam", caLamRepo.findAll());
        return "nhanvien-add";
    }

    // =================== XỬ LÝ THÊM ====================
    @PostMapping("/add")
    public String add(@ModelAttribute NhanVien nv) {

        // 1. Lưu nhân viên trước để lấy manv (ID)
        nhanVienRepo.save(nv);

        // 2. Tạo tài khoản tự động
        Account acc = new Account();
        acc.setUsername("nv" + nv.getManv()); // username auto
        acc.setPassword("123"); // password mặc định
        acc.setRole("STAFF"); // gán quyền nhân viên

        // 3. Lưu account
        accountRepo.save(acc);

        // 4. Gắn account vào nhân viên rồi lưu lại
        nv.setAccount(acc);
        nhanVienRepo.save(nv);

        return "redirect:/admin/nhanvien";
    }

    // =================== FORM SỬA ====================
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") int id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null)
            return "redirect:/login";

        NhanVien nv = nhanVienRepo.findById(id).orElse(null);

        model.addAttribute("nhanVien", nv);
        model.addAttribute("listCaLam", caLamRepo.findAll());
        return "nhanvien-edit";
    }

    // =================== XỬ LÝ SỬA ====================
    @PostMapping("/edit")
    public String edit(@ModelAttribute NhanVien nv) {
        nhanVienRepo.save(nv);
        return "redirect:/admin/nhanvien";
    }

    // =================== XÓA ====================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") int id) {
        nhanVienRepo.deleteById(id);
        return "redirect:/admin/nhanvien";
    }
}
