package com.review.agent.controller;

import com.review.agent.service.TagService;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标签接口
 */
@RestController
@RequestMapping("/tag")
public class TagController {
    @Resource
    private TagService tagService;

    @GetMapping("/get/all")
    public BaseResponse<?> getAll() {
        return ResultUtil.success(tagService.findAll());
    }

    @PostMapping("/add")
    public BaseResponse<?> add(@RequestBody Tag tag) {
        tagService.add(tag);
        return ResultUtil.success("ok");
    }

    @DeleteMapping("/delete")
    public BaseResponse<?> delete(@RequestParam("id") Integer id) {
        tagService.delete(id);
        return ResultUtil.success("ok");
    }
}
