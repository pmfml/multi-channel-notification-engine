-- Index on status to optimize NotificationLogRepository.findByStatus() and manual queries
CREATE INDEX IF NOT EXISTS idx_notification_log_status ON notification_log(status);

-- Index on created_at to optimize reverse-chronological sorting (ORDER BY created_at DESC)
CREATE INDEX IF NOT EXISTS idx_notification_log_created_at ON notification_log(created_at DESC);
