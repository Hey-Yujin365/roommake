package com.roommake.admin.management.controller;

import com.roommake.admin.management.service.QnaService;
import com.roommake.admin.management.vo.Qna;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/management/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @GetMapping("/detail/{id}")
    @ResponseBody
    public Qna detail(@PathVariable("id") int id) {
        Qna qna = qnaService.getQnaById(id);
        return qna;
    }
//    public Qna writeAnswer(@RequestParam("answer") String answer,
//                           Principal principal,
//                           Model model) {
//        qnaService.updateAnswer();
//    }

//    @RequestMapping("/mailSend")
//    public void mailSend(@RequestBody QnaMailDto mailDto, QnaAnswerForm form) {
//        mailDto.setSubject(form.getTitle());
//        mailDto.setTo("realgyj@gmail.com");
//        mailDto.setTemplate("admin/management/answer-email");
//
//        Map mailMap = new HashMap();
//        mailMap.put("qna", "어떤 제목");
//        mailDto.setContext(mailMap);
//        try {
//            qnaService.sendMail(mailDto);
//        } catch (Exception ex) {
//            throw new RuntimeException("이메일발송실패");
//        }
//    }
}