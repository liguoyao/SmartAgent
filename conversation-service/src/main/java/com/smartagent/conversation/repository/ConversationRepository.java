package com.smartagent.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartagent.conversation.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Conversation repository for database operations
 */
@Mapper
public interface ConversationRepository extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversations WHERE session_id = #{sessionId} AND is_active = true LIMIT 1")
    Optional<Conversation> findBySessionIdActive(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM conversations WHERE user_id = #{userId} AND is_active = true ORDER BY last_message_at DESC")
    List<Conversation> findActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM conversations WHERE is_active = true ORDER BY last_message_at DESC")
    List<Conversation> findAllActive();

    @Select("SELECT * FROM conversations WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Conversation> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM conversations WHERE started_at >= #{startTime} AND is_active = true")
    List<Conversation> findConversationsSince(@Param("startTime") LocalDateTime startTime);
}
