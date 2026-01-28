DELIMITER $$
DROP TRIGGER IF EXISTS articles_after_delete $$
CREATE TRIGGER articles_after_delete
AFTER DELETE ON articles
FOR EACH ROW
BEGIN
  DELETE FROM user_likes WHERE target_type = 1 AND target_id = OLD.id;
END $$
DELIMITER ;
