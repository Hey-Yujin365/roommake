package com.roommake.order.service;

import com.roommake.order.dto.*;
import com.roommake.order.mapper.DeliveryMapper;
import com.roommake.order.mapper.OrderClaimMapper;
import com.roommake.order.mapper.OrderMapper;
import com.roommake.order.vo.*;
import com.roommake.product.mapper.ProductMapper;
import com.roommake.product.vo.ProductDetail;
import com.roommake.user.enums.PointReasonEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderClaimService {

    private final OrderClaimMapper orderClaimMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final DeliveryMapper deliveryMapper;

    /**
     * 모든 주문취소사유를 반환한다.
     *
     * @return 모든 주문취소사유
     */
    @Transactional(readOnly = true)
    public List<OrderCancelReason> getAllCancelReasons() {

        return orderClaimMapper.getAllCancelReasons();
    }

    /**
     * 주문취소/반품/교환을 신청할 내역에 대한 주문정보, 결제정보, 배송지정보, 주문상세정보를 반환한다.
     *
     * @param orderId     주문번호
     * @param orderItemId 주문상세번호
     * @return 주문정보, 결제정보, 배송지정보, 주문상세정보(객체/리스트)가 담긴 OrderDto 객체
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderClaimByOrderId(int orderId, int orderItemId) {

        Payment payment = orderMapper.getPaymentByOrderId(orderId);
        Delivery delivery = orderMapper.getDeliveryByOrderId(orderId);
        OrderItemDto item = orderClaimMapper.getOrderItemDtoByOrderItemId(orderItemId);
        ProductDetail detail = orderClaimMapper.getProductDetailById(item.getProductDetailId());

        OrderDto orderDto = orderMapper.getOrderById(orderId);
        orderDto.setPayment(payment);
        orderDto.setDelivery(delivery);
        orderDto.setItem(item);
        orderDto.setDetail(detail);

        List<ProductDetail> details = productMapper.getProductDetailById(item.getProductId());
        orderDto.setDetails(details);

        return orderDto;
    }

    /**
     * 신규 주문취소 정보가 저장된 orderCancelForm 객체를 전달받아서 주문취소정보, 환불정보 생성 및 주문과 주문상세의 상태를 갱신하고,
     * 사용한 포인트가 있을 때, 적립포인트내역을 생성하고 유저의 보유포인트를 갱신한다.
     *
     * @param orderCancelForm 신규 주문취소 정보가 포함된 orderCancelForm 객체
     * @param userId          유저번호
     */
    @Transactional
    public void createOrderCancel(OrderCancelForm orderCancelForm, int userId) {

        // 1-1. OrderCancel 객체에 저장할 Order, OrderCancelReason 객체 획득
        Order order = new Order();
        order.setId(orderCancelForm.getOrderId());
        OrderCancelReason reason = orderClaimMapper.getCancelReasonById(orderCancelForm.getReasonId());

        // 1-2. 주문취소정보 생성
        OrderCancel orderCancel = new OrderCancel();
        orderCancel.setOrder(order);
        orderCancel.setReason(reason);

        orderClaimMapper.createOrderCancel(orderCancel);

        // 2-1. Refund 객체에 저장할 Payment 객체 획득
        Payment payment = new Payment();
        payment.setId(orderCancelForm.getPaymentId());

        // 2-2. 환불정보 생성
        Refund refund = new Refund();
        refund.setAmount(orderCancelForm.getTotalPrice());
        refund.setPayment(payment);

        orderClaimMapper.createCancelRefund(refund);

        // 3. 주문상태 컬럼 갱신
        orderClaimMapper.updateCancelOrderStatus(orderCancelForm.getOrderId());
        orderClaimMapper.updateCancelOrderItemStatus(orderCancelForm.getOrderId());

        // 4. 적립포인트내역 생성 및 유저의 보유포인트 갱신
        if (orderCancelForm.getUsePoint() != 0) {

            String pointReason = PointReasonEnum.CANCEL_ORDER.getReason();     // 적립 상세사유 고정문구
            String cancelReason = pointReason + orderCancelForm.getOrderId(); // 적립 상세사유 고정문구 + 주문번호

            orderMapper.createPlusPointHistory(orderCancelForm.getUsePoint(), userId, 8, cancelReason);
            orderMapper.addPointToUser(orderCancelForm.getUsePoint(), userId);
        }
    }

    /**
     * 주문취소정보, 주문취소사유, 결제정보, 환불정보, 주문상세정보를 반환한다.
     *
     * @param orderId 주문번호
     * @return 주문취소정보, 주문취소사유정보, 결제정보, 환불정보, 주문상세정보가 담긴 OrderCancelDto 객체
     */
    @Transactional(readOnly = true)
    public OrderCancelDto getOrderCancelByOrderId(int orderId) {

        Payment payment = orderMapper.getPaymentByOrderId(orderId);
        Refund refund = orderClaimMapper.getRefundByPaymentId(payment.getId());
        List<OrderItemDto> items = orderMapper.getItemsByOrderId(orderId);

        OrderCancelDto orderCancelDto = orderClaimMapper.getOrderCancelByOrderId(orderId);
        OrderCancelReason Reason = orderClaimMapper.getCancelReasonByCancelId(orderCancelDto.getOrderCancelId());

        orderCancelDto.setPayment(payment);
        orderCancelDto.setRefund(refund);
        orderCancelDto.setReason(Reason);
        orderCancelDto.setItems(items);

        return orderCancelDto;
    }

    /**
     * 모든 반품교환사유를 반환한다.
     *
     * @return 모든 반품교환사유
     */
    @Transactional(readOnly = true)
    public List<ReturnExchangeReason> getAllReturnExchangeReasons() {

        return orderClaimMapper.getAllReturnExchangeReasons();
    }

    /**
     * 신규 반품/교환 정보가 저장된 ReturnExchangeCreateForm 객체를 전달받아서 반품/교환정보 생성 및 주문상세의 상태를 갱신한다.
     *
     * @param form 신규 반품/교환 정보가 포함된 ReturnExchangeCreateForm 객체
     */
    @Transactional
    public void createReturnExchange(ReturnExchangeCreateForm form) {

        if (form.getType().equals("return")) {
            // 반품처리
            orderClaimMapper.createItemReturn(form);
            int statusId = 6;
            orderClaimMapper.updateClaimOrderItemStatus(form.getOrderItemId(), statusId);
        } else {
            // 교환처리
            // 1. 교환에 필요한 객체들 획득 및 저장 후 교환정보 생성
            Exchange exchange = new Exchange();       // exchangeId 없는 상태

            OrderItem item = orderClaimMapper.getOrderItemByOrderItemId(form.getOrderItemId());
            ReturnExchangeReason reason = orderClaimMapper.getReturnExchangeReasonById(form.getReasonId());
            Delivery collectionDelivery = deliveryMapper.getDeliveryById(form.getCollectionDeliveryId());
            Delivery reDelivery = deliveryMapper.getDeliveryById(form.getReDeliveryId());

            exchange.setOrderItem(item);
            exchange.setReason(reason);
            exchange.setCollectionDelivery(collectionDelivery);
            exchange.setReDelivery(reDelivery);
            exchange.setCollectionMemo(form.getCollectionMemo());
            exchange.setDeliveryMemo(form.getDeliveryMemo());
            exchange.setDetailedReason(form.getDetailedReason());

            orderClaimMapper.createExchange(exchange); // exchangeId 생성

            // 2. 교환상세에 필요한 객체들 획득 및 저장 후 교환상세정보 생성
            ExchangeDetail exchangeDetail = new ExchangeDetail();

            ProductDetail beforeProductDetail = orderClaimMapper.getProductDetailById(form.getBeforeDetailId());
            ProductDetail afterProductDetail = orderClaimMapper.getProductDetailById(form.getAfterDetailId());

            exchangeDetail.setExchange(exchange);
            exchangeDetail.setBeforeProductDetail(beforeProductDetail);
            exchangeDetail.setAfterProductDetail(afterProductDetail);

            orderClaimMapper.createExchangeDetail(exchangeDetail);

            // 3. 주문상세 상태 갱신
            int statusId = 8;
            orderClaimMapper.updateClaimOrderItemStatus(form.getOrderItemId(), statusId);
        }
    }

    /**
     * 반품정보, 반품사유정보, 회수지정보, 주문상세정보를 반환한다.
     *
     * @param itemId 주문상세번호
     * @return 반품정보, 반품사유정보, 회수지정보, 주문상세정보가 담긴 ReturnExchangeDto 객체
     */
    @Transactional(readOnly = true)
    public ReturnExchangeDto getItemReturnByOrderItemId(int itemId) {

        OrderItemDto item = orderClaimMapper.getOrderItemDtoByOrderItemId(itemId);
        ReturnExchangeDto dto = orderClaimMapper.getItemReturnByOrderItemId(itemId);
        Delivery delivery = orderClaimMapper.getReturnCollectionDeliveryByReturnId(dto.getItemReturnId());
        ReturnExchangeReason reason = orderClaimMapper.getReturnReasonByReturnId(dto.getItemReturnId());

        dto.setCollectionDelivery(delivery);
        dto.setItem(item);
        dto.setReason(reason);

        return dto;
    }

    /**
     * 교환정보, 교환상세정보, 주문상세정보, 교환사유정보, 회수지정보, 재배송지정보를 반환한다.
     *
     * @param itemId 주문상세번호
     * @return 교환정보, 교환상세정보, 주문상세정보, 교환사유정보, 회수지정보, 재배송지정보가 담긴 ReturnExchangeDto 객체
     */
    @Transactional(readOnly = true)
    public ReturnExchangeDto getExchangeByOrderItemId(int itemId) {

        ReturnExchangeDto dto = orderClaimMapper.getExchangeByOrderItemId(itemId);
        OrderItemDto item = orderClaimMapper.getOrderItemDtoByOrderItemId(itemId);
        ReturnExchangeReason reason = orderClaimMapper.getExchangeReasonByExchangeId(dto.getExchangeId());
        Delivery collectionDelivery = deliveryMapper.getDeliveryById(dto.getCollectionDelivery().getId());
        Delivery reDelivery = deliveryMapper.getDeliveryById(dto.getReDelivery().getId());

        // 교환상세정보에 전/후 상품 저장
        ExchangeDetail exchangeDetail = orderClaimMapper.getExchangeDetailByExchangeId(dto.getExchangeId());
        ProductDetail beforeDetail = orderClaimMapper.getProductDetailById(exchangeDetail.getBeforeProductDetail().getId());
        ProductDetail afterDetail = orderClaimMapper.getProductDetailById(exchangeDetail.getAfterProductDetail().getId());
        exchangeDetail.setBeforeProductDetail(beforeDetail);
        exchangeDetail.setAfterProductDetail(afterDetail);

        dto.setExchangeDetail(exchangeDetail);
        dto.setItem(item);
        dto.setReason(reason);
        dto.setCollectionDelivery(collectionDelivery);
        dto.setReDelivery(reDelivery);

        return dto;
    }
}
