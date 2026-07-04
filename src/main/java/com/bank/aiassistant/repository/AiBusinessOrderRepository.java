package com.bank.aiassistant.repository;

import com.bank.aiassistant.domain.entity.AiBusinessOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 业务订单对账 MyBatis Mapper。
 */
@Mapper
public interface AiBusinessOrderRepository {

    String BASE_COLUMNS = """
            order_id, pending_id, business_order_no, user_id, tool_name, status, created_time
            """;

    @Select("select count(1) > 0 from ai_business_order where pending_id = #{pendingId}")
    boolean existsByPendingId(String pendingId);

    @Select("select " + BASE_COLUMNS + " from ai_business_order where pending_id = #{pendingId}")
    Optional<AiBusinessOrder> findByPendingId(String pendingId);

    @Insert("""
            insert into ai_business_order (
              order_id, pending_id, business_order_no, user_id, tool_name, status, created_time
            ) values (
              #{orderId}, #{pendingId}, #{businessOrderNo}, #{userId}, #{toolName}, #{status}, #{createdTime}
            )
            """)
    int insert(AiBusinessOrder order);

    default AiBusinessOrder save(AiBusinessOrder order) {
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            order.setOrderId(UUID.randomUUID().toString());
        }
        if (order.getCreatedTime() == null) {
            order.setCreatedTime(LocalDateTime.now());
        }
        insert(order);
        return order;
    }
}
