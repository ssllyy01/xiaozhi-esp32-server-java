-- 1. 首先在sys_role表中添加新字段
ALTER TABLE `xiaozhi`.`sys_role` 
ADD COLUMN `modelId` int unsigned DEFAULT NULL COMMENT '模型ID' AFTER `voiceName`,
ADD COLUMN `sttId` int unsigned DEFAULT NULL COMMENT 'STT服务ID' AFTER `modelId`,
ADD COLUMN `temperature` FLOAT DEFAULT 0.7 COMMENT '温度' AFTER `modelId`,
ADD COLUMN `topP` FLOAT DEFAULT 0.9 COMMENT '核心采样' AFTER `temperature`,
ADD COLUMN `vadSpeechTh` FLOAT DEFAULT 0.5 COMMENT '语音检测阈值' AFTER `temperature`,
ADD COLUMN `vadSilenceTh` FLOAT DEFAULT 0.3 COMMENT '静音检测阈值' AFTER `vadSpeechTh`,
ADD COLUMN `vadEnergyTh` FLOAT DEFAULT 0.01 COMMENT '能量检测阈值' AFTER `vadSilenceTh`,
ADD COLUMN `vadSilenceMs` INT DEFAULT 1200 COMMENT '静音检测时间' AFTER `vadEnergyTh`;

-- 2. 将数据从sys_device表迁移到sys_role表
-- 使用JOIN语法更新sys_role表中的字段，从sys_device表获取对应的值
UPDATE `xiaozhi`.`sys_role` r
JOIN `xiaozhi`.`sys_device` d ON r.roleId = d.roleId
SET 
    r.modelId = d.modelId,
    r.sttId = d.sttId,
    r.vadSpeechTh = d.vadSpeechTh,
    r.vadSilenceTh = d.vadSilenceTh,
    r.vadEnergyTh = d.vadEnergyTh,
    r.vadSilenceMs = d.vadSilenceMs
WHERE d.roleId IS NOT NULL;

-- 3. 从sys_device表中移除这些字段（可选，取决于你是否想保留这些字段）
ALTER TABLE `xiaozhi`.`sys_device` 
DROP COLUMN `modelId`,
DROP COLUMN `sttId`,
DROP COLUMN `vadSpeechTh`,
DROP COLUMN `vadSilenceTh`,
DROP COLUMN `vadEnergyTh`,
DROP COLUMN `vadSilenceMs`;
