package com.review.agent.service;

import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.entity.pojo.DataInfo;
import com.review.agent.entity.pojo.SyncRecord;
import com.review.agent.entity.pojo.UserConfig;
import com.review.agent.entity.request.DataInfoRequest;
import com.review.agent.entity.projection.DataInfoVo;
import com.review.agent.repository.DataInfoRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.review.agent.common.constant.CommonConstant.*;

@Slf4j
@Service
public class DataInfoService {
    @Resource
    private DataInfoRepository dataInfoRepository;
    @Resource
    private SyncRecordService syncRecordService;
    @Resource
    private UserService userService;

    public Page<DataInfoVo> page(Pageable pageable, DataInfoRequest fileRequest) {
        Long userId = fileRequest.getUserId();
        Integer processedStatus = fileRequest.getProcessedStatus();
        String fileName = fileRequest.getFileName();
        Date startTime = fileRequest.getStartTime();
        Date endTime = fileRequest.getEndTime();
        Integer source = fileRequest.getSource();
        return dataInfoRepository.findByPage(pageable, userId, fileName, processedStatus, startTime, endTime, source);
    }

    public DataInfo findById(Long id) {
        return dataInfoRepository.findById(id).orElse(null);
    }

    /**
     * 执行同步数据
     * @param userId 用户ID
     * @throws IOException 扫描目录不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncData(Long userId) throws IOException {
        UserConfig userConfig = userService.getUserConfig(userId);
        String scanDirectory = userConfig.getScanDirectory();
        // 校验扫描目录是否存在
        File file = new File(scanDirectory);
        if (!file.exists()) {
            boolean mkdirsed = file.mkdirs();
            if (!mkdirsed) {
                ExceptionUtils.throwDataNotFound("scan directory is not exists");
            }
        }

        processSyncData(userId, file);
    }

    /**
     * 处理同步数据
     * @param userId 用户ID
     * @param file 扫描目录
     * @throws IOException 扫描目录不存在时抛出
     */
    private void processSyncData(Long userId, File file) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        File[] files = file.listFiles();
        if (files == null) {
            log.info("scan directory is empty");
            return;
        }

        // Batch query all existing files for this user to avoid N+1 queries
        List<DataInfo> existingFiles = dataInfoRepository.findByUserId(userId);
        java.util.Map<String, DataInfo> existingFileMap = existingFiles.stream()
            .collect(java.util.stream.Collectors.toMap(
                DataInfo::getFileName,
                f -> f,
                (existing, replacement) -> existing  // keep existing if duplicate filenames
            ));

        List<DataInfo> dataList = new ArrayList<>();
        for (File newFileData : files) {
            if (newFileData.isFile()) {
                upsertFile(userId, newFileData, dataList, existingFileMap);
            }
        }

        if (!dataList.isEmpty()) {
            dataInfoRepository.saveAll(dataList);
        }
        stopWatch.stop();

        // 保存同步记录
        SyncRecord syncRecord = new SyncRecord();
        syncRecord.setUserId(userId);
        syncRecord.setSyncCount(dataList.size());
        syncRecord.setSpendTime(stopWatch.getTotalTimeSeconds());
        syncRecordService.save(syncRecord);
    }

    /**
     * 封装数据
     * @param userId 用户ID
     * @param newFileData 文件数据
     * @param dataList 数据列表
     * @param existingFileMap 已存在的文件映射（从批量查询获取，避免N+1查询）
     */
    private void upsertFile(Long userId, File newFileData, List<DataInfo> dataList, java.util.Map<String, DataInfo> existingFileMap) {
        String content;
        try {
            content = FileUtils.readFileToString(newFileData, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String filePath = newFileData.getPath();
        String filename = newFileData.getName();


        long currentModifiedTime = 0;
        try {
            currentModifiedTime = Files.getLastModifiedTime(Path.of(filePath)).toMillis();
        } catch (IOException e) {
            log.error("❌无法获取修改时间: " + filePath);
        }
        Date currentDateTime = Date.from(Instant.ofEpochMilli(currentModifiedTime));

        // Look up existing file from map instead of querying database (N+1 fix)
        DataInfo existingData = existingFileMap.get(filename);

        // 新增文件
        if (existingData == null) {
            DataInfo dataInfo = new DataInfo();
            dataInfo.setUserId(userId);
            dataInfo.setFileName(newFileData.getName());
            dataInfo.setFileContent(content);
            dataInfo.setSource(DATA_SOURCE_LOCAL);
            dataInfo.setProcessedStatus(FILE_PROCESS_STATUS_NOT_PROCESSED);
            dataInfo.setCreatedTime(new Date());
            dataInfo.setUpdateTime(currentDateTime);
            dataList.add(dataInfo);
        } else {
            // 更新文件内容
            Date previousModifiedTime = existingData.getUpdateTime();
            long previousMilli = previousModifiedTime.toInstant().toEpochMilli();
            if (currentModifiedTime > previousMilli) {
                existingData.setFileContent(content);
                existingData.setProcessedStatus(FILE_PROCESS_STATUS_UPDATE);
                existingData.setUpdateTime(currentDateTime);
                dataList.add(existingData);
                log.info("✏️ 更新文件: " + filename);
            }
        }
    }

    public List<DataInfo> findByUserId(Long userId) {
        return dataInfoRepository.findByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DataInfo importData(Long userId, String originalFilename, String content, Integer source) {
        DataInfo existing = dataInfoRepository.findByUserIdAndFileName(userId,originalFilename);
        if (existing == null) {
            DataInfo dataInfo = new DataInfo();
            dataInfo.setUserId(userId);
            dataInfo.setFileName(originalFilename);
            dataInfo.setFileContent(content);
            dataInfo.setSource(source);
            dataInfo.setProcessedStatus(FILE_PROCESS_STATUS_NOT_PROCESSED);
            dataInfo.setCreatedTime(new Date());
            dataInfo.setUpdateTime(new Date());
            return dataInfoRepository.save(dataInfo);
        } else {
            existing.setFileContent(content);
            existing.setSource(source);
            existing.setProcessedStatus(FILE_PROCESS_STATUS_UPDATE);
            existing.setUpdateTime(new Date());
            return dataInfoRepository.save(existing);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public DataInfo createData(DataInfo dataInfo) {
        dataInfo.setProcessedStatus(FILE_PROCESS_STATUS_NOT_PROCESSED);
        dataInfo.setCreatedTime(new Date());
        dataInfo.setUpdateTime(new Date());
        if (dataInfo.getSource() == null) {
            dataInfo.setSource(DATA_SOURCE_LOCAL);
        }
        return dataInfoRepository.save(dataInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(DataInfo dataInfo) {
        dataInfoRepository.save(dataInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 软删除：设置 deleted = true 和 deleted_at = 当前时间
        dataInfoRepository.softDelete(id, new Date());
    }

    /**
     * 恢复已删除的数据
     * @param id 数据ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        DataInfo dataInfo = findById(id);
        if (dataInfo == null) {
            ExceptionUtils.throwDataNotFound("data info not found");
        }

        dataInfo.setDeleted(0);
        dataInfo.setDeletedAt(null);
        dataInfoRepository.save(dataInfo);
    }

    /**
     * 获取用户的所有文件（不包括已删除的）
     * @param userId 用户ID
     * @return 文件列表
     */
    public List<DataInfo> findAll(Long userId) {
        return dataInfoRepository.findByUserId(userId);
    }
}
