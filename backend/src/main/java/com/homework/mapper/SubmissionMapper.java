package com.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    @Select("SELECT AVG(total_score) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND deleted=0")
    BigDecimal selectAvgScore(@Param("assignmentId") Long assignmentId);

    @Select("SELECT MAX(total_score) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND deleted=0")
    BigDecimal selectMaxScore(@Param("assignmentId") Long assignmentId);

    @Select("SELECT MIN(total_score) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND deleted=0")
    BigDecimal selectMinScore(@Param("assignmentId") Long assignmentId);

    @Select("SELECT COUNT(*) FROM submissions WHERE assignment_id=#{assignmentId} AND status=4 AND total_score>=60 AND deleted=0")
    Integer selectPassCount(@Param("assignmentId") Long assignmentId);
}
