-- 2026-10-04 P2 回填 file_info.file_type：按 mime_type 归类为 image/video/document/other，与 FileUploadServiceImpl.resolveFileType 同口径。
UPDATE `file_info` SET `file_type` = CASE
  WHEN `mime_type` LIKE 'image/%' THEN 'image'
  WHEN `mime_type` LIKE 'video/%' THEN 'video'
  WHEN `mime_type` LIKE 'text/%'
    OR `mime_type` LIKE '%pdf%'
    OR `mime_type` LIKE '%word%'
    OR `mime_type` LIKE '%document%'
    OR `mime_type` LIKE '%excel%'
    OR `mime_type` LIKE '%sheet%'
    OR `mime_type` LIKE '%powerpoint%'
    OR `mime_type` LIKE '%presentation%' THEN 'document'
  ELSE 'other'
END
WHERE `file_type` IS NULL;