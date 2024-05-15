package com.roommake.order.service;

import com.roommake.cart.dto.CartCreateForm;
import com.roommake.cart.dto.CartItemDto;
import com.roommake.cart.mapper.CartMapper;
import com.roommake.email.service.MailService;
import com.roommake.order.dto.OrderCreateForm;
import com.roommake.order.dto.OrderDto;
import com.roommake.order.dto.OrderItemDto;
import com.roommake.order.mapper.DeliveryMapper;
import com.roommake.order.mapper.OrderMapper;
import com.roommake.order.vo.Delivery;
import com.roommake.order.vo.Order;
import com.roommake.order.vo.OrderItem;
import com.roommake.order.vo.Payment;
import com.roommake.product.mapper.ProductMapper;
import com.roommake.product.vo.Product;
import com.roommake.product.vo.ProductDetail;
import com.roommake.user.enums.PointReasonEnum;
import com.roommake.user.mapper.UserMapper;
import com.roommake.user.vo.MinusPointHistory;
import com.roommake.user.vo.PointType;
import com.roommake.user.vo.User;
import com.roommake.user.vo.UserGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final DeliveryMapper deliveryMapper;
    private final MailService mailService;
    private final UserMapper userMapper;
    private final CartMapper cartMapper;

    /**
     * 장바구니에 담긴 상품의 정보를 반환한다.
     *
     * @param forms 장바구니 상품의 상품번호, 상품상세번호, 장바구니번호, 상품수량이 포함된 CartCreateForm 객체 리스트
     * @return 장바구니 상품의 상세한 정보가 포함된 CartItemDto 객체 리스트
     */
    @Transactional(readOnly = true)
    public List<CartItemDto> getProductsByDetailId(List<CartCreateForm> forms) {

        List<CartItemDto> list = new ArrayList<>();
        for (CartCreateForm form : forms) {
            CartItemDto dto = orderMapper.getProductsByDetailId(form);
            // form에서 받아온 장바구니번호, 상품수량을 dto에 저장
            dto.setCartId(form.getCartId());
            dto.setAmount(form.getAmount());
            list.add(dto);
        }

        return list;
    }

    /**
     * 로그인한 유저의 기본 배송지를 반환한다.
     *
     * @param userId 유저 번호
     * @return 로그인한 유저의 기본 배송지
     */
    @Transactional(readOnly = true)
    public Delivery getDefaultDeliveryByUserId(int userId) {

        return orderMapper.getDefaultDeliveryByUserId(userId);
    }

    /**
     * 신규 주문 정보가 저장된 orderCreateForm 객체를 전달받아서 주문정보와 주문상세정보 및 결제정보를 생성한다.
     * 포인트 사용 시 차감포인트내역 생성 및 유저 보유포인트를 갱신하고, 연결된 장바구니 상품을 삭제한다.
     *
     * @param tid             카카오페이 결제번호
     * @param orderCreateForm 신규 주문 정보가 포함된 orderCreateForm 객체
     * @param userId          유저 번호
     * @return 생성된 주문번호
     */
    @Transactional
    public int createOrder(String tid, OrderCreateForm orderCreateForm, int userId) {

        // 시작 시간 측정
        long beforeTime = System.currentTimeMillis();
        User user = userMapper.getUserById(userId);

        // 주문완료 메일 설정
        String to = user.getEmail();                     // 답변 알림 받을 이메일
        String subject = "주문이 완료되었습니다.";           // 메일 발송시 제목
        Map<String, Object> content = new HashMap<>();   // 메일 콘텐츠를 담을 Map 생성
        content.put("title", orderCreateForm.getName()); // html 템플릿에 적용될 콘텐츠 담기

        mailService.sendEmail(to, subject, "order-email", content);   // 메일 발송시 필요한 정보를 전달한다.

        // 1. 주문정보 생성
        Delivery delivery = deliveryMapper.getDeliveryById(orderCreateForm.getDeliveryId());

        Order order = new Order();      // orderId 없는 상태
        order.setUser(user);
        order.setTotalPrice(orderCreateForm.getTotalPrice());
        order.setDelivery(delivery);
        order.setDeliveryMemo(orderCreateForm.getDeliveryMemo());

        orderMapper.createOrder(order); // orderId 생성

        // 2. 주문상세정보 생성
        List<CartCreateForm> items = orderCreateForm.getItems();
        for (CartCreateForm form : items) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            Product product = productMapper.getProductById(form.getProductId());
            ProductDetail detail = ProductDetail.builder().id(form.getProductDetailId()).build();

            item.setProduct(product);
            item.setProductDetail(detail);
            item.setAmount(form.getAmount());
            item.setPrice(form.getItemPrice());

            orderMapper.createOrderItem(item);
        }

        // 3. 결제정보 생성
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPrice(order.getTotalPrice());
        payment.setUsePoint(orderCreateForm.getUsePoint());
        payment.setTid(tid);

        orderMapper.createPayment(payment);

        // 4. 차감포인트 생성
        if (orderCreateForm.getUsePoint() != 0) {
            MinusPointHistory history = new MinusPointHistory();
            history.setAmount(orderCreateForm.getUsePoint());
            history.setUser(user);
            history.setPayment(payment);
            history.setPointType(PointType.getPointType(9));
            history.setMinusPointReason("주문번호 : " + order.getId());

            orderMapper.createMinusPointHistory(history);

            // 5. 유저 보유포인트 갱신
            orderMapper.minusPointToUser(orderCreateForm.getUsePoint(), userId);
        }

        // 6. 장바구니 상품 삭제
        List<Integer> cartIds = new ArrayList<>();
        for (CartCreateForm form : orderCreateForm.getItems()) {
            cartIds.add(form.getCartId());
        }
        cartMapper.deleteCart(cartIds);

        // 실행시간을 보기 위해 log 출력
        long afterTime = System.currentTimeMillis();
        long diffTime = afterTime - beforeTime;
        // 메일발송 및 주문완료에 걸린 시간 조회
        log.info("주문 완료 후 메일발송 총 실행 시간: " + diffTime + "ms");

        return order.getId();
    }

    /**
     * 주문정보, 주문상세정보, 결제정보, 배송지정보를 반환한다.
     *
     * @param orderId 주문번호
     * @return 주문정보, 결제정보, 배송지정보, 주문상세정보가 담긴 OrderDto 객체
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderById(int orderId) {

        Payment payment = orderMapper.getPaymentByOrderId(orderId);
        Delivery delivery = orderMapper.getDeliveryByOrderId(orderId);
        List<OrderItemDto> items = orderMapper.getItemsByOrderId(orderId);

        OrderDto orderDto = orderMapper.getOrderById(orderId);
        orderDto.setUsePoint(payment.getUsePoint());
        orderDto.setPayment(payment);
        orderDto.setDelivery(delivery);
        orderDto.setItems(items);
        orderDto.setDeliveryMemo(orderDto.getDeliveryMemo());

        return orderDto;
    }

    /**
     * 주문상세번호와 주문금액을 전달받아서 주문상세의 주문상태를 '구매확정'으로 갱신하고, 적립포인트내역 생성 및 유저의 보유포인트를 갱신한다.
     *
     * @param orderItemId 주문상세 번호
     * @param orderPrice  주문상세 금액
     * @param userId      유저 번호
     */
    @Transactional
    public int confirmOrderItemById(int orderItemId, int orderPrice, int userId) {

        User user = userMapper.getUserById(userId);

        int gradeId = user.getUserGrade().getId();                 // 회원등급번호
        UserGrade grade = orderMapper.getUserGradeById(gradeId);   // 회원등급 객체
        int pointRate = grade.getPointRate();                      // 회원등급별 적립률
        int point = (int) (orderPrice * (pointRate / 100.0));      // 등급별 적립률과 주문금액을 고려한 실제 적립 포인트

        String reason = PointReasonEnum.CONFIRM_ORDER.getReason(); // 적립 상세사유 고정문구
        String pointReason = reason + orderItemId;                 // 적립 상세사유 고정문구 + 주문상세번호

        orderMapper.updateConfirmOrderItemById(orderItemId);
        orderMapper.createPlusPointHistory(point, userId, 7, pointReason);
        orderMapper.addPointToUser(point, userId);

        // 모든 주문상세 구매확정 시, 해당 주문도 구매확정으로 갱신
        orderMapper.updateConfirmOrder();

        return point;
    }

    /**
     * 유저번호를 전달받아서 유저 정보를 반환한다. (구매확정 시 사용)
     *
     * @param userId 유저번호
     * @return 유저정보가 담긴 User 객체
     */
    @Transactional(readOnly = true)
    public User getUserById(int userId) {
        return userMapper.getUserById(userId);
    }

    /**
     * 매일 자정에 배송완료 후 7일이 지난 주문내역의 주문상태를 '구매확정'으로 갱신하고, 적립포인트내역 생성 및 유저의 보유포인트를 갱신한다.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateAutoConfirmOrderItems() {
        List<Order> orders = orderMapper.getOrdersByStatus();
        for (Order order : orders) {
            int userId = order.getUser().getId();
            List<OrderItemDto> orderItems = orderMapper.getItemsByOrderId(order.getId());

            for (OrderItemDto item : orderItems) {
                this.confirmOrderItemById(item.getOrderItemId(), item.getItemPrice(), userId);
            }
        }
    }
}
