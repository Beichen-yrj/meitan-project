package com.meitan.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meitan.entity.DataFile;
import com.meitan.mapper.DataFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_MODULE_TYPES = Set.of("ANALYSIS", "STATISTICS", "DETECTION");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xls", "csv");

    private final DataFileMapper dataFileMapper;

    @Value("${file.upload.path}")
    private String uploadPath;

    public DataFile uploadFile(MultipartFile file, Long userId, String moduleType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择有效的数据文件");
        }
        String normalizedModule = moduleType == null ? "" : moduleType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_MODULE_TYPES.contains(normalizedModule)) {
            throw new IllegalArgumentException("文件所属模块不正确");
        }

        String originalName = file.getOriginalFilename() == null ? "data.xlsx" : file.getOriginalFilename();
        originalName = Paths.get(originalName).getFileName().toString();
        String extension = FileUtil.extName(originalName).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 XLSX、XLS 或 CSV 数据文件");
        }

        Path userDirectory = resolveUserDirectory(userId);
        Files.createDirectories(userDirectory);
        String storedName = UUID.randomUUID() + "." + extension;
        Path destination = userDirectory.resolve(storedName).normalize();
        if (!destination.startsWith(userDirectory)) {
            throw new IOException("文件存储路径不安全");
        }
        file.transferTo(destination);

        DataFile dataFile = new DataFile();
        dataFile.setUserId(userId);
        dataFile.setModuleType(normalizedModule);
        dataFile.setFileName(originalName);
        dataFile.setFilePath(destination.toAbsolutePath().toString());
        dataFile.setFileSize(file.getSize());
        dataFile.setUploadTime(LocalDateTime.now());
        dataFileMapper.insert(dataFile);
        return dataFile;
    }

    public List<DataFile> listFiles(Long userId) {
        return dataFileMapper.selectList(new LambdaQueryWrapper<DataFile>()
                .eq(DataFile::getUserId, userId)
                .orderByDesc(DataFile::getUploadTime));
    }

    public void deleteFile(Long fileId, Long userId) throws IOException {
        DataFile dataFile = getOwnedFile(fileId, userId);
        Path userDirectory = resolveUserDirectory(userId);
        Path storedFile = Paths.get(dataFile.getFilePath()).toAbsolutePath().normalize();
        if (!storedFile.startsWith(userDirectory)) {
            throw new IOException("文件路径不属于当前账户");
        }
        Files.deleteIfExists(storedFile);
        dataFileMapper.deleteById(dataFile.getId());
    }

    public String getFilePath(Long fileId, Long userId) {
        return getOwnedFile(fileId, userId).getFilePath();
    }

    private DataFile getOwnedFile(Long fileId, Long userId) {
        DataFile dataFile = dataFileMapper.selectOne(new LambdaQueryWrapper<DataFile>()
                .eq(DataFile::getId, fileId)
                .eq(DataFile::getUserId, userId));
        if (dataFile == null) {
            throw new RuntimeException("文件不存在或无权访问");
        }
        return dataFile;
    }

    private Path resolveUserDirectory(Long userId) throws IOException {
        Path baseDirectory = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path userDirectory = baseDirectory.resolve("user-" + userId).normalize();
        if (!userDirectory.startsWith(baseDirectory)) {
            throw new IOException("用户文件目录不安全");
        }
        return userDirectory;
    }
}
