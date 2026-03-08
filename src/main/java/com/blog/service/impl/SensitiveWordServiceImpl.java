package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.SensitiveCheckResultDTO;
import com.blog.dto.SensitiveWordCreateDTO;
import com.blog.dto.SensitiveWordDTO;
import com.blog.entity.SensitiveWord;
import com.blog.mapper.SensitiveWordMapper;
import com.blog.service.SensitiveWordService;
import com.blog.utils.BusinessUtils;
import com.blog.utils.SensitiveWordFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 敏感词服务实现类
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Override
    public Result<SensitiveCheckResultDTO> checkContent(String content) {
        if (StringUtils.isBlank(content)) {
            return BusinessUtils.success(SensitiveCheckResultDTO.pass());
        }

        try {
            Set<String> hitWords = sensitiveWordFilter.getSensitiveWords(content);
            if (hitWords == null || hitWords.isEmpty()) {
                return BusinessUtils.success(SensitiveCheckResultDTO.pass());
            }
            return BusinessUtils.success(SensitiveCheckResultDTO.fail(new ArrayList<>(hitWords)));
        } catch (Exception e) {
            log.error("检测敏感词异常", e);
            return BusinessUtils.error("检测敏感词失败");
        }
    }

    @Override
    public Result<Void> validateContent(String content) {
        if (StringUtils.isBlank(content)) {
            return BusinessUtils.success();
        }

        try {
            Set<String> hitWords = sensitiveWordFilter.getSensitiveWords(content);
            if (hitWords != null && !hitWords.isEmpty()) {
                return BusinessUtils.error("内容包含敏感词：" + String.join("、", hitWords) + "，请修改后重试");
            }
            return BusinessUtils.success();
        } catch (Exception e) {
            log.error("验证敏感词异常", e);
            return BusinessUtils.error("验证敏感词失败");
        }
    }

    @Override
    public Result<List<String>> getHitWords(String content) {
        if (StringUtils.isBlank(content)) {
            return BusinessUtils.success(new ArrayList<>());
        }

        try {
            Set<String> hitWords = sensitiveWordFilter.getSensitiveWords(content);
            return BusinessUtils.success(new ArrayList<>(hitWords != null ? hitWords : new ArrayList<>()));
        } catch (Exception e) {
            log.error("获取命中敏感词异常", e);
            return BusinessUtils.error("获取敏感词失败");
        }
    }

    @Override
    public Result<String> replaceContent(String content) {
        if (StringUtils.isBlank(content)) {
            return BusinessUtils.success(content);
        }

        try {
            String result = sensitiveWordFilter.replaceSensitiveWords(content);
            return BusinessUtils.success(result);
        } catch (Exception e) {
            log.error("替换敏感词异常", e);
            return BusinessUtils.error("替换敏感词失败");
        }
    }

    @Override
    @Transactional
    public Result<Long> addWord(SensitiveWordCreateDTO createDTO) {
        try {
            // 检查是否已存在
            if (sensitiveWordMapper.existsSensitiveWord(createDTO.getWord())) {
                return BusinessUtils.error("敏感词已存在");
            }

            SensitiveWord word = new SensitiveWord();
            BeanUtils.copyProperties(createDTO, word);
            word.setCreateTime(LocalDateTime.now());
            word.setUpdateTime(LocalDateTime.now());

            int result = sensitiveWordMapper.insert(word);
            if (result > 0) {
                // 异步重载缓存，或在定时任务中重载，这里直接重载
                sensitiveWordFilter.reloadSensitiveWords();
                return BusinessUtils.success(word.getId());
            }
            return BusinessUtils.error("添加敏感词失败");
        } catch (Exception e) {
            log.error("添加敏感词异常", e);
            return BusinessUtils.error("添加敏感词失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> deleteWord(Long id) {
        try {
            SensitiveWord word = sensitiveWordMapper.selectById(id);
            if (word == null) {
                return BusinessUtils.error("敏感词不存在");
            }

            int result = sensitiveWordMapper.deleteById(id);
            if (result > 0) {
                sensitiveWordFilter.reloadSensitiveWords();
                return BusinessUtils.success();
            }
            return BusinessUtils.error("删除敏感词失败");
        } catch (Exception e) {
            log.error("删除敏感词异常", e);
            return BusinessUtils.error("删除敏感词失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> batchDeleteWords(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return BusinessUtils.success();
        }
        try {
            int result = sensitiveWordMapper.deleteBatchIds(ids);
            if (result > 0) {
                sensitiveWordFilter.reloadSensitiveWords();
                return BusinessUtils.success();
            }
            return BusinessUtils.error("批量删除敏感词失败");
        } catch (Exception e) {
            log.error("批量删除敏感词异常", e);
            return BusinessUtils.error("批量删除敏感词失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> updateWord(Long id, SensitiveWordCreateDTO createDTO) {
        try {
            SensitiveWord word = sensitiveWordMapper.selectById(id);
            if (word == null) {
                return BusinessUtils.error("敏感词不存在");
            }

            // 检查是否与其他词同名
            LambdaQueryWrapper<SensitiveWord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SensitiveWord::getWord, createDTO.getWord())
                    .ne(SensitiveWord::getId, id);
            if (sensitiveWordMapper.exists(queryWrapper)) {
                return BusinessUtils.error("敏感词已存在");
            }

            BeanUtils.copyProperties(createDTO, word);
            word.setUpdateTime(LocalDateTime.now());

            int result = sensitiveWordMapper.updateById(word);
            if (result > 0) {
                sensitiveWordFilter.reloadSensitiveWords();
                return BusinessUtils.success();
            }
            return BusinessUtils.error("更新敏感词失败");
        } catch (Exception e) {
            log.error("更新敏感词异常", e);
            return BusinessUtils.error("更新敏感词失败");
        }
    }

    @Override
    public Result<PageResult<SensitiveWordDTO>> getWordList(Integer page, Integer size, String keyword,
            String category) {
        try {
            Page<SensitiveWord> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<SensitiveWord> queryWrapper = new LambdaQueryWrapper<>();

            if (StringUtils.isNotBlank(keyword)) {
                queryWrapper.like(SensitiveWord::getWord, keyword);
            }
            if (StringUtils.isNotBlank(category)) {
                queryWrapper.eq(SensitiveWord::getCategory, category);
            }

            queryWrapper.orderByDesc(SensitiveWord::getUpdateTime);

            IPage<SensitiveWord> resultPage = sensitiveWordMapper.selectPage(pageParam, queryWrapper);

            List<SensitiveWordDTO> dtoList = resultPage.getRecords().stream().map(word -> {
                SensitiveWordDTO dto = new SensitiveWordDTO();
                BeanUtils.copyProperties(word, dto);
                return dto;
            }).collect(Collectors.toList());

            PageResult<SensitiveWordDTO> pageResult = new PageResult<>(
                    dtoList,
                    resultPage.getTotal(),
                    page,
                    size);

            return BusinessUtils.success(pageResult);
        } catch (Exception e) {
            log.error("查询敏感词列表异常", e);
            return BusinessUtils.error("查询敏感词列表失败");
        }
    }

    @Override
    @Transactional
    public Result<Integer> batchImport(List<String> words, String category, Integer level) {
        if (words == null || words.isEmpty()) {
            return BusinessUtils.success(0);
        }

        int successCount = 0;
        try {
            for (String w : words) {
                if (StringUtils.isBlank(w))
                    continue;
                String cleanWord = w.trim();

                // 检查是否已存在
                if (!sensitiveWordMapper.existsSensitiveWord(cleanWord)) {
                    SensitiveWord word = new SensitiveWord();
                    word.setWord(cleanWord);
                    word.setCategory(StringUtils.isBlank(category) ? "default" : category);
                    word.setLevel(level == null ? 1 : level);
                    word.setCreateTime(LocalDateTime.now());
                    word.setUpdateTime(LocalDateTime.now());

                    if (sensitiveWordMapper.insert(word) > 0) {
                        successCount++;
                    }
                }
            }

            if (successCount > 0) {
                sensitiveWordFilter.reloadSensitiveWords();
            }

            return BusinessUtils.success(successCount);
        } catch (Exception e) {
            log.error("批量导入敏感词异常", e);
            return BusinessUtils.error("批量导入失败，成功导入：" + successCount);
        }
    }

    @Override
    public Result<Void> reloadCache() {
        try {
            sensitiveWordFilter.reloadSensitiveWords();
            return BusinessUtils.success();
        } catch (Exception e) {
            log.error("重载敏感词缓存异常", e);
            return BusinessUtils.error("重载敏感词缓存失败");
        }
    }
}
