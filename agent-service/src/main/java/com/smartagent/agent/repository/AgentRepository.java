package com.smartagent.agent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartagent.agent.entity.Agent;
import com.smartagent.agent.entity.AgentType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * Agent mapper for database operations
 */
@Mapper
public interface AgentRepository extends BaseMapper<Agent> {

    @Select("SELECT * FROM agents WHERE is_active = true AND type = #{type}")
    List<Agent> findByType(@Param("type") AgentType type);

    @Select("SELECT * FROM agents WHERE is_active = true ORDER BY priority DESC")
    List<Agent> findAllActiveOrderByPriority();

    @Select("SELECT * FROM agents WHERE name = #{name} AND is_active = true")
    Optional<Agent> findByName(@Param("name") String name);

    @Select("SELECT * FROM agents WHERE id = #{id} AND is_active = true")
    Optional<Agent> findByIdActive(@Param("id") Long id);

    @Select("SELECT * FROM agents WHERE type = 'SUPERVISOR' AND is_active = true LIMIT 1")
    Optional<Agent> findSupervisorAgent();

    @Select("SELECT * FROM agents WHERE is_active = true AND type != 'SUPERVISOR' ORDER BY priority DESC")
    List<Agent> findAllSpecialistAgents();

    @Select("SELECT * FROM agents WHERE is_active = true AND type = #{type} ORDER BY priority DESC LIMIT 1")
    Optional<Agent> findBestAvailableAgent(@Param("type") AgentType type);
}
