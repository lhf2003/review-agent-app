package com.review.agent.service;

import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.entity.FileInfo;
import com.review.agent.repository.FileInfoRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileInfoService {
    @Resource
    private FileInfoRepository fileInfoRepository;

    public FileInfo findById(Long fileId) {
        return fileInfoRepository.findById(fileId).orElse(null);
    }

    public void add(FileInfo fileInfo) {
        fileInfoRepository.save(fileInfo);
    }

    public void update(FileInfo fileInfo) {
        FileInfo fileInfoInDb = findById(fileInfo.getId());
        if (fileInfoInDb == null) {
            log.error("file info not found, fileId: {}", fileInfo.getId());
            return;
        }
        BeanUtils.copyProperties(fileInfo, fileInfoInDb, ObjectTransformUtil.getNullPropertyNames(fileInfo));
        fileInfoRepository.save(fileInfoInDb);
    }
}