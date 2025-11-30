package com.review.agent.service;

import com.review.agent.entity.DataInfo;
import com.review.agent.entity.SyncRecord;
import com.review.agent.entity.UserConfig;
import com.review.agent.entity.request.DataInfoRequest;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class DataInfoService {
    @Resource
    private DataInfoRepository dataInfoRepository;
    @Resource
    private SyncRecordService syncRecordService;
    @Resource
    private UserService userService;

    public Page<DataInfo> page(Pageable pageable, DataInfoRequest fileRequest) {
        Long userId = fileRequest.getUserId();
        Integer processedStatus = fileRequest.getProcessedStatus();
        Date startTime = fileRequest.getStartTime();
        Date endTime = fileRequest.getEndTime();
        return dataInfoRepository.findByPage(pageable, userId, processedStatus, startTime, endTime);
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

        List<DataInfo> dataList = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File fileData : files) {
                if (fileData.isFile()) {
                    upsertFile(userId, fileData, dataList);
                }
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
     * @param fileData 文件数据
     * @param dataList 数据列表
     */
    private void upsertFile(Long userId, File fileData, List<DataInfo> dataList) {
        String content;
        try {
            content = FileUtils.readFileToString(fileData, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String filePath = fileData.getPath();
        DataInfo existing = dataInfoRepository.findByFilePath(filePath);
        if (existing == null) {
            DataInfo dataInfo = new DataInfo();
            dataInfo.setUserId(userId);
            dataInfo.setFilePath(filePath);
            dataInfo.setFileName(fileData.getName());
            dataInfo.setFileContent(content);
            dataInfo.setProcessedStatus(0);
            dataInfo.setCreatedTime(new Date());
            dataList.add(dataInfo);
        } else {
            existing.setFileContent(content);
            existing.setProcessedStatus(0);
            dataList.add(existing);
        }
    }

    public List<DataInfo> findByUserId(Long userId) {
        return dataInfoRepository.findByUserId(userId);
    }

    /**
     * 获取用户最后一次同步数据的数据
     * @param userId 用户ID
     * @return 最后一次同步数据的数据
     */
    private DataInfo getLastSyncDate(Long userId) {
        List<DataInfo> dataInfoList = findByUserId(userId);
        if (dataInfoList.isEmpty()) {
            return null;
        }
        return dataInfoList.get(dataInfoList.size() - 1);
    }

    public DataInfo importData(Long userId, String originalFilename, String content) {
        String filePath = "upload://" + userId + "/" + originalFilename;
        DataInfo existing = dataInfoRepository.findByFilePath(filePath);
        if (existing == null) {
            DataInfo dataInfo = new DataInfo();
            dataInfo.setUserId(userId);
            dataInfo.setFilePath(filePath);
            dataInfo.setFileName(originalFilename);
            dataInfo.setFileContent(content);
            dataInfo.setProcessedStatus(0);
            dataInfo.setCreatedTime(new Date());
            return dataInfoRepository.save(dataInfo);
        } else {
            existing.setFileContent(content);
            existing.setProcessedStatus(0);
            return dataInfoRepository.save(existing);
        }
    }

    public void update(DataInfo dataInfo) {
        dataInfoRepository.save(dataInfo);
    }
}
