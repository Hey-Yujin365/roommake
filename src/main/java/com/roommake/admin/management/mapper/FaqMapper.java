package com.roommake.admin.management.mapper;

import com.roommake.admin.management.vo.Faq;
import com.roommake.admin.management.vo.FaqCategory;
import com.roommake.dto.Criteria;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FaqMapper {
    void createFaq(Faq faq);

    void modifyFaq(Faq faq);

    Faq getFaqById(int id);

    int getTotalRows(Criteria criteria);

    List<Faq> getFaqs(Criteria criteria);

    List<FaqCategory> getFaqCategories();

    FaqCategory getFaqCategory(int faqId);

    void createFaqCategory(String name);

    void deleteFaqCategory(String name);
}
