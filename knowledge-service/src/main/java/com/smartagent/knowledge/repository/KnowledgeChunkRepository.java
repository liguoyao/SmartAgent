package com.smartagent.knowledge.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartagent.knowledge.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeChunkRepository extends BaseMapper<KnowledgeChunk> {
}
