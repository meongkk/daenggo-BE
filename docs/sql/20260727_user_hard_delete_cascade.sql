-- 회원 한 행을 삭제하면 회원 소유 데이터가 함께 삭제되도록 외래 키를 변경한다.
-- 이미지 DB 행은 삭제되지만 uploads 폴더의 실제 이미지 파일은 삭제하지 않는다.
-- MySQL Workbench 또는 mysql CLI에서 대상 DB를 선택한 뒤 한 번 실행한다.

DROP PROCEDURE IF EXISTS make_fk_delete_cascade;

DELIMITER $$

CREATE PROCEDURE make_fk_delete_cascade(
    IN child_table_name VARCHAR(64),
    IN child_column_name VARCHAR(64),
    IN parent_table_name VARCHAR(64),
    IN parent_column_name VARCHAR(64),
    IN cascade_constraint_name VARCHAR(64)
)
BEGIN
    DECLARE existing_constraint_name VARCHAR(64) DEFAULT NULL;
    DECLARE child_table_exists INT DEFAULT 0;
    DECLARE parent_table_exists INT DEFAULT 0;

    SELECT COUNT(*)
      INTO child_table_exists
      FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = child_table_name;

    SELECT COUNT(*)
      INTO parent_table_exists
      FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = parent_table_name;

    IF child_table_exists > 0 AND parent_table_exists > 0 THEN
        SELECT MAX(CONSTRAINT_NAME)
          INTO existing_constraint_name
          FROM information_schema.KEY_COLUMN_USAGE
         WHERE CONSTRAINT_SCHEMA = DATABASE()
           AND TABLE_NAME = child_table_name
           AND COLUMN_NAME = child_column_name
           AND REFERENCED_TABLE_NAME = parent_table_name
           AND REFERENCED_COLUMN_NAME = parent_column_name;

        IF existing_constraint_name IS NOT NULL THEN
            SET @drop_foreign_key_sql = CONCAT(
                    'ALTER TABLE `', child_table_name,
                    '` DROP FOREIGN KEY `', existing_constraint_name, '`'
            );
            PREPARE drop_foreign_key_statement FROM @drop_foreign_key_sql;
            EXECUTE drop_foreign_key_statement;
            DEALLOCATE PREPARE drop_foreign_key_statement;
        END IF;

        SET @add_foreign_key_sql = CONCAT(
                'ALTER TABLE `', child_table_name,
                '` ADD CONSTRAINT `', cascade_constraint_name,
                '` FOREIGN KEY (`', child_column_name,
                '`) REFERENCES `', parent_table_name,
                '` (`', parent_column_name, '`) ON DELETE CASCADE'
        );
        PREPARE add_foreign_key_statement FROM @add_foreign_key_sql;
        EXECUTE add_foreign_key_statement;
        DEALLOCATE PREPARE add_foreign_key_statement;
    END IF;
END$$

DELIMITER ;

-- users를 직접 참조하는 테이블
CALL make_fk_delete_cascade('refresh_tokens', 'user_id', 'users', 'user_id', 'fk_refresh_tokens_user_cascade');
CALL make_fk_delete_cascade('user_refresh_tokens', 'user_id', 'users', 'user_id', 'fk_user_refresh_tokens_user_cascade');
CALL make_fk_delete_cascade('favorites', 'user_id', 'users', 'user_id', 'fk_favorites_user_cascade');
CALL make_fk_delete_cascade('place_report', 'user_id', 'users', 'user_id', 'fk_place_report_user_cascade');
CALL make_fk_delete_cascade('board', 'user_id', 'users', 'user_id', 'fk_board_user_cascade');
CALL make_fk_delete_cascade('comment', 'user_id', 'users', 'user_id', 'fk_comment_user_cascade');
CALL make_fk_delete_cascade('group_members', 'user_id', 'users', 'user_id', 'fk_group_members_user_cascade');
CALL make_fk_delete_cascade('groups', 'owner_user_id', 'users', 'user_id', 'fk_groups_owner_cascade');
CALL make_fk_delete_cascade('pets', 'user_id', 'users', 'user_id', 'fk_pets_user_cascade');
CALL make_fk_delete_cascade('walk_records', 'user_id', 'users', 'user_id', 'fk_walk_records_user_cascade');

-- 회원의 게시글이 삭제될 때 함께 삭제할 데이터
CALL make_fk_delete_cascade('board_image', 'post_id', 'board', 'post_id', 'fk_board_image_board_cascade');
CALL make_fk_delete_cascade('comment', 'post_id', 'board', 'post_id', 'fk_comment_board_cascade');
CALL make_fk_delete_cascade('market_board', 'post_id', 'board', 'post_id', 'fk_market_board_board_cascade');
CALL make_fk_delete_cascade('sitter_board', 'post_id', 'board', 'post_id', 'fk_sitter_board_board_cascade');

-- 회원이 만든 그룹이 삭제될 때 모든 가입 관계 삭제
CALL make_fk_delete_cascade('group_members', 'group_id', 'groups', 'group_id', 'fk_group_members_group_cascade');

-- 회원의 산책 및 반려견 데이터가 삭제될 때 함께 삭제할 데이터
CALL make_fk_delete_cascade('walk_photos', 'walk_record_id', 'walk_records', 'walk_record_id', 'fk_walk_photos_record_cascade');
CALL make_fk_delete_cascade('walk_route_records', 'walk_record_id', 'walk_records', 'walk_record_id', 'fk_walk_routes_record_cascade');
CALL make_fk_delete_cascade('walk_record_pets', 'walk_record_id', 'walk_records', 'walk_record_id', 'fk_walk_pets_record_cascade');
CALL make_fk_delete_cascade('walk_record_pets', 'pet_id', 'pets', 'pet_id', 'fk_walk_pets_pet_cascade');

DROP PROCEDURE make_fk_delete_cascade;
