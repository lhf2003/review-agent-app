package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.pojo.DataInfo;
import com.review.agent.entity.request.DataInfoRequest;
import com.review.agent.entity.projection.DataInfoVo;
import com.review.agent.service.DataInfoService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件信息接口
 */
@RestController
@RequestMapping("/data")
public class DataInfoController {

    @Resource
    private DataInfoService dataInfoService;

    /**
     * 分页
     * @param pageable    分页信息
     * @param dataInfoRequest 文件数据查询请求
     * @return 文件数据分页列表
     */
    @PostMapping("/page")
    public BaseResponse<Page<DataInfoVo>> page(Pageable pageable, @RequestBody DataInfoRequest dataInfoRequest, @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId != null) {
            dataInfoRequest.setUserId(userId);
        }
        Page<DataInfoVo> dataInfoPage = dataInfoService.page(pageable, dataInfoRequest);
        return ResultUtil.success(dataInfoPage);
    }

//    /**
//     * 获取文件数据详情
//     * @param userId 用户ID
//     * @param id     数据ID
//     * @return 文件数据详情
//     */
//    @GetMapping("/detail")
//    public BaseResponse<DataInfo> detail(@RequestParam("userId") Long userId, @RequestParam("id") Long id) {
//        DataInfo dataInfo = dataInfoService.findById(id);
//        if (dataInfo == null) {
//            return ResultUtil.error("数据不存在");
//        }
//        return ResultUtil.success(dataInfo);
//    }

    /**
     * 导入文件数据
     * @param file 上传的文件
     * @return 导入结果
     */
    @PostMapping("/import")
    public BaseResponse<DataInfo> importData(@RequestHeader("userId") Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "upload.txt";
        }
        String content = new String(file.getBytes());
        DataInfo dataInfo = dataInfoService.importData(userId, originalFilename, content);
        return ResultUtil.success(dataInfo);
    }

    /**
     * 同步数据
     * @param userId 用户ID
     * @return 同步结果
     */
    @GetMapping("/sync")
    public BaseResponse<DataInfo> syncData(@RequestHeader("userId") Long userId) throws IOException {
        dataInfoService.syncData(userId);
        return ResultUtil.success(null);
    }
}
