package com.example.springbootbigfile;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


@RestController
@RequestMapping(value = "upload")
public class UploadController {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    @Value("${uploadFolder}")
    private String filePath;


    private static final String SEPARATOR = File.separator;

    /**
     * 上传文件
     *
     * @param request
     * @param guid
     * @param chunk
     * @param file
     */
    @PostMapping(value = "/upload")
    public JsonResult bigFile(HttpServletRequest request, @RequestParam String guid, @RequestParam Integer chunk, @RequestBody MultipartFile file) {
        try {
            // 临时目录用来存放所有分片文件
            String tempFileDir = filePath + SEPARATOR + guid;
            File parentFileDir = new File(tempFileDir);
            if (!parentFileDir.exists()) {
                parentFileDir.mkdirs();
            }
            // 分片处理时，前台会多次调用上传接口，每次都会上传文件的一部分到后台
            File tempPartFile = new File(parentFileDir, guid + "_" + chunk + ".part");
            FileUtils.copyInputStreamToFile(file.getInputStream(), tempPartFile);

            return JsonResult.success();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return JsonResult.failMessage(e.getMessage());
        }
    }

    /**
     * 合并文件
     * @param guid
     * @param fileName
     * @throws Exception
     */
    @PostMapping(value = "/merge")
    public JsonResult mergeFile(@RequestParam String guid, @RequestParam String fileName) {
        // 得到 destTempFile 就是最终的文件
        try {
            File parentFileDir = new File(filePath + SEPARATOR + guid);
            if (parentFileDir.isDirectory()) {
                File destTempFile = new File(filePath + SEPARATOR + "merge", fileName);
                if (!destTempFile.exists()) {
                    //先得到文件的上级目录，并创建上级目录，在创建文件,
                    destTempFile.getParentFile().mkdir();
                    try {
                        //创建文件
                        destTempFile.createNewFile(); //上级目录没有创建，这里会报错
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(parentFileDir.listFiles().length);
                for (int i = 0; i < parentFileDir.listFiles().length; i++) {
                    File partFile = new File(parentFileDir, guid + "_" + i + ".part");
                    FileOutputStream destTempfos = new FileOutputStream(destTempFile, true);
                    //遍历"所有分片文件"到"最终文件"中
                    FileUtils.copyFile(partFile, destTempfos);
                    destTempfos.close();
                }
                // 删除临时目录中的分片文件
                FileUtils.deleteDirectory(parentFileDir);
                return JsonResult.success();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResult.fail();
        }
        return null;
    }

}
