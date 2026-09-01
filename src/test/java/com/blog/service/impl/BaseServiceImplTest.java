package com.blog.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.PageDTO;
import com.blog.service.BaseService;
import com.blog.utils.BusinessUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseServiceImplTest {

    private static class TestEntity implements BusinessUtils.Updatable {
        private Long id;
        private Integer status;
        private LocalDateTime updateTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        @Override
        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }
    }

    private static class TestDTO {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    private static class TestCreateDTO {
    }

    private static class TestUpdateDTO {
    }

    private static class TestBaseServiceImpl
            extends BaseServiceImpl<TestEntity, TestDTO, TestCreateDTO, TestUpdateDTO> {
        private final BaseMapper<TestEntity> mapper;

        TestBaseServiceImpl(BaseMapper<TestEntity> mapper) {
            this.mapper = mapper;
        }

        @Override
        protected BaseMapper<TestEntity> getBaseMapper() {
            return mapper;
        }

        @Override
        public TestDTO toDTO(TestEntity entity) {
            TestDTO dto = new TestDTO();
            dto.setId(entity.getId());
            return dto;
        }

        @Override
        public TestEntity toEntity(TestCreateDTO createDTO) {
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            return entity;
        }

        @Override
        public TestEntity updateEntity(TestEntity entity, TestUpdateDTO updateDTO) {
            return entity;
        }
    }

    @Mock
    private BaseMapper<TestEntity> mapper;

    @InjectMocks
    private TestBaseServiceImpl service;

    // ==================== findById ====================

    @Test
    void findById_whenExists_shouldReturnDTO() {
        TestEntity entity = new TestEntity();
        entity.setId(10L);
        when(mapper.selectById(10L)).thenReturn(entity);

        TestDTO dto = service.findById(10L);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
    }

    @Test
    void findById_whenNullId_shouldThrowException() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无效的ID");
    }

    @Test
    void findById_whenNegativeId_shouldThrowException() {
        assertThatThrownBy(() -> service.findById(-1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无效的ID");
    }

    @Test
    void findById_whenNotFound_shouldThrowException() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("对象不存在");
    }

    // ==================== save ====================

    @Test
    void save_whenInsertSuccess_shouldReturnId() {
        TestEntity persisted = new TestEntity();
        persisted.setId(42L);
        when(mapper.insert(any(TestEntity.class))).thenReturn(1);

        Long id = service.save(new TestCreateDTO());

        assertThat(id).isNotNull();
        verify(mapper).insert(any(TestEntity.class));
    }

    @Test
    void save_whenInsertFails_shouldThrowException() {
        when(mapper.insert(any(TestEntity.class))).thenReturn(0);

        assertThatThrownBy(() -> service.save(new TestCreateDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("保存对象失败");
    }

    // ==================== update ====================

    @Test
    void update_whenExists_shouldUpdateSuccessfully() {
        TestEntity existing = new TestEntity();
        existing.setId(10L);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.updateById(any(TestEntity.class))).thenReturn(1);

        service.update(10L, new TestUpdateDTO());

        verify(mapper).updateById(any(TestEntity.class));
    }

    @Test
    void update_whenNotFound_shouldThrowException() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(99L, new TestUpdateDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("对象不存在");
    }

    @Test
    void update_whenUpdateFails_shouldThrowException() {
        TestEntity existing = new TestEntity();
        existing.setId(10L);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.updateById(any(TestEntity.class))).thenReturn(0);

        assertThatThrownBy(() -> service.update(10L, new TestUpdateDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("更新对象失败");
    }

    // ==================== delete ====================

    @Test
    void delete_whenExists_shouldDeleteSuccessfully() {
        TestEntity existing = new TestEntity();
        existing.setId(10L);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.deleteById(10L)).thenReturn(1);

        service.delete(10L);

        verify(mapper).deleteById(10L);
    }

    @Test
    void delete_whenNotFound_shouldThrowException() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("对象不存在");
    }

    @Test
    void delete_whenDeleteFails_shouldThrowException() {
        TestEntity existing = new TestEntity();
        existing.setId(10L);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.deleteById(10L)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("删除对象失败");
    }

    // ==================== findPage ====================

    @Test
    void findPage_shouldReturnPageResult() {
        IPage<TestEntity> page = mock(IPage.class);
        when(page.getTotal()).thenReturn(1L);
        when(page.getCurrent()).thenReturn(1L);
        when(page.getSize()).thenReturn(10L);
        when(page.getPages()).thenReturn(1L);
        when(page.getRecords()).thenReturn(List.of(new TestEntity() {
            {
                setId(1L);
            }
        }));
        when(mapper.selectPage(any(IPage.class), any())).thenReturn(page);

        PageDTO<TestDTO> result = service.findPage(1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    // ==================== findAll ====================

    @Test
    void findAll_shouldReturnAllRecords() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        when(mapper.selectList(null)).thenReturn(List.of(entity));

        List<TestDTO> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findByIds ====================

    @Test
    void findByIds_shouldReturnMatchingRecords() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        when(mapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(entity));

        List<TestDTO> result = service.findByIds(List.of(1L, 2L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== updateStatus ====================

    @Test
    void updateStatus_whenExists_shouldUpdateStatusAndTime() {
        TestEntity existing = new TestEntity();
        existing.setId(10L);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.updateById(any(TestEntity.class))).thenReturn(1);

        service.updateStatus(10L, 2);

        assertThat(existing.getStatus()).isEqualTo(2);
        verify(mapper).updateById(existing);
    }

    @Test
    void updateStatus_whenNotFound_shouldThrowException() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateStatus(99L, 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("对象不存在");
    }

    @Test
    void updateStatus_whenUpdateFails_shouldThrowException() {
        TestEntity existing = new TestEntity();
        existing.setId(10L);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.updateById(any(TestEntity.class))).thenReturn(0);

        assertThatThrownBy(() -> service.updateStatus(10L, 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("更新状态失败");
    }

    // ==================== count ====================

    @Test
    void count_shouldReturnTotal() {
        when(mapper.selectCount(any())).thenReturn(5L);

        Long total = service.count();

        assertThat(total).isEqualTo(5L);
    }

    // ==================== existsById ====================

    @Test
    void existsById_validIdExists_shouldReturnTrue() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        when(mapper.selectById(1L)).thenReturn(entity);

        boolean exists = service.existsById(1L);

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_validIdNotFound_shouldReturnFalse() {
        when(mapper.selectById(1L)).thenReturn(null);

        boolean exists = service.existsById(1L);

        assertThat(exists).isFalse();
    }

    @Test
    void existsById_nullId_shouldReturnFalse() {
        boolean exists = service.existsById(null);

        assertThat(exists).isFalse();
        verify(mapper, never()).selectById(any());
    }

    @Test
    void existsById_zeroId_shouldReturnFalse() {
        boolean exists = service.existsById(0L);

        assertThat(exists).isFalse();
        verify(mapper, never()).selectById(any());
    }

    @Test
    void existsById_negativeId_shouldReturnFalse() {
        boolean exists = service.existsById(-1L);

        assertThat(exists).isFalse();
        verify(mapper, never()).selectById(any());
    }
}
