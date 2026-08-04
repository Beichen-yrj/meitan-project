package com.meitan.controller;

import com.meitan.dto.ApiResponse;
import com.meitan.entity.DataFile;
import com.meitan.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** 上传到当前登录用户的专属目录。 */
    @PostMapping("/upload")
    public ApiResponse<DataFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("moduleType") String moduleType,
            @RequestAttribute Long userId) {
        try {
            return ApiResponse.ok(fileService.uploadFile(file, userId, moduleType));
        } catch (IllegalArgumentException exception) {
            return ApiResponse.error(400, exception.getMessage());
        } catch (IOException exception) {
            return ApiResponse.error("文件上传失败: " + exception.getMessage());
        }
    }

    /** 只返回当前登录用户自己的文件。 */
    @GetMapping
    public ApiResponse<List<DataFile>> list(@RequestAttribute Long userId) {
        return ApiResponse.ok(fileService.listFiles(userId));
    }

    /** 只能删除当前登录用户自己的文件。 */
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable Long fileId,
                                    @RequestAttribute Long userId) {
        try {
            fileService.deleteFile(fileId, userId);
            return ApiResponse.ok();
        } catch (RuntimeException exception) {
            return ApiResponse.error(404, exception.getMessage());
        } catch (IOException exception) {
            return ApiResponse.error("文件删除失败: " + exception.getMessage());
        }
    }
}
