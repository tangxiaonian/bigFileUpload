package com.example.demo.controller;

import com.example.demo.domain.MultipartFileParam;
import com.example.demo.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private FileService fileServiceImpl;

    @GetMapping("/index")
    public String test() {
        return "test";
    }

    @GetMapping("/upload1")
    public void upload(String id,HttpServletResponse response)throws Exception {
//        InputStream inputStream = fileServiceImpl.getUploadFile(id);
        InputStream inputStream = new FileInputStream("F:\\upload\\51b863ea0e3cc9c905f23ddd13d296af\\51b863ea0e3cc9c905f23ddd13d296af.mp4");
        ServletOutputStream outputStream = response.getOutputStream();
        byte[] readByte = new byte[1024];
        int len;
        while((len = inputStream.read(readByte)) > 0){
            outputStream.write(readByte, 0, len);
        }
        outputStream.close();
        inputStream.close();
    }

    @PostMapping("/upload")
    public Map<String, String> upload(MultipartFileParam multipartFileParam) {
        // 上传分片
        boolean fileUpload = fileServiceImpl.fileUpload(multipartFileParam);

        Map<String, String> map = new HashMap<>();

        int chunk = multipartFileParam.getChunk();

        String result = "分片" + chunk + "创建";

        map.put("message", fileUpload ? result + "success!" : result + "fail!");

        return map;
    }

    @GetMapping("/getUpload")
    public byte[] getUploadFile(String id) throws Exception {
        InputStream inputStream = fileServiceImpl.getUploadFile(id);
        // 将输入流 写出到 输出流 里面
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] readByte = new byte[1024];
        int len = 0;
        while((len = inputStream.read(readByte)) > 0){
            byteArrayOutputStream.write(readByte, 0, len);
        }
        byte[] bytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        inputStream.close();
        System.out.println(bytes.length);
        return bytes;
    }
}
