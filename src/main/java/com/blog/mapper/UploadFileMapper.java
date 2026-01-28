package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.UploadFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件上传记录Mapper接口
 */
@Mapper
public interface UploadFileMapper extends BaseMapper<UploadFile> {

    /**
     * 根据MD5值查询文件
     * @param md5Hash 文件MD5值
     * @return 文件记录
     */
    @Select("SELECT * FROM upload_files WHERE md5_hash = #{md5Hash} AND deleted = 0")
    UploadFile selectByMd5Hash(@Param("md5Hash") String md5Hash);

    /**
     * 查询用户的上传文件
     * @param uploadUserId 上传用户ID
     * @param status 文件状态（可选）
     * @return 文件列表
     */
    @Select("<script>" +
            "SELECT * FROM upload_files " +
            "WHERE upload_user_id = #{uploadUserId} AND deleted = 0 " +
            "<if test='status != null'>AND status = #{status}</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<UploadFile> selectFilesByUserId(@Param("uploadUserId") Long uploadUserId, @Param("status") Integer status);

    /**
     * 查询文件类型统计
     * @param uploadUserId 上传用户ID（可选）
     * @return 文件类型统计列表
     */
    @Select("<script>" +
            "SELECT file_type, COUNT(1) as file_count, SUM(file_size) as total_size " +
            "FROM upload_files " +
            "WHERE deleted = 0 " +
            "<if test='uploadUserId != null'>AND upload_user_id = #{uploadUserId}</if>" +
            "GROUP BY file_type " +
            "ORDER BY file_count DESC" +
            "</script>")
    List<FileTypeStatistics> selectFileTypeStatistics(@Param("uploadUserId") Long uploadUserId);

    /**
     * 统计用户上传文件数量
     * @param uploadUserId 上传用户ID
     * @return 文件数量
     */
    @Select("SELECT COUNT(1) FROM upload_files WHERE upload_user_id = #{uploadUserId} AND deleted = 0")
    int countUserFiles(@Param("uploadUserId") Long uploadUserId);

    /**
     * 统计用户上传文件总大小
     * @param uploadUserId 上传用户ID
     * @return 文件总大小
     */
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM upload_files WHERE upload_user_id = #{uploadUserId} AND deleted = 0")
    long sumUserFileSize(@Param("uploadUserId") Long uploadUserId);

    /**
     * 文件类型统计内部类
     */
    class FileTypeStatistics {
        private String fileType;
        private Integer fileCount;
        private Long totalSize;

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public Integer getFileCount() {
            return fileCount;
        }

        public void setFileCount(Integer fileCount) {
            this.fileCount = fileCount;
        }

        public Long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(Long totalSize) {
            this.totalSize = totalSize;
        }
    }
}