package com.roommake.admin.management.service;

import com.roommake.admin.management.dto.FaqForm;
import com.roommake.admin.management.mapper.FaqMapper;
import com.roommake.admin.management.vo.Faq;
import com.roommake.admin.management.vo.FaqCategory;
import com.roommake.dto.Criteria;
import com.roommake.dto.ListDto;
import com.roommake.dto.Pagination;
import com.roommake.user.mapper.UserMapper;
import com.roommake.user.vo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final UserMapper userMapper;
    private final FaqMapper faqMapper;

    /**
     * 자주묻는질문을 저장한다.
     *
     * @param form  저장할 질문에 대한 정보가 담긴 form객체
     * @param email 저장할 유저
     */
    public void createFaq(FaqForm form, String email) {

        User user = userMapper.getUserByEmail(email);
        FaqCategory faqCategory = FaqCategory.builder().id(form.getCategory()).build();

        Faq faq = Faq
                .builder()
                .title(form.getTitle())
                .content(form.getContent())
                .createDate(new Date())
                .updateDate(new Date())
                .category(faqCategory)
                .createByUser(user)
                .updateByUser(user)
                .build();

        faqMapper.createFaq(faq);
    }

    /**
     * 자주묻는질문을 수정한 후 해당 객체를 반환한다.
     *
     * @param id    수정할 자주묻는질문
     * @param form  수정할 정보가 담긴 form객체
     * @param email 수정할 유저
     * @return 수정된 자주묻는질문 객체
     */
    public Faq modifyFaq(int id, FaqForm form, String email) {

        Faq faq = faqMapper.getFaqById(id);
        User user = userMapper.getUserByEmail(email);
        FaqCategory faqCategory = faqMapper.getFaqCategory(form.getCategory());

        faq.setTitle(form.getTitle());
        faq.setContent(form.getContent());
        faq.setCategory(faqCategory);
        faq.setUpdateByUser(user);
        faq.setUpdateDate(new Date());

        faqMapper.modifyFaq(faq);

        return faq;
    }

    /**
     * 자주묻는질문을 삭제한다.
     *
     * @param id 삭제할 자주묻는질문
     */
    public void deleteFaq(int id) {
        Faq faq = faqMapper.getFaqById(id);
        faq.setDeleteYn("Y");
        faq.setDeleteDate(new Date());
        faqMapper.modifyFaq(faq);
    }

    /**
     * 페이징, 정렬, 검색, 필터링 후 자주묻는질문 리스트를 반환한다.
     *
     * @param criteria 리스트 반환 조건이 담긴 객체
     * @return 자주묻는질문 리스트
     */
    @Transactional(readOnly = true)
    public ListDto<Faq> getFaqs(Criteria criteria) {

        int totalRows = getTotalRows(criteria);

        Pagination pagination = new Pagination(criteria.getPage(), totalRows, criteria.getRows());

        criteria.setBegin(pagination.getBegin());
        criteria.setEnd(pagination.getEnd());

        List<Faq> faqs = faqMapper.getFaqs(criteria);

        ListDto<Faq> dto = new ListDto<>(faqs, pagination);
        return dto;
    }

    public int getTotalRows(Criteria criteria) {
        return faqMapper.getTotalRows(criteria);
    }

    /**
     * 페이징처리 없는 카테고리별 전체 자주묻는질문 리스트
     *
     * @param criteria
     * @return
     */
    public ListDto<Faq> getAllFaqs(Criteria criteria) {

        int totalRows = getTotalRows(criteria);

        criteria.setRows(getTotalRows(criteria));

        Pagination pagination = new Pagination(criteria.getPage(), totalRows, criteria.getRows());

        criteria.setBegin(pagination.getBegin());
        criteria.setEnd(pagination.getEnd());

        List<Faq> faqs = faqMapper.getFaqs(criteria);

        ListDto<Faq> dto = new ListDto<>(faqs, pagination);

        return dto;
    }

    /**
     * 자주묻는질문 id를 받아 해당 객체를 반환한다.
     *
     * @param id 자주묻는질문 id
     * @return 자주묻는질문 객체
     */
    @Transactional(readOnly = true)
    public Faq getFaqById(int id) {
        return faqMapper.getFaqById(id);
    }

    /**
     * 자주묻는질문 카테고리 리스트를 반환한다.
     *
     * @return 자주묻는질문 카테고리 리스트
     */
    @Transactional(readOnly = true)
    public List<FaqCategory> getFaqCategories() {

        return faqMapper.getFaqCategories();
    }

    /**
     * 카테고리 번호를 받아 카테고리를 반환한다.
     *
     * @param faqCatId 카테고리 번호
     * @return 카테고리 번호, 이름이 담긴 카테고리 객체
     */
    @Transactional(readOnly = true)
    public FaqCategory getFaqCategory(int faqCatId) {
        return faqMapper.getFaqCategory(faqCatId);
    }

    /**
     * 자주묻는질문 카테고리를 추가한다.
     *
     * @param name
     */
    public void createFaqCategory(String name) {
        faqMapper.createFaqCategory(name);
    }

    public void deleteFaqCategory(String name) {
        faqMapper.deleteFaqCategory(name);
    }
}
