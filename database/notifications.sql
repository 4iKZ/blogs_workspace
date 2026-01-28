-- ============================================================================
-- Migration: Add Foreign Key Validation for user_likes table
-- Date: 2025-01-22
-- Description: Adds trigger to validate target_id references exist for target_type
--
-- Related Issue: P2-3 - user_likes table lacks foreign key to articles
--
-- Note: Due to the polymorphic association pattern (target_id + target_type),
-- a standard foreign key cannot be used. Instead, we use triggers to validate
-- the reference integrity.
-- ============================================================================

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS `user_likes_before_insert`;

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS `user_likes_before_update`;

-- Create BEFORE INSERT trigger to validate target exists
DELIMITER ;;
CREATE TRIGGER `user_likes_before_insert` BEFORE INSERT ON `user_likes`
FOR EACH ROW
BEGIN
    DECLARE article_exists INT;
    DECLARE comment_exists INT;

    IF NEW.target_type = 1 THEN
        -- Validate article exists and is published
        SELECT COUNT(*) INTO article_exists
        FROM `articles`
        WHERE `id` = NEW.target_id AND `status` = 2;

        IF article_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot like article: article does not exist or is not published';
        END IF;
    ELSEIF NEW.target_type = 2 THEN
        -- Validate comment exists
        SELECT COUNT(*) INTO comment_exists
        FROM `comments`
        WHERE `id` = NEW.target_id;

        IF comment_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot like comment: comment does not exist';
        END IF;
    END IF;
END;;
DELIMITER ;

-- Create BEFORE UPDATE trigger to validate target exists
DELIMITER ;;
CREATE TRIGGER `user_likes_before_update` BEFORE UPDATE ON `user_likes`
FOR EACH ROW
BEGIN
    DECLARE article_exists INT;
    DECLARE comment_exists INT;

    IF NEW.target_type = 1 THEN
        -- Validate article exists and is published
        SELECT COUNT(*) INTO article_exists
        FROM `articles`
        WHERE `id` = NEW.target_id AND `status` = 2;

        IF article_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot update like: article does not exist or is not published';
        END IF;
    ELSEIF NEW.target_type = 2 THEN
        -- Validate comment exists
        SELECT COUNT(*) INTO comment_exists
        FROM `comments`
        WHERE `id` = NEW.target_id;

        IF comment_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot update like: comment does not exist';
        END IF;
    END IF;
END;;
DELIMITER ;

-- Note: The articles_after_delete trigger already handles cleanup when articles are deleted
-- The comments table has ON DELETE CASCADE, so comment deletion will handle cleanup
