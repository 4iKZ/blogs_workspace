#!/usr/bin/env bash
# Offline database recovery tool. Run only while the application is in maintenance mode.
set -Eeuo pipefail

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command is not available: $1"
}

checksum_file() {
  local target="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$target" > "${target}.sha256"
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$target" > "${target}.sha256"
  else
    die "Required SHA-256 command is not available (sha256sum or shasum)"
  fi
}

verify_database() {
  local database="$1"
  local table
  local table_count
  local foreign_key_checks

  for table in users articles comments; do
    if ! table_count="$(mysql --database="$database" --batch --skip-column-names \
      -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '$table';")"; then
      die "Could not verify core table after restore: $table"
    fi
    [[ "$table_count" == "1" ]] || die "Core table is missing after restore: $table"
    if ! mysql --database="$database" --batch --skip-column-names \
      -e "SELECT COUNT(*) FROM \`$table\`;" >/dev/null; then
      die "Core table cannot be queried after restore: $table"
    fi
  done

  if ! foreign_key_checks="$(mysql --database="$database" --batch --skip-column-names \
    -e 'SET FOREIGN_KEY_CHECKS = 1; SELECT @@FOREIGN_KEY_CHECKS;')"; then
    die "Could not restore or verify FOREIGN_KEY_CHECKS"
  fi
  [[ "$foreign_key_checks" == "1" ]] || die "FOREIGN_KEY_CHECKS could not be restored"
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

backup_directory="$(cd "$(dirname "$backup_file")" && pwd -P)"
safety_backup="$backup_directory/safety-${database}-$(date +%Y%m%d_%H%M%S).sql"

printf 'Creating safety backup: %s\n' "$safety_backup"
if ! mysqldump --databases "$database" --single-transaction --routines --events > "$safety_backup"; then
  die "Safety backup failed. Restore was not started. Partial safety backup: $safety_backup"
fi
checksum_file "$safety_backup"
printf 'Safety backup checksum: %s.sha256\n' "$safety_backup"

printf 'Restoring %s into database %s...\n' "$backup_file" "$database"
if ! mysql --database="$database" < "$backup_file"; then
  die "Restore failed. The safety backup remains at: $safety_backup"
fi

if ! verify_database "$database"; then
  die "Post-restore verification failed. The safety backup remains at: $safety_backup"
fi

printf 'Restore completed and verified. Safety backup retained at: %s\n' "$safety_backup"
