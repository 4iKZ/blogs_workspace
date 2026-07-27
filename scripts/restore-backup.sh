#!/usr/bin/env bash
# Offline database recovery tool. Run only while the application is in maintenance mode.
set -Eeuo pipefail
umask 077

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command is not available: $1"
}

private_mode() {
  local target="$1"
  local mode
  mode="$(stat -c '%a' "$target" 2>/dev/null || stat -f '%Lp' "$target")" || die "Could not read permissions: $target"
  [[ "$mode" == "$2" ]] || die "Unsafe permissions on $target (expected $2, found $mode)"
}

create_safety_directory() {
  local requested parent base
  requested="${BLOG_RESTORE_SAFETY_DIR:-$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)/.restore-safety}"
  parent="$(CDPATH= cd -- "$(dirname -- "$requested")" && pwd -P)" || die "Safety directory parent does not exist: $requested"
  base="$(basename -- "$requested")"
  [[ -n "$base" && "$base" != "/" && "$base" != "." && "$base" != ".." ]] || \
    die "Safety directory must be a named child directory: $requested"
  safety_root="$parent/$base"

  [[ ! -L "$safety_root" ]] || die "Safety directory must not be a symbolic link: $safety_root"
  if [[ -e "$safety_root" ]]; then
    [[ -d "$safety_root" ]] || die "Safety directory is not a directory: $safety_root"
  else
    mkdir -m 700 -- "$safety_root" || die "Could not create safety directory: $safety_root"
  fi
  [[ ! -L "$safety_root" && -O "$safety_root" ]] || die "Safety directory must be owned by the current user and not be a symbolic link"
  chmod 700 -- "$safety_root" || die "Could not restrict safety directory permissions"
  private_mode "$safety_root" 700
}

create_private_file() {
  local suffix="$1"
  local file
  file="$(mktemp "$safety_root/safety-${database}-XXXXXXXX.$suffix")" || die "Could not create private safety file"
  [[ ! -L "$file" ]] || die "Refusing symbolic link safety file: $file"
  chmod 600 -- "$file" || die "Could not restrict safety file permissions"
  private_mode "$file" 600
  printf '%s' "$file"
}

checksum_file() {
  local target="$1"
  checksum_file="$(create_private_file sha256)"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$target" > "$checksum_file"
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$target" > "$checksum_file"
  else
    die "Required SHA-256 command is not available (sha256sum or shasum)"
  fi
  private_mode "$checksum_file" 600
}

verify_database() {
  local table table_count orphan_count
  for table in users articles comments; do
    if ! table_count="$(mysql --database="$database" --batch --skip-column-names \
      -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '$table';")"; then
      die "Could not verify core table after restore: $table"
    fi
    [[ "$table_count" == "1" ]] || die "Core table is missing after restore: $table"
  done

  if ! orphan_count="$(mysql --database="$database" --batch --skip-column-names -e "
      SELECT
        (SELECT COUNT(*) FROM articles a LEFT JOIN users u ON a.author_id = u.id WHERE u.id IS NULL) +
        (SELECT COUNT(*) FROM articles a LEFT JOIN categories c ON a.category_id = c.id WHERE c.id IS NULL) +
        (SELECT COUNT(*) FROM comments c LEFT JOIN articles a ON c.article_id = a.id WHERE a.id IS NULL) +
        (SELECT COUNT(*) FROM comments c LEFT JOIN users u ON c.user_id = u.id WHERE u.id IS NULL);")"; then
    die "Could not run foreign-key integrity checks"
  fi
  [[ "$orphan_count" == "0" ]] || die "Foreign-key integrity check found $orphan_count orphaned rows"
}

[[ "$#" -eq 1 ]] || die "Usage: $0 /absolute/path/to/backup.sql"
backup_file="$1"
[[ -f "$backup_file" ]] || die "Backup file does not exist or is not a regular file: $backup_file"

require_command mysql
require_command mysqldump

read -r -p 'Database name: ' database
read -r -p 'Enter the same database name again: ' database_confirmation
[[ "$database" == "$database_confirmation" ]] || die "Database names do not match"
[[ "$database" =~ ^[A-Za-z0-9_]{1,64}$ ]] || die "Database name may only contain letters, numbers, and underscores"

read -r -p 'Confirm the application is in maintenance mode by typing MAINTENANCE: ' maintenance_confirmation
[[ "$maintenance_confirmation" == "MAINTENANCE" ]] || die "Restore cancelled because maintenance mode was not confirmed"

create_safety_directory
safety_backup="$(create_private_file sql)"
printf 'Creating safety backup: %s\n' "$safety_backup"
if ! mysqldump --databases "$database" --single-transaction --routines --events > "$safety_backup"; then
  die "Safety backup failed. Restore was not started. Partial safety backup: $safety_backup"
fi
checksum_file "$safety_backup"
printf 'Safety backup checksum: %s\n' "$checksum_file"

printf 'Restoring %s into database %s...\n' "$backup_file" "$database"
if ! { cat -- "$backup_file"; printf '\nSET FOREIGN_KEY_CHECKS=1;\n'; } | mysql --database="$database"; then
  die "Restore failed. The safety backup remains at: $safety_backup"
fi

verify_database
printf 'Restore completed and verified. Safety backup retained at: %s\n' "$safety_backup"
