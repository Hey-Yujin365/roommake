package com.roommake.admin.management.controller;

import com.roommake.admin.management.dto.FaqForm;
import com.roommake.admin.management.service.FaqService;
import com.roommake.admin.management.vo.Faq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/management/faq")
@RequiredArgsConstructor
@Tag(name = "자주묻는질문 API", description = "자주묻는질문 CRUD API를 제공한다.")
public class FaqController {

    private final FaqService faqService;

    @Operation(summary = "자주묻는질문 등록", description = "자주묻는질문을 등록한다.")
    @PostMapping("/create")
    @ResponseBody
    public String create(FaqForm form, Principal principal) {

        faqService.createFaq(form, principal.getName());

        return "redirect:/admin/management/faq";
    }

    @Operation(summary = "자주묻는질문 수정", description = "자주묻는질문을 수정한다.")
    @PostMapping("/modify/{id}")
    @ResponseBody
    public Faq modify(@PathVariable("id") int id, FaqForm form, Principal principal) {

        return faqService.modifyFaq(id, form, principal.getName());
    }

    @Operation(summary = "자주묻는질문 삭제", description = "자주묻는질문을 삭제한다.")
    @PostMapping("/delete")
    @ResponseBody
    public String delete(@RequestBody List<Integer> faqIds) {
        for (Integer faqId : faqIds) {
            Faq faq = faqService.getFaqById(faqId);
            faqService.deleteFaq(faq.getId());
        }
        return "redirect:/admin/management/faq";
    }

    @Operation(summary = "자주묻는질문 상세정보 조회", description = "자주묻는질문 상세정보를 제공한다.")
    @GetMapping("/detail/{id}")
    @ResponseBody
    public Faq detail(@PathVariable("id") int id) {
        return faqService.getFaqById(id);
    }

    @Operation(summary = "자주묻는질문 카테고리 추가", description = "자주묻는질문 카테고리를 추가한다.")
    @GetMapping("/create/faqcategory")
    public String createFaqCategory(@RequestParam String name) {
        faqService.createFaqCategory(name);

        return "redirect:/admin/management/faq";
    }

    @Operation(summary = "자주묻는질문 카테고리 삭제", description = "자주묻는질문 카테고리를 삭제한다.")
    @GetMapping("/delete/faqcategory")
    public String deleteFaqCategory(@RequestParam String name) {
        faqService.deleteFaqCategory(name);

        return "redirect:/admin/management/faq";
    }
}
