package com.depository_manage.controller.clck;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/script")
public class ScriptController {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PostMapping("/run-aa")
    public ResponseEntity<List<String>> runScript() {
        try {
            // 立即返回任务接受确认
            executorService.submit(this::executeBatchFile);
            return new ResponseEntity<>(HttpStatus.OK);
            // 返回信息提示任务正在处理中
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void executeBatchFile() {
        try {
            // 指定批处理文件的绝对路径
            String batchFilePath = "D:\\clck\\看板更新.bat";

            // 同步运行批处理文件，确保图片生成
            ProcessBuilder processBuilder = new ProcessBuilder(batchFilePath);
            processBuilder.environment().put("PYTHONUNBUFFERED", "1");
            Process process = processBuilder.start();
            process.waitFor(); // 等待批处理文件执行完毕

            if (process.exitValue() == 0) {
                System.out.println("Batch file executed successfully.");
            } else {
                System.err.println("Failed to execute batch file, exit code: " + process.exitValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to execute batch file: " + e.getMessage());
        }
    }

    @PostMapping("/get-images")
    public ResponseEntity<List<String>> getImages() {
        try {
            // 动态生成图片路径
            String userHomeDir = System.getProperty("user.home");
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String imageDir = userHomeDir + "\\Desktop\\看板图\\" + currentDate;
            File dir = new File(imageDir);

            if (!dir.exists() || !dir.isDirectory()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // 读取现有图片
            List<String> images = new ArrayList<>();
            for (File file : dir.listFiles()) {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                images.add(base64Image);
            }

            return ResponseEntity.ok(images);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
