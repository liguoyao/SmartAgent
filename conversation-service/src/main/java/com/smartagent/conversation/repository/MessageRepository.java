package com.smartagent.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartagent.conversation.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message repository for database operations
 */
@Mapper
public interface MessageRepository extends BaseMapper<Message> {

    @Select("SELECT * FROM messages WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<Message> findBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM messages WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<Message> findByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT * FROM messages WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Message> findRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM messages WHERE session_id = #{sessionId}")
    Long countBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM messages WHERE sender_type = 'USER' AND created_at >= #{since} ORDER BY created_at ASC")
    List<Message> findUserMessagesSince(@Param("since") LocalDateTime since);
}
