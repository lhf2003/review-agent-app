package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.DataInfo;
import com.review.agent.entity.request.DataInfoRequest;
import com.review.agent.entity.projection.DataInfoVo;
import com.review.agent.entity.vo.SessionTraceVo;
import com.review.agent.service.DataInfoService;
import com.review.agent.service.UserService;
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
    @Resource
    private SecurityUtils securityUtils;

    /**
     * 分页
     * @param pageable    分页信息
     * @param dataInfoRequest 文件数据查询请求
     * @return 文件数据分页列表
     */
    @PostMapping("/page")
    public BaseResponse<Page<DataInfoVo>> page(Pageable pageable, @RequestBody DataInfoRequest dataInfoRequest) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId != null) {
            dataInfoRequest.setUserId(userId);
        }
        Page<DataInfoVo> dataInfoPage = dataInfoService.page(pageable, dataInfoRequest);
        return ResultUtil.success(dataInfoPage);
    }

    /**
     * 导入文件数据
     * @param file 上传的文件
     * @return 导入结果
     */
    @PostMapping("/import")
    public BaseResponse<DataInfo> importData(@RequestPart("file") MultipartFile file,
                                             @RequestParam(value = "source", required = false, defaultValue = "0") Integer source) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "upload.txt";
        }
        String content = new String(file.getBytes());
        DataInfo dataInfo = dataInfoService.importData(securityUtils.getCurrentUserId(), originalFilename, content, source);
        return ResultUtil.success(dataInfo);
    }

    /**
     * 手动创建数据
     * @param dataInfo 数据信息
     * @return 创建结果
     */
    @PostMapping("/create")
    public BaseResponse<DataInfo> create(@RequestBody DataInfo dataInfo) {
        dataInfo.setUserId(securityUtils.getCurrentUserId());
        DataInfo created = dataInfoService.createData(dataInfo);
        return ResultUtil.success(created);
    }

    /**
     * 删除数据
     * @param id 数据ID
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public BaseResponse<Void> delete(@RequestParam("id") Long id) {
        dataInfoService.delete(id);
        return ResultUtil.success(null);
    }

    /**
     * 同步数据
     * @return 同步结果
     */
    @GetMapping("/sync")
    public BaseResponse<DataInfo> syncData() throws IOException {
        dataInfoService.syncData(securityUtils.getCurrentUserId());
        return ResultUtil.success(null);
    }

    /**
     * 获取文件的会话信息
     * @param fileId 文件ID
     * @return 会话信息
     */
    @GetMapping("/info")
    public BaseResponse<SessionTraceVo> get(@RequestParam("fileId") Long fileId) {
        Long userId = securityUtils.getCurrentUserId();
        SessionTraceVo sessionTraceVo = dataInfoService.getInfo(userId, fileId);
        return ResultUtil.success(sessionTraceVo);
    }
}
