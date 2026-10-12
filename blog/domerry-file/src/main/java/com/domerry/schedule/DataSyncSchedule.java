package com.domerry.schedule;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * 本系统为blog桶，刷新的music文件在myfile桶，属于不同的两个项目，如何刷新？
 */

 @Slf4j
//@Component
public class DataSyncSchedule {
    private static final String MINIO_BUCKET_NAME = "blog";
    private static final long MINIO_URL_EXPIRATION_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private static final String QUERY_SONG_SQL = "\n" +
            "            SELECT id, song_url, lrc, pic\n" +
            "            FROM fs_song\n" +
            "            WHERE status = 1\n" +
            "            ";

    private static final String UPDATE_SONG_SQL = "\n" +
            "            UPDATE fs_song\n" +
            "            SET song_url = ?, lrc = ?, pic = ?, update_time = NOW()\n" +
            "            WHERE id = ?\n" +
            "            ";

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private FileStorageService fileStorageService;

    /**
     * 每周一、周四凌晨一点刷新歌曲相关 MinIO 预签名 URL。
     */
    @DS("slave")
    @Scheduled(cron = "0 0/1 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void generateMinioUrl() {
        List<SongUrlRecord> songs = jdbcTemplate.query(QUERY_SONG_SQL, new SongUrlRowMapper());
        List<Object[]> batchArgs = new ArrayList<>();
        log.info("开始刷新 MinIO 歌曲 URL，查询到 {} 条记录", songs.size());

        for (SongUrlRecord song : songs) {
            String songUrl = regenerateMinioUrl(song.getSongUrl());
            String lrcUrl = regenerateMinioUrl(song.getLrc());
            String picUrl = regenerateMinioUrl(song.getPic());

            batchArgs.add(new Object[]{songUrl, lrcUrl, picUrl, song.getId()});
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(UPDATE_SONG_SQL, batchArgs);
        }

        log.info("MinIO 歌曲 URL 刷新完成，共查询 {} 条，更新 {} 条记录", songs.size(), batchArgs.size());
    }

    private String refreshMinioUrl(String originalUrl) {
        return refreshMinioUrl(originalUrl,
                new Date(System.currentTimeMillis() + MINIO_URL_EXPIRATION_MILLIS));
    }

    private String refreshMinioUrl(String originalUrl, Date expiration) {
        if (!StringUtils.hasText(originalUrl)) {
            return originalUrl;
        }

        try {
            String objectName = extractObjectName(originalUrl);
            if (!StringUtils.hasText(objectName)) {
                return originalUrl;
            }

            int lastSlashIndex = objectName.lastIndexOf('/');
            FileInfo fileInfo = new FileInfo();
            fileInfo.setPath(lastSlashIndex < 0
                    ? ""
                    : objectName.substring(0, lastSlashIndex + 1));
            fileInfo.setFilename(lastSlashIndex < 0
                    ? objectName
                    : objectName.substring(lastSlashIndex + 1));

            String presignedUrl = fileStorageService.generatePresignedUrl(fileInfo, expiration);
            return StringUtils.hasText(presignedUrl) ? presignedUrl : originalUrl;
        } catch (IllegalArgumentException exception) {
            log.warn("无法解析 MinIO 文件 URL，跳过刷新：{}", originalUrl, exception);
            return originalUrl;
        }
    }

    private String extractObjectName(String originalUrl) {
        String urlPath = URI.create(originalUrl).getPath();
        String bucketPrefix = "/" + MINIO_BUCKET_NAME + "/";

        if (!StringUtils.hasText(urlPath) || !urlPath.startsWith(bucketPrefix)) {
            return null;
        }

        return urlPath.substring(bucketPrefix.length());
    }

    private String regenerateMinioUrl(String originalUrl) {
        if (!StringUtils.hasText(originalUrl)) {
            return originalUrl;
        }

        try {
            String objectName = extractObjectName(originalUrl);
            if (!StringUtils.hasText(objectName)) {
                log.warn("跳过无法识别的 MinIO 文件地址：{}", originalUrl);
                return originalUrl;
            }

            return refreshMinioUrl(originalUrl);
        } catch (IllegalArgumentException exception) {
            log.warn("无法解析 MinIO 文件 URL，跳过刷新：{}", originalUrl, exception);
            return originalUrl;
        }
    }

    private static class SongUrlRecord {

        private final Integer id;
        private final String songUrl;
        private final String lrc;
        private final String pic;

        private SongUrlRecord(Integer id, String songUrl, String lrc, String pic) {
            this.id = id;
            this.songUrl = songUrl;
            this.lrc = lrc;
            this.pic = pic;
        }

        public Integer getId() {
            return id;
        }

        public String getSongUrl() {
            return songUrl;
        }

        public String getLrc() {
            return lrc;
        }

        public String getPic() {
            return pic;
        }
    }

    private static class SongUrlRowMapper implements RowMapper<SongUrlRecord> {

        @Override
        public SongUrlRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new SongUrlRecord(
                    resultSet.getInt("id"),
                    resultSet.getString("song_url"),
                    resultSet.getString("lrc"),
                    resultSet.getString("pic")
            );
        }
    }

}
