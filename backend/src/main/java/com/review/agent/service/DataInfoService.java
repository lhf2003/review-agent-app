package com.review.agent.service;

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
        return dataInfoRepository.findByPage(pageable, userId, fileName, processedStatus, startTime, endTime);
    }

    public DataInfo findById(Long id) {
        return dataInfoRepository.findById(id).orElse(null);
    }

    /**
     * 执行同步数据
     * @param userId 用户ID
     * @throws IOException 扫描目录不存在时抛出
     */
    public void syncData(Long userId) throws IOException {
        UserConfig userConfig = userService.getUserConfig(userId);
        String scanDirectory = userConfig.getScanDirectory();
        // 校验扫描目录是否存在
        File file = new File(scanDirectory);
        if (!file.exists()) {
            boolean mkdirsed = file.mkdirs();
            if (!mkdirsed) {
                throw new IOException("directory is not exists");
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

        List<DataInfo> dataList = new ArrayList<>();
        for (File newFileData : files) {
            if (newFileData.isFile()) {
                upsertFile(userId, newFileData, dataList);
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
     */
    private void upsertFile(Long userId, File newFileData, List<DataInfo> dataList) {
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
        DataInfo existingData = dataInfoRepository.findByFileName(filename);
        // 新增文件
        if (existingData == null) {
            DataInfo dataInfo = new DataInfo();
            dataInfo.setUserId(userId);
            dataInfo.setFilePath(filePath);
            dataInfo.setFileName(newFileData.getName());
            dataInfo.setFileContent(content);
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

    public DataInfo importData(Long userId, String originalFilename, String content) {
        DataInfo existing = dataInfoRepository.findByFileName(originalFilename);
        if (existing == null) {
            DataInfo dataInfo = new DataInfo();
            dataInfo.setUserId(userId);
            dataInfo.setFilePath(originalFilename);
            dataInfo.setFileName(originalFilename);
            dataInfo.setFileContent(content);
            dataInfo.setProcessedStatus(FILE_PROCESS_STATUS_NOT_PROCESSED);
            dataInfo.setCreatedTime(new Date());
            dataInfo.setUpdateTime(new Date());
            return dataInfoRepository.save(dataInfo);
        } else {
            existing.setFileContent(content);
            existing.setProcessedStatus(FILE_PROCESS_STATUS_UPDATE);
            existing.setUpdateTime(new Date());
            return dataInfoRepository.save(existing);
        }
    }

    public void update(DataInfo dataInfo) {
        dataInfoRepository.save(dataInfo);
    }
}
