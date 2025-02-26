package com.ygyin.coop.manager.sharding;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.enums.AreaLevelEnum;
import com.ygyin.coop.model.enums.AreaTypeEnum;
import com.ygyin.coop.service.AreaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态分表管理器，用于动态更新配置中的 actual-data-nodes 目标表
 */
//@Component
@Slf4j
public class DynamicShardingManager {

    @Resource
    private DataSource dataSource;

    @Resource
    private AreaService areaService;

    private static final String LOGIC_TABLE_NAME = "image";

    private static final String DATABASE_NAME = "logic_db"; // 配置文件中的数据库名称

    /**
     * 将管理器注册成 Bean，加载完成后获取所有分表并更新配置
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化动态分表配置...");
        updateShardingTableNodes();
    }

    /**
     * 获取所有动态表名，包括初始表 image 和分表 image_{areaId}
     */
    private Set<String> fetchAllImageTableNames() {
        // 为了测试方便，直接对所有团队空间分表（实际上线改为仅对旗舰版生效）
        Set<Long> areaIds = areaService.lambdaQuery()
                .eq(Area::getAreaType, AreaTypeEnum.TEAM.getVal())
                .list()
                .stream()
                .map(Area::getId)
                .collect(Collectors.toSet());
        // 获取所有物理表
        Set<String> tableNames = areaIds.stream()
                .map(areaId -> LOGIC_TABLE_NAME + "_" + areaId)
                .collect(Collectors.toSet());
        // 添加初始逻辑表
        tableNames.add(LOGIC_TABLE_NAME);
        return tableNames;
    }

    /**
     * 更新 ShardingSphere 的 actual-data-nodes 动态表名配置
     */
    private void updateShardingTableNodes() {
        Set<String> tableNames = fetchAllImageTableNames();
        // coop_db.image_1, ...
        String newActualDataNodes = tableNames.stream()
                .map(tableName -> "coop_db." + tableName) // 确保前缀合法
                .collect(Collectors.joining(","));
        log.info("动态分表 actual-data-nodes 配置: {}", newActualDataNodes);

        ContextManager contextManager = getContextManager();
        // 读取配置文件
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()
                .getDatabases()
                .get(DATABASE_NAME)
                .getRuleMetaData();

        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
        if (shardingRule.isPresent()) {
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
            List<ShardingTableRuleConfiguration> updatedRules = ruleConfig.getTables()
                    .stream()
                    .map(oldTableRule -> {
                        if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                            // 拼接出新的目标表名，更改配置中的 actual-data-nodes 目标表
                            ShardingTableRuleConfiguration newTableRuleConfig = new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, newActualDataNodes);
                            newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                            newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                            newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                            newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
                            return newTableRuleConfig;
                        }
                        return oldTableRule;
                    })
                    .collect(Collectors.toList());
            // 将规则设置到表中，并重新加载数据库
            ruleConfig.setTables(updatedRules);
            contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
            contextManager.reloadDatabase(DATABASE_NAME);
            log.info("动态分表规则更新成功！");
        } else {
            log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败。");
        }
    }

    /**
     * 获取 ShardingSphere ContextManager
     */
    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (SQLException e) {
            throw new RuntimeException("获取 ShardingSphere ContextManager 失败", e);
        }
    }

    /**
     * 根据新建的 Ultra 团队空间，动态创建图片分表
     * @param area
     */
    public void createImageSubTableByArea(Area area) {
        // 动态创建分表
        // 仅为旗舰版团队空间创建分表
        if (area.getAreaType() == AreaTypeEnum.TEAM.getVal() && area.getAreaLevel() == AreaLevelEnum.ULTRA.getVal()) {
            Long areaId = area.getId();
            String tableName = LOGIC_TABLE_NAME + "_" + areaId;
            // 创建新表
            String createTableSql = "CREATE TABLE " + tableName + " LIKE " + LOGIC_TABLE_NAME;
            try {
                SqlRunner.db().update(createTableSql);
                // 更新分表
                updateShardingTableNodes();
            } catch (Exception e) {
                log.error("根据空间创建图片分表失败，空间 id = {}", area.getId());
            }
        }
    }


}
