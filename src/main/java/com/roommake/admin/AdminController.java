package com.roommake.admin;

import com.roommake.admin.Dashboard.dto.DashboardDto;
import com.roommake.admin.Dashboard.dto.OrderStatusData;
import com.roommake.admin.Dashboard.service.DashBoardService;
import com.roommake.admin.Dashboard.vo.SalesData;
import com.roommake.admin.product.service.AdminProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@Tag(name = "관리자 API", description = "관리자 대시보드 데이터 관련 CRUD API를 제공한다.")
public class AdminController {
    private final AdminProductService adminProductService;
    private final DashBoardService dashBoardService;

    @GetMapping("/home")
    public String adminHome(Model model) {

        DashboardDto dto = dashBoardService.getAdminHomeDto();

        // 대시보드용 데이터
        model.addAttribute("today", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        model.addAttribute("orderStatusDataList", dto.getOrderStatusDataList());
        model.addAttribute("salesDataList", dto.getSalesDataList());
        model.addAttribute("noConfirmComplaints", dto.getNoConfirmComplaints());
        model.addAttribute("noAnswerQnas", dto.getNoAnswerQnas());
        model.addAttribute("newUserCnt", dto.getNewUserCnt());
        model.addAttribute("totalUserCnt", dto.getTotalUserCnt());

        return "admin/home";
    }

    @ResponseBody
    @GetMapping("/salesData")
    public List<SalesData> salesData() {

        return dashBoardService.getSalesData(LocalDate.now().minusDays(1).toString(), 7);
    }

    @ResponseBody
    @GetMapping("/orderStatusData")
    public List<OrderStatusData> orderStatusData() {

        return dashBoardService.getOrderStatusData(LocalDate.now().toString());
    }
}
