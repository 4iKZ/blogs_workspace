package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.ResultCode;
import com.blog.dto.UserDTO;
import com.blog.entity.User;
import com.blog.entity.Article;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.ArticleStatusTransitionService;
import com.blog.service.AuthSessionRevocationService;
import com.blog.service.FollowCountService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplSecurityTest {

    /** 当前登录用户（操作者）ID，见 setUpRequestContext */
    private static final long OPERATOR_ID = 1L;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserFollowMapper userFollowMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private AuthSessionRevocationService authSessionRevocationService;

    @Mock
    private ArticleStatusTransitionService articleStatusTransition;

    @Mock
    private FollowCountService followCountService;

    @InjectMocks
    private AdminServiceImpl service;

    @BeforeEach
    void setUpRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", OPERATOR_ID);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDownRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ==================== 用户状态 ====================

    @Test
    void updateUserStatus_disabled_shouldRevokeRefreshToken() {
        when(userMapper.selectById(7L)).thenReturn(userWithRole(7L, 1));
        when(authSessionRevocationService.updateStatusAndRevoke(7L, User.STATUS_DISABLED)).thenReturn(true);

        service.updateUserStatus(7L, User.STATUS_DISABLED);

        verify(authSessionRevocationService).updateStatusAndRevoke(7L, User.STATUS_DISABLED);
    }

    @Test
    void updateUserStatus_deletedStatus_shouldBeRejectedWithoutLookup() {
        // 删除只能走 deleteUser，状态接口不接受“已删除”
        assertThatThrownBy(() -> service.updateUserStatus(7L, User.STATUS_DELETED))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.BAD_REQUEST.getCode()));
        verifyNoInteractions(authSessionRevocationService, userMapper);
    }

    @Test
    void updateUserStatus_unknownStatus_shouldBeRejected() {
        assertThatThrownBy(() -> service.updateUserStatus(7L, 99))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.BAD_REQUEST.getCode()));
        verifyNoInteractions(authSessionRevocationService);
    }

    @Test
    void updateUserStatus_self_shouldBeForbiddenWithoutLookup() {
        assertThatThrownBy(() -> service.updateUserStatus(OPERATOR_ID, User.STATUS_DISABLED))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.FORBIDDEN.getCode()));
        verifyNoInteractions(authSessionRevocationService);
    }

    @Test
    void updateUserStatus_superAdminTarget_shouldBeForbidden() {
        when(userMapper.selectById(5L)).thenReturn(userWithRole(5L, 3));

        assertThatThrownBy(() -> service.updateUserStatus(5L, User.STATUS_DISABLED))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.FORBIDDEN.getCode()));
        verifyNoInteractions(authSessionRevocationService);
    }

    @Test
    void updateUserStatus_adminTargetByAdmin_shouldBeForbidden() {
        when(userMapper.selectById(5L)).thenReturn(userWithRole(5L, 2));
        when(userMapper.selectById(OPERATOR_ID)).thenReturn(userWithRole(OPERATOR_ID, 2));

        assertThatThrownBy(() -> service.updateUserStatus(5L, User.STATUS_DISABLED))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.FORBIDDEN.getCode()));
        verifyNoInteractions(authSessionRevocationService);
    }

    @Test
    void updateUserStatus_adminTargetBySuperAdmin_shouldSucceed() {
        when(userMapper.selectById(5L)).thenReturn(userWithRole(5L, 2));
        when(userMapper.selectById(OPERATOR_ID)).thenReturn(userWithRole(OPERATOR_ID, 3));
        when(authSessionRevocationService.updateStatusAndRevoke(5L, User.STATUS_DISABLED)).thenReturn(true);

        assertThat(service.updateUserStatus(5L, User.STATUS_DISABLED).isSuccess()).isTrue();
        verify(authSessionRevocationService).updateStatusAndRevoke(5L, User.STATUS_DISABLED);
    }

    // ==================== 用户软删除 ====================

    @Test
    void deleteUser_shouldSoftDeleteAndRevokeSessions() {
        when(userMapper.selectById(7L)).thenReturn(userWithRole(7L, 1));
        when(authSessionRevocationService.updateStatusAndRevoke(7L, User.STATUS_DELETED)).thenReturn(true);

        service.deleteUser(7L);

        verify(authSessionRevocationService).updateStatusAndRevoke(7L, User.STATUS_DELETED);
        verify(followCountService).detachUserRelations(7L);
        verify(userFollowMapper).delete(any());
        // 不得物理删除用户：文章、评论等外键会因此报错
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteUser_alreadyDeleted_shouldBeRejected() {
        when(userMapper.selectById(7L)).thenReturn(userWithStatus(7L, User.STATUS_DELETED));

        assertThatThrownBy(() -> service.deleteUser(7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.BAD_REQUEST.getCode()));
        verifyNoInteractions(authSessionRevocationService, followCountService, userFollowMapper);
    }

    @Test
    void deleteUser_superAdminTarget_shouldBeForbidden() {
        when(userMapper.selectById(5L)).thenReturn(userWithRole(5L, 3));

        assertThatThrownBy(() -> service.deleteUser(5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.FORBIDDEN.getCode()));
        verifyNoInteractions(authSessionRevocationService, followCountService, userFollowMapper);
    }

    @Test
    void deleteUser_adminTargetByAdmin_shouldBeForbidden() {
        when(userMapper.selectById(5L)).thenReturn(userWithRole(5L, 2));
        when(userMapper.selectById(OPERATOR_ID)).thenReturn(userWithRole(OPERATOR_ID, 2));

        assertThatThrownBy(() -> service.deleteUser(5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.FORBIDDEN.getCode()));
        verifyNoInteractions(authSessionRevocationService);
    }

    @Test
    void deleteUser_adminTargetBySuperAdmin_shouldSucceed() {
        when(userMapper.selectById(5L)).thenReturn(userWithRole(5L, 2));
        when(userMapper.selectById(OPERATOR_ID)).thenReturn(userWithRole(OPERATOR_ID, 3));
        when(authSessionRevocationService.updateStatusAndRevoke(5L, User.STATUS_DELETED)).thenReturn(true);

        assertThat(service.deleteUser(5L).isSuccess()).isTrue();
        verify(authSessionRevocationService).updateStatusAndRevoke(5L, User.STATUS_DELETED);
    }

    // ==================== 用户列表角色映射 ====================

    @Test
    void getUserList_shouldMapRoleToAdminOrUser() {
        // 与 AdminServiceImplTest 相同：提前初始化 Lambda 列缓存，避免排序条件解析失败
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "test"), User.class);
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(userWithRole(1L, 1), userWithRole(2L, 2), userWithRole(3L, 3)));
        page.setTotal(3L);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        var result = service.getUserList(1, 10, null, null);

        assertThat(result.getData().getItems())
                .extracting(UserDTO::getRole)
                .containsExactly("user", "admin", "admin");
    }

    @Test
    void getUserList_shouldExposeRoleLevelAndCanManageForOperatorAdmin() {
        // 操作者是普通管理员（role 2）：可以管理普通用户，不能管理管理员、超管和自己
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "test"), User.class);
        when(userMapper.selectById(OPERATOR_ID)).thenReturn(userWithRole(OPERATOR_ID, 2));
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(userWithRole(2L, 1), userWithRole(3L, 2), userWithRole(4L, 3), userWithRole(OPERATOR_ID, 2)));
        page.setTotal(4L);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        var items = service.getUserList(1, 10, null, null).getData().getItems();

        assertThat(items).extracting(UserDTO::getRoleLevel).containsExactly(1, 2, 3, 2);
        assertThat(items).extracting(UserDTO::getCanManage).containsExactly(true, false, false, false);
    }

    @Test
    void getUserList_superAdminOperatorCanManageAdminsButNotSuperAdmins() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "test"), User.class);
        when(userMapper.selectById(OPERATOR_ID)).thenReturn(userWithRole(OPERATOR_ID, 3));
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(userWithRole(2L, 2), userWithRole(3L, 3)));
        page.setTotal(2L);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        var items = service.getUserList(1, 10, null, null).getData().getItems();

        assertThat(items).extracting(UserDTO::getCanManage).containsExactly(true, false);
    }

    @Test
    void getUserList_deletedUserCannotBeManaged() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "test"), User.class);
        when(userMapper.selectById(OPERATOR_ID)).thenReturn(userWithRole(OPERATOR_ID, 3));
        User deleted = userWithRole(5L, 1);
        deleted.setStatus(User.STATUS_DELETED);
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(deleted));
        page.setTotal(1L);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        var items = service.getUserList(1, 10, null, null).getData().getItems();

        assertThat(items).extracting(UserDTO::getCanManage).containsExactly(false);
    }

    // ==================== 文章状态 ====================

    @Test
    void updateArticleStatus_mustNotPublishWithoutModerationDecision() {
        doThrow(new BusinessException("文章发布必须通过审核决定"))
                .when(articleStatusTransition).changeStatusByAdmin(9L, Article.STATUS_PUBLISHED);

        assertThatThrownBy(() -> service.updateArticleStatus(9L, Article.STATUS_PUBLISHED))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章发布必须通过审核决定");
        verifyNoInteractions(articleMapper);
    }

    // ==================== helpers ====================

    private static User userWithRole(long id, Integer role) {
        User user = new User();
        user.setId(id);
        user.setStatus(User.STATUS_ACTIVE);
        user.setRole(role);
        return user;
    }

    private static User userWithStatus(long id, Integer status) {
        User user = userWithRole(id, 1);
        user.setStatus(status);
        return user;
    }
}
