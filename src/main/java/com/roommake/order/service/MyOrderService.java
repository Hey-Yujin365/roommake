package com.roommake.order.service;

import com.roommake.dto.ListDto;
import com.roommake.dto.Pagination;
import com.roommake.order.dto.MyOrderCriteria;
import com.roommake.order.dto.OrderListDto;
import com.roommake.order.dto.UserOrderInfoDto;
import com.roommake.order.mapper.MyOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyOrderService {

    private final MyOrderMapper myOrderMapper;

    /**
     * 로그인한 유저의 모든 주문내역을 반환한다.
     *
     * @param userId 유저번호
     * @return 로그인한 유저의 모든 주문내역
     */
    @Transactional(readOnly = true)
    public ListDto<OrderListDto> getAllOrdersByUserId(int userId, MyOrderCriteria criteria) {

        int totalRows = myOrderMapper.getTotalRows(criteria, userId);
        Pagination pagination = new Pagination(criteria.getPage(), totalRows, criteria.getRows());
        criteria.setBegin(pagination.getBegin() - 1);
        criteria.setEnd(pagination.getEnd());

        if (totalRows > 0) {
            List<OrderListDto> orderList = myOrderMapper.getAllOrdersByUserId(userId, criteria);
            return new ListDto<OrderListDto>(orderList, pagination);
        }

        return new ListDto<OrderListDto>(List.of(), pagination);
    }

    /**
     * 로그인한 유저의 추천코드, 포인트, 구매등급을 반환한다.
     *
     * @param userId 유저번호
     * @return 로그인한 유저의 추천코드, 포인트, 구매등급
     */
    @Transactional(readOnly = true)
    public UserOrderInfoDto getUserOrderInfo(int userId) {
        return myOrderMapper.getUserOrderInfoByUserId(userId);
    }
}
