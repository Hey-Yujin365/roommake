package com.roommake.product.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/store")
public class ProductController {

    @GetMapping("/home")
    public String store() {
        return "store/home";
    }

    @GetMapping("/detail")
    public String detail() {
        return "store/product-detail";
    }

    @GetMapping("/category")
    public String category() {
        return "store/category-list";
    }

    // 스크랩 popup으로 이동하는 메소드
    @GetMapping("/popup")
    public String popup() {
        return "scrap-popup";
    }
}
