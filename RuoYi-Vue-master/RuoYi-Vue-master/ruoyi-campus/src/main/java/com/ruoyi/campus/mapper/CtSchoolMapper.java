package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtSchool;

/**
 * 学校 数据层
 * 
 * @author campus
 */
public interface CtSchoolMapper
{
    public List<CtSchool> selectCtSchoolList(CtSchool ctSchool);

    public CtSchool selectCtSchoolById(Long schoolId);

    public int insertCtSchool(CtSchool ctSchool);

    public int updateCtSchool(CtSchool ctSchool);

    public int deleteCtSchoolById(Long schoolId);

    public int deleteCtSchoolByIds(Long[] schoolIds);
}
