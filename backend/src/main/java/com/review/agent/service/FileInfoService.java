package com.review.agent.service;

import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.entity.DataInfo;
import com.review.agent.repository.DataInfoRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileInfoService {
    @Resource
    private DataInfoRepository dataInfoRepository;

    public DataInfo findById(Long fileId) {
        return dataInfoRepository.findById(fileId).orElse(null);
    }

    public void add(DataInfo dataInfo) {
        dataInfoRepository.save(dataInfo);
    }

    public void update(DataInfo dataInfo) {
        DataInfo dataInfoInDb = findById(dataInfo.getId());
        if (dataInfoInDb == null) {
            log.error("file info not found, fileId: {}", dataInfo.getId());
            return;
        }
        BeanUtils.copyProperties(dataInfo, dataInfoInDb, ObjectTransformUtil.getNullPropertyNames(dataInfo));
        dataInfoRepository.save(dataInfoInDb);
    }
}