package com.ygyin.coop.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * 图片表分表实现
 */
public class ImageShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     * 分表逻辑
     *
     * @param availableTargetNames 所有支持可用的分表表名列表，默认获取配置中的 actual-data-nodes 的目标表
     * @param preciseShardingValue 用于分表的列名
     * @return 实际要查的表名
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        Long areaId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        // areaId 为 null 表示要查询所有图片，返回整个逻辑表
        if (areaId == null)
            return logicTableName;

        // 根据 areaId 动态生成物理分表名
        String realTableName = "image_" + areaId;
        // 如果分表的表名如果已经在支持的分表表名列表中，返回该分表表名
        if (availableTargetNames.contains(realTableName))
            return realTableName;
        else
            return logicTableName;

    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
